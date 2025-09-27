package ru.sbt.edu_power.confluence_reporting.RTM_metrics.applied_metrics;

import ru.sbt.edu_power.confluence_reporting.RTM_metrics.RtmMetricsCalc;
import ru.sbt.edu_power.confluence_reporting.TableTune;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

// Метрика считает автоматизированные кейсы без указания автоматизатора
public class LowAutomatePercent extends AbstractMetrics {
    private final int expected;
    private final TableTune.BgColor bgColor = TableTune.BgColor.RED;
    private final Map<String, Integer> teamToSizeMap;

    public LowAutomatePercent(final RtmMetricsCalc calc, final int expected, final Map<String, Integer> teamToSizeMap) {
        super(calc);
        this.expected = expected;
        this.teamToSizeMap = teamToSizeMap;
    }
    
    @Override
    public Function<Integer, Boolean> predicateNeedAutomate() {
        return actual -> actual > 0;
    }

    @Override
    public Function<Integer, Boolean> predicate() {
        return actual -> actual < expected;
    }

    @Override
    public BiFunction<String, Integer, String> descriptionFunction() {
        return (team, size) -> String.format(
                "Не выполнена метрика по покрытию критичного функционала до %d%%",
                expected
        );
    }

    @Override
    public TableTune.BgColor getBgColor() {
        return bgColor;
    }

    @Override
    public Map<String, Integer> getTeamToSizeMap() {
        return teamToSizeMap;
    }
}
