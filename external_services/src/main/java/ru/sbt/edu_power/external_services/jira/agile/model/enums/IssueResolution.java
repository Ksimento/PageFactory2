package ru.sbt.edu_power.external_services.jira.agile.model.enums;

import java.util.Objects;
import java.util.stream.Stream;

public enum IssueResolution {
    UNRESOLVED("Unresolved"),
    DONE("Done"),
    WONT_DO("Won't Do"),
    DUPLICATE("Duplicate"),
    ;

    private final String value;

    IssueResolution(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static IssueResolution getByValue(final String value) {
        if (Objects.isNull(value)) {
            return UNRESOLVED;
        }
        return Stream.of(values())
                .filter(v -> value.equals(v.value))
                .findFirst()
                .orElse(UNRESOLVED);
    }
}
