package ru.sbt.edu_power.assist_bot.services.jenkins;

import com.google.gson.Gson;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.Nullable;
import ru.sbt.edu_power.external_services.jenkins.JenkinsHttpConnection;
import ru.sbt.edu_power.external_services.jenkins.downstream_job_finder.BuildElement;
import ru.sbt.edu_power.external_services.jira.JiraConnect;
import ru.sbt.edu_power.external_services.timer.Timer;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public final class DockerTagSearch {
    private static DockerTagSearch instance;
    private final EnumMap<Repo, Map<JobListElement, Cluster>> jobListElementList = new EnumMap<>(Repo.class);
    private final Gson gson = new Gson();

    private DockerTagSearch() {
    }

    public static DockerTagSearch getInstance() {
        if (instance == null) {
            instance = new DockerTagSearch();
        }
        return instance;
    }

    public void scanRepo() {
        Timer.startTimer("scanRepo");
        final List<Pair<Cluster, Repo>> repos = new ArrayList<>();
        for (final Repo repo : new Repo[]{Repo.BACK, Repo.FRONT}) {
            jobListElementList.put(repo, new HashMap<>());
            for (final Cluster c : Cluster.values()) {
                repos.add(new Pair<>(c, repo));
            }
        }
        // мультибранч каталога только в первом и втором кластере
        // TODO для каталога не реализована выгрузка тегов
        jobListElementList.put(Repo.CATALOG, new HashMap<>());
        repos.add(new Pair<>(Cluster.DEV1, Repo.CATALOG));
        repos.add(new Pair<>(Cluster.DEV2, Repo.CATALOG));
        repos.parallelStream().forEach(p -> scanRepo(p.getFirst(), p.getSecond()));
        log.info("scan repo complete {}", Timer.getDelta("scanRepo", "scanRepo"));
    }

    private void scanRepo(final Cluster cluster, final Repo repo) {
        final String url = cluster.getUrl() + repo.getUrl();
        final HttpResponse<JsonNode> response = JenkinsHttpConnection
                .getClusterByUrl(cluster.getUrl())
                .getApiJsonData(url);
        if (response.getStatus() == HttpStatus.SC_BAD_REQUEST) {
            return;
        }
        JiraConnect.checkResponse(response, url);
        for (final Object o : response.getBody().getObject().getJSONArray("jobs")) {
            final JobListElement jobListElement = gson.fromJson(o.toString(), JobListElement.class);
            if (Objects.nonNull(jobListElement.getColor()) && !jobListElement.getColor().contains("notbuilt")) {
                jobListElement.setName(jobListElement.getName());
                jobListElementList.get(repo).put(jobListElement, cluster);
            }
        }
    }

    // возвращает список сборок stable и unstable
    public synchronized Map<BuildElement, Cluster> getDockerTag(final Repo repo, final String keyword) {
        final Map<BuildElement, Cluster> cachedElements = getCachedElements(repo, keyword);
        if (cachedElements.isEmpty()) {
            updateCache(repo);
        }
        return getCachedElements(repo, keyword);
    }

    private void updateCache(final Repo repo) {
        jobListElementList.get(repo).clear();
        final List<Cluster> clusters = new ArrayList<>();
        clusters.add(Cluster.DEV1);
        clusters.add(Cluster.DEV2);
        if (repo != Repo.CATALOG) {
            clusters.add(Cluster.DEV3);
        }
        clusters.parallelStream().forEach(cl -> scanRepo(cl, repo));
    }

    private Map<BuildElement, Cluster> getCachedElements(final Repo repo, final String keyword) {
        return jobListElementList
                .get(repo).entrySet().parallelStream()
                .filter(e -> e.getKey().getName().contains(keyword))
                .map(this::getLastSuccessfulBuildElement)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond, (a, b) -> b));
    }

    @Nullable
    private Pair<BuildElement, Cluster> getLastSuccessfulBuildElement(final Map.Entry<JobListElement, Cluster> e) {
        final List<BuildElement> builds = JenkinsHttpConnection
                .getClusterByUrl(e.getKey().getUrl())
                .getAllBuilds(e.getKey().getUrl());

        final BuildElement buildElement = builds.stream()
                                                .filter(b -> BuildResult.SUCCESS.name().equals(b.getResult()) ||
                                                             BuildResult.UNSTABLE.name().equals(b.getResult())
                                                )
                                                .findFirst()
                                                .orElse(null);
        if (Objects.isNull(buildElement)) {
            return null;
        }
        buildElement.setSlackIcon(BuildResult.valueOf(buildElement.getResult()).getIcon());
        return new Pair<>(buildElement, e.getValue());
    }

    public enum Repo {
        FRONT("/job/EduPower/job/FrontBuild"),
        BACK("/job/EduPower/job/BackBuild"),
        CATALOG("/job/EduPower/job/CatalogBackBuild");

        private final String url;

        Repo(final String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }
    }

    public enum Cluster {
        DEV1("https://jenkins-dev.pcbltools.ru"),
        DEV2("https://jenkins2-dev.pcbltools.ru"),
        DEV3("https://jenkins3-dev.pcbltools.ru");

        private final String url;

        Cluster(final String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }
    }

    public enum BuildResult {
        UNSTABLE(":large_yellow_circle:"),
        SUCCESS(":large_green_circle:");

        private final String icon;

        BuildResult(final String icon) {
            this.icon = icon;
        }

        public String getIcon() {
            return icon;
        }
    }

    @Getter
    @Setter
    static class JobListElement {
        private String name;
        private String url;
        private String color;

        public void setName(final String name) {
            this.name = name.replace("%2F", "/");
        }
    }

    @Getter
    @AllArgsConstructor
    private static class Pair<F, S> {
        private final F first;
        private final S second;
    }
}
