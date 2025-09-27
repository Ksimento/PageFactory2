package ru.sbt.edu_power.test_manager;

import org.junit.Assert;
import ru.sbt.edu_power.external_services.jira.agile.JiraVersion;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraVersionModel;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.RegressTestRunCreation;
import ru.sbt.edu_power.test_manager.old_branch_notify.BBReposToChannelEnum;

import java.util.Objects;

public class Environment {
    public static final TCFields.ProjectId PROJECT;
    public static final JiraVersionModel VERSION;
    public static final String TEST_RUN_KEY;
    public static final TestManagerProcess TEST_MANAGER_PROCESS;
    public static final RegressTestRunCreation.Preset TEST_RUN_PRESET;
    public static final TCFields.Risk RISK_LEVEL;
    public static final boolean SKIP_RISK_UPDATE;
    public static final BBReposToChannelEnum REPOSITORY;

    static {
        PROJECT = Objects.isNull(System.getProperty("jiraProjectKey")) ||
                  System.getProperty("jiraProjectKey").isEmpty() ? null :
                TCFields.ProjectId.valueOf(System.getProperty("jiraProjectKey"));
        VERSION = Objects.isNull(PROJECT) ||
                  Objects.isNull(System.getProperty("version")) ||
                  System.getProperty("version").isEmpty() ? null :
                new JiraVersion().getJiraVersionByName(PROJECT.id, System.getProperty("version"));

        Assert.assertNotNull("Не указан процесс", System.getProperty("testManagerProcess"));
        TEST_MANAGER_PROCESS = TestManagerProcess.valueOf(System.getProperty("testManagerProcess"));

        TEST_RUN_KEY = System.getProperty("testRunKey");

        TEST_RUN_PRESET = RegressTestRunCreation.Preset.valueOf(System.getProperty(
                "testRunPreset",
                RegressTestRunCreation.Preset.FULL_REGRESS.name()
        ));

        RISK_LEVEL = TCFields.Risk.getByName(System.getProperty("riskLevel"));

        SKIP_RISK_UPDATE = "true".equals(System.getProperty("skipRiskUpdate", "false"));

        REPOSITORY = BBReposToChannelEnum.determine(System.getProperty("repository"));
    }
}
