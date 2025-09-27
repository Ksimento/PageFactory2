package ru.sbt.edu_power.confluence_reporting.RTM_metrics.applied_metrics;

import ru.sbt.edu_power.confluence_reporting.RTM_metrics.RtmMetricsCalc;
import ru.sbt.edu_power.confluence_reporting.TableTune;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class WithoutComponent_NOT_WORKING extends AbstractMetrics {
    private final int expected = 0;
    private final TableTune.BgColor bgColor = TableTune.BgColor.BLUE;
    private final Map<String, List<String>> teamToCaseListMap = new HashMap<>();

    public WithoutComponent_NOT_WORKING(final RtmMetricsCalc calc) {
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
//        return (team, size) -> String.format(
//                "Не указан компонент 'autotests' в автоматизированных кейсах: %s",
//                joinListWithLimit(teamToCaseListMap.get(team))
//        );
        return null;
    }

    @Override
    public TableTune.BgColor getBgColor() {
        return bgColor;
    }

    @Override
    public Map<String, Integer> getTeamToSizeMap() {
//        final Map<String, Integer> map = new HashMap<>();
//        calc.getRtmCollector().getUiCaseListByTeam().forEach((team, list) -> {
//            final List<String> withoutComponent = calc.selectByFieldIsEmpty(
//                    calc.selectByFieldValue(
//                            list,
//                            TMFields.AUTOMATED,
//                            TMFields.AutomatedStatus.AUTOMATED.getStatus(),
//                            TMFields.AutomatedStatus.ON_AUTOMATE.getStatus()
//                    ),
//                    TMFields.COMPONENT,
//                    true
//            );
//            final List<String> withAnotherComponent = calc.selectByFieldNOTValue(
//                    calc.selectByFieldValue(
//                            list,
//                            TMFields.AUTOMATED,
//                            TMFields.AutomatedStatus.AUTOMATED.getStatus(),
//                            TMFields.AutomatedStatus.ON_AUTOMATE.getStatus()
//                    ),
//                    TMFields.COMPONENT,
//                    "autotest"
//            );
//            teamToCaseListMap.put(team, calc.mergeList(withoutComponent, withAnotherComponent));
//            map.put(team, teamToCaseListMap.get(team).size());
//        });
//        return map;
        return null;
    }
}
