package ru.sbt.edu_power.external_services.jenkins.jobs;

import org.junit.Assert;
import ru.sbt.edu_power.external_services.PropReader;
import ru.sbt.edu_power.external_services.jenkins.allure.JobWithAllure;

import java.util.HashMap;
import java.util.Map;

public class QaUiSmokeJob extends JobWithAllure {
    private static final String JOB_URL = PropReader.get("jenkins.SmartSmokeAuto.job.url");
    private final Map<String, Object> params = new HashMap<>();

    public QaUiSmokeJob(final String devStand) {
        super(JOB_URL, devStand, Params.STAND_NAME.name());
        params.put(Params.STAND_NAME.name(), devStand);
    }

    @Override
    public void build() {
        Assert.assertFalse("Не заполнены параметры", params.isEmpty());
        build(params);
    }

    public QaUiSmokeJob addParam(final Params param, final Object value) {
        params.put(param.name(), value);
        return this;
    }

    @Override
    public void setParams(final Map<String, Object> params) {
        this.params.putAll(params);
    }

    @Override
    public Map<String, Object> getParams() {
        return params;
    }

    public enum Params {
        PROJECT,
        STAND_NAME,
        FRONTEND_BRANCH,
        DATA_SOURCE
    }
}
