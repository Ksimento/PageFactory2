package ru.sbt.edu_power.external_services.jira.tc_verifier;

import ru.sbt.edu_power.external_services.jira.JiraConnectionException;
import ru.sbt.edu_power.external_services.jira.test_manager.model.TestCaseModel;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class TCFilter {
    private final Map<String, TestCaseModel> map;

    private TCFilter(final Map<String, TestCaseModel> map) {
        this.map = new ConcurrentHashMap<>(map);
    }

    public static TCFilter of(final Map<String, TestCaseModel> map) {
        return new TCFilter(new HashMap<>(map));
    }

    public TCFilter filter(final Function<TestCaseModel, Boolean> predicate) {
        new ArrayList<>(map.keySet()).stream()
                                     .filter(c -> !predicate.apply(map.get(c)))
                                     .forEach(map::remove);
        return this;
    }

    public TCFilter filterByDate(
            final LocalDateTime expected,
            final Function<TestCaseModel, Date> dateField,
            final boolean isActualBeforeExpected
    ) {
        final ZoneId moscowZoneId = ZoneId.of("Europe/Moscow");
        new ArrayList<>(map.keySet())
                .stream()
                .filter(c -> {
                    final LocalDateTime actual = dateField
                            .apply(map.get(c))
                            .toInstant()
                            .atZone(moscowZoneId)
                            .toLocalDateTime();
                    return actual.isAfter(expected) == isActualBeforeExpected;
                })
                .forEach(map::remove);
        return this;
    }

    public TCFilter filter(final TCQueryBuilder queryBuilder) {
        queryBuilder.asQueryMap()
                    .forEach((field, values) ->
                            filterOr(TCFields.getFieldByFilterName(field.replace("\"", "")), splitValues((String) values))
                    );
        return this;
    }

    private String[] splitValues(final String value) {
        if (value.contains("\"")) {
            return value.split("\", \"");
        }
        if (value.contains("'")) {
            return value.split("', '");
        }
        return value.split(", ");
    }

    private TCFilter filterOr(final TCFields field, final String... values) {
        final List<String> v = Stream.of(values).map(t -> t.replaceAll("[\"']", "")).collect(Collectors.toList());
        switch (field) {
            case FOLDER:
                return filter(t -> t.getFolder().getParents().stream().anyMatch(v::contains));
            case TEST_TYPE:
                return filter(t -> v.contains(t.getTestType().value));
            case TEST_VIEW:
                return filter(t -> v.contains(t.getTestView().value));
            case RISK:
                return filter(t -> v.contains(t.getRisk().value));
            case PROJECT_ID:
                return filter(t -> v.contains(t.getProjectId().toString()));
            case STATUS:
                return filter(t -> v.contains(t.getStatus().getName()));
            case PRIORITY:
                return filter(t -> v.contains(t.getPriority().getName()));
            case AUTOMATED_STATUS:
                return filter(t -> v.contains(t.getAutomatedStatus().value));
            case TEAM:
            case AUTOMATOR:
            case NOT_AUTOMATED_REASON:
                return filter(t -> v.contains(t.getTeam()));
            case FRAMEWORK:
                return filter(t -> v.stream().anyMatch(v1 -> t.getFramework().stream().anyMatch(f -> v1.equals(f.value))));
            case ARCHIVED:
                return filter(t -> v.contains(t.getArchived().toString()));
            default:
                throw new JiraConnectionException("Не добавлен фильтр для поля " + field.name());

        }
    }

    public Map<String, TestCaseModel> toMap() {
        return map;
    }
}
