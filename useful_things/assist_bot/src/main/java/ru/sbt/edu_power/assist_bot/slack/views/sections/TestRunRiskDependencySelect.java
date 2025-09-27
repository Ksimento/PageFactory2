package ru.sbt.edu_power.assist_bot.slack.views.sections;

import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.OptionObject;
import com.slack.api.model.block.element.BlockElement;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractSelectFormField;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public class TestRunRiskDependencySelect extends AbstractSection {

    public TestRunRiskDependencySelect() {
        super(new Accessory());
    }

    public TestRunRiskDependencySelect(
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(new Accessory(), isAndCondition, constructConditions);
    }

    @Override
    public SectionBlock construct() {
        return SectionBlock.builder()
                .blockId(getId())
                .text(asText("Выбери минимальный риск который должен быть добавлен в тест-сет", false))
                .accessory(getAccessory().getElement())
                .build();
    }

    private static class Accessory extends AbstractSelectFormField {

        @Override
        public BlockElement getElement() {
            return getSelect("РКР", getOptions());
        }

        private List<OptionObject> getOptions() {
            final List<OptionObject> options = new ArrayList<>();
            options.add(asOptionObject("Все тест-кейсы", TCFields.Risk.LOWEST.name()));
            options.add(asOptionObject("Риск 2-5", TCFields.Risk.LOW.name()));
            options.add(asOptionObject("Риск 3-5", TCFields.Risk.MEDIUM.name()));
            options.add(asOptionObject("Риск 4-5", TCFields.Risk.HIGH.name()));
            options.add(asOptionObject("Риск 5", TCFields.Risk.CRITICAL.name()));
            return options;
        }

        @Override
        public String getName() {
            return "Минимальный риск для добавления в тест-сет";
        }
    }
}
