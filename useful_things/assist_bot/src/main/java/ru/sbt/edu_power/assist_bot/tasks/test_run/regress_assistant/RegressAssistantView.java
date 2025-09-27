package ru.sbt.edu_power.assist_bot.tasks.test_run.regress_assistant;

import lombok.Getter;
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces.IHasProjectAndVersions;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.LastReportSelectSection;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.TestRunSelectSection;
import ru.sbt.edu_power.external_services.jira.agile.JiraVersion;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraVersionModel;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.TestRunModel;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractModal;
import ru.sbt.edu_power.assist_bot.slack.views.sections.JiraProjectSelectSection;
import ru.sbt.edu_power.assist_bot.slack.views.sections.JiraVersionSelectSection;
import ru.sbt.edu_power.assist_bot.slack.views.sections.SingleCheckboxSection;
import ru.sbt.edu_power.assist_bot.slack.views.sections.SlackChannelSelectSection;
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces.IHasOverridingReportTs;
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces.IHasTestRunModel;
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces.ISlackChannel;

import java.util.concurrent.atomic.AtomicBoolean;

@Getter
public class RegressAssistantView extends AbstractModal implements ISlackChannel, IHasTestRunModel,
        IHasOverridingReportTs, IHasProjectAndVersions
{
    private final RegressAssistantDispatcher dispatcher = new RegressAssistantDispatcher(this);
    private final AtomicBoolean generateFinalReport = new AtomicBoolean(false);

    private final SlackChannelSelectSection slackChannelSelectSection = new SlackChannelSelectSection();

    private final LastReportSelectSection lastReportSelectSection = new LastReportSelectSection(
            slackChannelSelectSection,
            getUserId(),
            true,
            () -> slackChannelSelectSection.getAccessory().isFilled()
    );

    private final JiraProjectSelectSection jiraProjectSelectSection = new JiraProjectSelectSection(
            true,
            () -> lastReportSelectSection.getAccessory().isFilled()
    );

    private final JiraVersionSelectSection jiraVersionSelectSection =
            new JiraVersionSelectSection(jiraProjectSelectSection);

    private final TestRunSelectSection testRunSelectSection = new TestRunSelectSection(
            this,
            true,
            () -> jiraVersionSelectSection.getAccessory().isFilled()
    );

    private final SingleCheckboxSection finalReportGenerate = new SingleCheckboxSection(
            "Генерация итогового отчёта",
            "Выполнить итоговый отчёт",
            "По завершении прохождения тест-сета будет сгенерирован отчёт о прохождении регресса и выгружен в Confluence",
            true,
            () -> jiraVersionSelectSection.getAccessory().isFilled()
    );

    @Override
    public String getName() {
        return "Ассистент регресса";
    }

    @Override
    public void registerViewSubmit() {
        registerViewSubmit(dispatcher);
    }

    @Override
    public String getSlackChannel() {
        return slackChannelSelectSection.getAccessory().getValue();
    }

    @Override
    public TestRunModel getTestRunModel() {
        return dispatcher.getTestRunModel();
    }

    @Override
    public String getOverridingReportTs() {
        return "skip".equals(lastReportSelectSection.getAccessory().getValue()) ? "" : lastReportSelectSection
                .getAccessory()
                .getValue();
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
