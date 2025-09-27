package ru.sbt.edu_power.external_services.jira.test_manager.test_run;

import ru.sbt.edu_power.external_services.shared.TaskExecutionStatus;

public interface HasExecutableStatus {
    void updateStatus();
    TaskExecutionStatus getStatus();
}
