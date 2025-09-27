package ru.sbt.edu_power.assist_bot.tasks.test_run.regress_assistant;

import ru.sbt.edu_power.assist_bot.AssistBotException;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.test_run.TestRunStorage;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.tasks.FinalReportTask;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.tasks.RegressAssistantTask;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.TestRunSearch;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.TestRunModel;
import ru.sbt.edu_power.assist_bot.task_flow.Container;
import ru.sbt.edu_power.assist_bot.task_flow.IDispatcher;
import ru.sbt.edu_power.assist_bot.task_flow.TaskGlue;

import java.util.HashMap;

public class RegressAssistantDispatcher implements IDispatcher {
    private final RegressAssistantView view;
    private TestRunModel testRunModel;

    public RegressAssistantDispatcher(final RegressAssistantView view) {
        this.view = view;
    }

    public TestRunModel getTestRunModel() {
        return testRunModel;
    }

    @Override
    public void dispatch() {
        testRunModel = new TestRunSearch(view.getTestRunSelectSection().getAccessory().getValue())
                .getTestRunByKey()
                .orElseThrow(() -> new AssistBotException("Не найден тест-сет " +
                                                          view
                                                                  .getTestRunSelectSection()
                                                                  .getAccessory()
                                                                  .getValue()));
        final TestRunStorage testRunStorage = new TestRunStorage(testRunModel);
        final RegressAssistantTask regressAssistantTask = new RegressAssistantTask(
                view,
                new Container<>(testRunStorage),
                () -> "",
                view.getGenerateFinalReport()
        );
        final TaskGlue taskGlue = new TaskGlue(
                String.format(
                        "Ассистент регресса %s ver:%s",
                        testRunModel.getKey(),
                        testRunModel.getJiraVersionModel().getName()
                ), view.getUserId());
        if (view.getFinalReportGenerate().getAccessory().getBoolean()) {
            final FinalReportTask reportTask =
                    new FinalReportTask(view, HashMap::new);
            taskGlue.addQueueCompleteCondition(reportTask::isTaskComplete)
                    .add(
                            reportTask,
                            regressAssistantTask,
                            false,
                            // отчёт генерируется если завершилась задача ассистента регресса
                            regressAssistantTask::isTaskComplete,
                            // или если установлен признак view.generateFinalReport в true
                            view.getGenerateFinalReport()::get
                    );
        } else {
            taskGlue.addQueueCompleteCondition(regressAssistantTask::isTaskComplete);
        }
        taskGlue.add(regressAssistantTask).execute();
    }
}
