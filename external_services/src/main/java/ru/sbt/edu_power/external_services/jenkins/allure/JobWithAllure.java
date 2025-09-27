package ru.sbt.edu_power.external_services.jenkins.allure;

import ru.sbt.edu_power.external_services.jenkins.jobs.Job;

public abstract class JobWithAllure extends Job {
    private AllureHelper allure;

    protected JobWithAllure(final String jobUrl, final String devStand, final String standFieldName) {
        super(jobUrl, devStand, standFieldName);
    }

    public AllureHelper getAllure() {
        if (allure == null) {
            allure = new AllureHelper(getBuildUrl());
        }
        return allure;
    }

    public int getSummaryPercent() {
        return allure.getSummaryPercent();
    }
}
