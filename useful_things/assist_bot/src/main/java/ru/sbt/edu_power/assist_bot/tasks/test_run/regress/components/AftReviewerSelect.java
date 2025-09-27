package ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components;

import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.element.BlockElement;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractUserSelectFormField;

import java.util.function.BooleanSupplier;

public class AftReviewerSelect extends AbstractSection {
    private final String name;

    public AftReviewerSelect(final String name) {
        super(new Accessory());
        this.name = name;
    }

    public AftReviewerSelect(
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
                .text(asText(name, false))
                .accessory(getAccessory().getElement())
                .build();
    }

    private static class Accessory extends AbstractUserSelectFormField {

        @Override
        public BlockElement getElement() {
            return getUserSelect("Выбери пользователя");
        }

        @Override
        public String getName() {
            return "Выбор пользователя";
        }
    }
}
