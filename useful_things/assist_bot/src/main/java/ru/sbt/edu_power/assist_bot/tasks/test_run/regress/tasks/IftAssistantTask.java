package ru.sbt.edu_power.assist_bot.tasks.test_run.regress.tasks;

import ru.sbt.edu_power.assist_bot.reporting.ReportType;
import ru.sbt.edu_power.external_services.shared.TaskExecutionStatus;
import ru.sbt.edu_power.assist_bot.reporting.SlackReport;
import ru.sbt.edu_power.assist_bot.slack.views.Modal;
import ru.sbt.edu_power.assist_bot.task_flow.AbstractTask;
import ru.sbt.edu_power.assist_bot.task_flow.Container;
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces.ISlackChannel;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.issue.IftReport;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.issue.IssueStorage;

import java.time.Duration;
import java.util.function.Supplier;

public class IftAssistantTask extends AbstractTask<Modal> {
    private final SlackReport slackReport;
    private final Container<IssueStorage> issueAnalyticContainer;
    private final IftReport iftReport;
    private final String reportName;

    public IftAssistantTask(final Modal modal, final String reportName, final Container<IssueStorage> issueAnalyticContainer) {
        super(modal);
        this.reportName = reportName;
        slackReport = new SlackReport(
                ((ISlackChannel) getModal()).getSlackChannel(),
                Duration.ofHours(3L),
                getModal().getUserId(),
                reportName,
                null,
                ReportType.IFT,
                false
        );
        this.issueAnalyticContainer = issueAnalyticContainer;
        iftReport = new IftReport();
    }

    @Override
    public Supplier<Long> getIdleDuration() {
        return () -> getMillisFromMinutes(30);
    }

    @Override
    public TaskExecutionStatus getStatus() {
        return issueAnalyticContainer.getObject().getStatus();
    }

    @Override
    public void preExecution() {

    }

    @Override
    public void executeTask() {
        issueAnalyticContainer.getObject().slice();
        slackReport.updateReport(iftReport.getReport(issueAnalyticContainer.getObject().getLastSlice()));
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
        return reportName;
    }
}
