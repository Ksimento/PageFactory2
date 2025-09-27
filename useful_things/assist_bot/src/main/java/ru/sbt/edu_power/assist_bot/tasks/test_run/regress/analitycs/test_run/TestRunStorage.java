package ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.test_run;

import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.assist_bot.AssistBotException;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.AnalyticUtils;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.HasExecutableStatus;
import ru.sbt.edu_power.external_services.shared.TaskExecutionStatus;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.Execution;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.TestRunModel;
import ru.sbt.edu_power.assist_bot.services.slice.SliceStorage;

// Класс реализует процесс обновления данных из тест-сета
@Slf4j
public class TestRunStorage extends SliceStorage<Execution, TestRunSlice> implements HasExecutableStatus {
    private static final long serialVersionUID = 4453570607903633094L;
    private final TestRunModel testRunModel;
    private TaskExecutionStatus status = TaskExecutionStatus.NOT_STARTED;

    public TestRunStorage(final TestRunModel testRunModel) {
        this.testRunModel = testRunModel;
    }

    // метод снимает текущее состояние тест-сета и создаёт новый TestRunSlice, который добавляется в список testRunSlices
    @Override
    public void slice() {
        final TestRunSlice testRunSlice = new TestRunSlice(testRunModel.getKey());
        testRunSlice.load();
        if (!isEmpty()) {
            testRunSlice.optimize(getLastSlice());
        }
        testRunSlice.splitExecutions();
//        new CheckExecutionDateTime(
//                testRunModel,
//                testRunSlice
//        ).check();
        add(testRunSlice);
        if (size() == 1) {
            status = TaskExecutionStatus.REPEATABLE;
        }
        if (checkIfTestRunFinished()) {
            status = TaskExecutionStatus.SUCCESS;
        }
    }

    @Override
    public void updateStatus() {
        throw new AssistBotException("Метод не поддерживается");
    }

    @Override
    public TaskExecutionStatus getStatus() {
        return status;
    }

    public TestRunModel getTestRunModel() {
        return testRunModel;
    }

    private boolean checkIfTestRunFinished() {
        return get(size() - 1)
                .getUserToExecutionStatusToExecutionsMap()
                .values()
                .stream()
                .flatMap(m -> m.entrySet().stream())
                .filter(e -> AnalyticUtils.isInNotCompleteStatus(e.getKey()))
                .allMatch(e -> e.getValue().isEmpty());

    }

}
