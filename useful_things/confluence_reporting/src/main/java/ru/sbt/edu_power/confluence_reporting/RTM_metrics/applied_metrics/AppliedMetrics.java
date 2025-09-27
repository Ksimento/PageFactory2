package ru.sbt.edu_power.confluence_reporting.RTM_metrics.applied_metrics;

import ru.sbt.edu_power.confluence_reporting.TableTune;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface AppliedMetrics {
    Function<Integer, Boolean> predicate();
    Function<Integer, Boolean> predicateNeedAutomate();
    BiFunction<String, Integer, String> descriptionFunction();
    TableTune.BgColor getBgColor();
    Map<String, Integer> getTeamToSizeMap();
}
