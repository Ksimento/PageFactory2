package ru.sbt.edu_power.assist_bot.reporting;

import ru.sbt.edu_power.external_services.shared.TaskExecutionStatus;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractModal;
import ru.sbt.edu_power.assist_bot.task_flow.AbstractTask;

import java.time.Duration;
import java.util.function.Supplier;

public class SlackReportRenewingTask extends AbstractTask<AbstractModal> {
    private final Duration renewDuration;
    private final SlackReport slackReport;
    private TaskExecutionStatus status = TaskExecutionStatus.NOT_STARTED;

    public SlackReportRenewingTask(
            final AbstractModal modal,
            final Duration renewDuration,
            final SlackReport slackReport
    ) {
        super(modal);
        this.renewDuration = renewDuration;
        this.slackReport = slackReport;
    }

    @Override
    public Supplier<Long> getIdleDuration() {
        return renewDuration::toMillis;
    }

    @Override
    public TaskExecutionStatus getStatus() {
        if (!slackReport.isActive()) {
            status = TaskExecutionStatus.SUCCESS;
        }
        return status;
    }

    @Override
    public void preExecution() {

    }

    @Override
    public void executeTask() {
        slackReport.renew();
        status = TaskExecutionStatus.REPEATABLE;
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
        return "Переотправка репорта по таймеру";
    }
}
