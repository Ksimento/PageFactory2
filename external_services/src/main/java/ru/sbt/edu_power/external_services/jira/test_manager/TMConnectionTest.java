package ru.sbt.edu_power.external_services.jira.test_manager;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import ru.sbt.edu_power.external_services.PropReader;
import ru.sbt.edu_power.external_services.jira.JiraConnect;
import ru.sbt.edu_power.external_services.jira.JiraConnectionException;
import ru.sbt.edu_power.external_services.jira.JiraExporterUtils;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.ExecutionStatus;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.Execution;

// Тестирование подключения к Jira
@Slf4j
public class TMConnectionTest {

    public void executionExportPassSuccess() {
        final Execution execution = new Execution(
                ExecutionStatus.AUTO_PASS.getStatusName(),
                "2020-04-01T16:15:12.000Z",
                "2020-04-01T16:16:15.000Z",
                JiraExporterUtils.decodeData(PropReader.get("jira.login")),
                63000,
                null
        );
        final HttpResponse<JsonNode> httpResponsePass = JiraConnect.reportTestCaseToTestSet(
                PropReader.get("jira.testcase.id.example"),
                execution.toString(),
                PropReader.get("jira.testset.id.example")
        );
        Assert.assertTrue("Пробный тест кейс Passed не может быть отправлен", httpResponsePass.isSuccess());
        log.info("Export PASSED data to Jira ... PASSED");
    }

    public void executionExportFailSuccess() {
        final Execution execution = new Execution(
                ExecutionStatus.AUTO_FAIL.getStatusName(),
                "2020-04-01T16:15:12.000Z",
                "2020-04-01T16:16:15.000Z",
                JiraExporterUtils.decodeData(PropReader.get("jira.login")),
                1000,
                null
        );
        final HttpResponse<JsonNode> httpResponsePass = JiraConnect.reportTestCaseToTestSet(
                PropReader.get("jira.testcase.id.example"),
                execution.toString(),
                PropReader.get("jira.testset.id.example")
        );
        Assert.assertTrue("Пробный тест кейс Failed не может быть отправлен", httpResponsePass.isSuccess());
        log.info("Export FAILURE data to Jira ... PASSED");
    }

    public void executionExportFailed() {
        final Execution execution = new Execution(
                ExecutionStatus.AUTO_PASS.getStatusName(),
                "2020-04-01T16:15:12.000Z",
                "2020-04-01T16:16:15.000Z",
                JiraExporterUtils.decodeData(PropReader.get("jira.login")),
                1000,
                null
        );
        final HttpResponse<JsonNode> httpResponsePass = JiraConnect.reportTestCaseToTestSet(
                PropReader.get("jira.testcase.id.example.outside.testset"),
                execution.toString(),
                PropReader.get("jira.testset.id.example")
        );
        Assert.assertFalse("Пробный тест кейс Passed не может быть отправлен", httpResponsePass.isSuccess());
        log.info("Export data to Jira with wrong data ... PASSED");
    }

    public void checkTestSetStatus() {
        final String testSetId = System.getProperty("jira.testset.id");
        final HttpResponse<JsonNode> response = JiraConnect.getTestSet(testSetId, "status");
        if (!response.isSuccess()) {
            if (response.getStatus() == 404) {
                throw new JiraConnectionException(String.format("Тест-сет %s не найден", testSetId));
            }
            if (response.getStatus() == 401) {
                throw new JiraConnectionException("Ошибка авторизации");
            }
        }
        log.info("Test-set in available status ... PASSED");
    }
}
