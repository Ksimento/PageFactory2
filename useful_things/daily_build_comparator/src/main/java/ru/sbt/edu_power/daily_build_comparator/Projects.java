package ru.sbt.edu_power.daily_build_comparator;

import lombok.Getter;

@Getter
public enum Projects {
    E2E_JAVA_ACTUALIZATION("https://jenkins3-dev.pcbltools.ru/job/EduPower/job/DailyBuild/job/e2e-java-actualization/", "EDU"),
    E2E_JS_ACTUALIZATION("https://jenkins3-dev.pcbltools.ru/job/EduPower/job/DailyBuild/job/e2e-js-actualization/", "EDU"),
    S21_DAILY_BUILD("https://jenkins3-dev.pcbltools.ru/job/EduPower/job/DailyBuild/job/s21-daily-build/", "S21"),
    S21("https://jenkins3-dev.pcbltools.ru/job/EduPower/job/DailyBuild/job/s21-daily-build/", "S21");

    private final String jobUrl;
    private final String project;

    Projects(final String jobUrl, final String project) {
        this.jobUrl = jobUrl;
        this.project = project;
    }

    public String getJobUrl() {
        return jobUrl;
    }
}
