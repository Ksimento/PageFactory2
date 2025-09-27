package ru.sbt.edu_power.external_services.jira.agile.model;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import ru.sbt.edu_power.external_services.PropReader;
import ru.sbt.edu_power.external_services.ExternalServicesException;
import ru.sbt.edu_power.external_services.jira.JiraConnectionException;
import ru.sbt.edu_power.external_services.jira.JiraExporterUtils;
import ru.sbt.edu_power.external_services.jira.agile.model.enums.IssuePriority;
import ru.sbt.edu_power.external_services.jira.agile.model.enums.IssueProject;
import ru.sbt.edu_power.external_services.jira.agile.model.enums.IssueType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Класс реализует генерацию набора полей для создания нового issue в JIRA
 */
public final class IssueFields {
    private final EnumMap<Field, Object> fields = new EnumMap<>(Field.class);

    private IssueFields() {
    }

    public IssueFields(
            final IssueProject project,
            final IssueType issueType,
            final IssuePriority issuePriority,
            final String team,
            final String summary,
            final String description
    ) {
        setField(Field.PROJECT, project.name());
        setField(Field.ISSUE_TYPE, issueType.getValue());
        setField(Field.PRIORITY, issuePriority.getValue());
        setField(Field.TEAM, team);
        setField(Field.SUMMARY, summary);
        setField(Field.DESCRIPTION, description);
        setField(Field.REPORTER, JiraExporterUtils.decodeData(PropReader.get("jira.login")));
    }

    @SuppressWarnings("unchecked")
    public IssueFields setField(final Field field, final Object value) {
        switch (field) {
            case TEAM:
            case ASSIGNEE:
            case REPORTER:
            case PRIORITY:
            case ISSUE_TYPE:
            case PROJECT:
            case STATUS:
            case RESOLUTION:
            case DEVELOPER:
            case RELEASE_OBJECT:
            case RELEASE_TYPE:
                fields.put(field, mapOf(field.fieldParam, (String) value));
                break;
            case LABELS:
                if (!fields.containsKey(field)) {
                    fields.put(field, new HashSet<>());
                }
                ((Set<String>) fields.get(field)).add((String) value);
                break;
            case ISSUELINKS:
                if (!fields.containsKey(field)) {
                    fields.put(field, new HashSet<>());
                }
                ((Set<Object>) fields.get(field)).add(value);
                break;
            case SUMMARY:
            case DESCRIPTION:
            case CREATED:
            case INSTALL_PLANE_DATE:
            case FEATURE_FREEZE:
            case RESOLVED:
            case ID:
            case KEY:
                fields.put(field, value);
                break;
            case FIX_VERSIONS:
                if (!fields.containsKey(field)) {
                    fields.put(field, new HashSet<>());
                }
                ((Set<Map<String, String>>) fields.get(field)).add(mapOf(field.fieldParam, (String) value));
                break;
            default:
                throw new IllegalArgumentException("Необходимо добавить способ заполнения для поля " + field.name());
        }
        return this;
    }

    public static IssueFields of(final String json) {
        final JsonParser parser = new JsonParser();
        final JsonObject obj = parser.parse(json).getAsJsonObject();
        final IssueFields fields = new IssueFields();
        for (final Map.Entry<String, JsonElement> entry : obj.getAsJsonObject("fields").entrySet()) {
            final Field field = Field.from(entry.getKey());
            if (field != null) {
                if (field == Field.ISSUELINKS) {
                    getObjectList(field, entry.getValue()).forEach(v -> fields.setField(field, v));
                } else if (entry.getValue().isJsonArray()) {
                    getValueList(field, entry.getValue()).forEach(value -> fields.setField(field, value));
                } else {
                    fields.setField(field, getValue(field, entry.getValue()));
                }
            }
        }
        return fields;
    }

    private static String getValue(final Field field, final JsonElement element) {
        switch (field) {
            case TEAM:
            case ASSIGNEE:
            case REPORTER:
            case PRIORITY:
            case ISSUE_TYPE:
            case PROJECT:
            case STATUS:
            case RESOLUTION:
            case DEVELOPER:
            case RELEASE_OBJECT:
            case RELEASE_TYPE:
                return (element == null || element.isJsonNull()) ? null : element
                        .getAsJsonObject()
                        .getAsJsonPrimitive(field.fieldParam)
                        .getAsString();
            case SUMMARY:
            case DESCRIPTION:
            case INSTALL_PLANE_DATE:
            case FEATURE_FREEZE:
            case CREATED:
            case RESOLVED:
                return (element == null || element.isJsonNull()) ? null : element.getAsString();
            case FIX_VERSIONS:
            case LABELS:
            case ISSUELINKS:
                throw new JiraConnectionException("Поле является списком " + field.name());
            default:
                throw new IllegalArgumentException("Поле не поддерживается " + field.name());
        }
    }

    private static List<String> getValueList(final Field field, final JsonElement element) {
        final List<String> values = new ArrayList<>();
        switch (field) {
            case LABELS:
                element.getAsJsonArray().forEach(e ->
                        values.add(e.getAsString())
                );
                break;
            case FIX_VERSIONS:
                element.getAsJsonArray().forEach(e ->
                        values.add(e.getAsJsonObject().getAsJsonPrimitive(field.fieldParam).getAsString())
                );
                break;
            default:
                throw new IllegalArgumentException("Поле не поддерживается " + field.name());
        }
        return values;
    }

    private static List<Object> getObjectList(final Field field, final JsonElement element) {
        final List<Object> values = new ArrayList<>();
        final Gson gson = new Gson();
        switch (field) {
            case ISSUELINKS:
                element.getAsJsonArray().forEach(e -> {
                    final IssueLinks issueLink = gson.fromJson(e.getAsJsonObject().toString(), IssueLinks.class);
                    values.add(issueLink);
                });
                break;
            default:
                throw new IllegalArgumentException("Поле не поддерживается " + field.name());
        }
        return values;
    }

    @SuppressWarnings("unchecked")
    public <T> Set<T> getObjectList(final Field field) {
        switch (field) {
            case ISSUELINKS:
            case FIX_VERSIONS:
            case LABELS:
                return (Set<T>) fields.get(field);
            default:
                throw new JiraConnectionException("Поле является списком " + field.name());
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> getValues(final Field field) {
        switch (field) {
            case FIX_VERSIONS:
                final Set<Map<String, String>> data = (Set<Map<String, String>>) fields.get(field);
                return getFixVersions(data);
            default:
                throw new ExternalServicesException("Поле не поддерживается " + field.name());
        }
    }

    @SuppressWarnings("unchecked")
    public String get(final Field field) {
        switch (field) {
            case SUMMARY:
            case DESCRIPTION:
            case CREATED:
            case INSTALL_PLANE_DATE:
            case FEATURE_FREEZE:
            case RESOLVED:
            case ID:
            case KEY:
                return (String) fields.get(field);
            case TEAM:
            case ASSIGNEE:
            case REPORTER:
            case PRIORITY:
            case ISSUE_TYPE:
            case PROJECT:
            case STATUS:
            case RESOLUTION:
            case DEVELOPER:
            case RELEASE_OBJECT:
            case RELEASE_TYPE:
                if (Objects.nonNull(fields.get(field))) {
                    return ((Map<String, String>) fields.get(field)).get(field.getFieldParam());
                } else {
                    return "";
                }
            case LABELS:
            case FIX_VERSIONS:
                return Objects.isNull(fields.get(field)) ? null : String.join(
                        ", ",
                        getFixVersions((Set<Map<String, String>>) fields.get(field))
                );
            default:
                throw new ExternalServicesException("Не реализовано получение данных из поля " + field);
        }
    }

    private List<String> getFixVersions(final Set<Map<String, String>> data) {
        return data.stream()
                .map(m -> m.get(Field.FIX_VERSIONS.fieldParam))
                .collect(Collectors.toList());

    }

    @Getter
    public enum Field {
        KEY("key", "key", ""),
        LABELS("labels", "labels", ""),
        ASSIGNEE("assignee", "assignee", "name"),
        REPORTER("reporter", "reporter", "name"),
        PRIORITY("priority", "priority", "name"),
        FIX_VERSIONS("fixVersions", "fixVersion", "name"),
        ISSUE_TYPE("issuetype", "issuetype", "name"),
        PROJECT("project", "project", "key"),
        DESCRIPTION("description", "description", ""),
        SUMMARY("summary", "summary", ""),
        TEAM("customfield_10201", "Team", "value"),
        STATUS("status", "status", "name"),
        RESOLUTION("resolution", "resolution", "name"),
        CREATED("created", "created", ""),
        ISSUELINKS("issuelinks", "issuelinks", ""),
        DEVELOPER("customfield_10906", "developer", "name"),
        ID("id", "id", ""),
        RESOLVED("resolutiondate", "resolved", ""),
        // Поля в Релиз 2.0
        RELEASE_OBJECT("customfield_10206", "Объект", "value"),
        RELEASE_TYPE("customfield_12107", "Тип релиза", "value"),
        FEATURE_FREEZE("customfield_11801", "Фича-фриз план", ""),
        INSTALL_PLANE_DATE("customfield_11802", "Установка план", ""),
        ;

        private final String fieldName;
        private final String forQuery;
        private final String fieldParam;

        Field(final String fieldName, final String forQuery, final String fieldParam) {
            this.fieldName = fieldName;
            this.forQuery = forQuery;
            this.fieldParam = fieldParam;
        }

        public static Field from(final String fieldName) {
            return Stream.of(Field.values())
                    .filter(field -> fieldName.equals(field.fieldName))
                    .findFirst()
                    .orElse(null);
        }
    }

    public Map<String, Object> asMap() {
        return fields.entrySet()
                .stream()
                .collect(Collectors.toMap(e -> e.getKey().fieldName, Map.Entry::getValue));
    }

    private Map<String, String> mapOf(final String key, final String value) {
        final Map<String, String> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    private Set<Object> setOf(final Object obj) {
        final Set<Object> set = new HashSet<>();
        set.add(obj);
        return set;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final IssueFields that = (IssueFields) o;

        return fields.equals(that.fields);
    }

    @Override
    public int hashCode() {
        return fields.hashCode();
    }
}
