package ru.sbt.edu_power.external_services.jira.test_manager;

import java.util.Arrays;

public enum TMFields {
    STATUS("status", false, false, null),
    PRIORITY("priority", false, false, null),
    OWNER("owner", false, false, null),
    FOLDER("folder", false, false, null),
    LABELS("labels", false, true, ","),
    AUTOMATED("Автоматизирован", true, false, null),
    NOT_AUTOMATE_REQUIRED_REASON("Причина невозможности автоматизации", true, false, null),
    AUTOMATOR("Автоматизатор", true, false, null),
    TEAM("Team", true, false, null),
    TEST_TYPE("Тип теста", true, false, null),
    TEST_VIEW("Вид теста", true, false, null),
    FRAMEWORK("Framework", true, false, null),
    ISSUE_LINKS("issueLinks", false, true, ","),
    TEST_SCRIPT("testScript", false, false, null),
    COMPONENT("component", false, false, null),
    CREATED_ON("createdOn", false, false, null),
    RISK("Риск", true, false, null),
    ID("id", false, false, null);

    private final String fieldName;
    // Для дополнительных (самостоятельно добавленных) полей
    private final boolean isCustomField;
    // Представляет ли поле из себя список раздельных значений
    private final boolean isMultiple;
    // Разделитель множественных значений, передаваемых одной строкой
    private final String separator;

    TMFields(final String fieldName, final boolean isCustomField, final boolean isMultiple, final String separator) {
        this.fieldName = fieldName;
        this.isCustomField = isCustomField;
        this.isMultiple = isMultiple;
        this.separator = separator;
    }

    public String getFieldName() {
        return fieldName;
    }

    public boolean isMultiple() {
        return isMultiple;
    }

    public String getSeparator() {
        return separator;
    }

    public boolean isCustomField() {
        return isCustomField;
    }

    public static TMFields getByFieldName(final String fieldName) {
        for (final TMFields enumField : TMFields.values()) {
            if (fieldName.equalsIgnoreCase(enumField.getFieldName())) {
                return enumField;
            }
        }
        throw new IllegalArgumentException("Enum не определён для поля " + fieldName);
    }

    public enum AutomatedStatus {
        NOT_REQUIRED("0 - Не требуется"),
        AUTOMATED("1 - Да"),
        NOT_AUTOMATE("2 - Нет"),
        ON_AUTOMATE("3 - На автоматизации"),
        EXCLUDED("4 - Отключен");

        private final String status;

        AutomatedStatus(final String status) {
            this.status = status;
        }

        public String getStatus() {
            return status;
        }
    }

    public enum AutomationFramework {
        EMPTY("Empty"),
        SELENIUM("Selenium"),
        JS("JS"),
        SELENIUM_JS("Selenium+JS");

        private final String framework;

        AutomationFramework(final String framework) {
            this.framework = framework;
        }

        public String getFramework() {
            return framework;
        }

        public static AutomationFramework decode(final String framework) {
            return Arrays
                    .stream(AutomationFramework.values())
                    .filter(name -> framework.equalsIgnoreCase(name.framework))
                    .findFirst()
                    .orElse(null);
        }
    }

    public enum Status {
        DRAFT("Draft"),
        DEPRECATED("Deprecated"),
        APPROVED("Approved"),
        DISABLED("Disabled");

        private final String statusName;

        Status(final String statusName) {
            this.statusName = statusName;
        }

        public String getStatusName() {
            return statusName;
        }

        public static Status getByStatusName(final String statusName) {
            for (final Status status : Status.values()) {
                if (statusName.equalsIgnoreCase(status.getStatusName())) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Enum не определён для значения " + statusName);
        }
    }

    public enum Priority {
        HIGH("High"),
        MEDIUM("Medium"),
        LOW("Low");

        private final String priorityName;

        Priority(final String priorityName) {
            this.priorityName = priorityName;
        }

        public String getPriorityName() {
            return priorityName;
        }

        public static Priority getByPriorityName(final String priorityName) {
            for (final Priority priority : Priority.values()) {
                if (priorityName.equalsIgnoreCase(priority.getPriorityName())) {
                    return priority;
                }
            }
            throw new IllegalArgumentException("Enum не определён для значения " + priorityName);
        }
    }
}
