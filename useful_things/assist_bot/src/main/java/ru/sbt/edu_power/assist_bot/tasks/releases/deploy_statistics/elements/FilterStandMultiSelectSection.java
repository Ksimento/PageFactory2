package ru.sbt.edu_power.assist_bot.tasks.releases.deploy_statistics.elements;

import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.OptionObject;
import com.slack.api.model.block.element.BlockElement;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractMultiStaticSelectFormField;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FilterStandMultiSelectSection extends AbstractSection {

    public FilterStandMultiSelectSection(
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(new Accessory(), isAndCondition, constructConditions);
    }

    @Override
    public SectionBlock construct() {
        return SectionBlock.builder()
                .blockId(getId())
                .text(asText("Выбери стенды для ограничения данных в отчёте", false))
                .accessory(getAccessory().getElement())
                .build();
    }

    private static class Accessory extends AbstractMultiStaticSelectFormField {

        @Override
        public BlockElement getElement() {
            return getSelectList(getOptions());
        }

        private List<OptionObject> getOptions() {
            return Stream.of("SP14", "SP15", "SP18")
                    .map(stand -> OptionObject.builder().text(asText(stand, false)).value(stand).build())
                    .collect(Collectors.toList());
        }

        @Override
        public String getName() {
            return "Выбор стендов";
        }
    }
}
