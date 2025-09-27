package ru.sbt.edu_power.assist_bot.tasks.test_run.regress.tasks;

import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.shared.TaskExecutionStatus;
import ru.sbt.edu_power.assist_bot.runner.Main;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractModal;
import ru.sbt.edu_power.assist_bot.task_flow.AbstractTask;
import ru.sbt.edu_power.assist_bot.task_flow.QueueExecutor;
import ru.sbt.edu_power.assist_bot.task_flow.templates.SlackMessage;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.test_case_repartition.components.ITestRunRepartition;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.test_case_repartition.TestCaseMove;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.test_case_repartition.TestCaseRepartitionView;

// Задача на перемещение тест-кейсов от одного пользователя к другому по данным из TestCaseMove
@Slf4j
public class TestCaseMoveTask extends AbstractTask<AbstractModal> {
    private final TestCaseMove testCaseMove;
    private TaskExecutionStatus status = TaskExecutionStatus.NOT_STARTED;

    public TestCaseMoveTask(
            final AbstractModal modal,
            final TestCaseMove testCaseMove
    ) {
        super(modal);
        this.testCaseMove = testCaseMove;
    }

    public TestCaseMoveTask(
            final QueueExecutor queueExecutor,
            final TestCaseRepartitionView modal,
            final TestCaseMove testCaseMove
    ) {
        super(queueExecutor, modal);
        this.testCaseMove = testCaseMove;
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
        try {
            status = TaskExecutionStatus.IN_PROGRESS;
            testCaseMove.execute();
            status = TaskExecutionStatus.SUCCESS;
        } catch (final Throwable e) {
            status = TaskExecutionStatus.FAILED;
            log.error("{}", e.toString());
        }
    }

    @Override
    public void postExecution() {

    }

    @Override
    public void reExecuteTask() {
        new SlackMessage(
                () -> String.format(
                        "Произошла ошибка при перемещении тест-кейсов от %s к %s, необходимо залезть в логи и разобраться",
                        testCaseMove.getUserFrom().getDisplayName(),
                        testCaseMove.getUserTo().getDisplayName()
                ),
                ((ITestRunRepartition) getModal()).getChatId(),
                Main.QUEUE_EXECUTOR,
                getModal()
        );
        status = TaskExecutionStatus.SUCCESS;
    }

    @Override
    public void throwsException() {

    }

    @Override
    public String getTaskName() {
        return "Перемещение тестов в тест-сете на другого пользователя";
    }
}
