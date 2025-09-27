package ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components;

import com.slack.api.model.block.SectionBlock;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.RegressTestRunCreation;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractFormField;
import ru.sbt.edu_power.assist_bot.task_flow.Container;

import java.util.function.BooleanSupplier;

public class CheckRegressCreationButtonSection extends AbstractSection {
    private final Container<RegressTestRunCreation> regressTestRunCreationContainer;

    public CheckRegressCreationButtonSection(
            final AbstractFormField accessory,
            final Container<RegressTestRunCreation> regressTestRunCreationContainer
    ) {
        super(accessory);
        this.regressTestRunCreationContainer = regressTestRunCreationContainer;
    }

    public CheckRegressCreationButtonSection(
            final AbstractFormField accessory,
            final boolean isAndCondition,
            final Container<RegressTestRunCreation> regressTestRunCreationContainer,
            final BooleanSupplier... constructConditions
    ) {
        super(accessory, isAndCondition, constructConditions);
        this.regressTestRunCreationContainer = regressTestRunCreationContainer;
    }

    @Override
    public SectionBlock construct() {
        return null;
    }
}
