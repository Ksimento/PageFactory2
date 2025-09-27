package ru.sbt.edu_power.risk_assessment;

import ru.sbt.edu_power.external_services.jira.JiraConnect;

public class Main {
    public static void main(final String[] args) {
        JiraConnect.configureConnection();
        final TestCaseRiskAssessment assessment = new TestCaseRiskAssessment();
        assessment.collect(System.getProperty("jiraProjectKey"));
    }
}
