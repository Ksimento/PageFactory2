package ru.sbt.edu_power.external_services.jira.agile.model.enums;

import java.util.stream.Stream;

public enum IssueStatus {
    NEED_TEST("NeedTest"),
    IN_QA("in QA"),
    IFT("IFT"),
    CANCELLED("Cancelled"),
    OPEN("Open"),
    REOPENED("Reopened"),
    IN_PROGRESS("IN PROGRESS"),
    IN_REVIEW("in Review"),
    NEED_MERGE("Need Merge"),
    RESOLVED("Resolved"),
    CANCELLED_1("Отмена"),
    INTEGRATION_TESTING("Интеграционное тестирование"),
    RELEASE_INSTALLED("Релиз установлен"),
    UNDEFINED("undefined");

    private final String value;

    IssueStatus(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static IssueStatus getByValue(final String value) {
        return Stream.of(values())
                     .filter(v -> value.equals(v.value))
                     .findFirst()
                     .orElse(UNDEFINED);
    }
}
