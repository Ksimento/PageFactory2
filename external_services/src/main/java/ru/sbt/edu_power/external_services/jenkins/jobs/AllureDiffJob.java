package ru.sbt.edu_power.external_services.jenkins.jobs;

import ru.sbt.edu_power.external_services.PropReader;

public class AllureDiffJob extends Job {
    private static final String JOB_URL = PropReader.get("jenkins.allureDiff.job.url");

    public AllureDiffJob() {
        super(JOB_URL);
    }

    public AllureDiffJob addParam(final Params param, final Object value) {
        addParam(param.name(), value);
        return this;
    }

    public enum Params {
        JIRA_PROJECT_KEY,
        JOB_LINK_1,
        JOB_LINK_2,
        SLACK_CHANNEL
    }
}
