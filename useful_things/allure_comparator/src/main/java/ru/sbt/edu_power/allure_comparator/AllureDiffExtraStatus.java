package ru.sbt.edu_power.allure_comparator;

import ru.sbt.edu_power.external_services.ExternalServicesException;

import java.util.Arrays;
import java.util.stream.Stream;

public enum AllureDiffExtraStatus {
    PASS(new String[]{"passed"}),
    FAIL(new String[]{"broken", "failed"}),
    SKIP(new String[]{"skipped", "unknown"}),
    UNDEFINED(new String[]{}),
    EQUALS(new String[]{});

    private final String[] statuses;

    AllureDiffExtraStatus(final String[] statuses) {
        this.statuses = statuses;
    }

    public static AllureDiffExtraStatus getByAllureStatus(final String allureStatus) {
        return Stream.of(AllureDiffExtraStatus.values())
                     .filter(s -> Arrays.asList(s.statuses).contains(allureStatus))
                     .findFirst()
                     .orElseThrow(() -> new ExternalServicesException("Неизвестный статус прохождения теста " +
                                                                      allureStatus));
    }
}
