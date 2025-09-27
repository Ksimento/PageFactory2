package ru.sbt.edu_power.allure_comparator.timeline;

import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.jenkins.allure.enums.AllureReport;
import ru.sbt.edu_power.external_services.jenkins.allure.enums.Children;
import ru.sbt.edu_power.external_services.jenkins.allure.enums.Teams;
import ru.sbt.edu_power.external_services.shared.fail_categories.FailCategoriesCore;
import ru.sbt.edu_power.external_services.shared.fail_categories.IFailCategories;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Slf4j
class TimelineElement {
    private final boolean isSuccess;
    private final boolean isSkipped;
    private boolean isSoftAsserted;
    private final long startTime;
    private final long endTime;
    private final String tags;
    private final String team;
    @Setter
    private double relativePosition;
    @Setter
    private double relativeWidth;
    private final List<FailPoint> failPoints = new ArrayList<>();
    private final Children children;
    private Long failedTime;

    TimelineElement(final Children children) {
        final String jiraProjectKey = System.getProperty("jiraProjectKey", "EDU");
        tags = children.getTags()
                .stream()
                .map(t -> t.replace("@", ""))
                .filter(t -> t.startsWith(jiraProjectKey))
                .collect(Collectors.joining(" "));
        this.children = children;
        team = children
                .getTags()
                .stream()
                .map(t -> t.replace("@", ""))
                .filter(e -> Arrays.stream(Teams.values()).map(Teams::name).collect(Collectors.toList()).contains(e))
                .findAny()
                .orElse("undefined");
        isSuccess = "passed".equals(children.getStatus());
        isSkipped = "skipped".equals(children.getStatus());
        startTime = children.getReport().getTime().getStart();
        endTime = children.getReport().getTime().getStop();
        if (!isSuccess && !isSkipped) {
            try {
                children.getReport().getTestStage().getSteps().forEach(step -> {
                    if ("unknown".equals(step.getStatus())) {
                        isSoftAsserted = true;
                        failPoints.add(
                                new FailPoint(
                                        step.getTime().getStart(),
                                        FailCategoriesCore.determine(
                                                jiraProjectKey,
                                                step.getName(),
                                                getErrorTrace(step)
                                        )
                                )
                        );
                    }
                    if ("failed".equals(step.getStatus()) || "broken".equals(step.getStatus())) {
                        failedTime = step.getTime().getStart();
                        failPoints.add(
                                new FailPoint(
                                        step.getTime().getStart(),
                                        FailCategoriesCore.determine(
                                                jiraProjectKey,
                                                step.getName(),
                                                getErrorTrace(step)
                                        )
                                )
                        );
                        isSoftAsserted = false;
                    }
                });
            } catch (final NullPointerException e) {
                log.error(
                        "Ошибка получения репорта:\n{}",
                        new GsonBuilder().setPrettyPrinting().create().toJson(children)
                );
                e.printStackTrace();
            }
        }
    }

    private String getErrorTrace(AllureReport.Step report) {
        return children.getLink().contains("-js") ? children.getReport().getDescription() + report.getStatusMessage() : report.getStatusMessage() +
                report.getStatusTrace();
    }

    public int compareByStartTime(final TimelineElement compared) {
        return (int) (startTime - compared.startTime);
    }

    public int compareByEndTime(final TimelineElement compared) {
        return (int) (endTime - compared.endTime);
    }

    @Getter
    public static class FailPoint {
        private final long time;
        private final IFailCategories category;
        @Setter
        private double position;

        public FailPoint(long time, IFailCategories category) {
            this.time = time;
            this.category = category;
        }

    }
}