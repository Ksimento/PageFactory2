package ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components;

import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.OptionObject;
import com.slack.api.model.block.element.BlockElement;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractSelectFormField;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SmokeTestAcceptPercentSelect extends AbstractSection {
    protected SmokeTestAcceptPercentSelect() {
        super(new Accessory());
    }

    public SmokeTestAcceptPercentSelect(
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(new Accessory(), isAndCondition, constructConditions);
    }

    @Override
    public SectionBlock construct() {
        return SectionBlock.builder()
                           .blockId(getId())
                           .text(asText(
                                   "Выбери допустимый процент прохождения Smoke тестов. Если процент будет ниже, тестирование будет прекращено",
                                   false
                           ))
                           .accessory(getAccessory().getElement())
                           .build();

    }

    private static class Accessory extends AbstractSelectFormField {
        private final String name = "Допустимый процент прохождения Smoke";

        @Override
        public BlockElement getElement() {
            final String[] options = new String[]{"0", "70", "85", "90", "93", "97", "100"};
            final List<OptionObject> optionList = Stream.of(options)
                                                        .map(o -> OptionObject
                                                                .builder()
                                                                .text(asText(o + "%", false))
                                                                .value(o)
                                                                .build())
                                                        .collect(Collectors.toList());
            return getSelect(name, optionList);
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
