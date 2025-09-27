package ru.sbt.edu_power.assist_bot.tasks.test_run.regress.tasks;

import ru.sbt.edu_power.external_services.shared.TaskExecutionStatus;
import ru.sbt.edu_power.assist_bot.task_flow.AbstractTask;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.RegressStartView;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.test_run.TestCaseRemoveFromTestRun;

public class RemoveUnusedTestCasesTask extends AbstractTask<RegressStartView> {
    private TaskExecutionStatus status = TaskExecutionStatus.NOT_STARTED;

    public RemoveUnusedTestCasesTask(final RegressStartView modal) {
        super(modal);
    }

    @Override
    public TaskExecutionStatus getStatus() {
        return status;
    }

    @Override
    public void preExecution() {

    }

    @Override
    public void executeTask() {
        final TestCaseRemoveFromTestRun testCaseRemoveFromTestRun = new TestCaseRemoveFromTestRun(getModal().getTestRunKey());
        testCaseRemoveFromTestRun.execute();
        status = TaskExecutionStatus.SUCCESS;
    }

    @Override
    public void postExecution() {

    }

    @Override
    public void reExecuteTask() {

    }

    @Override
    public void throwsException() {

    }

    @Override
    public String getTaskName() {
        return "Удаление не запущенных тест-кейсов из тест-сета " + getModal().getTestRunKey();
    }
}
