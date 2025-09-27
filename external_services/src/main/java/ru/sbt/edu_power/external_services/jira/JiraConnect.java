package ru.sbt.edu_power.external_services.jira;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HttpHeaders;
import com.google.gson.Gson;
import kong.unirest.Config;
import kong.unirest.ContentType;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import ru.sbt.edu_power.external_services.ESUtils;
import ru.sbt.edu_power.external_services.PropReader;
import ru.sbt.edu_power.external_services.jira.agile.model.Issue;
import ru.sbt.edu_power.external_services.jira.agile.model.IssueQuery;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraUser;
import ru.sbt.edu_power.external_services.jira.test_manager.model.TestCase;
import ru.sbt.edu_power.external_services.jira.test_manager.model.TestCaseCreation;
import ru.sbt.edu_power.external_services.jira.test_manager.model.TestCaseFolderCreation;
import ru.sbt.edu_power.external_services.jira.test_manager.model.TestCaseSearch;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.TestRunBulkUpdateItems;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.TestRunCreateModel;
import ru.sbt.edu_power.external_services.timer.Timer;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.stream.IntStream;

@Slf4j
public class JiraConnect {
    private static final String JIRA_LOGIN = JiraExporterUtils.decodeData(PropReader.get("jira.login"));
    private static final String JIRA_PASS = JiraExporterUtils.decodeData(PropReader.get("jira.pass"));
    private static final String TEST_SET_REPORT = PropReader.get("jira.api.testset.report.by.testcase");
    private static final String TEST_CASE_SEARCH = PropReader.get("jira.api.testcase.search");
    private static final String TEST_SET_GET = PropReader.get("jira.api.testset.get");
    private static final String TEST_SET_GET_TEST_RESULT = PropReader.get("jira.api.testset.get.testresult");
    private static final String TEST_GET_TEST_RESULT_LATEST = PropReader.get("jira.api.testresult.latest");
    private static final String TEST_CASE_UPDATE = PropReader.get("jira.api.testcase.update");
    private static final String TEST_CASE_REST_UPDATE = PropReader.get("jira.rest.api.testcase.update");
    private static final String TEST_CASE_CREATE = PropReader.get("jira.api.testcase.create");
    private static final String TEST_RUN_CREATE = PropReader.get("jira.api.testrun");
    private static final String TEST_RUN_ITEMS_BULK_SAVE = PropReader.get("jira.api.testrun.item.bulk.save");
    private static final String JIRA_CREATE_ISSUE = PropReader.get("jira.api.issue");
    private static final String JIRA_SEARCH_ISSUES = PropReader.get("jira.api.issue.search");
    private static final String JIRA_TEST_SET_EXECUTIONS_GET = PropReader.get("jira.api.testset.executions.get");
    private static final String JIRA_TEST_RUN_LAST_TEST_RESULTS_GET = PropReader.get("jira.api.testrun.last.test.results");
    private static final String JIRA_TEST_CASE_EXECUTIONS_GET = PropReader.get("jira.api.testcase.executions");
    private static final String JIRA_GET_USER = PropReader.get("jira.api.get.user");
    private static final String JIRA_GET_PROJECT = PropReader.get("jira.api.project");
    private static final String TEST_CYCLES_FOLDERS = PropReader.get("jira.test.cycles.folder");
    private static final String TEST_CYCLES_SUBFOLDER_CREATE = PropReader.get("jira.test.cycles.subfolder.create");
    private static final String JIRA_VERSIONS = PropReader.get("jira.api.versions");
    private static final String JIRA_TEST_CASE_FOLDER_CREATE = PropReader.get("jira.api.testcase.folder.create");
    // Кэш для сохранения пользователей, полученных из джиры
    private static final Map<String, JiraUser> USER_MAP = new HashMap<>();
    public static final Gson GSON = new Gson();

    private static final String JIRA_SESSION_LOGOUT = PropReader.get("jira.api.session.logout");

    public static Map<String, JiraUser> getUserMap() {
        return USER_MAP;
    }

    // получаем идентификаторы версий в джире
    public static HttpResponse<JsonNode> getJiraVersions(final String projectId) {
        final String url = JIRA_VERSIONS.replace("{projectId}", projectId);
        HttpResponse<JsonNode> response = Unirest.get(url)
                .basicAuth(JIRA_LOGIN, JIRA_PASS)
                .asJson();
        jiraSessionLogout(response);
        checkResponse(response, url);
        return response;
    }

    // создаём поддиректории в разделе Cycles
    // index: 100
    // name: "Название поддиректории"
    // parentId: 215
    // projectId: 10101
    public static HttpResponse<JsonNode> createSubfolderInTestCycles(final Map<String, Object> map) {
        HttpResponse<JsonNode> response = Unirest.post(TEST_CYCLES_SUBFOLDER_CREATE)
                .basicAuth(JIRA_LOGIN, JIRA_PASS)
                .body(map)
                .asJson();
        jiraSessionLogout(response);
        checkResponse(response, TEST_CYCLES_SUBFOLDER_CREATE, map);
        return response;
    }

    public static String getProject(final String project, final boolean isEndsWith) {
        HttpResponse<JsonNode> response = Unirest.get(JIRA_GET_PROJECT)
                .basicAuth(JIRA_LOGIN, JIRA_PASS)
                .asJson();
        jiraSessionLogout(response);
        checkResponse(response, JIRA_GET_PROJECT);
        JSONArray jsonArray = response.getBody().getArray();
        Optional<String> idOptional = IntStream.range(0, jsonArray.length())
                .mapToObj(jsonArray::getJSONObject)
                .filter(jsonObject -> isEndsWith ? jsonObject.getString("name").toLowerCase().endsWith(project.toLowerCase().replace("-", " ")) : jsonObject.getString("name").equals(project))
                .map(jsonObject -> jsonObject.getString("id"))
                .findFirst();
        return idOptional.orElse(null);
    }

    // получаем список директорий из раздела Cycles по циферному ID проектной области
    public static HttpResponse<JsonNode> getTestCyclesFolderTree(final String projectId) {
        final String url = TEST_CYCLES_FOLDERS.replace("{projectId}", projectId);
        HttpResponse<JsonNode> response = Unirest.get(url)
                .basicAuth(JIRA_LOGIN, JIRA_PASS)
                .asJson();
        jiraSessionLogout(response);
        checkResponse(response, url);
        return response;
    }

    // получаем профиль пользователя по его логину
    public static JiraUser jiraGetUser(final String username) {
        if (Objects.isNull(username) || username.isEmpty()) {
            if (!USER_MAP.containsKey("empty_name")) {
                USER_MAP.put("empty_name", generateUnknownUser(username));
            }
            return USER_MAP.get("empty_name");
        }
        if (!USER_MAP.containsKey(username)) {
            final JiraUser user;
            final HttpResponse<JsonNode> responseByName = jiraGetUser(username, "username");
            if (responseByName.getStatus() == HttpStatus.SC_OK) {
                user = GSON.fromJson(responseByName.getBody().toString(), JiraUser.class);
            } else {
                // Некоторые пользователи представлены в джире не по имени пользователя, а по ключу
                final HttpResponse<JsonNode> responseByKey = jiraGetUser(username, "key");
                if (responseByKey.getStatus() == HttpStatus.SC_OK) {
                    user = GSON.fromJson(responseByKey.getBody().toString(), JiraUser.class);
                } else {
                    checkResponse(responseByKey, JIRA_GET_USER);
                    user = generateUnknownUser(username);
                }
            }
            USER_MAP.put(username, user);
        }
        return USER_MAP.get(username);
    }

    private static JiraUser generateUnknownUser(final String username) {
        return new JiraUser(
                Objects.isNull(username) || username.isEmpty() ? "Empty Name" : username,
                "no-email@no-email.no",
                "Пользователь не определён",
                "UTC",
                true,
                true
        );
    }

    private static HttpResponse<JsonNode> jiraGetUser(final String username, final String requestParam) {
        final Map<String, Object> query = new HashMap<>();
        query.put(requestParam, username);
        HttpResponse<JsonNode> response = Unirest.get(JIRA_GET_USER)
                .basicAuth(JIRA_LOGIN, JIRA_PASS)
                .queryString(query)
                .asJson();
        jiraSessionLogout(response);
        return response;
    }

    public static HttpResponse<JsonNode> jiraIssueCreate(final Issue issue) {
        HttpResponse<JsonNode> response = Unirest.post(JIRA_CREATE_ISSUE)
                .basicAuth(JIRA_LOGIN, JIRA_PASS)
                .body(issue)
                .asJson();
        jiraSessionLogout(response);
        return response;
    }

    public static synchronized JSONObject jiraIssueSearch(final IssueQuery query) {
        final JSONObject node = new JSONObject();
        node.put("issues", new JSONArray());
        int page = 0;
        while (true) {
            final HttpResponse<JsonNode> response = jiraIssueSearchPageable(query, page);
            page++;
            node.put("total", response.getBody().getObject().getInt("total"));
            if (response.getBody().getObject().getJSONArray("issues").isEmpty()) {
                break;
            }
            response.getBody().getObject().getJSONArray("issues")
                    .forEach(node.getJSONArray("issues")::put);
        }
        return node;
    }

    private static HttpResponse<JsonNode> jiraIssueSearchPageable(final IssueQuery query, final int page) {
        query.setStartAt(query.getMaxResults() * page);
        HttpResponse<JsonNode> response = Unirest.post(JIRA_SEARCH_ISSUES)
                .basicAuth(JIRA_LOGIN, JIRA_PASS)
                .body(query)
                .asJson();
        jiraSessionLogout(response);
        checkResponse(response, JIRA_SEARCH_ISSUES, query);
        return response;
    }

    //    Метод выполняет отправку данных в Jira
    public static HttpResponse<JsonNode> reportTestCaseToTestSet(
            final String testCaseId,
            final String data,
            final String testSetId
    ) {
        final String apiUrl = TEST_SET_REPORT
                .replace("{TESTSET}", testSetId)
                .replace("{TESTCASE}", testCaseId);
        HttpResponse<JsonNode> response = Unirest.put(apiUrl)
                .basicAuth(JIRA_LOGIN, JIRA_PASS)
                .body(data)
                .asJson();
        jiraSessionLogout(response);
        return response;
    }

    public static synchronized HttpResponse<JsonNode> testCaseSearch(final TestCaseSearch query) {
        final ObjectMapper objectMapper = new ObjectMapper();
        final Map<String, Object> data = objectMapper.convertValue(
                query,
                new TypeReference<Map<String, Object>>() {
                }
        );
        HttpResponse<JsonNode> response = Unirest.get(TEST_CASE_SEARCH)
                .basicAuth(JIRA_LOGIN, JIRA_PASS)
                .queryString(data)
                .asJson();
        jiraSessionLogout(response);
        return response;
    }

    public static HttpResponse<JsonNode> getTestSet(final String testSetId, final String fields) {
        final Map<String, Object> query = new HashMap<>();
        query.put("fields", fields);
        HttpResponse<JsonNode> response = Unirest.get(TEST_SET_GET.replace("{testRunKey}", testSetId))
                .basicAuth(JIRA_LOGIN, JIRA_PASS)
                .queryString(query)
                .asJson();
        jiraSessionLogout(response);
        return response;
    }

    // Выгружает данные из тест сета без шагов
    public static HttpResponse<JsonNode> getTestSetResult(final String testSetId) {
        HttpResponse<JsonNode> response = Unirest.get(TEST_SET_GET_TEST_RESULT.replace("{testRunKey}", testSetId))
                .basicAuth(JIRA_LOGIN, JIRA_PASS)
                .asJson();
        jiraSessionLogout(response);
        return response;
    }

    // Выгружает данные по последнему прогону теста
    public static HttpResponse<JsonNode> getTestResultLatest(final String testCaseKey) {
        HttpResponse<JsonNode> response = Unirest.get(TEST_GET_TEST_RESULT_LATEST.replace("{testCaseKey}", testCaseKey))
                .basicAuth(JIRA_LOGIN, JIRA_PASS)
                .asJson();
        jiraSessionLogout(response);
        return response;
    }

    // Метод для обновления полей через rest запрос
    public static HttpResponse<JsonNode> testCaseUpdateRest(
            String testCaseId, String projectId, String field, String fieldValue
    ) {
        String url = TEST_CASE_REST_UPDATE.replace("{testCaseId}", testCaseId);
        String body = "{\"id\":{testCaseId},\"projectId\":{projectId},\"{field}\":{fieldValue},\"testData\":[],\"parameters\":[]}"
                .replace("{testCaseId}", testCaseId)
                .replace("{projectId}", projectId)
                .replace("{field}", field)
                .replace("{fieldValue}", fieldValue);
        HttpResponse<JsonNode> response = Unirest.put(url)
                .basicAuth(JIRA_LOGIN, JIRA_PASS)
                .body(body)
                .asJson();
        jiraSessionLogout(response);
        return response;
    }

    // Выполнение запроса на обновление данных
    public static HttpResponse<JsonNode> testCaseUpdate(final String testCaseKey, final TestCase data) {
        HttpResponse<JsonNode> response = Unirest.put(TEST_CASE_UPDATE.replace("{testCaseKey}", testCaseKey))
                .basicAuth(JIRA_LOGIN, JIRA_PASS)
                .body(data)
                .asJson();
        jiraSessionLogout(response);
        return response;
    }

    // Выполнение запроса на обновление данных
    public static HttpResponse<JsonNode> testCaseUpdate(final String testCaseKey, final Object data) {
        final String url = TEST_CASE_UPDATE.replace("{testCaseKey}", testCaseKey);
        HttpResponse<JsonNode> response = Unirest.put(url)
                .basicAuth(JIRA_LOGIN, JIRA_PASS)
                .body(data)
                .asJson();
        jiraSessionLogout(response);
        checkResponse(response, url, data);
        return response;
    }

    // Создание тест-кейса
    public static HttpResponse<JsonNode> testCaseCreate(final TestCaseCreation testCaseCreation) {
        HttpResponse<JsonNode> response = Unirest.post(TEST_CASE_CREATE)
                .basicAuth(JIRA_LOGIN, JIRA_PASS)
                .body(testCaseCreation)
                .asJson();
        jiraSessionLogout(response);
        JiraConnect.checkResponse(response, TEST_CASE_CREATE, testCaseCreation);
        return response;
    }

    // Создание директории для тест-кейса
    public static HttpResponse<JsonNode> testCaseFolderCreate(final TestCaseFolderCreation model) {
        HttpResponse<JsonNode> response = Unirest.post(JIRA_TEST_CASE_FOLDER_CREATE)
                .basicAuth(JIRA_LOGIN, JIRA_PASS)
                .body(model)
                .asJson();
        jiraSessionLogout(response);
        checkResponse(response, JIRA_TEST_CASE_FOLDER_CREATE, model);
        return response;
    }

    // Создание тест-сета
    public static HttpResponse<JsonNode> testRunCreate(final TestRunCreateModel testRunCreateModel) {
        HttpResponse<JsonNode> response = Unirest.post(TEST_RUN_CREATE)
                .basicAuth(JIRA_LOGIN, JIRA_PASS)
                .body(testRunCreateModel)
                .asJson();
        jiraSessionLogout(response);
        checkResponse(response, TEST_RUN_CREATE, testRunCreateModel);
        return response;
    }

    // Добавление тест-кейсов в тест-сет
    public static HttpResponse<?> testRunItemsBulkUpdate(final TestRunBulkUpdateItems update) {
        HttpResponse<JsonNode> response = Unirest.put(TEST_RUN_ITEMS_BULK_SAVE)
                .basicAuth(JIRA_LOGIN, JIRA_PASS)
                .body(update)
                .asJson();
        jiraSessionLogout(response);
        return response;
    }

    // Получение списка результатов прохождения регресса
    public static synchronized HttpResponse<JsonNode> testSetExecutionsGet(final String testRunKey) {
        final String url = JIRA_TEST_SET_EXECUTIONS_GET.replace("{testRunKey}", testRunKey);
        HttpResponse<JsonNode> response = Unirest.get(url)
                .basicAuth(JIRA_LOGIN, JIRA_PASS)
                .asJson();
        jiraSessionLogout(response);
        checkResponse(response, url);
        return response;
    }

    // Получение специальных данных last test result - это специфичные сущности из тест-сета
    // Не являются экзекушенами, которые получают в testSetExecutionsGet()
    public static HttpResponse<JsonNode> testRunLastTestResultGet(final Integer testRunId) {
        final String url = JIRA_TEST_RUN_LAST_TEST_RESULTS_GET.replace("{testRunId}", testRunId.toString());
        HttpResponse<JsonNode> response = Unirest.get(url)
                .basicAuth(JIRA_LOGIN, JIRA_PASS)
                .asJson();
        jiraSessionLogout(response);
        checkResponse(response, url);
        return response;
    }

    // Получение списка результатов прохождения тест-кейса по ID тест-кейса
    public HttpResponse<JsonNode> testCaseExecutionsGetNoStatic(final String testCaseId) {
        return testCaseExecutionsGetNoStatic(testCaseId, 100, 0);
    }

    public HttpResponse<JsonNode> testCaseExecutionsGetNoStatic(
            final String testCaseId,
            final int limit,
            final int offset
    ) {
        final String url = JIRA_TEST_CASE_EXECUTIONS_GET.replace("{testCaseId}", testCaseId);
        final String fields = "testResultStatus(name),automated,testCase,testRun,executionTime,executionDate,issueLinks";
        HttpResponse<JsonNode> response = Unirest
                .get(url)
                .basicAuth(JIRA_LOGIN, JIRA_PASS)
                .queryString("fields", fields)
                .queryString("limit", limit)
                .queryString("offset", offset)
                .asJson();
        jiraSessionLogout(response);
        return response;
    }

    // Получение списка результатов прохождения тест-кейса по ID тест-кейса
    public static HttpResponse<JsonNode> testCaseExecutionsGet(final String testCaseId) {
        return testCaseExecutionsGet(testCaseId, 100, 0);
    }

    public static HttpResponse<JsonNode> testCaseExecutionsGet(
            final String testCaseId,
            final int limit,
            final int offset
    ) {
        final String url = JIRA_TEST_CASE_EXECUTIONS_GET.replace("{testCaseId}", testCaseId);
        final String fields = "testResultStatus(name),automated,testCase,testRun,executionTime,executionDate,issueLinks";
        HttpResponse<JsonNode> response = Unirest.get(url)
                .basicAuth(JIRA_LOGIN, JIRA_PASS)
                .queryString("fields", fields)
                .queryString("limit", limit)
                .queryString("offset", offset)
                .asJson();
        return response;
    }

    public static void checkResponse(final HttpResponse<?> response, final Object request, final Object... body) {
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
        throw new JiraConnectionException(message);
    }

    public static void configureConnection() {
        configAuth(Unirest.config())
                .setDefaultHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                .setDefaultHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.toString());

    }

    public static Config configAuth(final Config unirestConfig) {
        try (final InputStream is = JiraConnect.class.getClassLoader().getResourceAsStream("user_auth.pfx")) {
            final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            final String CERT_PASS = JiraExporterUtils.decodeData(PropReader.get("cert.pass"));
            ks.load(is, CERT_PASS.toCharArray());
            return unirestConfig.clientCertificateStore(ks, CERT_PASS);
        } catch (final IOException | KeyStoreException | CertificateException | NoSuchAlgorithmException e) {
            throw new JiraConnectionException(e);
        }
    }

    public static TestRunCreateModel getTestRunByKey(final String testRunKey) {
        final String url = TEST_RUN_CREATE +
                "/" +
                testRunKey +
                "?fields=id,key,projectId,name,projectVersionId,plannedStartDate,plannedEndDate,testCaseCount";
        HttpResponse<JsonNode> response = Unirest.get(url)
                .basicAuth(JIRA_LOGIN, JIRA_PASS)
                .asJson();
        jiraSessionLogout(response);
        checkResponse(response, url);
        return GSON.fromJson(response.getBody().toString(), TestRunCreateModel.class);
    }

    /**
     * Выполняем разлогин из текущей сессии в Jira
     */
    public static void jiraSessionLogout(final HttpResponse<JsonNode> response) {
        try {
            response.getHeaders().all().forEach(h -> {
                if (h.getValue().contains("atlassian.xsrf.token")) {
                    BooleanSupplier sessionLogout = () -> {
                        String newToken = h.getValue().substring(h.getValue().indexOf("=") + 1, h.getValue().indexOf(";"));
                        HttpResponse<JsonNode> newResponseClosed = Unirest.get(JIRA_SESSION_LOGOUT + newToken).asJson();
                        if (newResponseClosed.getStatus() != 200) {
                            log.info(newResponseClosed.getStatusText());
                            log.info(newResponseClosed.getParsingError().toString());
                            log.info(newResponseClosed.getRequestSummary().toString());
                            log.info("Не удалось закрыть сессию");
                            ESUtils.freeze(1000);
                            log.info("Повторно закрываю сессию");
                            return false;
                        }
                        return true;
                    };
                    Timer.executeTimer(30, sessionLogout);
                }
            });
        } catch (Exception e) {
            log.error("Возникла ошибка при закрытии сессии");
        }
    }
}
