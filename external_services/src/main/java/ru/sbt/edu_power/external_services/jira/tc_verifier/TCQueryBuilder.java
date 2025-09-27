package ru.sbt.edu_power.external_services.jira.tc_verifier;

import ru.sbt.edu_power.external_services.jira.JiraConnectionException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TCQueryBuilder {
    private final Map<String, Object> mainFields = new HashMap<>();
    private final Map<String, Object> query = new HashMap<>();

    public TCQueryBuilder() {
    }

    public TCQueryBuilder addField(final TCFields field, final Object... values) {
        final Object value;
        switch (field) {
            case ARCHIVED:
                value = values[0];
                break;
            case FOLDER:
                value = Stream.of(values).map(String::valueOf).collect(Collectors.joining(", "));
                break;
            case AUTOMATED_STATUS:
                value = Stream.of(values)
                              .filter(Objects::nonNull)
                              .map(v -> ((TCFields.AutomatedStatus) v).value)
                              .map(this::quoteValue)
                              .collect(Collectors.joining(", "));
                break;
            case FRAMEWORK:
                value = Stream.of(values)
                              .filter(Objects::nonNull)
                              .map(v -> ((TCFields.Framework) v).value)
                              .map(this::quoteValue)
                              .collect(Collectors.joining(", "));
                break;
            case TEST_TYPE:
                value = Stream.of(values)
                              .filter(Objects::nonNull)
                              .map(v -> ((TCFields.TestType) v).value)
                              .map(this::quoteValue)
                              .collect(Collectors.joining(", "));
                break;
            case TEST_VIEW:
                value = Stream.of(values)
                              .filter(Objects::nonNull)
                              .map(v -> ((TCFields.TestView) v).value)
                              .map(this::quoteValue)
                              .collect(Collectors.joining(", "));
                break;
            case PROJECT_ID:
                if(values[0] instanceof Integer){
                    value = values[0];
                }
                else {
                    value = Stream.of(values)
                                  .map(v -> ((TCFields.ProjectId) v).id)
                                  .map(String::valueOf)
                                  .collect(Collectors.joining(", "));
                }
                break;
            case STATUS:
                value = Stream.of(values)
                              .map(v -> ((TCFields.Status) v).value)
                              .map(this::singleQuoteValue)
                              .collect(Collectors.joining(", "));
                break;
            case TEAM:
                value = Stream.of(values)
                              .filter(Objects::nonNull)
                              .map(Object::toString)
                              .map(this::quoteValue)
                              .collect(Collectors.joining(", "));
                break;
            case PRIORITY:
                value = Stream.of(values)
                              .map(v -> ((TCFields.Priority) v).value)
                              .map(this::singleQuoteValue)
                              .collect(Collectors.joining(", "));
                break;
            case RISK:
                value = Stream.of(values)
                              .map(v -> ((TCFields.Risk) v).value)
                              .map(this::quoteValue)
                              .collect(Collectors.joining(", "));
                break;
            case LABELS:
                value = Stream.of(values)
                              .map(Object::toString)
                              .map(this::singleQuoteValue)
                              .collect(Collectors.joining(", "));
                break;
            case KEY_NAME:
                value = Stream.of(values)
                              .map(Object::toString)
                              .map(this::quoteValue)
                              .collect(Collectors.joining(", "));
                break;
            default:
                throw new JiraConnectionException("Не поддерживается выполнение запроса по полю " + field.name());
        }
        if (value instanceof String && ((String) value).isEmpty()) {
            return this;
        }
        switch (field) {
            case ARCHIVED:
                mainFields.put(field.value, value);
                break;
            case STATUS:
            case FOLDER:
                if (query.containsKey(field.filterName)) {
                    query.put(field.filterName, query.get(field.filterName) + ", " + value);
                } else {
                    query.put(field.filterName, value);
                }
                break;
            default:
                final String preparedValue = field.isCustom ? quoteValue(field.filterName) : field.filterName;
                if (query.containsKey(preparedValue)) {
                    query.put(preparedValue, query.get(preparedValue) + ", " + value);
                } else {
                    query.put(preparedValue, value);
                }
                break;
        }
        return this;
    }

    public TCQueryBuilder addMainField(final String key, final Object value) {
        mainFields.put(key, value);
        return this;
    }

    public Map<String, Object> asQueryMap() {
        return query;
    }

    public Map<String, Object> asQuery() {
        final Map<String, Object> queryMap = new HashMap<>(mainFields);
        final List<String> params = new ArrayList<>();
        query.forEach((k, v) -> {
                    if (k.equals(TCFields.KEY_NAME.filterName)) {
                        params.add("testCase.".concat(k).concat(" LIKE ").concat(v.toString()));
                    } else {
                        params.add("testCase.".concat(k).concat(" IN (").concat(v.toString()).concat(")"));
                    }
                }
        );
        queryMap.put("query", String.join(" AND ", params));
        return queryMap;
    }

    private String quoteValue(final String value) {
        return "\"" + value + "\"";
    }

    private String singleQuoteValue(final String value) {
        return "'" + value + "'";
    }
}
