package ru.sbt.edu_power.external_services.jenkins;

import com.google.common.net.HttpHeaders;
import com.google.gson.Gson;
import kong.unirest.ContentType;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.MultipartBody;
import kong.unirest.Unirest;
import kong.unirest.UnirestInstance;
import kong.unirest.json.JSONArray;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import ru.sbt.edu_power.external_services.ESUtils;
import ru.sbt.edu_power.external_services.PropReader;
import ru.sbt.edu_power.external_services.ExternalServicesException;
import ru.sbt.edu_power.external_services.jenkins.downstream_job_finder.BuildElement;
import ru.sbt.edu_power.external_services.jenkins.jobs.Job;
import ru.sbt.edu_power.external_services.jira.JiraConnect;
import ru.sbt.edu_power.external_services.jira.JiraConnectionException;
import ru.sbt.edu_power.external_services.jira.JiraExporterUtils;
import ru.sbt.edu_power.external_services.timer.Timer;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

@Slf4j
public final class JenkinsHttpConnection {
    private static final String LOGIN = "molokovich.a.a";
    private static final String BUILD_WITH_PARAMETERS = "/buildWithParameters";
    private static final String API_JSON = "/api/json";
    private static final String API_JSON_ALL_BUILDS = "/api/json?tree=allBuilds[id,displayName,number,queueId,result,url,timestamp,actions[causes[upstreamBuild],parameters[name,value]]]";
    private static final String TOKEN1 = PropReader.get("jenkins1.token");
    private static final String TOKEN2 = PropReader.get("jenkins2.token");
    private static final String TOKEN3 = PropReader.get("jenkins3.token");
    private static JenkinsHttpConnection cluster1;
    private static JenkinsHttpConnection cluster2;
    private static JenkinsHttpConnection cluster3;
    private final UnirestInstance unirest;
    private final Gson gson = new Gson();

    private JenkinsHttpConnection(final String token) {
        unirest = Unirest.spawnInstance();
        try (final InputStream is = JiraConnect.class.getClassLoader().getResourceAsStream("user_auth.pfx")) {
            final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            final String CERT_PASS = JiraExporterUtils.decodeData(PropReader.get("cert.pass"));
            ks.load(is, CERT_PASS.toCharArray());

            unirest.config()
                    .clientCertificateStore(ks, CERT_PASS)
                    .setDefaultBasicAuth(LOGIN, token)
                    .setDefaultHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.toString())
                    .connectTimeout(1200000)
                    .socketTimeout(600000);
        } catch (final IOException | KeyStoreException | CertificateException | NoSuchAlgorithmException e) {
            throw new JiraConnectionException(e);
        }
    }

    public UnirestInstance getUnirest() {return unirest;}

    public static JenkinsHttpConnection getClusterByUrl(final String url) {
        if (url.startsWith(Cluster.DEV1.getUrl())) {
            return getCluster1();
        }
        if (url.startsWith(Cluster.DEV2.getUrl())) {
            return getCluster2();
        }
        if (url.startsWith(Cluster.DEV3.getUrl())) {
            return getCluster3();
        }
        throw new ExternalServicesException("Не определен кластер по URL " + url);
    }

    public static JenkinsHttpConnection getCluster1() {
        if (cluster1 == null) {
            cluster1 = new JenkinsHttpConnection(TOKEN1);
        }
        return cluster1;
    }

    public static JenkinsHttpConnection getCluster2() {
        if (cluster2 == null) {
            cluster2 = new JenkinsHttpConnection(TOKEN2);
        }
        return cluster2;
    }

    public static JenkinsHttpConnection getCluster3() {
        if (cluster3 == null) {
            cluster3 = new JenkinsHttpConnection(TOKEN3);
        }
        return cluster3;
    }

    /**
     * Метод выполняет запуск джобы с параметрами
     *
     * @param parameters мапа название поле джобы - значение для поля
     * @return идентификатор очереди, по которому можно будет идентифицировать стартовавшую джобу
     */
    @SneakyThrows
    public Integer buildWithParameters(
            final Map<String, Object> parameters,
            final List<Job.FileField> fileParams,
            final String jobUrl
    ) {
        // заполняем текстовые поля
        final MultipartBody request = unirest.post(jobUrl + BUILD_WITH_PARAMETERS).fields(parameters);
        // заполняем поля с файлами
        fileParams.forEach(p -> request.field(p.getFieldName(), p.getBytes(), p.getFileName()));
        log.info("Выполнен запрос на сборку JOB в Jenkins: {}\nField Params:\n{}\nFile Params:\n{}",
                request.getUrl(),
                parameters,
                fileParams
        );
        final HttpResponse<?> response = request.asEmpty();
        JiraConnect.checkResponse(response, jobUrl + BUILD_WITH_PARAMETERS, parameters);
        final String location = response.getHeaders().get("location").get(0);
        final String[] parts = location.split("/");
        return Integer.parseInt(parts[parts.length - 1]);
    }

    public HttpResponse<JsonNode> getApiJsonData(final String jobUrl) {
        final String url = jobUrl + API_JSON;
        return unirest.get(url).asJson();
    }

    public HttpResponse<JsonNode> getJsonData(final String jobUrl) {
        return unirest.get(jobUrl).asJson();
    }

    public List<BuildElement> getAllBuilds(final String jobUrl) {
        final List<BuildElement> buildElementList = new ArrayList<>();
        int page = 0;
        while (true) {
            final List<BuildElement> pageBuilds = getPageBuilds(jobUrl, page, 50);
            if (pageBuilds.isEmpty()) {
                break;
            }
            buildElementList.addAll(pageBuilds);
            page++;
        }
        return buildElementList;
    }

    // получить часть от всех билдов в виде страницы
    public List<BuildElement> getPageBuilds(final String jobUrl, final int page, final int limit) {
        final List<BuildElement> buildElementList = new ArrayList<>();
        final JSONArray array = getPage(jobUrl + API_JSON_ALL_BUILDS, page, limit);
        if (Objects.nonNull(array) && !array.isEmpty()) {
            for (final Object o : array) {
                buildElementList.add(gson.fromJson(o.toString(), BuildElement.class));
            }
        }
        return buildElementList;
    }

    private JSONArray getPage(final String url, final int page, final int limit) {
        final String range = String.format("%%7B%d,%d%%7D", page * limit, (page + 1) * limit);
        final AtomicReference<HttpResponse<JsonNode>> response = new AtomicReference<>();
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final String rangedUrl = url + range;
        final BooleanSupplier waitWhenDataBeLoaded = () -> {
            try {
                final HttpResponse<JsonNode> r = unirest.get(rangedUrl).asJson();
                if (r.getStatus() >= HttpStatus.SC_BAD_REQUEST) {
                    log.warn("Неверный статус при получении страницы данных {}\n{}\n{}", r.getStatus(), rangedUrl, r);
                }
                response.set(r);
                return true;
            } catch (final Throwable e) {
                error.set(e);
                log.warn("Ошибка загрузки страницы данных из дженкинса");
                ESUtils.freeze(5000);
                return false;
            }
        };
        final boolean result = Timer.executeTimer(60, waitWhenDataBeLoaded);
        if (!result) {
            throw new JiraConnectionException(error.get());
        }
        JiraConnect.checkResponse(response.get(), url + range);
        return response.get().getBody().getObject().getJSONArray("allBuilds");
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
}
