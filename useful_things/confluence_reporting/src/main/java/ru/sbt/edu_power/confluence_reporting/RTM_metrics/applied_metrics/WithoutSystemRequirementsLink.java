package ru.sbt.edu_power.confluence_reporting.RTM_metrics.applied_metrics;

import ru.sbt.edu_power.confluence_reporting.RTM_metrics.RtmMetricsCalc;
import ru.sbt.edu_power.confluence_reporting.TableTune;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFilter;
import ru.sbt.edu_power.external_services.jira.test_manager.model.TestCaseModel;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class WithoutSystemRequirementsLink extends AbstractMetrics {
    private final int expected = 0;
    private final LocalDateTime expectedDate;
    private final TableTune.BgColor bgColor = TableTune.BgColor.YELLOW;
    private final Map<String, Map<String, TestCaseModel>> teamToCaseListMap = new HashMap<>();

    public WithoutSystemRequirementsLink(final RtmMetricsCalc calc) {
        super(calc);
        // собираем данные только по кейсам двух-недельной давности
        expectedDate = LocalDateTime.now().minusYears(2L);
    }

    @Override
    public Function<Integer, Boolean> predicate() {
        return (actual) -> actual > expected;
    }

    @Override
    public Function<Integer, Boolean> predicateNeedAutomate() {
        return null;
    }

    @Override
    public BiFunction<String, Integer, String> descriptionFunction() {
        return (team, size) -> String.format(
                "Не прилинкованы СТ в кейсах: %s",
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
            teamToCaseListMap.put(
                    team,
                    TCFilter.of(list)
                            .filterByDate(expectedDate, TestCaseModel::getCreatedOn, false)
                            .filter(t -> t.getConfluencePageLinks().isEmpty())
                            .toMap()
            );
            map.put(team, teamToCaseListMap.get(team).size());
        });
        return map;
    }
}
