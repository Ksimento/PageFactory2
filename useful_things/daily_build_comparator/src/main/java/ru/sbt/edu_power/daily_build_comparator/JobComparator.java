package ru.sbt.edu_power.daily_build_comparator;

import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.jenkins.jobs.AllureDiffJob;
import ru.sbt.edu_power.external_services.jenkins.JenkinsHttpConnection;
import ru.sbt.edu_power.external_services.jenkins.jobs.JobStatus;
import ru.sbt.edu_power.external_services.jenkins.downstream_job_finder.BuildElement;

import java.util.List;
import java.util.Optional;

@Slf4j
public class JobComparator {
    private static final String LAST_BUILD_ID = System.getProperty("buildId");
    private static final Projects PROJECT = Projects.valueOf(System.getProperty("projectKey"));
    private static final String SLACK_CHANNEL = System.getProperty("slackChannel");

    public void execute() {
        final List<BuildElement> buildElements = JenkinsHttpConnection
                .getClusterByUrl(PROJECT.getJobUrl())
                .getAllBuilds(PROJECT.getJobUrl());
        final int lastBuildId = Integer.parseInt(LAST_BUILD_ID);
        final BuildElement lastBuild = getBuildById(buildElements, lastBuildId)
                .orElseThrow(() -> new JobComparatorException(String.format(
                        "Не найдена сборка номер %d для джобы %s",
                        lastBuildId,
                        PROJECT.getJobUrl()
                )));
        for (int i = lastBuildId - 1; i > 0; i--) {
            final Optional<BuildElement> buildCandidate = getBuildById(buildElements, i);
            if (!buildCandidate.isPresent()) {
                continue;
            }
            if (
                    JobStatus.SUCCESS.name().equals(buildCandidate.get().getResult()) ||
                    JobStatus.UNSTABLE.name().equals(buildCandidate.get().getResult())
            ) {
                final AllureDiffJob allureDiffJob = new AllureDiffJob();
                allureDiffJob.addParam(AllureDiffJob.Params.JIRA_PROJECT_KEY, PROJECT.getProject())
                             .addParam(AllureDiffJob.Params.JOB_LINK_1, lastBuild.getUrl())
                             .addParam(AllureDiffJob.Params.JOB_LINK_2, buildCandidate.get().getUrl())
                             .addParam(AllureDiffJob.Params.SLACK_CHANNEL, SLACK_CHANNEL)
                             .build();
                log.info("{}", allureDiffJob.getBuildUrl());
                return;
            }
        }
        throw new JobComparatorException("Не найдены успешные сборки для сравнения");
    }

    private Optional<BuildElement> getBuildById(final List<BuildElement> buildElements, final int buildId) {
        return buildElements.stream()
                            .filter(b -> buildId == b.getNumber())
                            .map(Optional::of)
                            .findFirst()
                            .orElse(Optional.empty());
    }
}
