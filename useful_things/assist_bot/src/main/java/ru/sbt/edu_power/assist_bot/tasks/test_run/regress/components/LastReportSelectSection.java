package ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components;

import com.slack.api.model.Attachment;
import com.slack.api.model.Message;
import com.slack.api.model.User;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.block.composition.OptionObject;
import com.slack.api.model.block.element.BlockElement;
import org.jetbrains.annotations.Nullable;
import ru.sbt.edu_power.assist_bot.services.channel_reader.ChannelReader;
import ru.sbt.edu_power.assist_bot.slack.users.SlackUsers;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractSelectFormField;
import ru.sbt.edu_power.assist_bot.slack.views.sections.SlackChannelSelectSection;
import ru.sbt.edu_power.external_services.ESUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LastReportSelectSection extends AbstractSection {

    public LastReportSelectSection(
            final SlackChannelSelectSection channelSelect,
            final String userId,
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(new Accessory(channelSelect, userId), isAndCondition, constructConditions);
    }

    @Override
    public SectionBlock construct() {
        return SectionBlock.builder()
                           .blockId(getId())
                           .text(asText("Можно использовать форму предыдущего не завершившегося отчета", false))
                           .accessory(getAccessory().getElement())
                           .build();
    }

    private static class Accessory extends AbstractSelectFormField {
        private final SlackChannelSelectSection channelSelect;
        private final String userId;

        Accessory(final SlackChannelSelectSection channelSelect, final String userId) {
            this.channelSelect = channelSelect;
            this.userId = userId;
        }

        @Override
        public BlockElement getElement() {
            return getCachedElement(
                    () -> getSelect(getName(), getOptions()),
                    channelSelect.getAccessory().getValue()
            );
        }

        private List<OptionObject> getOptions() {
            final ChannelReader channelReader = new ChannelReader(channelSelect.getAccessory().getValue(), userId, 100);
            if (channelReader.isNotInChannel()) {
                return Collections.singletonList(OptionObject.builder()
                                                             .value("none")
                                                             .text(asText("Необходимо добавить бота в канал", false))
                                                             .build()
                );
            }
            final User user = SlackUsers.getInstance().getUserByName("regressman");
            channelReader.waitWhenChanelBeRead();
            final List<Message> messages = channelReader.search(user)
                                                        .stream()
                                                        .filter(this::isRegressReport)
                                                        .filter(this::isRegressReportNotEnded)
                                                        .collect(Collectors.toList());
            final List<OptionObject> options = messages.stream()
                                                       .map(m -> OptionObject.builder()
                                                                             .value(m.getTs())
                                                                             .text(asMiddleCutText(getMessageName(m), 75, false))
                                                                             .build()
                                                       )
                                                       .limit(5)
                                                       .collect(Collectors.toList());
            options.add(0, OptionObject.builder()
                                       .value("skip")
                                       .text(asText("Использовать новый отчёт", false))
                                       .build()
            );
            return options;
        }

        private boolean isRegressReport(final Message message) {
            return Objects.nonNull(message.getAttachments()) &&
                   message.getAttachments().stream()
                          .anyMatch(a ->
                                  attachmentHasBlocks(a, "Всего", "Завершено", "Осталось пройти")
                          );
        }

        private boolean isRegressReportNotEnded(final Message message) {
            return Objects.nonNull(message.getAttachments()) &&
                   message.getAttachments().stream()
                          .noneMatch(a ->
                                  attachmentHasBlocks(a, "Все тест-кейсы пройдены!")
                          );
        }

        private boolean attachmentHasBlocks(final Attachment attachment, final String... text) {
            if (Objects.nonNull(attachment.getFallback()) && !attachment.getFallback().isEmpty()) {
                final boolean result = Stream.of(text)
                                             .allMatch(attachment.getFallback()::contains);
                if (result) {
                    return true;
                }
            }
            if (Objects.isNull(attachment.getBlocks()) || attachment.getBlocks().isEmpty()) {
                return false;
            }
            final String blockText = attachment.getBlocks()
                                               .stream()
                                               .map(Object::toString)
                                               .collect(Collectors.joining("\n"));
            return Stream.of(text)
                         .allMatch(blockText::contains);
        }

        private String getMessageName(final Message message) {
            final List<String> attachments = message.getAttachments()
                                                    .stream()
                                                    .filter(a ->
                                                            attachmentHasBlocks(a, "Версия", "Проект")
                                                    )
                                                    .map(a -> getBlockFromAttachment(a, "Версия", "Проект"))
                                                    .filter(Objects::nonNull)
                                                    .map(l -> ((MarkdownTextObject) ((SectionBlock) l).getText())
                                                            .getText()
                                                            .split("\\|")[1].split(">")[0])
                                                    .collect(Collectors.toList());

            if (attachments.isEmpty()) {
                return "";
            }
            return attachments.get(0);
        }

        @Nullable
        private LayoutBlock getBlockFromAttachment(final Attachment attachment, final String... text) {
            if (Objects.isNull(attachment.getBlocks()) || attachment.getBlocks().isEmpty()) {
                return null;
            }
            final List<LayoutBlock> blocks = attachment.getBlocks()
                                                       .stream()
                                                       .filter(b -> Stream.of(text)
                                                                          .allMatch(t -> b.toString().contains(t))
                                                       )
                                                       .collect(Collectors.toList());
            if (blocks.isEmpty()) {
                return null;
            }
            return blocks.get(0);
        }

        @Override
        public String getName() {
            return "Использовать предыдущий отчёт";
        }
    }
}
