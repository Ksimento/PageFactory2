package ru.sbt.edu_power.confluence_reporting.RTM_metrics.applied_metrics;

import ru.sbt.edu_power.confluence_reporting.RTM_metrics.RtmMetricsCalc;
import ru.sbt.edu_power.confluence_reporting.TableTune;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TestTypeNotRegress extends AbstractMetrics {
    private final int expected = 3;
    private final TableTune.BgColor bgColor = TableTune.BgColor.RED;
    private final Map<String, Map<String, List<String>>> teamToTestTypeToCaseList = new HashMap<>();

    public TestTypeNotRegress(final RtmMetricsCalc calc) {
        super(calc);
    }

    @Override
    public Function<Integer, Boolean> predicate() {
        return actual -> actual > expected;
    }

    @Override
    public Function<Integer, Boolean> predicateNeedAutomate() {
        return null;
    }

    @Override
    public BiFunction<String, Integer, String> descriptionFunction() {
        return (team, size) ->
                teamToTestTypeToCaseList.get(team).keySet().stream()
                        .filter(s -> !s.equals(TCFields.TestType.REGRESS.value) &&
                                !s.equals(TCFields.TestType.NOT_FOR_REGRESS.value))
                        .map(s -> String.format(
                                "Тест-кейсы в статусе %s: %s",
                                s,
                                joinListWithLimit(teamToTestTypeToCaseList.get(team).get(s))
                        ))
                        .collect(Collectors.joining("\n"));
    }

    @Override
    public TableTune.BgColor getBgColor() {
        return bgColor;
    }

    @Override
    public Map<String, Integer> getTeamToSizeMap() {
        return teamToTestTypeToCaseList.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue()
                                .entrySet()
                                .stream()
                                .filter(s ->
                                        !s.getKey()
                                                .equals(TCFields.TestType.REGRESS.value) &&
                                                !s.getKey()
                                                        .equals(TCFields.TestType.NOT_FOR_REGRESS.value)
                                )
                                .mapToInt(k -> k.getValue().size())
                                .sum()
                ));
    }

    // Распределение кейсов по типу
    public Map<String, List<String>> testCaseTestType() {
        final Map<String, List<String>> map = new HashMap<>();
        calc.getRtmCollector().getUiCaseListByTeam().forEach((team, list) -> {
            final Map<String, List<String>> testTypeMap = new HashMap<>();
            list.forEach((c, t) -> {
                if (t.getTestType() == null) {
                    if (!testTypeMap.containsKey("НЕ УКАЗАН")) {
                        testTypeMap.put("НЕ УКАЗАН", new ArrayList<>());
                    }
                    testTypeMap.get("НЕ УКАЗАН").add(c);
                } else {
                    if (!testTypeMap.containsKey(t.getTestType().value)) {
                        testTypeMap.put(t.getTestType().value, new ArrayList<>());
                    }
                    testTypeMap.get(t.getTestType().value).add(c);
                }
            });
            map.put(
                    team,
                    testTypeMap.keySet()
                            .stream()
                            .map(s -> s + ": " + testTypeMap.get(s).size())
                            .collect(Collectors.toList())
            );
            teamToTestTypeToCaseList.put(team, testTypeMap);
        });
        return map;
    }
}
