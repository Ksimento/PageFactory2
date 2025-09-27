package ru.sbt.edu_power.allure_comparator;

import ru.sbt.edu_power.allure_comparator.timeline.TimelineReportGenerator;

public class Main {
    public static void main(final String[] args) {
        final SuiteCollectorComparator comparator = new SuiteCollectorComparator();
        comparator.compareData();
        comparator.generateReport();
        comparator.generateReportFailedTags();
        comparator.saveIndexHtml();
        comparator.slackNotification();
        final TimelineReportGenerator timelineReportGenerator = new TimelineReportGenerator(comparator.getActualCollector());
        timelineReportGenerator.generate();
        timelineReportGenerator.saveHtml();
    }
}
