package ru.sbt.edu_power.external_services.jenkins.jobs;

import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.PropReader;

@Slf4j
public class FullDeployJob extends Job {
    private static final String JOB_URL = PropReader.get("jenkins.fullDeploy.job.url");

    public FullDeployJob(final String devStand) {
        super(JOB_URL, devStand, Params.STAND_NAME.name());
    }

    public FullDeployJob addParam(final Params param, final Object value) {
        addParam(param.name(), value);
        return this;
    }

    public enum Params {
        STAND_NAME,
        CONFIG_BRANCH,
        FILE_DEPLOY_VERSION,
        ENABLE_WHITELIST,
        ENV_CATALOG,
        CLEANUP_STAND,
        TIME_SHIFT,
//        DB_DUMP
    }
}
