package ru.sbt.edu_power.assist_bot.task_flow.templates;

import ru.sbt.edu_power.assist_bot.slack.SlackClient;
import ru.sbt.edu_power.assist_bot.slack.views.Modal;
import ru.sbt.edu_power.assist_bot.task_flow.AbstractTask;
import ru.sbt.edu_power.assist_bot.task_flow.QueueExecutor;
import ru.sbt.edu_power.external_services.shared.TaskExecutionStatus;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class SlackMessage extends AbstractTask<Modal> {
    private int attempts = 10;
    private TaskExecutionStatus status = TaskExecutionStatus.NOT_STARTED;
    private final Supplier<String> message;
    private final String chatId;

    public SlackMessage(
            final Supplier<String> message, final String chatId, final Modal modal
    ) {
        super(modal);
        this.message = message;
        this.chatId = chatId;
    }

    public SlackMessage(
            final Supplier<String> message, final String chatId, final QueueExecutor queueExecutor,
            final Modal modal
    ) {
        super(queueExecutor, modal);
        this.message = message;
        this.chatId = chatId;
    }

    public SlackMessage(
            final Supplier<String> message, final String chatId, final QueueExecutor queueExecutor,
            final Modal modal,
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(queueExecutor, modal, isAndCondition, constructConditions);
        this.message = message;
        this.chatId = chatId;
    }

    @Override
    public TaskExecutionStatus getStatus() {
        return status;
    }

    @Override
    public void preExecution() {
        // do nothing
    }

    @Override
    public void executeTask() {
        if (SlackClient.sendText(message.get(), chatId)) {
            status = TaskExecutionStatus.SUCCESS;
        } else {
            status = TaskExecutionStatus.FAILED;
        }
    }

    @Override
    public void postExecution() {
        // do nothing
    }

    @Override
    public void reExecuteTask() {
        executeTask();
    }

    @Override
    public void decrementAttempts() {
        attempts--;
    }

    @Override
    public boolean hasAttempts() {
        return attempts > 0;
    }

    @Override
    public void throwsException() {

    }

    @Override
    public String getTaskName() {
        return "Отправка сообщения: " + message.get();
    }
}
