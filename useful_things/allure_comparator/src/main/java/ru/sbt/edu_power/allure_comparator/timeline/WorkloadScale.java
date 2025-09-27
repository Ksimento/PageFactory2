package ru.sbt.edu_power.allure_comparator.timeline;

public class WorkloadScale extends AbstractScale {
    public void insert(final TimelineElement element) {
        insert(position -> position >= element.getStartTime() && position < element.getEndTime()) ;
    }
}
