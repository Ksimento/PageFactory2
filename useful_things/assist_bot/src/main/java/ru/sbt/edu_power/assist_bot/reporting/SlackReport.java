package ru.sbt.edu_power.assist_bot.reporting;

import com.slack.api.bolt.App;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.chat.ChatDeleteResponse;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.chat.ChatUpdateResponse;
import com.slack.api.model.Attachment;
import com.slack.api.model.block.ContextBlock;
import com.slack.api.model.block.ContextBlockElement;
import com.slack.api.model.block.DividerBlock;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.block.element.BlockElement;
import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.assist_bot.slack.views.Element;
import ru.sbt.edu_power.external_services.ESUtils;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbt.edu_power.assist_bot.runner.Main;
import ru.sbt.edu_power.assist_bot.slack.SlackClient;
import ru.sbt.edu_power.assist_bot.task_flow.TaskGlue;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

@Slf4j
public class SlackReport extends Element {
    private static final App APP = Main.SLACK_DISPATCHER.getApp();
    private static final String BOT_TOKEN = Main.BOT_TOKEN;
    private final String channel;
    private String channelId;
    // период времени, через который будет удалено сообщение с предупреждением о том, что репорт перенесён в конец чата
    private final Duration removeAfterRenewDuration = Duration.ofSeconds(20L);
    // блокировка обновления репорта на период выполнения renew операции
    private volatile boolean lockReportUpdate;
    // timestamp активного сообщение с репортом
    private String currentTs;
    // timestamp устаревшего сообщения с репортом
    private String renewingTs;
    // текст в текущем сообщении о перенесении репорта в конец чата
    private final String renewMessage = "Сообщение с отчётом перенесено в конец чата :point_down:";
    // состояние репорта. true - репорт активен и выполняет renew операцию
    private boolean isActive = true;
    private static final int TIMEOUT_SECONDS = 20;
    // Текущий набор блоков в отчёте (для выполнения renew операции)
    private final List<LayoutBlock> sendingBlocks = new ArrayList<>();
    // Информация о времени последнего обновления отчёта
    private ContextBlock lastUpdatedBlock;
    private String color;
    private final String userId;
    private final ReportType reportType;


    /**
     * @param channel             канал для постинга репорта
     * @param reportRenewDuration период времени, через который сообщение с репортом будет переотправлена а старое сообщение будет удалено
     */
    public SlackReport(
            final String channel,
            final Duration reportRenewDuration,
            final String userId,
            final String reportName,
            final String currentTs,
            final ReportType reportType,
            final boolean isProtected
    ) {
        this.reportType = reportType;
        this.channel = channel;
        this.channelId = channel;
        if (Objects.nonNull(currentTs) && !currentTs.isEmpty()) {
            this.currentTs = currentTs;
        }
        this.userId = userId;
        final LocalDateTime timeToStart = LocalDateTime.now().plus(reportRenewDuration);
        new TaskGlue("Переотправка репорта по таймеру '" + reportName + "'", userId, isProtected)
                .addQueueStartCondition(() -> LocalDateTime.now().isAfter(timeToStart) || !isActive)
                .add(new SlackReportRenewingTask(null, reportRenewDuration, this))
                .addQueueCompleteCondition(() -> !isActive)
                .execute();
    }

    public void updateReport(final List<LayoutBlock> blocks) {
        updateReport(blocks, "#00ad99");
    }

    // выполняет обновление данных в сообщении с репортом. Если сообщение первое - будет отправка нового сообщения
    public void updateReport(final List<LayoutBlock> blocks, final String color) {
        this.color = color;
        sendingBlocks.clear();
        sendingBlocks.addAll(blocks);
        final ContextBlockElement lastUpdatedTime = MarkdownTextObject.builder()
                                                                      .text("<!date^" +
                                                                            (System.currentTimeMillis() / 1000) +
                                                                            "^Отчёт обновлен {date_num} {time_secs} (время местное)|?>")
                                                                      .build();

        final ContextBlockElement reportTypeBlock = MarkdownTextObject.builder()
                                                                      .text(String.format("*%s*", reportType.getReportName()))
                                                                      .build();

        lastUpdatedBlock = ContextBlock.builder()
                                       .elements(Arrays.asList(reportTypeBlock, lastUpdatedTime))
                                       .build();
        if (Objects.isNull(currentTs) || currentTs.isEmpty()) {
            postReport();
        } else {
            updateCurrentReport();
        }
    }

    // выполняет переотправку существующего отчёта для перемещения его позиции в самый низ чата
    public void renew() {
        // Если currentTs сброшено, то обновление отчёта не выполняется
        if (Objects.isNull(currentTs) || currentTs.isEmpty()) {
            return;
        }
        // блокируем обновление отчёта перед его переносом в конец чата
        lockReportUpdate = true;
        renewingTs = currentTs;
        postReport();
        renewMessageUpdate();
    }

    private void renewMessageUpdate() {
        // в устревшем сообщении меняем отчёт на фразу о том, что отчёт пермещён в конец чата
        final BooleanSupplier waitWhenRenewMessageBeUpdated = () -> {
            try {
                final ChatUpdateResponse response = APP.client().chatUpdate(r -> r
                        .token(BOT_TOKEN)
                        .channel(channelId)
                        .ts(renewingTs)
                        .text(renewMessage)
                        .blocks(new ArrayList<>())
                );
                if (!response.isOk()) {
                    if ("message_not_found".equals(response.getError())) {
                        currentTs = "";
                    }
                    log.error("{}", response);
                    return true;
                }
            } catch (final SlackApiException | IOException e) {
                log.error("", e);
                ESUtils.freeze(3000);
                return false;
            }
            return true;
        };
        Timer.executeTimer(TIMEOUT_SECONDS, waitWhenRenewMessageBeUpdated);
        // если currentTs обнулился - значит сообщение было удалено и операцию renew нужно прекратить
        if (Objects.isNull(currentTs) || currentTs.isEmpty()) {
            return;
        }
        // разблокируем обновление данных отчёта
        lockReportUpdate = false;
        ESUtils.freeze(removeAfterRenewDuration.toMillis());
        // подождали немного и удаляем устаревшее сообщение из чата
        final BooleanSupplier waitWhenOldMessageBeDeleted = () -> {
            try {
                final ChatDeleteResponse response = APP.client().chatDelete(r -> r
                        .token(BOT_TOKEN)
                        .channel(channelId)
                        .ts(renewingTs)
                );
                if (!response.isOk()) {
                    if ("message_not_found".equals(response.getError())) {
                        currentTs = "";
                    }
                    log.error("{}", response);
                    return true;
                }
            } catch (final SlackApiException | IOException e) {
                log.error("", e);
                ESUtils.freeze(3000);
                return false;
            }
            return true;
        };
        Timer.executeTimer(TIMEOUT_SECONDS, waitWhenOldMessageBeDeleted);
    }

    // отправка нового отчёта в канал, сохранение currentTs этого сообщения
    private void postReport() {
        final BooleanSupplier waitWhenReportBePosted = () -> {
            try {
                final ChatPostMessageResponse response = APP.client().chatPostMessage(r -> r
                        .token(BOT_TOKEN)
                        .channel(channel)
                        .attachments(generateAttachment())
                        .blocks(getHeader())
                        .text(reportType.getReportName())
                        .mrkdwn(true)
                );
                if (!response.isOk()) {
                    log.error("{}", response);
                    ESUtils.freeze(3000);
                    return false;
                }
                currentTs = response.getTs();
                channelId = response.getChannel();
            } catch (final SlackApiException | IOException e) {
                log.error("", e);
                ESUtils.freeze(3000);
                return false;
            }
            return true;
        };
        final boolean result = Timer.executeTimer(
                TIMEOUT_SECONDS,
                waitWhenReportBePosted
        );
        if (!result) {
            SlackClient.sendText("Не удалось выполнить отправку сообщения, смотрите логи", userId);
            log.error("Не удалось выполнить отправку сообщения, смотрите логи");
        }
    }

    // обновление данных в текущем сообщении отчёта по currentTs
    private void updateCurrentReport() {
        final BooleanSupplier waitWhenReportBeUpdated = () -> {
            if (lockReportUpdate) {
                return false;
            }
            try {
                final ChatUpdateResponse response = APP.client().chatUpdate(r -> r
                        .token(BOT_TOKEN)
                        .channel(channelId)
                        .ts(currentTs)
                        .attachments(generateAttachment())
                        .blocks(getHeader())
                        .text(reportType.getReportName())
                );
                if (!response.isOk()) {
                    log.error("{}", response);
                    ESUtils.freeze(3000);
                    return false;
                }
            } catch (final SlackApiException | IOException e) {
                log.error("", e);
                ESUtils.freeze(3000);
                return false;
            }
            return true;
        };
        final boolean result = Timer.executeTimer(
                TIMEOUT_SECONDS,
                waitWhenReportBeUpdated
        );
        if (!result) {
            SlackClient.sendText("Не удалось выполнить отправку сообщения, смотрите логи", userId);
            log.error("Не удалось выполнить отправку сообщение, смотрите логи");
        }
    }

    private List<LayoutBlock> getHeader() {
        final List<LayoutBlock> reportHeader = new ArrayList<>();
        reportHeader.add(lastUpdatedBlock);
        reportHeader.add(DividerBlock.builder().build());
        return reportHeader;
    }

    private List<Attachment> generateAttachment() {
        final String fallback = sendingBlocks
                .stream()
                .map(Object::toString)
                .collect(Collectors.joining("\n"));
        return Collections.singletonList(
                Attachment.builder()
                          .blocks(sendingBlocks)
                          .color(color)
                          .fallback(fallback.isEmpty() ? "empty report" : fallback)
                          .build()
        );
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(final boolean active) {
        isActive = active;
    }
}
