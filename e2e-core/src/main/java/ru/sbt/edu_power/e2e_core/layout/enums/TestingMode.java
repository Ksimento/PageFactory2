package ru.sbt.edu_power.e2e_core.layout.enums;

import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.util.stream.Stream;

public enum TestingMode {
    // тестирование оформления, контента, размеров и расположения
    FULL_LAYOUT("верстки"),
    // тестирование только оформления и контента
    DECORATION_ONLY("оформления");

    private final String name;

    TestingMode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static TestingMode determine(final String value) {
        return Stream.of(TestingMode.values())
                .filter(n -> value.equals(n.name))
                .findFirst()
                .orElseThrow(() -> new AutotestError("Режим тестирования не определён по значению " + value));
    }
}
