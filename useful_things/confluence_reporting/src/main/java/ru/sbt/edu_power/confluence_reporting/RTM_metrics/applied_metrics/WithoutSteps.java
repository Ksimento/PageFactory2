package ru.sbt.edu_power.confluence_reporting.RTM_metrics.applied_metrics;

import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.confluence_reporting.RTM_metrics.RtmMetricsCalc;
import ru.sbt.edu_power.confluence_reporting.TableTune;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFilter;
import ru.sbt.edu_power.external_services.jira.test_manager.model.TestCaseModel;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

@Slf4j
public class WithoutSteps extends AbstractMetrics {
    private final int expected = 0;
    private final TableTune.BgColor bgColor = TableTune.BgColor.RED;
    private final Map<String, Map<String, TestCaseModel>> teamToCaseListMap = new HashMap<>();

    public WithoutSteps(final RtmMetricsCalc calc) {
        super(calc);
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
                "В тест-кейсах не содержится шагов: %s",
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
                                    .filter(t -> {
                                                if (t.getTestScript().getSteps().isEmpty()) {
                                                    return true;
                                                }
                                                if (t.getTestScript().getSteps().size() > 1) {
                                                    return false;
                                                }
                                                final String description = t.getTestScript()
                                                                            .getSteps()
                                                                            .iterator()
                                                                            .next()
                                                                            .getDescription();
                                                if (description == null) {
                                                    log.error("Тест-кейс содержит NULL в степах {}", t.getKey());
                                                    return true;
                                                }
                                                return description.isEmpty();
                                            }
                                    )
                                    .toMap()
                    );
                    map.put(team, teamToCaseListMap.get(team).size());
                }
        );
        return map;
    }
}
