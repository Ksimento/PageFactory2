package ru.sbt.edu_power.allure_comparator.timeline;

import lombok.Getter;
import ru.sbt.edu_power.external_services.jenkins.allure.enums.AllureReport;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
class Timeline {
    private final List<TimelineElement> elements = new ArrayList<>();
    // номер строки -> список элементов в этой строке
    private final Map<Integer, List<TimelineElement>> rowToElementMap = new LinkedHashMap<>();
    // ID потока из теста -> номер строки
    private final Map<String, Integer> threadToRowNumber = new TreeMap<>();
    private final WorkloadScale workloadScale = new WorkloadScale();
    private final ErrorScale errorScale = new ErrorScale();

    public void addElement(final TimelineElement element) {
        elements.add(element);
    }

    public Optional<Long> getStart() {
        final Optional<TimelineElement> firstElement = elements.stream().min(TimelineElement::compareByStartTime);
        return firstElement.map(TimelineElement::getStartTime);
    }

    public Optional<Long> getEnd() {
        final Optional<TimelineElement> firstElement = elements.stream().max(TimelineElement::compareByEndTime);
        return firstElement.map(TimelineElement::getStartTime);
    }

    public void spreadData() {
        for (final TimelineElement element : elements) {
            final Optional<String> threadTag = element.getChildren().getReport().getLabels()
                    .stream()
                    .filter(l -> "thread".equals(l.getName()))
                    .map(AllureReport.Label::getValue)
                    .findFirst();
            final String rowName = threadTag.orElse("undefined");
            threadToRowNumber.put(rowName, 0);
        }
        final AtomicInteger counter = new AtomicInteger(0);
        threadToRowNumber.keySet().forEach(k -> threadToRowNumber.put(k, counter.getAndIncrement()));
        for (final TimelineElement element : elements) {
            final Optional<String> threadTag = element.getChildren().getReport().getLabels()
                    .stream()
                    .filter(l -> "thread".equals(l.getName()))
                    .map(AllureReport.Label::getValue)
                    .findFirst();
            final String rowName = threadTag.orElse("undefined");
            if (!rowToElementMap.containsKey(threadToRowNumber.get(rowName))) {
                rowToElementMap.put(threadToRowNumber.get(rowName), new ArrayList<>());
            }
            rowToElementMap.get(threadToRowNumber.get(rowName)).add(element);
        }

        setRelativePosition();
    }

    public void createWorkloadScale() {
        workloadScale.generateScale(getStart().orElse(0L), getEnd().orElse(0L), 1000);
        elements.forEach(workloadScale::insert);
    }

    public void createErrorScale() {
        errorScale.generateScale(getStart().orElse(0L), getEnd().orElse(0L), 55);
        elements.forEach(errorScale::insert);
    }

    private void setRelativePosition() {
        final long start = getStart().orElse(0L);
        final double length = getEnd().orElse(0L) - start;
        elements.forEach(element -> {
            final long left = element.getStartTime() - start;
            final long width = element.getEndTime() - element.getStartTime();
            final double relativeWidth = width * 90 / length;
            final double relativePosition = length == 0 ? 0 : left * 90 / length + 1;

            element.setRelativePosition(relativePosition);
            element.setRelativeWidth(relativeWidth);
            element.getFailPoints().forEach(point -> {
                final double pos = (point.getTime() - element.getStartTime()) * 100 / (double) width;
                point.setPosition(pos);
            });
        });
    }

}