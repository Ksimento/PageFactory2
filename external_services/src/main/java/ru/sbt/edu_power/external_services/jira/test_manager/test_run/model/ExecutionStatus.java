package ru.sbt.edu_power.external_services.jira.test_manager.test_run.model;

import ru.sbt.edu_power.external_services.jira.JiraConnectionException;

import java.util.stream.Stream;

public enum ExecutionStatus {
    AUTO_PASS("AutoPass"),
    AUTO_FAIL("AutoFail"),
    N_A("N/A"),
    BLOCKED("Blocked"),
    PASS("Pass"),
    IN_PROGRESS("In Progress"),
    FAIL("Fail"),
    NOT_EXECUTED("Not Executed");


    private final String statusName;

    ExecutionStatus(final String statusName) {
        this.statusName = statusName;
    }

    public String getStatusName() {
        return statusName;
    }

    public static ExecutionStatus getStatusByName(final String name) {
        return Stream.of(ExecutionStatus.values())
                .filter(status -> status.statusName.equals(name))
                .findFirst()
                .orElseThrow(() -> new JiraConnectionException("Не известный статус " + name));
    }
}
