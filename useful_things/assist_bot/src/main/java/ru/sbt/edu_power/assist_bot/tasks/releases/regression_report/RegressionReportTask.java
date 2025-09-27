package ru.sbt.edu_power.assist_bot.tasks.releases.regression_report;

import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.assist_bot.slack.SlackClient;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractModal;
import ru.sbt.edu_power.assist_bot.task_flow.AbstractTask;
import ru.sbt.edu_power.external_services.shared.TaskExecutionStatus;

import java.util.function.Supplier;

@Slf4j
public class RegressionReportTask extends AbstractTask<AbstractModal> {
    private final RegressionReport regressionReport = new RegressionReport("release_candidates-notify", null);
    private final RegressionReportCollector regressionReportCollector = new RegressionReportCollector();
    private TaskExecutionStatus status = TaskExecutionStatus.NOT_STARTED;
    private boolean firstReportSend;

    protected RegressionReportTask() {
        super(null);
    }

    @Override
    public TaskExecutionStatus getStatus() {
        return status;
    }

    @Override
    public void preExecution() {

    }

    @Override
    public Supplier<Long> getIdleDuration() {
        return () -> getMillisFromMinutes(30);
    }

    @Override
    public void executeTask() {
        try {
            if (status == TaskExecutionStatus.NOT_STARTED) {
                status = TaskExecutionStatus.REPEATABLE;
            }
            regressionReportCollector.updateCollection();
            if (regressionReportCollector.isEmpty()) {
                return;
            }
            regressionReport.send(regressionReportCollector);
            if (!firstReportSend) {
                regressionReport.renew();
                firstReportSend = true;
            }
        } catch (final Throwable e) {
            log.error("", e);
            SlackClient.sendText("Ошибка в работе демона " + getTaskName() + "\n" + e, null);
        }
    }

    @Override
    public void postExecution() {

    }

    @Override
    public void reExecuteTask() {
        executeTask();
    }

    @Override
    public void throwsException() {

    }

    @Override
    public String getTaskName() {
        return "Публикация обобщённого отчёта в #release_candidates-notify";
    }
}
