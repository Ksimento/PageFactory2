package ru.sbt.edu_power.external_services.jenkins.allure;

import com.google.gson.Gson;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import org.apache.http.HttpStatus;
import ru.sbt.edu_power.external_services.ExternalServicesException;
import ru.sbt.edu_power.external_services.data.Archiver;
import ru.sbt.edu_power.external_services.jenkins.JenkinsHttpConnection;
import ru.sbt.edu_power.external_services.jenkins.downstream_job_finder.BuildElement;
import ru.sbt.edu_power.external_services.nexus.AssetModel;
import ru.sbt.edu_power.external_services.nexus.NexusConnect;
import ru.sbt.edu_power.external_services.nexus.NexusRepo;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

// Класс реализует кеширование данных из коллектора аллюр-отчётов в нексус
public final class NexusCache {
    private static final String ASSETS_PATH = "ALLURE_DIFF";
    private final List<AssetModel> assets;
    private final Gson gson = new Gson();
    private static NexusCache INSTANCE;

    public static synchronized NexusCache getInstance() {
        if (Objects.isNull(INSTANCE)) {
            INSTANCE = new NexusCache();
        }
        return INSTANCE;
    }

    private NexusCache() {
        assets = NexusConnect.getInstance()
                             .getAssets(NexusRepo.JAVA_E2E_STATS)
                             .stream()
                             .filter(a -> a.getPath().startsWith(ASSETS_PATH))
                             .collect(Collectors.toList());
    }

    // проверяет наличие кэша в нексусе
    public boolean isCached(final String id) {
        return assets.stream()
                     .anyMatch(a -> a.getPath().contains(id));
    }

    // загружает архив с данными из нексуса
    public String downloadData(final String id) {
        final String dataUrl = assets
                .stream()
                .filter(a -> a.getPath().contains(id))
                .map(AssetModel::getDownloadUrl)
                .findFirst()
                .orElse("");
        if (dataUrl.isEmpty()) {
            return "";
        }
        final byte[] zippedData = NexusConnect.getInstance().downloadBlobAsBytes(dataUrl);
        final Archiver archiver = new Archiver()
                .setArchiveBytes(zippedData)
                .unGzipArchive();
        return new String(archiver.sourceAsBytes(), StandardCharsets.UTF_8);
    }

    // выгружает данные в нексус
    public void uploadData(final String id, final Object data) {
        final Archiver archiver = new Archiver()
                .setSourceBytes(gson.toJson(data).getBytes())
                .gzipSource();
        NexusConnect.getInstance()
                    .uploadData(
                            NexusRepo.JAVA_E2E_STATS,
                            archiver.archiveAsBytes(),
                            ASSETS_PATH,
                            id + ".json.gzip"
                    );
    }

    // получает ID билда (поле QueueId)
    public String getId(final String buildUrl) {
        final HttpResponse<JsonNode> response = JenkinsHttpConnection
                .getClusterByUrl(buildUrl)
                .getApiJsonData(buildUrl);
        if (response.getStatus() == HttpStatus.SC_NOT_FOUND) {
            throw new ExternalServicesException("Билд более не доступен по ссылке " + buildUrl);
        }
        final BuildElement element = gson.fromJson(response.getBody().getObject().toString(), BuildElement.class);
        return element.getQueueId().toString();
    }
}
