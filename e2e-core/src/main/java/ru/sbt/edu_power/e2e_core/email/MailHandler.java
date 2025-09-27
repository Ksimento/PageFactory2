package ru.sbt.edu_power.e2e_core.email;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class MailHandler {
    private String IMAP_serverName = "imap.yandex.ru";
    private final int IMAP_port = 993;
    private final String login;
    private final String password;
    private Session session;
    private Email email;

    public MailHandler(
            final String login,
            final String password
    ) {
        this.login = DataProcessing.decodeValue(login);
        this.password = DataProcessing.decodeValue(password);
        createSession();
    }
    public MailHandler(
            final String login,
            final String password,
            final String server
    ) {
        this.login = DataProcessing.decodeValue(login);
        this.password = DataProcessing.decodeValue(password);
        IMAP_serverName = server;
        createSession();
    }

    //    Создание объекта сессии для подключения к почтовому ящику
    private void createSession() {
        if (null != session) {
            return;
        }
        final Properties properties = new Properties();
        properties.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        properties.setProperty("mail.debug", "false");
        session = Session.getDefaultInstance(properties);
    }

    public Email getEmail() {
        if (email == null) {
            getContent();
        }
        return email;
    }

    private void getContent() {
        try (final Store store = session.getStore("imap")) {
            store.connect(IMAP_serverName, IMAP_port, login, password);
            try (final Folder inbox = store.getFolder("INBOX")) {
                final AtomicReference<Message[]> messages = new AtomicReference<>();
                inbox.open(Folder.READ_ONLY);
                final BooleanSupplier waitWhenMailBeReceived = () -> {
                    try {
                        messages.set(inbox.getMessages());
                    } catch (final MessagingException e) {
                        throw new AutotestError("Ошибка при получении сообщений", e);
                    }
                    return messages.get().length > 0;
                };
                Timer.executeTimerThrowable(120, "Нет писем в почтовом ящике", waitWhenMailBeReceived);
                final Message message = messages.get()[0];
                email = new Email();
                email.setTopic(message.getSubject());
                email.setFrom(message.getFrom()[0].toString());
                email.setMessageBody(getText(((Multipart) message.getContent()).getBodyPart(0)));
                final Pattern pattern = Pattern.compile("[\r\n]+");
                email.setMessage(
                        pattern.matcher(
                                email.getMessageBody().replace(email.getTopic(), "")
                        ).replaceAll(" "));
                email.setActivationLink(getLink(email.getMessageBody()));
                email.setSmsCode(getSmsCode(email.getMessageBody()));
            } catch (final IOException e) {
                throw new AutotestError("Ошибка доступа к контенту письма", e);
            }
        } catch (final NoSuchProviderException e) {
            throw new AutotestError("Неверно указан провайдер", e);
        } catch (final MessagingException e) {
            throw new AutotestError("Ошибка при получении сообщений", e);
        }
    }

    public void deleteMessages() {
        try (final Store store = session.getStore("imap")) {
            store.connect(IMAP_serverName, IMAP_port, login, password);
            try (final Folder inbox = store.getFolder("INBOX")) {
                inbox.open(Folder.READ_WRITE);
                final int count = inbox.getMessageCount();
                if (count > 0) {
                    inbox.setFlags(1, count, new Flags(Flags.Flag.DELETED), true);
                }
                if (inbox.getMessageCount() > 0) {
                    log.info("Сообщения не удалены");
                    DriverUtils.freeze(DriverConstants.CONVERT_TO_MILLISECONDS);
                    inbox.expunge();
                    deleteMessages();
                }
            }
        } catch (final NoSuchProviderException e) {
            throw new AutotestError("Неверно указан провайдер", e);
        } catch (final MessagingException e) {
            throw new AutotestError("Ошибка при получении сообщений", e);
        }
    }

    public void checkMassageText(final String message, final boolean isPresent) {
        try (final Store store = session.getStore("imap")) {
            store.connect(IMAP_serverName, IMAP_port, login, password);
            try (final Folder inbox = store.getFolder("INBOX")) {
                inbox.open(Folder.READ_WRITE);
                boolean fag = false;
                for (Message text : inbox.getMessages()) {
                    if(message.equals(text.getSubject())){
                        fag=true;
                        break;
                    }
                }
                if(isPresent && !fag){
                    throw new AutotestError(String.format("Сообщение с текстом \"%s\" ДОЛЖНО присутствовать в почте", message));
                }
                if(!isPresent && fag){
                    throw new AutotestError(String.format("Сообщение с текстом \"%s\" НЕ должно присутствовать в почте", message));
                }
            }
        } catch (final NoSuchProviderException e) {
            throw new AutotestError("Неверно указан провайдер", e);
        } catch (final MessagingException e) {
            throw new AutotestError("Ошибка при получении сообщений", e);
        }
    }

    private String getText(final Part p) throws
                                         MessagingException, IOException {
        if (p.isMimeType("text/*")) {
            return (String) p.getContent();
        }
        if (p.isMimeType("multipart/*")) {
            final Multipart mp = (Multipart) p.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                final String s = getText(mp.getBodyPart(i));
                if (s != null) {
                    return s;
                }
            }
        }
        return "";
    }

    // получает ссылку активации из тела сообщения
    private String getLink(final String message) {
        if (message.contains("<a")) {
            return Jsoup.parse(message)
                        .selectFirst("a")
                        .attr("href");
        } else {
            final List<String> parts = Arrays.asList(message.replace("\r","").replace("\n"," ").split(" "));
            return parts.stream()
                        .filter(line -> line.startsWith("https:"))
                        .findFirst()
                        .orElse("");
        }
    }

    // получает sms код из 6 цифр из тела сообщения
    private String getSmsCode(final String message) {
        final Pattern pattern = Pattern.compile(".*?(\\d{6}).*");
        final Matcher matcher = pattern.matcher(message);
        return matcher.find() ? matcher.replaceAll("$1") : "";
    }
}
