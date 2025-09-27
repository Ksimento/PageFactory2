package ru.sbt.edu_power.external_services.bitbucket;


import com.google.gson.Gson;
import kong.unirest.Config;
import kong.unirest.ContentType;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.UnirestInstance;
import kong.unirest.json.JSONArray;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import ru.sbt.edu_power.external_services.PropReader;
import ru.sbt.edu_power.external_services.bitbucket.models.BBRepos;
import ru.sbt.edu_power.external_services.bitbucket.models.BranchModel;
import ru.sbt.edu_power.external_services.bitbucket.models.BranchRestrictionModel;
import ru.sbt.edu_power.external_services.bitbucket.models.pull_request.LatestCommitPullRequest;
import ru.sbt.edu_power.external_services.bitbucket.models.pull_request.PullRequestModel;
import ru.sbt.edu_power.external_services.jira.JiraConnect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// Класс интеграции с битбакетом
@Slf4j
public class BBConnection {
    private static final String LOGIN = System.getProperty("bbLogin");
    private static final String PASSWORD = System.getProperty("bbPassword");
    private static final String BRANCHES = PropReader.get("bb.api.get.brunches");
    private static final String DELETE_BRANCHES = PropReader.get("bb.api.delete.brunches");
    private static final String PULL_REQUEST = PropReader.get("bb.api.get.pull-request");
    private static final String PULL_REQUEST_OVERVIEW = PropReader.get("bb.api.get.pull-request.overview");
    private static final String PULL_REQUEST_ACTIVITIES = PropReader.get("bb.api.get.pull-request.activities");
    private static final String PULL_REQUEST_ADD_COMMENT = PropReader.get("bb.api.get.pull-request.add.comments");
    private static final String PULL_REQUEST_CHANGES = PropReader.get("bb.api.get.pull-request.add.changes");
    private static final String PULL_REQUEST_LATEST_COMMITS = PropReader.get("bb.api.get.pull-request.latest.commits");
    private static final String BRANCH_PERMISSIONS = PropReader.get("bb.branch-permissions.restrictions");
    private static final Gson GSON = new Gson();

    // метод получает список существующих веток, можно использовать фильтр-фразу
    public static List<BranchModel> getBranches(final BBRepos repo, final String filterText) {
        final String url = BRANCHES
                .replace("{PROJECT}", repo.getProject().name())
                .replace("{REPO}", repo.getRepoName());
        final Map<String, Object> params = new HashMap<>();
        if (!filterText.isEmpty()) {
            params.put("filterText", filterText);
        }
        params.put("orderBy", "MODIFICATION");
        params.put("boostMatches", true);
        params.put("details", true);
        final JSONArray jsonArray = getJsonArrayResponse(url, params);
        final List<BranchModel> branches = new ArrayList<>();
        for (final Object o : jsonArray) {
            branches.add(GSON.fromJson(o.toString(), BranchModel.class));
        }
        return branches;
    }

    public static List<PullRequestModel.Activities> getActivities(final String pr, final BBRepos repo) {
        final String url = PULL_REQUEST_ACTIVITIES
                .replace("{PR}", pr).replace("{REPO}", repo.getRepoName());
        final JSONArray jsonArray = getJsonArrayResponse(url, new HashMap<>());

        final List<PullRequestModel.Activities> activities = new ArrayList<>();
        for (final Object o : jsonArray) {
            activities.add(GSON.fromJson(o.toString(), PullRequestModel.Activities.class));
        }
        return activities;
    }

    public static List<PullRequestModel.Paths> getPaths(final String pr, final BBRepos repo) {
        final String url = PULL_REQUEST_CHANGES
                .replace("{PR}", pr).replace("{REPO}", repo.getRepoName());
        final JSONArray jsonArray = getJsonArrayResponse(url, new HashMap<>());

        final List<PullRequestModel.Paths> paths = new ArrayList<>();
        for (final Object o : jsonArray) {
            paths.add(GSON.fromJson(o.toString(), PullRequestModel.Paths.class));
        }
        return paths;
    }

    public static boolean addCommentsInPR(final String pr, final BBRepos repo, final String text) {
        final String url = PULL_REQUEST_ADD_COMMENT
                .replace("{PR}", pr).replace("{REPO}", repo.getRepoName());
        Map<String, Object> body = new HashMap<>();
        body.put("text", text);
        body.put("severity", "NORMAL");
        final HttpResponse<JsonNode> response = bbConnection()
                .post(url)
                .header("Content-Type", "application/json;charset=UTF-8")
                .body(body)
                .asJson();
        try {
            checkResponse(response, url, body);
            return true;
        } catch (BBConnectionException e) {
            return false;
        }
    }

    public static HttpResponse<JsonNode> deleteBranch(
            final BBRepos repo,
            final String branchName
    ) {
        String url = DELETE_BRANCHES
                .replace("{PROJECT}", repo.getProject().name())
                .replace("{REPO}", repo.getRepoName());
        Map<String, Object> params = new HashMap<>();
        params.put("name", branchName);
        UnirestInstance instance = bbConnection();
        return instance
                .delete(
                        url
                )
                .header("Content-Type", "application/json")
                .body(params)
                .asJson();
    }

    public static List<PullRequestModel> getPullRequest(final BBRepos repo) {
        String url = PULL_REQUEST
                .replace("{PROJECT}", repo.getProject().name())
                .replace("{REPO}", repo.getRepoName());
        final Map<String, Object> params = new HashMap<>();
        params.put("state", "MERGED");
        params.put("at", "refs/heads/master");
        final JSONArray jsonArray = getJsonArrayResponse(url, params);
        final List<PullRequestModel> pullRequests = new ArrayList<>();
        for (final Object o : jsonArray) {
            PullRequestModel pr = GSON.fromJson(o.toString(), PullRequestModel.class);
            pr.setLinkPR(PULL_REQUEST_OVERVIEW
                    .replace("{PROJECT}", repo.getProject().name())
                    .replace("{REPO}", repo.getRepoName())
                    .replace("{ID}", pr.getId()));
            pullRequests.add(pr);
        }
        return pullRequests;
    }

    public static LatestCommitPullRequest getLatestCommitPullRequest(final String latestCommit) {
        String url = PULL_REQUEST_LATEST_COMMITS
                .replace("{LATEST_COMMIT}", latestCommit);
        final Map<String, Object> params = new HashMap<>();
        final JSONArray jsonArray = new JSONArray();
        final HttpResponse<JsonNode> response = bbConnection()
                .get(url)
                .queryString(params)
                .asJson();
        checkResponse(response, url, params);
        for (final Object o : response.getBody().getObject().getJSONArray("values")) {
            jsonArray.put(o.toString());
        }
        final LatestCommitPullRequest latest = new LatestCommitPullRequest();
        if (jsonArray.isEmpty()) {
            latest.setUrl("no build");
            return latest;
        } else {
            return GSON.fromJson(jsonArray.get(0).toString(), LatestCommitPullRequest.class);
        }
    }

    public static List<BranchRestrictionModel> getBranchRestrictions(final BBRepos repo) {
        final String url = BRANCH_PERMISSIONS
                .replace("{PROJECT}", repo.getProject().name())
                .replace("{REPO}", repo.getRepoName());
        final JSONArray jsonArray = getJsonArrayResponse(url, null);
        final List<BranchRestrictionModel> branches = new ArrayList<>();
        for (final Object o : jsonArray) {
            branches.add(GSON.fromJson(o.toString(), BranchRestrictionModel.class));
        }
        return branches;
    }

    // метод выполняет сбор всех данных выводимых постранично в один массив
    private static JSONArray getJsonArrayResponse(final String url, final Map<String, Object> params) {
        final JSONArray jsonArray = new JSONArray();
        int page = 0;
        final int limit = 20;
        while (true) {
            if (page < 600) {
                params.put("start", page * limit);
                params.put("limit", limit);
                final HttpResponse<JsonNode> response = bbConnection()
                        .get(url)
                        .queryString(Objects.isNull(params) ? new HashMap<>() : params)
                        .asJson();

                checkResponse(response, url, params);
                for (final Object o : response.getBody().getObject().getJSONArray("values")) {
                    jsonArray.put(o);
                }
                if (response.getBody().getObject().getBoolean("isLastPage")) {
                    break;
                }
                page++;
            } else {
                return jsonArray;
            }
        }
        return jsonArray;
    }

    // загрузка страницы данных через GET
    private static synchronized HttpResponse<JsonNode> get(
            final String url,
            final Map<String, Object> params
    ) {
        if (Objects.isNull(LOGIN) || LOGIN.isEmpty()) {
            throw new BBConnectionException("Не задан логин для авторизации в Nexus");
        }
        if (Objects.isNull(PASSWORD) || PASSWORD.isEmpty()) {
            throw new BBConnectionException("Не задан пароль для авторизации в Nexus");
        }
        try (final UnirestInstance unirestInstance = Unirest.spawnInstance()) {
            JiraConnect.configAuth(unirestInstance.config())
                       .setDefaultBasicAuth(LOGIN, PASSWORD)
                       .addDefaultHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.toString());
            return unirestInstance.get(url).queryString(params)
                                  .connectTimeout(1200000)
                                  .socketTimeout(600000)
                                  .asJson();
        }
    }

    public static UnirestInstance bbConnection() {
        if (Objects.isNull(LOGIN) || LOGIN.isEmpty()) {
            throw new BBConnectionException("Не задан логин для авторизации в Nexus");
        }
        if (Objects.isNull(PASSWORD) || PASSWORD.isEmpty()) {
            throw new BBConnectionException("Не задан пароль для авторизации в Nexus");
        }
        Config config = JiraConnect.configAuth(Unirest.spawnInstance().config())
                                   .setDefaultBasicAuth(LOGIN, PASSWORD)
                                   .addDefaultHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.toString());
        return new UnirestInstance(config);
    }

    private static void checkResponse(final HttpResponse<?> response, final Object request, final Object... body) {
        if (response.isSuccess()) {
            return;
        }
        final String message = response.getBody() == null ?
                response.getStatusText() :
                ((JsonNode) response.getBody()).toPrettyString();
        log.error("Запрос вызвавший ошибку: {}", GSON.toJson(request));
        log.error("Код возврата: {}", response.getStatus());
        for (final Object o : body) {
            log.error("Параметры запроса {}", GSON.toJson(o));
        }
        throw new BBConnectionException(message);
    }
}
