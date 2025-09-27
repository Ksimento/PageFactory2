package ru.sbt.edu_power.assist_bot.slack.views.sections;

import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.element.BlockElement;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractSingleCheckBoxFormField;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;

import java.util.function.BooleanSupplier;

public class SingleCheckboxSection extends AbstractSection {
    // пояснительный текст секции
    private final String text;

    public SingleCheckboxSection(
            final String text,
            final String name,
            final String description
    ) {
        super(new Accessory(name, description));
        this.text = text;
    }

    public SingleCheckboxSection(
            final String text,
            final String name,
            final String description,
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(new Accessory(name, description), isAndCondition, constructConditions);
        this.text = text;
    }

    @Override
    public SectionBlock construct() {
        return SectionBlock.builder()
                           .blockId(getId())
                           .text(asMrkdn(text))
                           .accessory(getAccessory().getElement())
                           .build();
    }

    private static class Accessory extends AbstractSingleCheckBoxFormField {
        // название для чекбокса
        private final String name;
        // описание для чекбокса
        private final String description;

        Accessory(final String name, final String description) {
            this.name = name;
            this.description = description;
        }

        @Override
        public BlockElement getElement() {
            return getCheckBox(name, description);
        }

        @Override
        public String getName() {
            return "Чекбокс " + name;
        }
    }
}
