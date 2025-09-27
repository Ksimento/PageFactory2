package ru.sbt.edu_power.external_services.jira.agile.model;

import lombok.Getter;

@Getter
public class IssueLinks {
    private String id;
    private OutwardIssue outwardIssue;
    private OutwardIssue inwardIssue;

    @Getter
    public static class OutwardIssue {
        private String key;
        private Fields fields;

    }

    @Getter
    public static class Fields {
        private IssueType issuetype;
    }

    @Getter
    public static class IssueType {
        private String name;
    }
}
