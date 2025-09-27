package ru.sbt.edu_power.external_services.jenkins.allure;

import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.ESUtils;
import ru.sbt.edu_power.external_services.ExternalServicesException;
import ru.sbt.edu_power.external_services.jenkins.jobs.QaJavaUiParallelJob;
import ru.sbt.edu_power.external_services.jenkins.downstream_job_finder.BuildElement;
import ru.sbt.edu_power.external_services.jenkins.downstream_job_finder.DownstreamJobFinder;
import ru.sbt.edu_power.external_services.timer.Timer;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

@Slf4j
public class SuiteGrabber {
    public static final String QA_JAVA_UI_PARALLEL = "https://jenkins3-dev.pcbltools.ru/job/EduPower/job/QA/job/qa-java-ui-parallel/";
    public static final String QA_JS_UI = "https://jenkins3-dev.pcbltools.ru/job/EduPower/job/QA/job/qa-js-ui/";
    public static final String QA_JS_UI_BOOTCAMP = "https://jenkins3-dev.pcbltools.ru/job/EduPower/job/QA/job/qa-team-bootcamp/";
    public static final String QA_TEAM_FT_1 = "https://jenkins3-dev.pcbltools.ru/job/EduPower/job/QA/job/qa-team-mediateka/";
    public static final String QA_JAVA_API = "https://jenkins3-dev.pcbltools.ru/job/EduPower/job/QA/job/qa-java-api/";
    public static final String QA_JAVA_UI_PARALLEL_NAME = "qa-java-ui-parallel";
    public static final String QA_JAVA_API_NAME = "qa-java-api";
    private final NexusCache nexusCache = NexusCache.getInstance();

    public void grab(final SuiteCollector collector, final String buildUrl) {
        final String id = nexusCache.getId(buildUrl);
        if (nexusCache.isCached(id)) {
            final AtomicReference<Throwable> error = new AtomicReference<>();
            final BooleanSupplier waitWhenDataBeDownloaded = () -> {
                try {
                    collector.recoverCollector(nexusCache.downloadData(id));
                } catch (final Throwable e) {
                    error.set(e);
                    ESUtils.freeze(1000);
                    return false;
                }
                return true;
            };
            if (!Timer.executeTimer(10, waitWhenDataBeDownloaded)) {
                if (Objects.nonNull(error.get())) {
                    throw new ExternalServicesException(error.get());
                }
            }
            log.info("Данные получены из кэша для джобы {} id: {}", buildUrl, id);
        } else {
            grabBehavior(JobType.determineJobType(buildUrl), buildUrl, collector);
            final AtomicReference<Throwable> error = new AtomicReference<>();
            final BooleanSupplier waitWhenDataBeUploaded = () -> {
                try {
                    nexusCache.uploadData(id, collector.getCollection());
                } catch (final Throwable e) {
                    error.set(e);
                    ESUtils.freeze(1000);
                    return false;
                }
                return true;
            };
            if (!Timer.executeTimer(10, waitWhenDataBeUploaded)) {
                if (Objects.nonNull(error.get())) {
                    throw new ExternalServicesException(error.get());
                }
            }
            log.info("Данные получены из джобы {} id: {}", buildUrl, id);
        }
    }

    private void grabBehavior(final JobType jobType, final String buildUrl, final SuiteCollector collector) {
        switch (jobType) {
            case SIMPLE_ALLURE_JOB:
                collectAllure(new AllureHelper(buildUrl), collector);
                break;
            case QA_JAVA_UI_PARALLEL:
                grabBehaviorForParallelJob(buildUrl, collector);
                break;
            default:
                final int nbBuildId = getBuildId(buildUrl);
                try {
                    if (jobType.isHasJavaParallel()) {
                        // находим билд qa-java-ui-parallel
                        final String parallel = searchDownstreamJob(
                                QA_JAVA_UI_PARALLEL,
                                nbBuildId,
                                QA_JAVA_UI_PARALLEL_NAME,
                                buildUrl
                        );
                        grabBehaviorForParallelJob(parallel, collector);
                    }
                    if (jobType.isHasJsUi()) {
                        // находим билд qa-js-ui
                        final String qaJsUi = searchDownstreamJob(QA_JS_UI, nbBuildId, "qa-js-ui", buildUrl);
                        collectAllure(new AllureHelper(qaJsUi), collector);
                    }
                    if (jobType.isHasBootcamp()) {
                        // находим билд qa-team-bootcamp
                        final String qaJsUi = searchDownstreamJob(QA_JS_UI_BOOTCAMP, nbBuildId, "qa-team-bootcamp", buildUrl);
                        collectAllure(new AllureHelper(qaJsUi), collector);
                    }
                    if (jobType.isHasJsFt1Ui()) {
                        // находим билд qa-team-mediateka
                        final String qaTeamFt1 = searchDownstreamJob(QA_TEAM_FT_1, nbBuildId, "qa-team-mediateka", buildUrl);
                        collectAllure(new AllureHelper(qaTeamFt1), collector);
                    }
                    if (jobType.isHasJavaApi()) {
                        // находим билд qa-java-api
                        final String qaJavaApi = searchDownstreamJob(QA_JAVA_API, nbBuildId, QA_JAVA_API_NAME, buildUrl);
                        collectAllure(new AllureHelper(qaJavaApi), collector);
                    }
                } catch (final ExternalServicesException e) {
                    log.error("", e);
                }
                break;
        }
    }

    private String searchDownstreamJob(
            final String downstreamJobUrl,
            final int buildId,
            final String jobName,
            final String nbUrl
    ) {
        final DownstreamJobFinder downstreamJobFinder = new DownstreamJobFinder(downstreamJobUrl);
        final List<BuildElement> downstreamBuilds = downstreamJobFinder.search(buildId);
        if (downstreamBuilds.isEmpty()) {
            throw new ExternalServicesException(String.format(
                    "Для ночной сборки '%s' не найдена джоба '%s'", nbUrl, jobName));
        }
        return downstreamBuilds.get(downstreamBuilds.size() - 1).getUrl();
    }

    private void grabBehaviorForParallelJob(
            final String buildUrl,
            final SuiteCollector collector
    ) {
        final QaJavaUiParallelJob parallelJob = new QaJavaUiParallelJob("");
        parallelJob.setBuildUrl(buildUrl);
        parallelJob.getAllures().values().forEach(a -> collectAllure(a, collector));
    }

    public static int getBuildId(final String buildUrl) {
        final String[] parts = buildUrl.split("/");
        for (final String part : parts) {
            if (checkIfNumber(part)) {
                return Integer.parseInt(part);
            }
        }
        throw new ExternalServicesException("В ссылке отсутствует ID сборки " + buildUrl);
    }

    private static boolean checkIfNumber(final String value) {
        if (Objects.isNull(value) || value.isEmpty()) {
            return false;
        }
        for (final char c : value.toCharArray()) {
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    private void collectAllure(final AllureHelper allure, final SuiteCollector collector) {
        allure.enrichmentChild();
        collector.add(allure);
    }
}
