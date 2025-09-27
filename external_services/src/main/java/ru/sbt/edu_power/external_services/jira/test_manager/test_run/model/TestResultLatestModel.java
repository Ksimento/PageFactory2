package ru.sbt.edu_power.external_services.jira.test_manager.test_run.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TestResultLatestModel {
    private String executedBy;
    private String actualEndDate;
    private String actualStartDate;
    private String userKey;
    private String assignedTo;
    private String testCaseKey;
    private Boolean automated;
    private Integer id;
    private String key;
    private String status;
    private List<String> issueLinks;
}