package ru.sbt.edu_power.external_services.jira.agile.model.enums;

public enum IssueType {
    DEVTASK("DevTask"),
    EPIC("Epic"),
    CHANGE_REQUEST("Change Request"),
    STORY("Story"),
    TASK("Task"),
    INCIDENT("Incident"),
    BUG("Bug"),
    DEVOPS("DevOps"),
    ANALYSIS("Analysis"),
    UX_TASK("UX-task"),
    RISK("Риск"),
    SUB_TASK("Sub-task"),
    RELEASE_2_0("Release 2.0"),
    ;

    private final String value;

    IssueType(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
