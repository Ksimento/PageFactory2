package ru.sbt.edu_power.allure_comparator;

import com.google.gson.Gson;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.jenkins.JenkinsHttpConnection;
import ru.sbt.edu_power.external_services.jenkins.jobs.JobStatus;
import ru.sbt.edu_power.external_services.jenkins.allure.JobType;
import ru.sbt.edu_power.external_services.jenkins.downstream_job_finder.BuildElement;
import ru.sbt.edu_power.external_services.jira.JiraConnect;

import java.util.List;

@Slf4j
public class AllureComparator {
    @Getter
    private final String jiraProjectKey = System.getProperty("jiraProjectKey");
    private String jobA = System.getProperty("jobA", "");
    private String jobB = System.getProperty("jobB", "");
    private final boolean isSpecifiedSequence = Boolean.getBoolean("useSpecifiedSequence");
    @Getter
    private String expectedJobUrl;
    @Getter
    private String actualJobUrl;
    @Getter
    private JobType expectedJobType;
    @Getter
    private JobType actualJobType;

    protected void prepareData() {
        normalizeJobUrl();
        log.info("jobA: {}", jobA);
        log.info("jobB: {}", jobB);
        identifyJobs();
        log.info("expectedJobUrl: {}", expectedJobUrl);
        log.info("actualJobUrl: {}", actualJobUrl);
    }

    private void normalizeJobUrl() {
        if (jobA.isEmpty()) {
            if (jobB.isEmpty()) {
                throw new AllureComparatorException("Необходимо заполнить хотя бы одно поле");
            }
            jobB = getLastBuild(removeAllurePart(jobB));
            return;
        }
        if (jobB.isEmpty()) {
            jobB = getLastBuild(removeAllurePart(jobA));
            jobA = "";
            return;
        }
        jobA = getLastBuild(removeAllurePart(jobA));
        jobB = getLastBuild(removeAllurePart(jobB));
    }

    private String removeAllurePart(final String buildUrl) {
        if (buildUrl.contains("/allure")) {
            return buildUrl.split("/allure")[0];
        }
        return buildUrl.replaceAll("/$", "");
    }

    // если в URL есть lastBuild - сначала найдём последний успешный билд для этой джобы
    private String getLastBuild(final String buildUrl) {
        if (buildUrl.contains("preLastBuild")) {
            final String jobUrl = buildUrl.split("/preLastBuild")[0];
            final List<BuildElement> lastBuilds = JenkinsHttpConnection.getCluster3().getPageBuilds(jobUrl, 0, 10);
            boolean lastBuild = true;
            for (final BuildElement buildElement : lastBuilds) {
                if (JobStatus.SUCCESS.name().equals(buildElement.getResult()) || JobStatus.UNSTABLE.name().equals(buildElement.getResult())) {
                    if (lastBuild) {
                        lastBuild = false;
                        continue;
                    }
                    return buildElement.getUrl();
                }
            }
        } else if (buildUrl.contains("lastBuild")) {
            final String jobUrl = buildUrl.split("/lastBuild")[0];
            final List<BuildElement> lastBuilds = JenkinsHttpConnection.getCluster3().getPageBuilds(jobUrl, 0, 10);
            for (final BuildElement buildElement : lastBuilds) {
                if (JobStatus.SUCCESS.name().equals(buildElement.getResult()) || JobStatus.UNSTABLE.name().equals(buildElement.getResult())) {
                    return buildElement.getUrl();
                }
            }
        }
        return buildUrl;
    }

    private void identifyJobs() {
        if (isSpecifiedSequence || jobA.isEmpty()) {
            expectedJobUrl = jobA;
            actualJobUrl = jobB;
        } else {
            expectedJobUrl = getJobTimestamp(jobA) < getJobTimestamp(jobB) ? jobA : jobB;
            actualJobUrl = expectedJobUrl.equals(jobA) ? jobB : jobA;
        }
        expectedJobType = JobType.determineJobType(expectedJobUrl);
        actualJobType = JobType.determineJobType(actualJobUrl);
    }


    private Long getJobTimestamp(final String jobUrl) {
        final HttpResponse<JsonNode> response = JenkinsHttpConnection.getClusterByUrl(jobUrl).getApiJsonData(jobUrl);
        JiraConnect.checkResponse(response, jobUrl);
        final BuildElement buildElement = new Gson().fromJson(
                response.getBody().getObject().toString(),
                BuildElement.class
        );
        return buildElement.getTimestamp();
    }


}
