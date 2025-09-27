package ru.sbt.edu_power.confluence_reporting.RTM_metrics.applied_metrics;

import ru.sbt.edu_power.confluence_reporting.RTM_metrics.RtmMetricsCalc;
import ru.sbt.edu_power.confluence_reporting.TableTune;
import ru.sbt.edu_power.external_services.confluence.ConfluenceConnectException;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFilter;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFolder;
import ru.sbt.edu_power.external_services.jira.test_manager.model.TestCaseModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class TestCaseInRootFolder extends AbstractMetrics {
    private final int expected = 0;
    private final TableTune.BgColor bgColor = TableTune.BgColor.BLUE;
    private final Map<String, Map<String, TestCaseModel>> teamToCaseListMap = new HashMap<>();

    public TestCaseInRootFolder(final RtmMetricsCalc calc) {
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
        return (team, size) -> String.format(
                "Тест-кейсы находятся в корне папки '/Регресс' или во вложенных в неё папках: %s",
                joinListWithLimit(teamToCaseListMap.get(team).keySet())
        );
    }

    @Override
    public TableTune.BgColor getBgColor() {
        return bgColor;
    }

    @Override
    public Map<String, Integer> getTeamToSizeMap() {
        final Map<String, Integer> map = new HashMap<>();
        calc.getRtmCollector().getUiCaseListByTeam().forEach((team, list) -> {
                    final TCFolder folder = calc.getRtmCollector().getCollector()
                            .getFolder(list.values().iterator().next().getProjectId());
                    final List<Integer> folderList = new ArrayList<>();
                    final TCFolder regressFolder = folder.getChildren()
                                                         .stream()
                                                         .filter(f -> "Регресс".equals(f.getName()))
                                                         .findFirst()
                                                         .orElseThrow(() -> new ConfluenceConnectException(
                                                                 "В проекте отсутствует папка Регресс"));
                    folderList.add(regressFolder.getId());
                    regressFolder.getChildren()
                                 .forEach(f ->
                                         folderList.add(f.getId())
                                 );
                    teamToCaseListMap.put(
                            team,
                            TCFilter.of(list)
                                    .filter(t -> folderList.contains(t.getFolder().getId()))
                                    .toMap()
                    );
                    map.put(team, teamToCaseListMap.get(team).size());
                }
        );
        return map;
    }
}
