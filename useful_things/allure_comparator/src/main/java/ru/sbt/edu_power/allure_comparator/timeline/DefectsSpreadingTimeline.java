package ru.sbt.edu_power.allure_comparator.timeline;

import lombok.Getter;
import ru.sbt.edu_power.external_services.jenkins.allure.SuiteCollector;
import ru.sbt.edu_power.external_services.jenkins.allure.enums.Children;

import java.util.*;

public class DefectsSpreadingTimeline {
    private final SuiteCollector actualCollector;
    // массив из названий джобы (это будет считаться прогоном на отдельном стенде) к списку аллюр отчётов
    private final Map<String, List<Children>> runToChildrenList = new HashMap<>();
    @Getter
    private final Map<String, Timeline> timelineMap = new HashMap<>();

    public DefectsSpreadingTimeline(final SuiteCollector actualCollector) {
        this.actualCollector = actualCollector;
    }

    public void sortData() {
        actualCollector.getCollection().values().stream()
                .flatMap(List::stream)
                .forEach(ch -> {
                    final String jobName = getJobName(ch);
                    if (!runToChildrenList.containsKey(jobName)) {
                        runToChildrenList.put(jobName, new ArrayList<>());
                    }
                    runToChildrenList.get(jobName).add(ch);
                });
    }

    public void generateTimeline() {
        runToChildrenList.forEach((run, list) -> {
            final Timeline timeline = new Timeline();
            list.forEach(ch -> timeline.addElement(new TimelineElement(ch)));
            timeline.spreadData();
            timeline.createWorkloadScale();
            timeline.createErrorScale();
            timelineMap.put(run, timeline);
        });
    }

    public Map<String, Timeline> getTimelineMap() {
        return timelineMap;
    }

    // выделяю название джобы из ссылки на репорт
    private String getJobName(final Children children) {
        final String[] pathParts = children.getLink().split("/job/");
        return pathParts[pathParts.length - 1].split("/")[0];
    }

}
