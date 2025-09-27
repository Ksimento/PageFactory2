package ru.sbt.edu_power.external_services.bitbucket.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OutgoingPullRequestMetadataModel {
    private PullRequest pullRequest;

    @Getter
    @Setter
    public static class PullRequest {
        private int id;
        private String title;
        private String state;
        private boolean open;
        private boolean closed;
        private long createdDate;
        private long updatedDate;
        private Author author;
    }

    @Getter
    @Setter
    public static class Author {
        private GitUserModel user;
    }
}
