package ru.sbt.edu_power.external_services.bitbucket.models.pull_request;

import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class LatestCommitPullRequest {
        private String state;
        private String key;
        private String name;
        private String url;
        private String description;
        private long dateAdded;
}