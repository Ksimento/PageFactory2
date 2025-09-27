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

public class TestCaseStatusNotApproved extends AbstractMetrics {
    private final int expected = 3;
    private final TableTune.BgColor bgColor = TableTune.BgColor.YELLOW;
    private final Map<String, Map<String, List<String>>> teamToStatusToCaseList = new HashMap<>();

    public TestCaseStatusNotApproved(final RtmMetricsCalc calc) {
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
                teamToStatusToCaseList.get(team).keySet().stream()
                                      .filter(s -> !s.equals(TCFields.Status.APPROVED.value) &&
                                                   !s.equals(TCFields.Status.DISABLED.value) &&
                                                   !s.equals(TCFields.Status.NEED_REFACTORING.value))
                                      .map(s -> String.format(
                                              "Тест-кейсы в статусе %s: %s",
                                              s,
                                              joinListWithLimit(teamToStatusToCaseList.get(team).get(s))
                                      ))
                                      .collect(Collectors.joining("\n"));
    }

    @Override
    public TableTune.BgColor getBgColor() {
        return bgColor;
    }

    @Override
    public Map<String, Integer> getTeamToSizeMap() {
        return teamToStatusToCaseList.entrySet()
                                     .stream()
                                     .collect(Collectors.toMap(
                                             Map.Entry::getKey,
                                             e -> {
                                                 final int notApproved = (int) e.getValue()
                                                                                .entrySet()
                                                                                .stream()
                                                                                .filter(s -> !s.getKey()
                                                                                               .equals(TCFields.Status.APPROVED.value) &&
                                                                                             !s.getKey()
                                                                                               .equals(TCFields.Status.DISABLED.value)  &&
                                                                                             !s.getKey()
                                                                                               .equals(TCFields.Status.NEED_REFACTORING.value))
                                                                                .mapToLong(k -> k.getValue().size())
                                                                                .sum();
                                                 final int all = (int) e.getValue()
                                                                        .values()
                                                                        .stream()
                                                                        .mapToLong(List::size)
                                                                        .sum();
                                                 return (notApproved * 100) / all;
                                             }
                                     ));
    }

    // Распределение кейсов по статусам
    public Map<String, List<String>> testCaseStatus() {
        final Map<String, List<String>> map = new HashMap<>();
        calc.getRtmCollector().getUiCaseListByTeam().forEach((team, list) -> {
            final Map<String, List<String>> statusMap = new HashMap<>();
            list.forEach((c, t) -> {
                if (!statusMap.containsKey(t.getStatus().getName())) {
                    statusMap.put(t.getStatus().getName(), new ArrayList<>());
                }
                statusMap.get(t.getStatus().getName()).add(c);
            });
            map.put(
                    team,
                    statusMap.keySet()
                             .stream()
                             .map(s -> s + ": " + statusMap.get(s).size())
                             .collect(Collectors.toList())
            );
            teamToStatusToCaseList.put(team, statusMap);
        });
        return map;
    }
}
