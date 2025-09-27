package ru.sbt.edu_power.external_services.jira.test_manager.model;

import lombok.Data;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;
import ru.sbt.edu_power.external_services.jira.test_manager.TMFields;

import java.util.HashMap;
import java.util.Map;

@Data
public class TestCase {
    private final Map<String, String> customFields = new HashMap<>();

    public TestCase() {
    }

    public void addCustomFieldParam(final TMFields name, final String value) {
        customFields.put(name.getFieldName(), value);
    }

    public void addCustomFieldParam(final TCFields name, final String value) {
        customFields.put(name.value, value);
    }
}
