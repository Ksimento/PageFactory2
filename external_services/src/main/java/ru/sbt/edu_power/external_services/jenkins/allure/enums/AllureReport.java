package ru.sbt.edu_power.external_services.jenkins.allure.enums;

import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
public class AllureReport {
    private String fullName;
    private String statusMessage;
    private String description;
    private Set<Label> labels = new HashSet<>();
    private Time time;
    private TestStage testStage;

    @Getter
    @Setter
    public static class Label {
        private String name;
        private String value;
    }

    @Getter
    @Setter
    public static class Time {
        private Long duration;
        private Long start;
        private Long stop;
    }

    @Getter
    @Setter
    public static class TestStage {
        private final Set<Step> steps = new LinkedHashSet<>();
    }

    @Getter
    @Setter
    public static class Step {
        private String name;
        private Time time;
        private String status;
        private String statusMessage;
        private String statusTrace;
        private final Set<Attachment> attachments = new HashSet<>();
    }
    @Getter
    @Setter
    public static class Attachment
    {
        private String name;
    }
}
