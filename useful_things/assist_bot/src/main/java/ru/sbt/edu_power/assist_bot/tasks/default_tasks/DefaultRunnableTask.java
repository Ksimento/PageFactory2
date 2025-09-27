package ru.sbt.edu_power.assist_bot.tasks.default_tasks;

import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.shared.TaskExecutionStatus;
import ru.sbt.edu_power.assist_bot.slack.SlackClient;
import ru.sbt.edu_power.assist_bot.slack.views.Modal;
import ru.sbt.edu_power.assist_bot.task_flow.AbstractTask;
import ru.sbt.edu_power.assist_bot.task_flow.QueueExecutor;
import ru.sbt.edu_power.assist_bot.task_flow.Reset;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

// Шаблон для выполнения произвольного кода через очередь задач переданного через лямбду
@Slf4j
public class DefaultRunnableTask extends AbstractTask<Modal> implements Reset {
    private final Supplier<TaskExecutionStatus> executionFunction;
    private TaskExecutionStatus status = TaskExecutionStatus.NOT_STARTED;
    private final String taskName;

    public DefaultRunnableTask(
            final String taskName,
            final Supplier<TaskExecutionStatus> executionFunction,
            final Modal modal
    ) {
        super(modal);
        this.executionFunction = executionFunction;
        this.taskName = taskName;
    }

    public DefaultRunnableTask(
            final String taskName,
            final Supplier<TaskExecutionStatus> executionFunction,
            final QueueExecutor queueExecutor,
            final Modal modal
    ) {
        super(queueExecutor, modal);
        this.executionFunction = executionFunction;
        this.taskName = taskName;
    }

    public DefaultRunnableTask(
            final String taskName,
            final Supplier<TaskExecutionStatus> executionFunction,
            final QueueExecutor queueExecutor,
            final Modal modal,
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(queueExecutor, modal, isAndCondition, constructConditions);
        this.executionFunction = executionFunction;
        this.taskName = taskName;
    }

    @Override
    public TaskExecutionStatus getStatus() {
        return status;
    }

    @Override
    public void setStatus(final TaskExecutionStatus status) {
        this.status = status;
    }

    @Override
    public void preExecution() {
        if (Objects.nonNull(getModal())) {
            SlackClient.sendText("Выполнен запуск задания " + taskName, getModal().getUserId());
        } else {
            log.error(
                    "Нельзя выполнить отправку сообщения, так как в задание '{}' не передан параметр modal",
                    taskName
            );
        }
    }

    @Override
    public void executeTask() {
        status = executionFunction.get();
    }

    @Override
    public void postExecution() {
        if (Objects.nonNull(getModal())) {
            SlackClient.sendText("Задание завершено " + taskName, getModal().getUserId());
        } else {
            log.error(
                    "Нельзя выполнить отправку сообщения, так как в задание '{}' не передан параметр modal",
                    taskName
            );
        }
    }

    @Override
    public void reExecuteTask() {
        reset();
        executeTask();
    }

    @Override
    public void throwsException() {

    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    @Override
    public void reset() {
        log.info("Выполняется сброс состояния задачи {}", getTaskName());
        status = TaskExecutionStatus.NOT_STARTED;
    }
}
