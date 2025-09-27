package ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components;

import com.slack.api.app_backend.interactive_components.response.Option;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.element.BlockElement;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractHasExternalLoadFormField;

import java.util.Collections;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;

public class StageSelectSection extends AbstractSection {
    public StageSelectSection() {
        super(new Accessory());
    }

    public StageSelectSection(
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(new Accessory(), isAndCondition, constructConditions);
    }

    @Override
    public SectionBlock construct() {
        return SectionBlock.builder()
                           .text(asText("Введи номер стейджа в формате spXX и выбери из списка", false))
                           .accessory(getAccessory().getElement())
                           .blockId(getId())
                           .build();
    }

    private static class Accessory extends AbstractHasExternalLoadFormField {

        @Override
        public BlockElement getElement() {
            return getSelect("spXX", 4);
        }

        @Override
        public String getName() {
            return "Выбор стенда STAGE";
        }

        @Override
        public void callback() {
            APP.blockSuggestion(getId(), (req, ctx) -> {
                final String value = req.getPayload().getValue();
                final Pattern pattern = Pattern.compile("sp\\d\\d");
                final Option option;
                if (pattern.matcher(value).matches()) {
                    final String stand = "https://" + value + ".pcbltools.ru";
                    option = Option.builder()
                                   .text(asText(stand, false))
                                   .value(value)
                                   .build();
                } else {
                    option = Option.builder()
                                   .text(asText(":x: Допустимое название spXX", true))
                                   .value("false")
                                   .build();
                }
                return ctx.ack(r -> r.options(Collections.singletonList(option)));
            });
        }
    }
}
