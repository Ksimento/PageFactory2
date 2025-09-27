package ru.sbt.edu_power.external_services.bitbucket.models;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

@Getter
@Setter
public class BranchListJiraIssuesModel extends ArrayList<BranchListJiraIssuesModel.JiraIssue> {
    private static final long serialVersionUID = 5287909092455258578L;

    @Getter
    @Setter
    public static class JiraIssue {
        private String key;
        private String url;
    }
}
