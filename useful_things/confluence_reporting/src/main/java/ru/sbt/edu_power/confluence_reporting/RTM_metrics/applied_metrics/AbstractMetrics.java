package ru.sbt.edu_power.confluence_reporting.RTM_metrics.applied_metrics;

import ru.sbt.edu_power.confluence_reporting.RTM_metrics.RtmMetricsCalc;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class AbstractMetrics implements AppliedMetrics {
    protected final RtmMetricsCalc calc;
    private static final int CASE_LIST_LIMIT = 20;

    protected AbstractMetrics(final RtmMetricsCalc calc) {
        this.calc = calc;
    }

    protected String joinListWithLimit(final Collection<String> list) {
        final String joined;
        if (list.size() > CASE_LIST_LIMIT + 3) {
            final int remainder = (list.size() - CASE_LIST_LIMIT) % 10;
            final String text;
            if ((list.size() - CASE_LIST_LIMIT) < 20 && (list.size() - CASE_LIST_LIMIT) > 5) {
                text = " тест-кейсов";
            } else if ((list.size() - CASE_LIST_LIMIT) < 20) {
                text = " тест-кейса";
            } else if (remainder == 1) {
                text = " тест-кейс";
            } else if (remainder > 1 && remainder < 5) {
                text = " тест-кейса";
            } else {
                text = " тест-кейсов";
            }
            joined = String.join(", ", sublist(list)) + " и ещё " + (list.size() - CASE_LIST_LIMIT) + text;
        } else {
            joined = String.join(", ", list);
        }
        return joined;
    }

    private Set<String> sublist(final Collection<String> list) {
        final Iterator<String> iterator = list.iterator();
        return IntStream.range(0, CASE_LIST_LIMIT).mapToObj(i -> iterator.next()).collect(Collectors.toSet());
    }

}
