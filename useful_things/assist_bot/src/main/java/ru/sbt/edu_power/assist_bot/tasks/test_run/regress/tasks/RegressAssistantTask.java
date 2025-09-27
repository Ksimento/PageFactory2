package ru.sbt.edu_power.assist_bot.tasks.test_run.regress.tasks;

import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.assist_bot.reporting.ReportType;
import ru.sbt.edu_power.external_services.shared.TaskExecutionStatus;
import ru.sbt.edu_power.assist_bot.reporting.SlackReport;
import ru.sbt.edu_power.assist_bot.slack.views.Modal;
import ru.sbt.edu_power.assist_bot.task_flow.AbstractTask;
import ru.sbt.edu_power.assist_bot.task_flow.Container;
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces.IHasOverridingReportTs;
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces.ISlackChannel;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.test_run.TestRunStorage;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.test_run.TestRunReports;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

@Slf4j
public class RegressAssistantTask extends AbstractTask<Modal> {
    private final Container<TestRunStorage> testRunStorageContainer;
    private final SlackReport slackReport;
    private final TestRunReports testRunReports;

    public RegressAssistantTask(
            final Modal modal,
            final Container<TestRunStorage> testRunStorageContainer,
            final Supplier<String> autotestProgress,
            final AtomicBoolean generateFinalReport
    ) {
        super(modal);
        this.testRunStorageContainer = testRunStorageContainer;
        slackReport = new SlackReport(
                ((ISlackChannel) getModal()).getSlackChannel(),
                Duration.ofHours(3L),
                getModal().getUserId(),
                RegressAssistantTask.getFullTaskName(testRunStorageContainer),
                ((IHasOverridingReportTs) modal).getOverridingReportTs(),
                ReportType.REGRESS,
                false
        );
        testRunReports = new TestRunReports(testRunStorageContainer.getObject(), autotestProgress, generateFinalReport);
    }

    public TestRunStorage getTestRunStorage() {
        return testRunStorageContainer.getObject();
    }

    @Override
    public Supplier<Long> getIdleDuration() {
        return () -> getMillisFromMinutes(30);
    }

    @Override
    public TaskExecutionStatus getStatus() {
        return testRunStorageContainer.getObject().getStatus();
    }

    @Override
    public void preExecution() {

    }

    @Override
    public void executeTask() {
        testRunStorageContainer.getObject().slice();
        slackReport.updateReport(
                testRunReports.getFullReport(),
                testRunReports.getReportColor()
        );
        if (getStatus() == TaskExecutionStatus.SUCCESS) {
            slackReport.setActive(false);
        }
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
        return RegressAssistantTask.getFullTaskName(testRunStorageContainer);
    }

    private static String getFullTaskName(final Container<TestRunStorage> testRunAnalyticContainer) {
        return String.format(
                "Ассистент регресса %s ver:%s",
                testRunAnalyticContainer.getObject().getTestRunModel().getKey(),
                testRunAnalyticContainer.getObject().getTestRunModel().getJiraVersionModel().getName()
        );
    }
}
