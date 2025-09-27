package ru.sbt.edu_power.external_services.bitbucket.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GitUserModel {
    private String name;
    private String emailAddress;
    private String displayName;
    private boolean active;
}
