package ru.sbt.edu_power.assist_bot.tasks.test_run.regress;

import ru.sbt.edu_power.assist_bot.AssistBotException;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.tasks.FinalReportTask;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.tasks.RemoveUnusedTestCasesTask;
import ru.sbt.edu_power.external_services.shared.TaskExecutionStatus;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.TestRunSearch;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.TestRunModel;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbt.edu_power.assist_bot.slack.SlackClient;
import ru.sbt.edu_power.assist_bot.task_flow.Container;
import ru.sbt.edu_power.assist_bot.task_flow.IDispatcher;
import ru.sbt.edu_power.assist_bot.task_flow.Task;
import ru.sbt.edu_power.assist_bot.task_flow.TaskGlue;
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.DefaultFullDeployTask;
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.DefaultJavaUiParallelAftTask;
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.DefaultJsFt1UiAftTask;
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.DefaultJsUiAftTask;
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.DefaultRunnableTask;
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.DefaultSmartSmokeTask;
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces.IJobTask;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.issue.IssueStorage;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.test_run.TestRunStorage;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.tasks.IftAssistantTask;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.tasks.RegressAssistantTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

public class RegressStartDispatcher implements IDispatcher {
    private final RegressStartView view;
    private final List<String> standList = new ArrayList<>();
    private String javaUiStand;
    private String jsUiStand;
    private String jsFt1UiStand;
    private final List<TaskGlue> deployAndTestingExecutors = new ArrayList<>();
    private TestRunModel testRunModel;

    public RegressStartDispatcher(final RegressStartView view) {
        this.view = view;
    }

    @Override
    public void dispatch() {
        final RegressPreset regressPreset = RegressPreset.valueOf(view
                .getRegressPresetSelectSection()
                .getAccessory()
                .getValue());
        prepareThreads();
        switch (regressPreset) {
            case FULL_REGRESS:
            case FULL_REGRESS_MFE:
            case SMALL_REGRESS:
            case SMALL_REGRESS_MFE:
                standList.forEach(this::generateDeployAndTestingScenario);
                break;
            case HOT_FIX:
            case HOT_FIX_MFE:
                standList.forEach(this::generateDeployAndTestingScenario);
                // задача на удаление не запущенных тестов
                final RemoveUnusedTestCasesTask removeUnusedTestCasesTask = new RemoveUnusedTestCasesTask(view);
                new TaskGlue(removeUnusedTestCasesTask.getTaskName(), view.getUserId())
                        .addQueueStartCondition(() -> deployAndTestingExecutors.stream().allMatch(TaskGlue::isComplete))
                        .addQueueCompleteCondition(removeUnusedTestCasesTask::isTaskComplete)
                        .add(removeUnusedTestCasesTask)
                        .execute();
                break;
        }
        final List<RegressAssistants> assistants = getCheckedAssistants();
        if (assistants.contains(RegressAssistants.REGRESSION)) {
            new Thread(this::startRegressionControlScenario, "Поток для запуска ассистента регресса").start();
        }
        if (assistants.contains(RegressAssistants.IFT_NEED_TEST)) {
            startIftAssistant();
        }
    }

    private List<RegressAssistants> getCheckedAssistants() {
        return view.getRegressAssistCheckBoxSection()
                   .getAccessory()
                   .getValues()
                   .stream()
                   .map(RegressAssistants::valueOf)
                   .collect(Collectors.toList());
    }

    public TestRunModel getTestRunModel() {
        if (Objects.isNull(testRunModel)) {
            if (view.getCreateTestRunButtonSection().getAccessory().isFilled()) {
                if (Objects.isNull(view
                        .getRegressTestRunCreationContainer()
                        .getObject()
                        .getBlankTestRunCreation())) {
                    SlackClient.sendText(
                            "Объект BlankTestRunCreation не был создан при создании тест-сета",
                            view.getUserId()
                    );
                    throw new AssistBotException("Ошибка при получении TestRunModel");
                }
                testRunModel = view
                        .getRegressTestRunCreationContainer()
                        .getObject()
                        .getBlankTestRunCreation()
                        .getTestRunModel();
            } else {
                final String testRunKey = view
                        .getTestRunSelectSection()
                        .getAccessory()
                        .getValue();
                testRunModel = new TestRunSearch(testRunKey)
                        .getTestRunByKey()
                        .orElseThrow(() -> new AssistBotException(
                                "Не найден тест-сет " + testRunKey));
            }
        }
        return testRunModel;
    }

    private void prepareThreads() {
        javaUiStand = view.getStandSelectSectionJavaUI().getAccessory().getValue();
        jsUiStand = view.getStandSelectSectionJsUI().getAccessory().getValue();
        jsFt1UiStand = view.getStandSelectSectionJsFt1UI().getAccessory().getValue();
        if (!javaUiStand.isEmpty()) {
            standList.add(javaUiStand);
        }
        if (!jsUiStand.isEmpty() && !standList.contains(jsUiStand)) {
            standList.add(jsUiStand);
        }
        if (!jsFt1UiStand.isEmpty() && !standList.contains(jsFt1UiStand)) {
            standList.add(jsFt1UiStand);
        }
    }

    private void generateDeployAndTestingScenario(final String stand) {
        // создаём цепочку исполнения
        final TaskGlue taskGlue = new TaskGlue(view.getName() + "_" + stand, view.getUserId());
        deployAndTestingExecutors.add(taskGlue);
        // запуск смоков
        final String smokeGoodPercent = view.getSmokeTestAcceptPercentSelect().getAccessory().getValue();
        final DefaultSmartSmokeTask smartSmokeTask = new DefaultSmartSmokeTask(stand, view);
        if (view.getDeployStands().getAccessory().getBoolean()) {
            // деплой стенда
            final DefaultFullDeployTask fullDeployTask = new DefaultFullDeployTask(stand, view);
            taskGlue.add(fullDeployTask)
                    .add(smartSmokeTask, fullDeployTask, true, fullDeployTask::isTaskComplete);
        } else {
            taskGlue.add(smartSmokeTask);
        }
        // проверка смоков что они выполнились на 80%
        final DefaultRunnableTask checkSmoke = new DefaultRunnableTask(
                "контроль прохождение Smoke тестов на стенде " + stand,
                () -> smartSmokeTask.getQaUiSmokeJobContainer().getObject().getAllure().getSummaryPercent() >
                      Integer.parseInt(smokeGoodPercent) ?
                        TaskExecutionStatus.SUCCESS : TaskExecutionStatus.FAILED,
                view
        );
        taskGlue.add(checkSmoke, smartSmokeTask, true, smartSmokeTask::isTaskComplete);
        if ("0".equals(smokeGoodPercent)) {
            smartSmokeTask.setStatus(TaskExecutionStatus.SUCCESS);
            checkSmoke.setStatus(TaskExecutionStatus.SUCCESS);
        }
        // добавляем в цепочку автотесты
        if (stand.equals(javaUiStand)) {
            final DefaultJavaUiParallelAftTask javaUiParallelAftTask = new DefaultJavaUiParallelAftTask(
                    stand,
                    view
            );
            taskGlue.add(javaUiParallelAftTask, checkSmoke, true, checkSmoke::isTaskComplete);
            taskGlue.addQueueCompleteCondition(javaUiParallelAftTask::isTaskComplete);
        }

        if (stand.equals(jsUiStand)) {
            final DefaultJsUiAftTask jsUiAftTask = new DefaultJsUiAftTask(stand, view);
            taskGlue.add(jsUiAftTask, checkSmoke, true, checkSmoke::isTaskComplete);
            taskGlue.addQueueCompleteCondition(jsUiAftTask::isTaskComplete);
        }

        if (stand.equals(jsFt1UiStand)) {
            final DefaultJsFt1UiAftTask jsFt1UiAftTask = new DefaultJsFt1UiAftTask(stand, view);
            taskGlue.add(jsFt1UiAftTask, checkSmoke, true, checkSmoke::isTaskComplete);
            taskGlue.addQueueCompleteCondition(jsFt1UiAftTask::isTaskComplete);
        }
        // запуск очереди на исполнение
        taskGlue.execute();
    }

    // запуск ассистента проведения регресса
    private void startRegressionControlScenario() {
        // ожидаем создания пустого тест-сета
        final BooleanSupplier waitWhenTestRunBeCreated = () ->
                Objects.isNull(view.getRegressTestRunCreationContainer().getObject()) ||
                Objects.nonNull(getTestRunModel());
        Timer.executeTimer(60, waitWhenTestRunBeCreated);
        final TestRunStorage testRunStorage = new TestRunStorage(getTestRunModel());
        view.getTestRunAnalyticContainer().setObject(testRunStorage);
        final RegressAssistantTask regressAssistantTask = new RegressAssistantTask(
                view,
                new Container<>(testRunStorage),
                this::getAutotestProgress,
                view.getGenerateFinalReport()
        );
        final TaskGlue regressAssistant = new TaskGlue(
                "Ассистент проведения регресса '" + getTestRunModel().getName() + "'",
                view.getUserId()
        )
                .addQueueStartCondition(() ->
                        Objects.isNull(view.getRegressTestRunCreationContainer().getObject()) ||
                        view.getRegressTestRunCreationContainer().getObject().getStatus() == TaskExecutionStatus.SUCCESS
                )
                .add(regressAssistantTask);

        // генерация итогового отчёта о тестировании в Confluence
        if (view.getFinalReportGenerate().getAccessory().getBoolean()) {
            final FinalReportTask finalReportTask = new FinalReportTask(view, this::getAutotestJobList);
            regressAssistant
                    .add(
                            finalReportTask,
                            regressAssistantTask,
                            false,
                            regressAssistantTask::isTaskComplete,
                            view.getGenerateFinalReport()::get
                    )
                    .addQueueCompleteCondition(finalReportTask::isTaskComplete);
        } else {
            regressAssistant.addQueueCompleteCondition(regressAssistantTask::isTaskComplete);
        }
        regressAssistant.execute();
    }

    private void startIftAssistant() {
        final String name = String.format("IFT ассистент %s: %s", view.getProject(), view.getDeployVersion().getName());
        final IftAssistantTask task = new IftAssistantTask(
                view,
                name,
                new Container<>(new IssueStorage(
                        view.getJiraProjectId(),
                        view.getDeployVersion()
                )));
        new TaskGlue(name, view.getUserId())
                .add(task)
                .addQueueCompleteCondition(task::isTaskComplete)
                .execute();
    }

    // Генерация строки о текущем прохождении автотестов
    private String getAutotestProgress() {
        final List<Task> deployTasks = new ArrayList<>();
        final List<Task> smokeTasks = new ArrayList<>();
        final List<Task> autotestTasks = getAutotestTaskList();
        deployAndTestingExecutors.forEach(tg -> tg.getQueue().forEach(d -> {
            if (d.getDependent() instanceof DefaultFullDeployTask) {
                deployTasks.add(d.getDependent());
            } else if (d.getDependent() instanceof DefaultSmartSmokeTask) {
                smokeTasks.add(d.getDependent());
            }
        }));
        final List<String> taskStatus = new ArrayList<>();
        taskStatus.add("Статус прохождения автотестов");
        if (!deployTasks.stream().allMatch(t -> t.getStatus() == TaskExecutionStatus.SUCCESS)) {
            deployTasks.forEach(task -> taskStatus.add(formatTask(task)));
        }
        if (!smokeTasks.stream().allMatch(t -> t.getStatus() == TaskExecutionStatus.SUCCESS)) {
            smokeTasks.forEach(task ->
                    taskStatus.add(formatTask(task)));
        }
        autotestTasks.forEach(task -> taskStatus.add(formatTask(task)));
        return String.join("\n", taskStatus);
    }

    private Map<String, String> getAutotestJobList() {
        return getAutotestTaskList().stream()
                                    .collect(Collectors.toMap(
                                            Task::getTaskName,
                                            t -> ((IJobTask) t).getBuildUrl(),
                                            (a, b) -> b
                                    ));
    }

    private List<Task> getAutotestTaskList() {
        final List<Task> jobList = new ArrayList<>();
        deployAndTestingExecutors.forEach(tg -> tg.getQueue().forEach(d -> {
                    if (
                            d.getDependent() instanceof DefaultJavaUiParallelAftTask ||
                            d.getDependent() instanceof DefaultJsUiAftTask ||
                            d.getDependent() instanceof DefaultJsFt1UiAftTask
                    ) {
                        jobList.add(d.getDependent());
                    }
                }
        ));
        return jobList;
    }

    private String formatTask(final Task task) {
        return String.format(
                "%s: %s %s %s",
                task.getTaskName(),
                task.getStatus(),
                getJobBuildUrl(task),
                formatTaskTime(task)
        );
    }

    private String getJobBuildUrl(final Task task) {
        if (task.getStatus() == TaskExecutionStatus.NOT_STARTED) {
            return "";
        }
        return String.format("<%s|build>", ((IJobTask) task).getBuildUrl());
    }

    private String formatTaskTime(final Task task) {
        final Duration taskDuration = task.getTaskExecutionDuration();
        if (taskDuration.toMinutes() < 3) {
            return "";
        }
        return String.format("Продолжительность %dч %dм", taskDuration.toHours(), taskDuration.toMinutes() % 60);
    }
}
