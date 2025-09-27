package ru.sbt.edu_power.assist_bot.services.jenkins;

import ru.sbt.edu_power.external_services.jenkins.jobs.Job;
import ru.sbt.edu_power.external_services.jenkins.jobs.QaJavaUiParallelJob;
import ru.sbt.edu_power.external_services.jenkins.jobs.QaJsUiJob;
import ru.sbt.edu_power.external_services.jenkins.jobs.QaTeamFt1Job;
import ru.sbt.edu_power.external_services.jenkins.downstream_job_finder.BuildElement;
import ru.sbt.edu_power.external_services.jenkins.downstream_job_finder.DownstreamJobFinder;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;
import ru.sbt.edu_power.assist_bot.slack.views.sections.JiraProjectSelectSection;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NightBuildJobScanner {
    private final JiraProjectSelectSection jiraProjectSelectSection;
    private QaJavaUiParallelJob qaJavaUiParallelJob;
    private QaJsUiJob qaJsUiJob;
    private QaTeamFt1Job qaTeamFt1Job;
    private final Map<Integer, BuildElement> buildsMap = new HashMap<>();

    public NightBuildJobScanner(final JiraProjectSelectSection jiraProjectSelectSection) {
        this.jiraProjectSelectSection = jiraProjectSelectSection;
    }

    public QaJavaUiParallelJob getQaJavaUiParallelJob() {
        return qaJavaUiParallelJob;
    }

    public QaJsUiJob getQaJsUiJob() {
        return qaJsUiJob;
    }

    public QaTeamFt1Job getQaTeamFt1Job() {
        return qaTeamFt1Job;
    }

    // метод выполняет поиск билдов E2E джоб, которые являются дочерними к выбранной ночной джобе и проставляет их URL в объекты этих джоб
    public String setQaJobs(final Integer nightBuildId) {
        qaJavaUiParallelJob = new QaJavaUiParallelJob("");
        if (setBuildUrl(qaJavaUiParallelJob, nightBuildId)) {
            return "Не найдена джоба для тестов Java UI Parallel, выполнить сравнение невозможно";
        }
        qaJsUiJob = new QaJsUiJob("");
        if (setBuildUrl(qaJsUiJob, nightBuildId)) {
            return "Не найдена джоба для тестов JS UI, выполнить сравнение невозможно";
        }
        qaTeamFt1Job = new QaTeamFt1Job("");
        if (setBuildUrl(qaTeamFt1Job, nightBuildId)) {
            return "Не найдена джоба для тестов JS UI FT1, выполнить сравнение невозможно";
        }
        return "";
    }

    // метод выполняет поиск дочернего билда для указанной E2E джобы и устанавливает в него URL билда
    // возвращает true если операция была НЕУСПЕШНОЙ
    private boolean setBuildUrl(final Job job, final Integer nightBuildId) {
        final DownstreamJobFinder finder = new DownstreamJobFinder(job.getJobUrl());
        final List<BuildElement> buildElementList = finder.search(nightBuildId);
        if (buildElementList.isEmpty()) {
            return true;
        }
        job.setBuildUrl(buildElementList.get(0).getUrl());
        return false;
    }

    // форматирование номера билда с указанием давности
    public String formatBuildElement(final BuildElement element) {
        final Duration duration = Duration.of(System.currentTimeMillis() - element.getTimestamp(), ChronoUnit.MILLIS);
        return "(" + formatDuration(duration) + ") - number " + element.getNumber();
    }

    private String formatDuration(final Duration duration) {
        if (duration.toMinutes() < 60) {
            return duration.toMinutes() + "m";
        }
        if (duration.toHours() < 24) {
            return duration.toHours() + "h";
        }
        return duration.toDays() + "d";
    }

    public List<BuildElement> getSuccessfulBuilds() {
        final List<BuildElement> builds = new ArrayList<>();
        final DownstreamJobFinder finder = new DownstreamJobFinder(
                NBJob.valueOf(
                        TCFields.ProjectId
                                .valueOf(jiraProjectSelectSection.getAccessory().getValue())
                                .name()
                ).getUrl()
        );
        finder.getBuildElementList().stream()
              .filter(b -> "SUCCESS".equals(b.getResult()))
              .forEach(builds::add);
        return builds;
    }

    private enum NBJob {
        EDU("https://jenkins3-dev.pcbltools.ru/job/EduPower/job/NightlyBuild/job/edu-nightly-build/"),
        S21("https://jenkins3-dev.pcbltools.ru/job/EduPower/job/NightlyBuild/job/s21-nightly-build/");

        private final String url;

        NBJob(final String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }
    }
}
