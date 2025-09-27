package ru.sbt.edu_power.assist_bot.slack;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Replacement {
    private final String key;
    private final String value;

    public Replacement(final String key, final Integer value) {
        this.key = key;
        this.value = value.toString();
    }
}
