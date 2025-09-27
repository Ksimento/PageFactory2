package ru.sbt.edu_power.test_manager.regress_report;

import org.junit.Assert;
import ru.sbt.edu_power.confluence_reporting.regress_metrics.MeasureHandleRegressTime;
import ru.sbt.edu_power.confluence_reporting.regress_metrics.MeasureHandleRegressTimeByQaEngineers;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.TestRunSearch;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.TestRunModel;
import ru.sbt.edu_power.test_manager.Environment;
import ru.sbt.edu_power.test_manager.TestManagerException;

public class RegressReport  {

    public void report() {
        Assert.assertNotNull("Не указан тест-сет", Environment.TEST_RUN_KEY);
        Assert.assertFalse("Не указан тест-сет", Environment.TEST_RUN_KEY.isEmpty());
        final TestRunSearch testRunSearch = new TestRunSearch(Environment.TEST_RUN_KEY);
        final TestRunModel testRunModel = testRunSearch
                .getTestRunByKey()
                .orElseThrow(() -> new TestManagerException("Не удалось найти тест-сет " + Environment.TEST_RUN_KEY));
        final ConfluenceRegressReporting confluenceRegressReporting = new ConfluenceRegressReporting(testRunModel);
        final String byTeamPage = confluenceRegressReporting.getReportPage("по командам");
        final MeasureHandleRegressTime measureHandleRegressTime =
                new MeasureHandleRegressTime(
                        byTeamPage,
                        testRunModel.getKey()
                );
        measureHandleRegressTime.generate();

        final String byQaPage = confluenceRegressReporting.getReportPage("по тестировщикам");
        new MeasureHandleRegressTimeByQaEngineers(
                byQaPage,
                testRunModel.getKey()
        ).generate();
    }
}
