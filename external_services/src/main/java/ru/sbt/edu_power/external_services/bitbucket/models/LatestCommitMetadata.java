package ru.sbt.edu_power.external_services.bitbucket.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LatestCommitMetadata {
    private String displayId;
    private GitUserModel committer;
    private long committerTimestamp;
    private String message;
}
