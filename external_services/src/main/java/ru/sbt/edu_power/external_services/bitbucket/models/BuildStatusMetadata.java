package ru.sbt.edu_power.external_services.bitbucket.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BuildStatusMetadata {
    private int successful;
    private int inProgress;
    private int failed;
}