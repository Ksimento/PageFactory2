package ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces;

import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.TestRunModel;

public interface IHasTestRunModel {
    TestRunModel getTestRunModel();
}
