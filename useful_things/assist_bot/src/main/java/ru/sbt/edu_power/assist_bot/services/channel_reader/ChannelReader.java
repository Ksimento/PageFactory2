package ru.sbt.edu_power.assist_bot.services.channel_reader;

import com.slack.api.methods.response.conversations.ConversationsHistoryResponse;
import com.slack.api.model.Message;
import com.slack.api.model.User;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.ESUtils;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbt.edu_power.assist_bot.runner.Main;
import ru.sbt.edu_power.assist_bot.slack.SlackClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

@Slf4j
public class ChannelReader {
    private final String chanelId;
    // ID пользователя вызывавшего модуль для отправки нотификации в случае сбоя
    private final String userId;
    private final List<Message> messages = new ArrayList<>();
    private boolean notInChannel;
    private boolean readComplete;

    public ChannelReader(final String channelId, final String userId, final int maxMessages) {
        this.chanelId = channelId;
        this.userId = userId;
        if (checkChannel()) {
            new Thread(() -> {
                readChannel(maxMessages, null);
                readComplete = true;
            }).start();
        }
    }

    public void waitWhenChanelBeRead() {
        while (!readComplete) {
            ESUtils.freeze(500);
        }
    }

    public List<Message> search(final User user) {
        return messages.stream()
                       .filter(m -> {
                                   final String author = m.getUser();
                                   if (Objects.isNull(author)) {
                                       return false;
                                   }
                                   return author.equals(user.getId());
                               }
                       )
                       .collect(Collectors.toList());
    }

    public List<Message> getMessages() {
        return messages;
    }

    public boolean isNotInChannel() {
        return notInChannel;
    }

    @SneakyThrows
    private boolean checkChannel() {
        final ConversationsHistoryResponse response = Main.SLACK_DISPATCHER
                .getApp()
                .client()
                .conversationsHistory(r -> r
                        .limit(3)
                        .token(Main.BOT_TOKEN)
                        .channel(chanelId)
                );
        if (!response.isOk()) {
            notInChannel = "not_in_channel".equals(response.getError());
            return false;
        }
        return true;
    }

    private void readChannel(int maxMessages, final String cursor) {
        if (maxMessages < 1) {
            return;
        }
        if (Objects.nonNull(cursor) && cursor.isEmpty()) {
            return;
        }
        final int limit = Math.min(200, maxMessages);
        final AtomicReference<ConversationsHistoryResponse> response = new AtomicReference<>();
        final BooleanSupplier reader = () -> {
            try {
                response.set(Main.SLACK_DISPATCHER
                        .getApp()
                        .client()
                        .conversationsHistory(r -> r
                                .limit(limit)
                                .token(Main.BOT_TOKEN)
                                .channel(chanelId)
                                .cursor(cursor)
                        ));
            } catch (final Throwable e) {
                log.error("", e);
                SlackClient.sendText(
                        String.format("Не удалось выполнить чтение из канала %s\n%s", chanelId, e),
                        userId
                );
                ESUtils.freeze(3000);
                return false;
            }
            if (!response.get().isOk()) {
                log.error("Ошибка чтения канала:\n{}", response.get());
                ESUtils.freeze(3000);
                return false;
            }
            return true;
        };
        if (Timer.executeTimer(30, reader)) {
            messages.addAll(response.get().getMessages());
        }
        maxMessages = maxMessages - limit;
        if (Objects.nonNull(response.get().getResponseMetadata())) {
            readChannel(maxMessages, response.get().getResponseMetadata().getNextCursor());
        }
    }
}
