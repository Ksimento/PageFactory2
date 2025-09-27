package ru.sbt.edu_power.external_services.jira.test_manager.model;

import lombok.Getter;
import lombok.Setter;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;

import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
public class TestCaseCreation {
    private String projectKey;
    private String name;
    private String precondition;
    private String objective;
    private String folder;
    private String status;
    private String priority;
    private final Map<String, String> customFields = new HashMap<>();

    public void addCustomFieldParam(final TCFields name, final String value) {
        customFields.put(name.value, value);
    }
}
