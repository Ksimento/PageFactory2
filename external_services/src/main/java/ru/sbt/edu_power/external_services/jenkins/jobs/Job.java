package ru.sbt.edu_power.external_services.jenkins.jobs;

import com.google.gson.Gson;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.junit.Assert;
import ru.sbt.edu_power.external_services.ESUtils;
import ru.sbt.edu_power.external_services.jenkins.JenkinsHttpConnection;
import ru.sbt.edu_power.external_services.jenkins.model.StepPipe;
import ru.sbt.edu_power.external_services.shared.TaskExecutionStatus;
import ru.sbt.edu_power.external_services.jenkins.downstream_job_finder.BuildElement;
import ru.sbt.edu_power.external_services.jira.JiraConnect;
import ru.sbt.edu_power.external_services.timer.Timer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import static ru.sbt.edu_power.external_services.jira.JiraConnect.GSON;

@Getter
public abstract class Job implements IsBuild {
    private String DEV_STAND = "";
    private final String jobUrl;
    private Integer queueId;
    private String buildUrl;
    private TaskExecutionStatus status = TaskExecutionStatus.NOT_STARTED;
    private final Map<String, Object> params = new HashMap<>();
    private final List<FileField> fileParams = new ArrayList<>();
    private final Gson gson = new Gson();

    protected Job(final String jobUrl, final String devStand, final String standFieldName) {
        this.jobUrl = Objects.requireNonNull(jobUrl);
        DEV_STAND = Objects.requireNonNull(devStand);
        params.put(standFieldName, devStand);
    }

    protected Job(final String jobUrl) {
        this.jobUrl = Objects.requireNonNull(jobUrl);
    }

    @Override
    public IsBuild addParam(final String name, final Object value) {
        params.put(name, value);
        return this;
    }

    public IsBuild addParam(final FileField fileField) {
        fileParams.add(fileField);
        return this;
    }

    @Override
    public void setParams(final Map<String, Object> params) {
        this.params.putAll(params);
    }

    // опрашиваем джобу в течении 10 минут на предмет появления Build ID
    public synchronized String getBuildUrl() {
        if (buildUrl != null) {
            return buildUrl;
        }
        final BooleanSupplier waitWhenDeployStarted = () -> {
            searchBuildUrl();
            if (buildUrl.isEmpty()) {
                ESUtils.freeze(10000);
                return false;
            }
            return true;
        };
        Timer.executeTimerThrowable(600, "Джоба не стартовала за 10 минут", waitWhenDeployStarted);
        return buildUrl;
    }

    // метод нужен только для отладки, что бы подставить URL до существующей сборки
    public void setBuildUrl(final String buildUrl) {
        this.buildUrl = buildUrl;
    }

    public void build(final Map<String, Object> params) {
        status = TaskExecutionStatus.IN_PROGRESS;
        queueId = JenkinsHttpConnection.getClusterByUrl(jobUrl).buildWithParameters(params, fileParams, jobUrl);
    }

    @Override
    public void build() {
        Assert.assertFalse("Не заданы параметры", params.isEmpty());
        build(params);
    }

    /**
     * Метод получает из джобы список последних запущенных билдов,
     * проходит по каждому билду и сверяет queueId с тем, что был
     * получен при запуске джобы
     */
    private void searchBuildUrl() {
        final List<BuildElement> buildListNode = JenkinsHttpConnection.getClusterByUrl(jobUrl).getAllBuilds(jobUrl);
        if (Objects.isNull(queueId)) {
            buildUrl = "";
            return;
        }
        buildUrl = buildListNode.stream()
                                .filter(b -> queueId.equals(b.getQueueId()))
                                .map(BuildElement::getUrl)
                                .findFirst()
                                .orElse("");
    }

    public String getBuildStatus() {
        getBuildUrl();
        final HttpResponse<JsonNode> response = JenkinsHttpConnection
                .getClusterByUrl(buildUrl)
                .getApiJsonData(buildUrl);
        JiraConnect.checkResponse(response, buildUrl);
        final BuildElement element = gson.fromJson(response.getBody().getObject().toString(), BuildElement.class);
        if (element.getResult() == null || (element.getDuration() == 0 && !"FAILURE".equals(element.getResult()))) {
            return TaskExecutionStatus.IN_PROGRESS.name();
        }
        return element.getResult();
    }

    public static List<StepPipe> getPipeMfe(final String id){
        final String url = JenkinsHttpConnection.Cluster.DEV3.getUrl() + "/blue/rest/organizations/jenkins/pipelines/EduPower/pipelines/QA/pipelines/MfeTest/runs/{ID}/nodes/?limit=10000".replace("{ID}",id);
        final HttpResponse<JsonNode> response = JenkinsHttpConnection
                .getClusterByUrl(url)
                .getUnirest().get(url).asJson();
        JiraConnect.checkResponse(response, url);
        final List<StepPipe> stepsPipe = new ArrayList<>();
        for (final Object o : response.getBody().getArray()) {
            stepsPipe.add(GSON.fromJson(o.toString(), StepPipe.class));
        }
        return stepsPipe;
    }

    @Override
    public String paramsToString() {
        return params.entrySet()
                     .stream()
                     .map(e -> e.getKey() + ": " + e.getValue())
                     .collect(Collectors.joining("\n"));
    }

    public void updateStatus() {
        final String jobStatus;
        if (status == TaskExecutionStatus.IN_PROGRESS) {
            jobStatus = getBuildStatus();
        } else {
            jobStatus = status.name();
        }
        switch (jobStatus) {
            case "":
            case "IN_PROGRESS":
                status = TaskExecutionStatus.IN_PROGRESS;
                break;
            case "NOT_STARTED":
                status = TaskExecutionStatus.NOT_STARTED;
                break;
            case "SUCCESS":
            case "UNSTABLE":
                status = TaskExecutionStatus.SUCCESS;
                break;
            default:
                status = TaskExecutionStatus.FAILED;
        }
    }

    public TaskExecutionStatus getStatus() {
        return status;
    }

    @NoArgsConstructor
    @Setter
    @Getter
    public static class BuildList {
        private Set<BuildItem> builds = new HashSet<>();
    }

    @NoArgsConstructor
    @Setter
    @Getter
    public static class BuildItem {
        private Integer number;
        private String url;
    }

    @Getter
    public static class FileField {
        private final String fieldName;
        private final byte[] bytes;
        private final String fileName;

        public FileField(final String fieldName, final byte[] bytes, final String fileName) {
            this.fieldName = fieldName;
            this.bytes = bytes;
            this.fileName = fileName;
        }

        @Override
        public String toString() {
            return "FileField{" +
                   "fieldName='" + fieldName + '\'' +
                   ", bytes=" + new String(bytes) +
                   ", fileName='" + fileName + '\'' +
                   '}';
        }
    }
}
