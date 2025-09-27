package ru.sbt.edu_power.external_services.bitbucket.models;

import ru.sbt.edu_power.external_services.ExternalServicesException;

import java.util.stream.Stream;

// название проектных областей
public enum BBProjects {
    EDUPOWER("EDU"),
    S21("S21"),
    FRS("FRS"),
    ;

    private final String simpleName;

    BBProjects(final String simpleName) {
        this.simpleName = simpleName;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public static BBProjects getBySimpleName(final String simpleName) {
        return Stream.of(BBProjects.values())
                     .filter(e -> simpleName.equals(e.getSimpleName()))
                     .findFirst()
                     .orElseThrow(() -> new ExternalServicesException("Не найдено соответствие для названия " +
                                                                      simpleName));
    }
}
