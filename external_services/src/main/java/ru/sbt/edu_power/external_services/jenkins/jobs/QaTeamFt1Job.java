package ru.sbt.edu_power.external_services.jenkins.jobs;

import ru.sbt.edu_power.external_services.PropReader;
import ru.sbt.edu_power.external_services.jenkins.allure.JobWithAllure;

import java.util.HashMap;
import java.util.Map;

public class QaTeamFt1Job extends JobWithAllure {
    private static final String JOB_URL = PropReader.get("jenkins.qaTeamFt1.job.url");
    private String TEST_SET_KEY;

    public QaTeamFt1Job(final String devStand) {
        super(JOB_URL, devStand, Params.STAND_NAME.name());
    }

    public void setTEST_SET_KEY(final String TEST_SET_KEY) {
        this.TEST_SET_KEY = TEST_SET_KEY;
    }

    public void buildByParams() {
        final Map<String, Object> params = new HashMap<>();
        params.put(Params.STAND_NAME.name(), getDEV_STAND());
        params.put(Params.JS_UI_TEST_TAG.name(), "@FT1");
        params.put(Params.JS_UI_FF_TEST_TAG.name(), "");
        params.put(Params.JAVA_API_TEST_TAG.name(), "");
        params.put(Params.TEST_SET_ID.name(), TEST_SET_KEY);
        params.put(Params.SLACK_NOTIFY.name(), false);
        params.put(Params.STAGE_MOCKS.name(), true);
        build(params);
    }

    public QaTeamFt1Job addParam(final Params param, final Object value) {
        addParam(param.name(), value);
        return this;
    }

    public enum Params {
        STAND_NAME,
        FRONTEND_DOCKER_TAG,
        JS_UI_TEST_TAG,
        JS_UI_V4_TEST_TAG,
        JS_UI_FF_TEST_TAG,
        JAVA_API_TEST_TAG,
        TEST_SET_ID,
        SLACK_NOTIFY,
        CONFIG_BRANCH,
        STAGE_MOCKS
    }
}
