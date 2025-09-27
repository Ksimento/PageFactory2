package ru.sbt.edu_power.external_services.jenkins.jobs;

import ru.sbt.edu_power.external_services.PropReader;
import ru.sbt.edu_power.external_services.jenkins.allure.JobWithAllure;

public class QaJsUiJob extends JobWithAllure {
    private static final String JOB_URL = PropReader.get("jenkins.eduFrontEnd2End.job.url");

    public QaJsUiJob(final String devStand) {
        super(JOB_URL, devStand, Params.STAND_NAME.name());
    }

    public QaJsUiJob addParam(final Params param, final Object value) {
        addParam(param.name(), value);
        return this;
    }

    public enum Params {
        STAND_NAME,
        FRONTEND_BRANCH,
        JS_UI_TEST_TAG,
        JS_UI_V4_TEST_TAG,
        STAGE_DEPLOY,
        TEST_SET_ID,
        SLACK_NOTIFY
    }
}
