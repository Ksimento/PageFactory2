package ru.sbt.edu_power.assist_bot.tasks.test_run.total_regress_report;

import ru.sbt.edu_power.assist_bot.AssistBotException;
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces.IHasProjectAndVersions;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.TestRunSelectSection;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.tasks.FinalReportTask;
import ru.sbt.edu_power.external_services.jira.agile.JiraVersion;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraVersionModel;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.TestRunSearch;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.TestRunModel;
import ru.sbt.edu_power.assist_bot.slack.SlackClient;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractModal;
import ru.sbt.edu_power.assist_bot.slack.views.sections.JiraProjectSelectSection;
import ru.sbt.edu_power.assist_bot.slack.views.sections.JiraVersionSelectSection;
import ru.sbt.edu_power.assist_bot.task_flow.TaskGlue;
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces.IHasTestRunModel;

import java.util.HashMap;
import java.util.Optional;

// Модалка для задачи создания итогового отчёта о прохождении регресса
public class TotalRegressReportView extends AbstractModal implements IHasTestRunModel, IHasProjectAndVersions {
    private final JiraProjectSelectSection jiraProjectSelectSection =
            new JiraProjectSelectSection();

    private final JiraVersionSelectSection jiraVersionSelectSection =
            new JiraVersionSelectSection(jiraProjectSelectSection);

    private final TestRunSelectSection testRunSelectSection = new TestRunSelectSection(
            this,
            true,
            () -> jiraVersionSelectSection.getAccessory().isFilled()
    );

    @Override
    public String getName() {
        return "Отчет по регрессу";
    }

    @Override
    public void registerViewSubmit() {
        registerViewSubmit(() -> {
            final FinalReportTask task = new FinalReportTask(this, HashMap::new);
            new TaskGlue("Итоговый отчёт " + testRunSelectSection.getAccessory().getValue(), getUserId())
                    .addQueueCompleteCondition(task::isTaskComplete)
                    .add(task)
                    .execute();
        });
    }

    @Override
    public TestRunModel getTestRunModel() {
        final TestRunSearch search = new TestRunSearch(testRunSelectSection.getAccessory().getValue());
        final Optional<TestRunModel> model = search.getTestRunByKey();
        if (!model.isPresent()) {
            SlackClient.sendText("Не найден тест-сет " + testRunSelectSection.getAccessory().getValue(), getUserId());
            throw new AssistBotException("Не найден тест-сет " + testRunSelectSection.getAccessory().getValue());
        }
        return model.get();
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
