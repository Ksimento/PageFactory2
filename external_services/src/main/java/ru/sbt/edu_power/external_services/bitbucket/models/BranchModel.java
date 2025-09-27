package ru.sbt.edu_power.external_services.bitbucket.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

// модель данных для парсинга списка веток
@Getter
@Setter
public class BranchModel {
    private String id;
    private String displayId;
    private String latestCommit;
    private Metadata metadata;

    @Override
    public String toString() {
        final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }

    @Getter
    @Setter
    public static class Metadata {
        @SerializedName(value = "com.atlassian.bitbucket.server.bitbucket-jira:branch-list-jira-issues")
        private BranchListJiraIssuesModel branchListJiraIssuesModel;
        @SerializedName(value = "com.atlassian.bitbucket.server.bitbucket-branch:latest-commit-metadata")
        private LatestCommitMetadata latestCommitMetadata;
        @SerializedName(value = "com.atlassian.bitbucket.server.bitbucket-ref-metadata:outgoing-pull-request-metadata")
        private OutgoingPullRequestMetadataModel outgoingPullRequestMetadataModel;
        @SerializedName(value = "com.atlassian.bitbucket.server.bitbucket-build:build-status-metadata")
        private BuildStatusMetadata buildStatusMetadata;
    }
}
