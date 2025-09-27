package ru.sbt.edu_power.external_services.nexus;

import com.google.common.net.HttpHeaders;
import com.google.gson.Gson;
import kong.unirest.ContentType;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.MultipartBody;
import kong.unirest.Unirest;
import kong.unirest.UnirestInstance;
import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.PropReader;
import ru.sbt.edu_power.external_services.jira.JiraConnect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public final class NexusConnect {
    private static NexusConnect INSTANCE;
    private static final String LOGIN = System.getProperty("nexusLogin");
    private static final String PASSWORD = System.getProperty("nexusPassword");
    private static final String ASSETS = PropReader.get("nexus.api.service.assets");
    private static final String COMPONENT = PropReader.get("nexus.api.service.component");
    private final UnirestInstance unirest = Unirest.spawnInstance();
    private final Gson gson = new Gson().newBuilder().setDateFormat("YYYY-MM-dd'T'HH:mm:ss.S").create();

    private NexusConnect() {
    }

    public static NexusConnect getInstance() {
        if (Objects.isNull(INSTANCE)) {
            if (Objects.isNull(LOGIN) || LOGIN.isEmpty()) {
                throw new NexusConnectException("Не задан логин для авторизации в Nexus");
            }
            if (Objects.isNull(PASSWORD) || PASSWORD.isEmpty()) {
                throw new NexusConnectException("Не задан пароль для авторизации в Nexus");
            }
            INSTANCE = new NexusConnect();

            JiraConnect.configAuth(INSTANCE.unirest.config())
                       .setDefaultBasicAuth(LOGIN, PASSWORD)
                       .setDefaultHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                       .setDefaultHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.toString())
                    .connectTimeout(1200000)
                    .socketTimeout(600000);
        }
        return INSTANCE;
    }

    public List<AssetModel> getAssets(final NexusRepo nexusRepo) {
        final List<AssetModel> assets = new ArrayList<>();
        collectAssets(nexusRepo, null, assets);
        return assets;
    }

    public byte[] downloadBlobAsBytes(final String url) {
        final HttpResponse<byte[]> response = unirest.get(url).asBytes();
        checkResponse(response, url);
        return response.getBody();
    }

    public AssetModel getAsset(final String id) {
        final String url = ASSETS + "/" + id;
        final HttpResponse<JsonNode> response = unirest.get(url).asJson();
        checkResponse(response, url);
        return gson.fromJson(response.getBody().toString(), AssetModel.class);
    }

    public void uploadData(
            final NexusRepo repo,
            final byte[] data,
            final String uploadDirectory,
            final String fileName
    ) {
        try (final UnirestInstance unirestInstance = Unirest.spawnInstance()) {
            JiraConnect.configAuth(unirestInstance.config())
                       .setDefaultBasicAuth(LOGIN, PASSWORD)
                       .setDefaultHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.toString());

            final MultipartBody multipartBody = unirestInstance
                    .post(COMPONENT)
                    .field("raw.directory", uploadDirectory)
                    .field("raw.asset1", data, fileName)
                    .field("raw.asset1.filename", fileName)
                    .queryString("repository", repo.getRepoName());
            final HttpResponse<JsonNode> response = multipartBody.asJson();
            checkResponse(response, COMPONENT, multipartBody.getBody().orElse(null));
        }
    }

    public void deleteAsset(final String id) {
        final String url = COMPONENT + "/" + id;
        final HttpResponse response = unirest.delete(url).asEmpty();
        checkResponse(response, url);
    }

    private void collectAssets(
            final NexusRepo repo,
            final String continuationToken,
            final List<AssetModel> assets
    ) {
        final Map<String, Object> params = new HashMap<>();
        params.put("repository", repo.getRepoName());
        if (Objects.nonNull(continuationToken) && !continuationToken.isEmpty()) {
            params.put("continuationToken", continuationToken);
        }
        final HttpResponse<JsonNode> response = unirest.get(ASSETS).queryString(params).asJson();
        checkResponse(response, ASSETS, params);
        if (Objects.nonNull(response.getBody()) && response.getBody().getObject().has("items")) {
            for (final Object o : response.getBody().getObject().getJSONArray("items")) {
                assets.add(gson.fromJson(o.toString(), AssetModel.class));
            }
            if (response.getBody().getObject().has("continuationToken") &&
                Objects.nonNull(response.getBody().getObject().get("continuationToken"))) {
                final String ct = response.getBody().getObject().getString("continuationToken");
                if (Objects.nonNull(ct) && !ct.isEmpty()) {
                    collectAssets(repo, ct, assets);
                }
            }
        }

    }

    private void checkResponse(final HttpResponse<?> response, final Object request, final Object... body) {
        if (response.isSuccess()) {
            return;
        }
        final String message = response.getBody() == null ?
                response.getStatusText() :
                ((JsonNode) response.getBody()).toPrettyString();
        log.error("Запрос вызвавший ошибку: {}", gson.toJson(request));
        log.error("Код возврата: {}", response.getStatus());
        for (final Object o : body) {
            log.error("Параметры запроса {}", gson.toJson(o));
        }
        throw new NexusConnectException(message);
    }
}
