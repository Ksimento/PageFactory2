package ru.sbt.edu_power.assist_bot.slack.views.sections;

import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.element.BlockElement;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractDatePickerFormField;

import java.util.function.BooleanSupplier;

public class DatePickerSection extends AbstractSection {
    private final String name;

    public DatePickerSection(
            final String name
    ) {
        super(new Accessory());
        this.name = name;
    }

    public DatePickerSection(
            final String name,
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(new Accessory(), isAndCondition, constructConditions);
        this.name = name;
    }

    @Override
    public SectionBlock construct() {
        return SectionBlock.builder()
                .blockId(getId())
                .accessory(getAccessory().getElement())
                .text(asText(name, false))
                .build();
    }

    private static class Accessory extends AbstractDatePickerFormField {
        @Override
        public BlockElement getElement() {
            return getDatePickerElement();
        }

        @Override
        public String getName() {
            return "Выбор даты";
        }
    }
}
