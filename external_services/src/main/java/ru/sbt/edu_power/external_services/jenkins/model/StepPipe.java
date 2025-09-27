package ru.sbt.edu_power.external_services.jenkins.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class StepPipe {
    private String displayName;
    private String result;
    private String state;
}
