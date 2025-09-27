package ru.sbt.edu_power.assist_bot.task_flow;

import lombok.Getter;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
public class QueueInfo {
    private final String name;
    private final LocalDateTime start = LocalDateTime.now(ZoneId.of("UTC"));

    public QueueInfo(final String name) {
        this.name = name;
    }
}
