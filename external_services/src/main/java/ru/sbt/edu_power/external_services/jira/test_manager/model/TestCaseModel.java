package ru.sbt.edu_power.external_services.jira.test_manager.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.ExternalServicesException;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.TestResults;

import java.util.Arrays;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@NoArgsConstructor
@Setter
@Slf4j
public class TestCaseModel {
    private Date createdOn;
    private Boolean archived;
    private Set<IssueLinks> issueLinks = new HashSet<>();
    private Integer id;
    private String key;
    private String owner;
    private String objective;
    private Set<ConfluencePageLinks> confluencePageLinks = new HashSet<>();
    private Priority priority;
    private Set<String> labels = new HashSet<>();
    private TestCaseModelFolder folder;
    private Set<CustomFieldValues> customFieldValues = new HashSet<>();
    private TestScript testScript;
    private String name;
    private Status status;
    private Integer projectId;
    private Integer estimatedTime;
    private TestResults testResults;
    // имитированное поле, в исходной модели его нет, сюда записываем значения кастомных полей, конвертированные из ID
    private EnumMap<TCFields, Object> customFields = new EnumMap<>(TCFields.class);

    public void update(final TCFields field, final Object value) {
        switch (field) {
            case TEAM:
            case NOT_AUTOMATED_REASON:
            case AUTOMATOR:
            case TEST_TYPE:
            case TEST_VIEW:
            case FRAMEWORK:
            case AUTOMATED_STATUS:
            case RISK:
                putCustomField(field, value);
                break;
            default:
                throw new ExternalServicesException("Не поддерживается обновление поля " + field.name());
        }
    }

    public void putCustomFields(final EnumMap<TCFields, Object> value) {
        customFields.putAll(value);
    }

    public void putCustomField(final TCFields field, final Object value) {
        customFields.put(field, value);
    }

    public TCFields.AutomatedStatus getAutomatedStatus() {
        return (TCFields.AutomatedStatus) customFields.get(TCFields.AUTOMATED_STATUS);
    }

    public TCFields.TestView getTestView() {
        return (TCFields.TestView) customFields.get(TCFields.TEST_VIEW);
    }

    public TCFields.TestType getTestType() {
        return (TCFields.TestType) customFields.get(TCFields.TEST_TYPE);
    }

    public TCFields.Risk getRisk() {
        return (TCFields.Risk) customFields.get(TCFields.RISK);
    }

    @SuppressWarnings("unchecked")
    public List<TCFields.Framework> getFramework() {
        return (List<TCFields.Framework>) customFields.get(TCFields.FRAMEWORK);
    }

    public boolean getHandleRiskManagement() {
        final Object value = customFields.get(TCFields.HANDLE_RISK_MANAGEMENT);
        return !Objects.isNull(value) && (boolean) value;
    }

    public String getTeam() {
        String team = (String)customFields.get(TCFields.TEAM);
        if(Objects.isNull(team)) {
            log.info(String.format("Необходимо проверить тест-кейс \"%s\", возможно изменена команда на несуществующую",
                    key));
        }
        return team;
    }

    public String getAutomator() {
        return (String) customFields.get(TCFields.AUTOMATOR);
    }

    public String getNotAutomatedReason() {
        return (String) customFields.get(TCFields.NOT_AUTOMATED_REASON);
    }

    public TCFields.Layout getLayout(final TCFields layoutName) {
        final List<TCFields> layoutFields = Arrays.asList(
                TCFields.LAYOUT_DESKTOP,
                TCFields.LAYOUT_MOBILE_PORTRAIT,
                TCFields.LAYOUT_TABLET_LANDSCAPE,
                TCFields.LAYOUT_TABLET_PORTRAIT
        );
        if (!layoutFields.contains(layoutName)) {
            throw new ExternalServicesException("Метод не поддерживает поле " + layoutName.name());
        }
        return (TCFields.Layout) customFields.get(layoutName);
    }

    @NoArgsConstructor
    @Getter
    @Setter
    public static class IssueLinks {
        private String issueId;
        private String issueKey; // искусственное поле для сохранения человеческих ID прилиникованных дефектов
    }

    @NoArgsConstructor
    @Getter
    @Setter
    public static class ConfluencePageLinks {
        private String pageId;
    }

    @NoArgsConstructor
    @Getter
    @Setter
    public static class Priority {
        private String name;
    }

    @NoArgsConstructor
    @Getter
    @Setter
    public static class CustomFieldValues {
        private String stringValue;
        private Boolean booleanValue;
        private Integer intValue;
        private Integer customFieldId;
        private CustomField customField;

        @NoArgsConstructor
        @Getter
        @Setter
        public static class CustomField {
            private String name;
            private Set<Option> options = new HashSet<>();
        }

        @Getter
        @Setter
        public static class Option {
            private String name;
            private Integer id;
        }

    }

    @NoArgsConstructor
    @Getter
    @Setter
    public static class TestScript {
        private Set<Steps> steps = new HashSet<>();
        private StepByStepScript stepByStepScript;

        @NoArgsConstructor
        @Getter
        @Setter
        public static class StepByStepScript {
            private Integer id;
            private Set<Steps> steps;
        }

        @NoArgsConstructor
        @Getter
        @Setter
        public static class Steps {
            private String description;
        }
    }

    @NoArgsConstructor
    @Getter
    @Setter
    public static class Status {
        private String name;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public Boolean getArchived() {
        return archived;
    }

    public Set<IssueLinks> getIssueLinks() {
        return issueLinks;
    }

    public Integer getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getOwner() {
        return owner;
    }

    public String getObjective() {
        return objective;
    }

    public Set<ConfluencePageLinks> getConfluencePageLinks() {
        return confluencePageLinks;
    }

    public Priority getPriority() {
        return priority;
    }

    public Set<String> getLabels() {
        return labels;
    }

    public TestCaseModelFolder getFolder() {
        return folder;
    }

    public TestScript getTestScript() {
        return testScript;
    }

    public String getName() {
        return name;
    }

    public Status getStatus() {
        return status;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public Set<CustomFieldValues> getCustomFieldValues() {
        return customFieldValues;
    }

    public TestResults getTestResults() {
        return testResults;
    }

    public Integer getEstimatedTime() {
        return estimatedTime;
    }
}
