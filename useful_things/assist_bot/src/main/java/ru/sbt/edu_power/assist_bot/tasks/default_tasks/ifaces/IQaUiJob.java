package ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces;

import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;

public interface IQaUiJob {
    String getFrontendBranch();
    TCFields.ProjectId getJiraProjectId();
    boolean getSlackNotify();
    String getTestRunKey();
    boolean getHackSql();
}
