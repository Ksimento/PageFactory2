package ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces;

import ru.sbt.edu_power.external_services.jira.agile.model.JiraVersionModel;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;

public interface IHasProjectAndVersions {
    TCFields.ProjectId getJiraProjectId();
    JiraVersionModel getDeployVersion();
    JiraVersionModel getTestRunVersion();
}
