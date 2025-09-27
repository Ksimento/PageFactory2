package ru.sbt.edu_power.assist_bot.tasks.test_run.regress.tasks;

import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.shared.TaskExecutionStatus;
import ru.sbt.edu_power.confluence_reporting.regress_metrics.MeasureHandleRegressTime;
import ru.sbt.edu_power.confluence_reporting.regress_metrics.MeasureHandleRegressTimeByQaEngineers;
import ru.sbt.edu_power.external_services.jenkins.allure.SuiteCollector;
import ru.sbt.edu_power.external_services.jenkins.allure.SuiteGrabber;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.TestRunModel;
import ru.sbt.edu_power.assist_bot.reporting.ConfluenceRegressReporting;
import ru.sbt.edu_power.assist_bot.slack.SlackClient;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractModal;
import ru.sbt.edu_power.assist_bot.task_flow.AbstractTask;
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces.IHasTestRunModel;

import java.util.Map;
import java.util.function.Supplier;

@Slf4j
public class FinalReportTask extends AbstractTask<AbstractModal> {
    private TaskExecutionStatus status = TaskExecutionStatus.NOT_STARTED;
    private final Supplier<Map<String, String>> autotestJobLinks;

    public FinalReportTask(
            final AbstractModal modal,
            final Supplier<Map<String, String>> autotestJobLinks
    ) {
        super(modal);
        this.autotestJobLinks = autotestJobLinks;
    }

    @Override
    public TaskExecutionStatus getStatus() {
        return status;
    }

    @Override
    public void preExecution() {
        SlackClient.sendText("Выполняется публикация финального отчета", getModal().getUserId());
    }

    @Override
    public void executeTask() {
        final TestRunModel testRunModel = ((IHasTestRunModel) getModal()).getTestRunModel();
        final ConfluenceRegressReporting confluenceRegressReporting = new ConfluenceRegressReporting(testRunModel);
        final String byTeamPage = confluenceRegressReporting.getReportPage("по командам");
        final MeasureHandleRegressTime measureHandleRegressTime =
                new MeasureHandleRegressTime(
                        byTeamPage,
                        testRunModel.getKey()
                );
        final Map<String, String> links = autotestJobLinks.get();
        links.forEach(measureHandleRegressTime::addLink);
        measureHandleRegressTime.generate();
        final String byQaPage = confluenceRegressReporting.getReportPage("по тестировщикам");
        new MeasureHandleRegressTimeByQaEngineers(
                byQaPage,
                testRunModel.getKey()
        ).generate();
        links.forEach((name, link) -> {
            final SuiteGrabber suiteGrabber = new SuiteGrabber();
            suiteGrabber.grab(new SuiteCollector(), link);
        });
        status = TaskExecutionStatus.SUCCESS;
    }

    @Override
    public void postExecution() {
        SlackClient.sendText("Публикация финального отчета выполнена", getModal().getUserId());
    }

    @Override
    public void reExecuteTask() {

    }

    @Override
    public void throwsException() {

    }

    @Override
    public String getTaskName() {
        return "Сбор итогового отчёта о прохождении регресса";
    }
}
