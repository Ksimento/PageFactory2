package ru.sbt.edu_power.external_services.jenkins.jobs;

import ru.sbt.edu_power.external_services.PropReader;
import ru.sbt.edu_power.external_services.jenkins.allure.JobWithAllure;

public class QaJavaUiJob extends JobWithAllure {
    private static final String JOB_URL = PropReader.get("jenkins.qaJavaUi.job.url");

    public QaJavaUiJob(final String devStand) {
        super(JOB_URL, devStand, Params.STAND_NAME.name());
    }

    public QaJavaUiJob addParam(final Params param, final Object value) {
        addParam(param.name(), value);
        return this;
    }

    public enum Params {
        STAND_NAME,
        JIRA_PROJECT_KEY,
        FRONTEND_BRANCH,
        JAVA_UI_TEST_TAG,
        JAVA_UI_THREAD_COUNT,
        SCREEN_SIZE_PRESET,
        HACK_SQL,
        DISABLE_SKIPPED_TESTS,
        TEST_SET_ID,
        SLACK_NOTIFY
    }
}
