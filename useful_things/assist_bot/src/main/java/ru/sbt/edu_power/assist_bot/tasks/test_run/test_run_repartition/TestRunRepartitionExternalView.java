package ru.sbt.edu_power.assist_bot.tasks.test_run.test_run_repartition;

import lombok.Getter;
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces.IHasProjectAndVersions;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.test_run.TestRunSlice;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.TestRunSelectSection;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.test_case_repartition.TestCaseRepartition;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.test_case_repartition.TestCaseRepartitionDispatcher;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.test_case_repartition.components.ITestRunRepartition;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.test_case_repartition.components.TestCaseRepartitionCheckboxSection;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.test_case_repartition.components.TestCaseUserAssignedSelectSection;
import ru.sbt.edu_power.assist_bot.tasks.test_run.test_run_repartition.components.TestRunStatCollectButtonSection;
import ru.sbt.edu_power.external_services.jira.JiraConnect;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractModal;
import ru.sbt.edu_power.assist_bot.slack.views.sections.JiraProjectSelectSection;
import ru.sbt.edu_power.assist_bot.slack.views.sections.JiraVersionSelectSection;
import ru.sbt.edu_power.assist_bot.slack.views.sections.SlackChannelSelectSection;
import ru.sbt.edu_power.assist_bot.task_flow.Container;
import ru.sbt.edu_power.external_services.jira.agile.JiraVersion;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraVersionModel;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;

import java.util.List;
import java.util.Objects;

@Getter
public class TestRunRepartitionExternalView extends AbstractModal implements ITestRunRepartition,
        IHasProjectAndVersions
{
    private final Container<TestRunSlice> testRunSliceContainer = new Container<>();
    private final Container<TestCaseRepartition> testCaseRepartitionContainer = new Container<>();

    private final SlackChannelSelectSection slackChannelSelectSection = new SlackChannelSelectSection();

    private final JiraProjectSelectSection jiraProjectSelectSection = new JiraProjectSelectSection(
            true,
            () -> slackChannelSelectSection.getAccessory().isFilled()
    );

    private final JiraVersionSelectSection jiraVersionSelectSection =
            new JiraVersionSelectSection(
                    jiraProjectSelectSection,
                    true,
                    () -> jiraProjectSelectSection.getAccessory().isFilled()
            );

    private final TestRunSelectSection testRunSelectSection = new TestRunSelectSection(
            this,
            true,
            () -> jiraVersionSelectSection.getAccessory().isFilled()
    );

    private final TestRunStatCollectButtonSection testRunStatCollectButtonSection = new TestRunStatCollectButtonSection(
            testRunSliceContainer,
            testRunSelectSection,
            true,
            () -> testRunSelectSection.getAccessory().isFilled()
    );

    private final TestCaseUserAssignedSelectSection testCaseUserAssignedSelectSectionFrom = new TestCaseUserAssignedSelectSection(
            "Выбери у кого забрать тест-кейсы",
            testRunSliceContainer,
            true,
            true,
            () -> testRunStatCollectButtonSection.getAccessory().isFilled(),
            () -> Objects.nonNull(testRunSliceContainer.getObject())
    );

    private final TestCaseUserAssignedSelectSection testCaseUserAssignedSelectSectionTo = new TestCaseUserAssignedSelectSection(
            "Выбери кому передать тест-кейсы",
            testRunSliceContainer,
            false,
            true,
            () -> testCaseUserAssignedSelectSectionFrom.getAccessory().isFilled()
    );

    private final TestCaseRepartitionCheckboxSection testCaseRepartitionCheckboxSection = new TestCaseRepartitionCheckboxSection(
            "Наборы тест-кейсов для передачи",
            () -> JiraConnect.jiraGetUser(testCaseUserAssignedSelectSectionFrom.getAccessory().getValue()),
            testRunSliceContainer,
            testCaseRepartitionContainer,
            true,
            () -> testCaseUserAssignedSelectSectionTo.getAccessory().isFilled()
    );

    @Override
    public String getName() {
        return "Передача тест-кейсов";
    }

    @Override
    public void registerViewSubmit() {
        registerViewSubmit(new TestCaseRepartitionDispatcher(this));
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
        return slackChannelSelectSection.getAccessory().getValue();
    }

    @Override
    public List<String> getFolders() {
        return testCaseRepartitionCheckboxSection.getAccessory().getValues();
    }

    @Override
    public TCFields.ProjectId getJiraProjectId() {
        return TCFields.ProjectId.valueOf(jiraProjectSelectSection.getAccessory().getValue());
    }

    @Override
    public JiraVersionModel getDeployVersion() {
        return new JiraVersion().getJiraVersionById(getJiraProjectId().id, jiraVersionSelectSection.getAccessory().getValue());
    }

    @Override
    public JiraVersionModel getTestRunVersion() {
        return getDeployVersion();
    }
}
