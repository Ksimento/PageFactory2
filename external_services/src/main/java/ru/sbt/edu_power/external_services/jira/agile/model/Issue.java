package ru.sbt.edu_power.external_services.jira.agile.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Класс реализует объект для создания нового issue в JIRA
 */
public class Issue {
    private final Map<String, String> update = new HashMap<>();
    private final Map<String, Object> fields;

    public Issue(final IssueFields fields) {
        this.fields = fields.asMap();
    }

    public Map<String, Object> getFields() {
        return fields;
    }
}
