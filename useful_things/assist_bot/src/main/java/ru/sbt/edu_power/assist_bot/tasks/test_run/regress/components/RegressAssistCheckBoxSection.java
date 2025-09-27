package ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components;

import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.OptionObject;
import com.slack.api.model.block.element.BlockElement;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractCheckBoxFormField;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.RegressAssistants;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RegressAssistCheckBoxSection extends AbstractSection {

    public RegressAssistCheckBoxSection() {
        super(new Accessory());
    }

    public RegressAssistCheckBoxSection(
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(new Accessory(), isAndCondition, constructConditions);
    }

    @Override
    public SectionBlock construct() {
        return SectionBlock.builder()
                           .blockId(getId())
                           .accessory(getAccessory().getElement())
                           .text(asText("Выбор ассистента", false)
                           ).build();
    }


    private static class Accessory extends AbstractCheckBoxFormField {

        @Override
        public BlockElement getElement() {
            final List<OptionObject> options = Stream.of(RegressAssistants.values())
                                                     .map(v -> OptionObject.builder()
                                                                           .text(asText(v.getDescription(), false))
                                                                           .value(v.name())
                                                                           .build()
                                                     ).collect(Collectors.toList());
            return getCheckBoxList(options);
        }

        @Override
        public String getName() {
            return "Выбор ассистента";
        }
    }
}
