package ru.sbt.edu_power.assist_bot.tasks.test_run.test_run_creation;

import ru.sbt.edu_power.external_services.shared.TaskExecutionStatus;
import ru.sbt.edu_power.external_services.jira.agile.JiraVersion;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraVersionModel;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.RegressTestRunCreation;
import ru.sbt.edu_power.assist_bot.runner.Main;
import ru.sbt.edu_power.assist_bot.slack.SlackClient;
import ru.sbt.edu_power.assist_bot.task_flow.IDispatcher;
import ru.sbt.edu_power.assist_bot.task_flow.templates.SlackMessage;

public class TestRunCreationDispatcher implements IDispatcher {
    private final TestRunCreationView view;

    public TestRunCreationDispatcher(final TestRunCreationView view) {
        this.view = view;
    }

    @Override
    public void dispatch() {

        final TCFields.ProjectId project = TCFields.ProjectId.valueOf(view.getJiraProjectSelectSection().getAccessory().getValue());
        final JiraVersionModel version =
                new JiraVersion()
                        .getJiraVersionById(project.id, view.getJiraVersionSelectSection().getAccessory().getValue());

        // создаём новый тест-сет
        final RegressTestRunCreation regressTestRunCreation = new RegressTestRunCreation(project, version);
        final TCFields.Risk risk = TCFields.Risk.valueOf(view.getTestRunRiskDependencySelect().getAccessory().getValue());
        if (risk != TCFields.Risk.LOWEST) {
            SlackClient.sendText(
                    "Перед создание тест-сета будет выполнено обновление поля Риск в тест-кейсах",
                    view.getUserId()
            );
        }
        final RegressTestRunCreation.Preset preset = RegressTestRunCreation
                .Preset
                .valueOf(view.getPresetSelectSection().getAccessory().getValue());
        new Thread(() -> regressTestRunCreation.create(preset, risk), "Создание тест-сета " + preset.name()).start();

        // добавляем задачу на уведомление в слак
        addSlackMessage(regressTestRunCreation);
    }

    private void addSlackMessage(final RegressTestRunCreation regressTestRunCreation) {
        new SlackMessage(
                () -> "Тест сет создан " + regressTestRunCreation.getBlankTestRunCreation().getTestRunModel().getKey(),
                view.getSlackChannelSelectSection().getAccessory().getValue(),
                Main.QUEUE_EXECUTOR,
                null,
                true,
                () -> regressTestRunCreation.getStatus() == TaskExecutionStatus.SUCCESS
        );
    }
}
