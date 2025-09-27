package ru.sbt.edu_power.external_services.jenkins.jobs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import ru.sbt.edu_power.external_services.PropReader;
import ru.sbt.edu_power.external_services.ExternalServicesException;
import ru.sbt.edu_power.external_services.jenkins.allure.AllureHelper;
import ru.sbt.edu_power.external_services.jenkins.downstream_job_finder.BuildElement;
import ru.sbt.edu_power.external_services.jenkins.downstream_job_finder.DownstreamJobFinder;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraVersionModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QaJavaUiParallelJob extends Job {
    private static final String JOB_URL = PropReader.get("jenkins.autotestParallel.job.url");
    private static final String TESTS_AUTO_JOB_URL = PropReader.get("jenkins.testsAuto.job.url");
    // tests-auto URL to test tag
    private final Map<String, AllureHelper> downstreamJobs = new HashMap<>();

    public QaJavaUiParallelJob(final String devStand) {
        super(JOB_URL, devStand, Params.STAND_NAME.name());
    }

    public QaJavaUiParallelJob addParam(final Params param, final Object value) {
        addParam(param.name(), value);
        return this;
    }

    public String getBranchName(final JiraVersionModel version) {
        if (version.getName().contains("-rc")) {
            return version.getName().split("-")[0].replaceAll("\\d$", "0");
        }
        return version.getName();
    }

    public Map<String, AllureHelper> getAllures() {
        downstreamJobs.clear();
        collectDownstreamJobs(getBuildUrl());
        return new HashMap<>(downstreamJobs);
    }

    private void collectDownstreamJobs(final String buildUrl) {
        final String[] parts = buildUrl.split("/");
        final int jobId = Integer.parseInt(parts[parts.length - 1]);
        final DownstreamJobFinder downstreamJobFinder = new DownstreamJobFinder(TESTS_AUTO_JOB_URL);
        final List<BuildElement> builds = downstreamJobFinder.search(jobId);
        builds.forEach(e -> downstreamJobs.put(
                downstreamJobFinder.getParamByName(e.getActions(), "JAVA_UI_TEST_TAG"),
                new AllureHelper(e.getUrl())
        ));
    }

    private JsonObject getJsonObjectFromArray(final JsonArray array, final String hasKey) {
        for (final JsonElement element : array) {
            final JsonObject jsonObject = element.getAsJsonObject();
            if (jsonObject.has(hasKey)) {
                return jsonObject;
            }
        }
        throw new ExternalServicesException(String.format(
                "В представленном массиве нет элемента с ключем %s\n%s",
                hasKey,
                array
        ));
    }

    private JsonObject getJsonObjectFromArray(final JsonArray array, final String hasKey, final String hasValue) {
        for (final JsonElement element : array) {
            final JsonObject jsonObject = element.getAsJsonObject();
            if (jsonObject.has(hasKey)
                && jsonObject.getAsJsonPrimitive(hasKey).isString()
                && hasValue.equals(jsonObject.getAsJsonPrimitive(hasKey).getAsString())
            ) {
                return jsonObject;
            }
        }
        throw new ExternalServicesException(String.format(
                "В представленном массиве нет элемента с ключем %s и значением %s\n%s",
                hasKey,
                hasValue,
                array
        ));
    }

    public enum Params {
        JIRA_PROJECT_KEY,
        STAND_NAME,
        TEST_SET_ID,
        TEST_PACK,
        FRONTEND_BRANCH,
        SLACK_NOTIFY
    }
}
