package ru.sbt.edu_power.assist_bot.slack.views.sections;

import com.slack.api.app_backend.interactive_components.response.Option;
import com.slack.api.model.Conversation;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.element.BlockElement;
import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractHasExternalLoadFormField;
import ru.sbt.edu_power.assist_bot.slack.SlackClient;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

@Slf4j
public class SlackChannelSelectSection extends AbstractSection {

    public SlackChannelSelectSection() {
        super(new Accessory());
    }

    public SlackChannelSelectSection(
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(new Accessory(), isAndCondition, constructConditions);
    }

    @Override
    public SectionBlock construct() {
        return SectionBlock
                .builder()
                .blockId(getId())
                .accessory(getAccessory().getElement())
                .text(asText("Выбери канал слака, в который будут отправляться данные", false))
                .build();
    }

    private static class Accessory extends AbstractHasExternalLoadFormField {
        private final List<Conversation> channels = SlackClient.getChannels();

        @Override
        public BlockElement getElement() {
            return getSelect(getName(), 2);
        }


        @Override
        public String getName() {
            return "Выбор канала слака";
        }

        @Override
        public void callback() {
            APP.blockSuggestion(getId(), (req, ctx) -> {
                final String keyWord = req.getPayload().getValue();
                final List<Option> suggestion = channels.stream()
                                                        .filter(ch -> ch.getName().contains(keyWord))
                                                        .limit(10)
                                                        .map(ch -> Option
                                                                .builder()
                                                                .text(asText(ch.getName(), false))
                                                                .value(ch.getId())
                                                                .build())
                                                        .collect(Collectors.toList());

                return ctx.ack(r -> r.options(suggestion));
            });
        }
    }
}
