package ru.sbt.edu_power.external_services.jira.test_manager.test_run.model;

import lombok.Getter;
import lombok.Setter;

// Класс реализует модель данных для jira.api.testrun.last.test.results
// используется только для функциональности удаления тест-кейсов из тест-сета
@Getter
@Setter
public class TestRunResult {
    private int id;
    private LastTestResult lastTestResult;

    @Getter
    @Setter
    public static class LastTestResult {
        // execution id
        private int id;
        private int testCaseId;
    }
}
