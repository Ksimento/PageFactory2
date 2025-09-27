package ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.test_case_repartition;

import lombok.Getter;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.test_run.TestRunSlice;
import ru.sbt.edu_power.external_services.jira.JiraConnect;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractModal;
import ru.sbt.edu_power.assist_bot.task_flow.Container;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.test_case_repartition.components.ITestRunRepartition;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.test_case_repartition.components.TestCaseRepartitionCheckboxSection;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.test_case_repartition.components.TestCaseUserAssignedSelectSection;

import java.util.List;

@Getter
public class TestCaseRepartitionView extends AbstractModal implements ITestRunRepartition {
    private final Container<TestRunSlice> testRunSliceContainer = new Container<>();
    private final Container<TestCaseRepartition> testCaseRepartitionContainer = new Container<>();
    private final String slackChannelId;
    private final TestCaseRepartitionDispatcher dispatcher = new TestCaseRepartitionDispatcher(this);

    private final TestCaseUserAssignedSelectSection testCaseUserAssignedSelectSectionTo = new TestCaseUserAssignedSelectSection(
            "Выбери кому передать тест-кейсы",
            testRunSliceContainer,
            false
    );

    private final TestCaseUserAssignedSelectSection testCaseUserAssignedSelectSectionFrom = new TestCaseUserAssignedSelectSection(
            "Выбери у кого забрать тест-кейсы",
            testRunSliceContainer,
            true,
            true,
            () -> testCaseUserAssignedSelectSectionTo.getAccessory().isFilled()
    );

    private final TestCaseRepartitionCheckboxSection testCaseRepartitionCheckboxSection = new TestCaseRepartitionCheckboxSection(
            "Наборы тест-кейсов для передачи",
            () -> JiraConnect.jiraGetUser(testCaseUserAssignedSelectSectionFrom.getAccessory().getValue()),
            testRunSliceContainer,
            testCaseRepartitionContainer,
            true,
            () -> testCaseUserAssignedSelectSectionFrom.getAccessory().isFilled()
    );

    public TestCaseRepartitionView(final TestRunSlice testRunSlice, final String slackChannelId) {
        testRunSliceContainer.setObject(testRunSlice);
        this.slackChannelId = slackChannelId;
    }

    @Override
    public String getName() {
        return "Передача тест-кейсов";
    }

    @Override
    public void registerViewSubmit() {
        registerViewSubmit(dispatcher);
    }

    @Override
    public String getFromUserId() {
        return testCaseUserAssignedSelectSectionFrom.getAccessory().getValue();
    }

    @Override
    public String getToUserId() {
        return testCaseUserAssignedSelectSectionTo.getAccessory().getValue();
    }

    @Override
    public TestRunSlice getTestRunSlice() {
        return testRunSliceContainer.getObject();
    }

    @Override
    public TestCaseRepartition getTestCaseRepartition() {
        return testCaseRepartitionContainer.getObject();
    }

    @Override
    public String getChatId() {
        return slackChannelId;
    }

    @Override
    public List<String> getFolders() {
        return testCaseRepartitionCheckboxSection.getAccessory().getValues();
    }
}
