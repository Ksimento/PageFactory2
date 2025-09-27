package ru.sbt.edu_power.allure_comparator.timeline;

import java.util.List;
import java.util.Map;

public interface Scale {
    void generateScale(long start, long end, int steps);
    int getMax();
    int getMin();
    void insert(TimelineElement element);
    int getScaleStepsNumber();
    long getScaleStep();
    List<Integer> getScaleData();
    Map<Long, Integer> getMap();
}
