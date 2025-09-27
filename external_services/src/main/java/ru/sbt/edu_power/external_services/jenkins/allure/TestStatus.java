package ru.sbt.edu_power.external_services.jenkins.allure;

import java.util.stream.Stream;

public enum TestStatus {
    PASSED,
    SKIPPED,
    FAILED,
    BROKEN,
    UNKNOWN;

    public static TestStatus determine(final String status) {
        return Stream.of(TestStatus.values())
                .filter(s -> status.equalsIgnoreCase(s.name()))
                .findFirst()
                .orElse(UNKNOWN);
    }
}
