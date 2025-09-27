package ru.sbt.edu_power.test_duration_report;

import ru.sbt.edu_power.external_services.jira.JiraConnect;

public class Main {
    public static void main(final String[] args) {
        JiraConnect.configureConnection();
        final String buildUrl = System.getProperty("buildUrl");
        final String projectKey = System.getProperty("projectKey");
        final String pageId = System.getProperty("pageId");
        new TestDurationTable(buildUrl, projectKey, pageId).execute();
    }
}
