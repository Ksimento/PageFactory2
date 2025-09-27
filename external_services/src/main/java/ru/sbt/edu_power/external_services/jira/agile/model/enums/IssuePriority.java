package ru.sbt.edu_power.external_services.jira.agile.model.enums;

import ru.sbt.edu_power.external_services.jira.JiraConnectionException;

import java.util.stream.Stream;

public enum IssuePriority {
    BLOCKER("Blocker"),
    CRITICAL("Critical"),
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low"),
    LOWEST("Lowest"),
    UNDEFINED("Undefined");

    private final String value;

    IssuePriority(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static IssuePriority getByName(final String name) {
        return Stream.of(IssuePriority.values())
                     .filter(v -> v.value.equals(name))
                     .findFirst()
                     .orElse(UNDEFINED);
    }
}
