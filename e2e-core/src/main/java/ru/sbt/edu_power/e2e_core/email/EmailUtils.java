package ru.sbt.edu_power.e2e_core.email;

import ru.sbtqa.tag.datajack.Stash;
import ru.sbtqa.tag.qautils.errors.AutotestError;

public class EmailUtils {
    public static String getImapEmailService(final String email) {
        if(email.equals("gmail")){
            return "imap." + email + ".com";
        }
        return "imap." + email + ".ru";
    }

    public static void saveStashData(final MailHandler mailHandler, final String attribute, final String key) {
        final String actualAttribute;
        switch (attribute) {
            case "ссылку":
                actualAttribute = mailHandler.getEmail().getActivationLink();
                if (actualAttribute.isEmpty()) {
                    throw new AutotestError("Сообщение не содержит ссылки: " + mailHandler.getEmail().getMessage());
                }
                break;
            case "тему":
                actualAttribute = mailHandler.getEmail().getTopic();
                break;
            case "сообщение":
                actualAttribute = mailHandler.getEmail().getMessage();
                break;
            case "sms код":
                actualAttribute = mailHandler.getEmail().getSmsCode();
                if (actualAttribute.isEmpty()) {
                    throw new AutotestError("Сообщение не содержит SMS кода: " +
                            mailHandler.getEmail().getMessage());
                }
                break;
            default:
                throw new AutotestError("Не верно выбран атрибут: " + attribute);
        }
        Stash.put(key, actualAttribute);
    }
}
