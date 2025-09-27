package ru.sbt.edu_power.assist_bot.slack;

import com.slack.api.bolt.App;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.conversations.ConversationsListResponse;
import com.slack.api.methods.response.users.UsersLookupByEmailResponse;
import com.slack.api.model.Conversation;
import com.slack.api.model.ConversationType;
import com.slack.api.model.Attachment;
import com.slack.api.model.block.LayoutBlock;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.assist_bot.AssistBotException;
import ru.sbt.edu_power.assist_bot.roles.roles.UserRolesRepository;
import ru.sbt.edu_power.assist_bot.runner.Main;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraUser;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class SlackClient {
    private static final Map<JiraUser, String> USER_TO_CHAT_ID_MAP = new HashMap<>();
    private static final App APP = Main.SLACK_DISPATCHER.getApp();


    public static synchronized String getUserChatId(final JiraUser user) {
        if (USER_TO_CHAT_ID_MAP.containsKey(user)) {
            return USER_TO_CHAT_ID_MAP.get(user);
        }
        try {
            final UsersLookupByEmailResponse response =
                    APP.client()
                       .usersLookupByEmail(req ->
                               req.email(user.getEmailAddress())
                                  .token(Main.BOT_TOKEN)
                       );
            if (!response.isOk()) {
                return "";
            }
            if (!response.getUser().getId().isEmpty()) {
                USER_TO_CHAT_ID_MAP.put(user, response.getUser().getId());
            }
            return response.getUser().getId();
        } catch (final Exception e) {
            log.info(e.getMessage());
            return "";
        }
    }

    public static boolean sendMessage(
            final String chatId,
            final String messageCode,
            final Replacement... replacements
    ) {
        if (!MessagesRepository.has(messageCode)) {
            throw new SlackError("В репозитории сообщений нет сообщения с кодом " + messageCode);
        }
        final String message = MessagesRepository.get(messageCode, replacements);
        return sendText(message, chatId);
    }

    public static synchronized boolean sendText(final String message, final String chatId) {
        final String sendChatId;
        final String sendMessage;
        if (Objects.isNull(chatId) || chatId.isEmpty()) {
            sendChatId = UserRolesRepository.getAdminUser();
            sendMessage = "Сообщение для несуществующего чата или пользователя\n" + message;
        } else {
            sendChatId = chatId;
            sendMessage = message;
        }
        try {
            final ChatPostMessageResponse response = APP.client().chatPostMessage(req ->
                    req.channel(sendChatId)
                       .text(sendMessage)
                       .mrkdwn(true)
                       .token(Main.BOT_TOKEN)
            );
            if (!response.isOk()) {
                log.info(
                        "Message code: {}\nОшибка отправки сообщения\n{}",
                        message,
                        response
                );
                return false;
            }
        } catch (final Exception e) {
            log.info("Message: {}\n{}", message, e.getMessage());
            return false;
        }
        return true;
    }

    public static synchronized void sendMessageWithAttachments(
            final String text,
            final List<Attachment> attachments,
            final String channel,
            final String userId
    ) {
        try {
            final ChatPostMessageResponse response = APP.client()
                    .chatPostMessage(r -> r
                            .token(Main.BOT_TOKEN)
                            .text(text)
                            .attachments(attachments)
                            .mrkdwn(true)
                            .channel(channel)
                    );
            if (!response.isOk()) {
                log.error("{}", response);
                if (Objects.nonNull(userId) && !userId.isEmpty()) {
                    sendText("Не удалось отправить нотификацию " + text + "\n" + response, userId);
                }
            }
        } catch (final SlackApiException e) {
            log.error("Ошибка соединения со слаком", e);
        } catch (final IOException e) {
            throw new AssistBotException(e);
        }
    }

    public static synchronized boolean sendBlock(final List<LayoutBlock> blocks, final String chatId) {
        return sendBlock(blocks, "Обновление данных", chatId);
    }

    public static synchronized boolean sendBlock(final List<LayoutBlock> blocks, final String text, final String chatId) {
        try {
            final ChatPostMessageResponse response = APP.client().chatPostMessage(req ->
                    req.blocks(blocks)
                       .channel(chatId)
                       .text(text)
                       .token(Main.BOT_TOKEN)
            );
            if (!response.isOk()) {
                log.info(
                        "Ошибка отправки сообщения\nError {}\nNeeded {}",
                        response.getError(),
                        response.getNeeded()
                );
                return false;
            }
        } catch (final Exception e) {
            log.info(e.getMessage());
            return false;
        }
        return true;
    }

    @SneakyThrows
    public static List<Conversation> getChannels() {
        final ConversationsListResponse response = APP.client().conversationsList(r -> r
                .excludeArchived(true)
                .token(Main.BOT_TOKEN)
                .types(Arrays.asList(ConversationType.PUBLIC_CHANNEL, ConversationType.PRIVATE_CHANNEL))
                .limit(1000)
        );
        if (!response.isOk()) {
            log.error("{}", response);
        }
        return response.getChannels();
    }

    public static String getChannelId(final String channelName) {
        return getChannels().stream()
                            .filter(c -> c.getName().equals(channelName))
                            .map(Conversation::getId)
                            .findFirst()
                            .orElse("");
    }

    public static String wordDeclination(
            final int n,
            final String single,
            final String simple,
            final String multiple
    ) {
        final String declination;
        if (n == 0) {
            declination = multiple;
        } else {
            final int h = Math.abs(n) % 100;
            final int n1 = h % 10;
            if (h > 10 && h < 20) {
                declination = multiple;
            } else if (n1 > 1 && n1 < 5) {
                declination = simple;
            } else if (n1 == 1) {
                declination = single;
            } else {
                declination = multiple;
            }
        }
        return n + " " + declination;
    }
}
