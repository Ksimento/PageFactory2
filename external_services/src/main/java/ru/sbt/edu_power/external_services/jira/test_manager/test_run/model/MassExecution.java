package ru.sbt.edu_power.external_services.jira.test_manager.test_run.model;

import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

// Класс описывает модель данных экзекушена, загружаемых через веб-апи массово (на странице тест-кейса)
@Getter
public class MassExecution {
    private TestResultStatus testResultStatus;
    private final Set<IssueLinks> issueLinks = new HashSet<>();
    private String executionDate;

    @Getter
    public static class TestResultStatus {
        private String name;
    }

    @Getter
    public static class IssueLinks {
        private String issueId;
        private String issueKey;
    }
}
