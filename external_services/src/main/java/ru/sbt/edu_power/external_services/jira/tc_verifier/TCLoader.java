package ru.sbt.edu_power.external_services.jira.tc_verifier;

import com.google.gson.Gson;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import ru.sbt.edu_power.external_services.ESUtils;
import ru.sbt.edu_power.external_services.ExternalServicesException;
import ru.sbt.edu_power.external_services.PropReader;
import ru.sbt.edu_power.external_services.jira.JiraConnect;
import ru.sbt.edu_power.external_services.jira.JiraConnectionException;
import ru.sbt.edu_power.external_services.jira.JiraExporterUtils;
import ru.sbt.edu_power.external_services.jira.test_manager.model.TestCaseModel;
import ru.sbt.edu_power.external_services.timer.Timer;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
public class TCLoader {
    private static final String JIRA_LOGIN = JiraExporterUtils.decodeData(PropReader.get("jira.login"));
    private static final String JIRA_PASS = JiraExporterUtils.decodeData(PropReader.get("jira.pass"));
    private static final String MINIMAL_FIELDS = "id,status,customFieldValues,archived,projectId,priority";
    private static final String ALL_FIELDS = collectFields();
    private static final String SEARCH_URL = "https://jira.pcbltools.ru/jira/rest/tests/1.0/testcase/search";
    private static final String TEST_CASE_URL = "https://jira.pcbltools.ru/jira/rest/tests/1.0/testcase/";
    private static final String CUSTOM_FIELDS_URL = "https://jira.pcbltools.ru/jira/rest/tests/1.0/project/{PROJECT_ID}/customfields/testcase";
    private static final String FOLDERS_URL = "https://jira.pcbltools.ru/jira/rest/tests/1.0/project/{PROJECT_ID}/foldertree/testcase";
    private final Integer pageSize = 300;
    private int page;
    private JSONArray pageArray;

    public JSONArray download(final String key) {
        final TestCaseModel t = new Gson().fromJson(
                downloadData(key, null).get(0).toString(),
                TestCaseModel.class
        );
        decodeCustomFields(t, key);
        final TCQueryBuilder builder = new TCQueryBuilder();
        if (t.getArchived() || t.getTestType() == null || t.getTestView() == null || t.getTeam() == null) {
            builder.addField(TCFields.KEY_NAME, key);
        } else {
            builder.addField(TCFields.TEST_VIEW, t.getTestView())
                    .addField(TCFields.TEST_TYPE, t.getTestType())
                    .addField(TCFields.TEAM, t.getTeam());
        }
        builder.addField(TCFields.ARCHIVED, t.getArchived())
                .addField(TCFields.PROJECT_ID, TCFields.ProjectId.getById(t.getProjectId()));
        return downloadData(null, builder);
    }

    // для кейсов, загруженных нативно, все наборы кастомных полей включены в сам кейс, не нужно делать отдельную загрузку данных
    private void decodeCustomFields(final TestCaseModel model, final String key) {
        final EnumMap<TCFields, Object> data = new EnumMap<>(TCFields.class);
        for (final TestCaseModel.CustomFieldValues v : model.getCustomFieldValues()) {
            final TCFields field = TCFields.getFieldByName(v.getCustomField().getName());
            try {
                switch (field) {
                    case TEST_VIEW:
                        data.put(
                                field,
                                TCFields.TestView.getByValue(getOptionValue(
                                        v.getCustomField().getOptions(),
                                        v.getIntValue()
                                ), key)
                        );
                        break;
                    case TEST_TYPE:
                        data.put(
                                field,
                                TCFields.TestType.getByValue(getOptionValue(
                                        v.getCustomField().getOptions(),
                                        v.getIntValue()
                                ), key)
                        );
                        break;
                    case RISK:
                        final TCFields.Risk risk = Objects.isNull(v.getIntValue()) ? TCFields.Risk.UNDEFINED :
                                TCFields.Risk.getByValue(getOptionValue(
                                        v.getCustomField().getOptions(),
                                        v.getIntValue()
                                ), key);
                        data.put(field, risk);
                        break;
                    case TEAM:
                        data.put(field, getOptionValue(v.getCustomField().getOptions(), v.getIntValue()));
                        break;
                    case NOT_AUTOMATED_REASON:
                    case AUTOMATOR:
                        data.put(field, v.getStringValue());
                        break;
                    default:
                        // do nothing
                }
            } catch (final Throwable e) {
                log.error(
                        "При получении данных из тест-кейса {} получено исключение для поля {}\n{}",
                        key,
                        field,
                        new Gson().toJson(v)
                );
                throw new ExternalServicesException(e);
            }
        }
        model.setCustomFields(data);
    }

    // находим элемент в списке опций по известному id
    private String getOptionValue(final Set<TestCaseModel.CustomFieldValues.Option> options, final int valueId) {
        return options.stream()
                .filter(o -> o.getId() == valueId)
                .map(TestCaseModel.CustomFieldValues.Option::getName)
                .findFirst()
                .orElse("");
    }

    public JSONArray download(final TCQueryBuilder queryBuilder) {
        return downloadData(null, queryBuilder);
    }

    public JSONArray downloadCustomFieldsSchema(final Integer projectId) {
        HttpResponse<JsonNode> response = Unirest
                .get(CUSTOM_FIELDS_URL.replace("{PROJECT_ID}", projectId.toString()))
                .basicAuth(JIRA_LOGIN, JIRA_PASS)
                .asJson();
        JiraConnect.jiraSessionLogout(response);
        if (!response.isSuccess()) {
            throw new JiraConnectionException("Не удалось получить схему кастомных полей");
        }
        return response.getBody().getArray();
    }

    public JSONObject downloadFolderSchema(final Integer projectId) {
        HttpResponse<JsonNode> response = Unirest
                .get(FOLDERS_URL.replace("{PROJECT_ID}", projectId.toString()))
                .basicAuth(JIRA_LOGIN, JIRA_PASS)
                .asJson();
        JiraConnect.jiraSessionLogout(response);
        if (!response.isSuccess()) {
            throw new JiraConnectionException("Не удалось получить схему кастомных полей");
        }
        return response.getBody().getObject();
    }

    public JSONArray download(final TCFields field, final Object... dataType) {
        final TCQueryBuilder queryBuilder = new TCQueryBuilder();
        queryBuilder.addField(field, dataType);
        return downloadData(null, queryBuilder);
    }

    private synchronized JSONArray downloadData(final String testCaseKey, final TCQueryBuilder queryBuilder) {
        if (testCaseKey == null) {
            return downloadMany(queryBuilder);
        } else if (queryBuilder == null) {
            final JSONArray array = new JSONArray();
            array.put(downloadSingle(testCaseKey));
            return array;
        }
        throw new JiraConnectionException("Нельзя загружать одновременно тест-кейс и делать поиск по запросу");
    }

    private synchronized JSONObject downloadSingle(final String testCaseKey) {
        HttpResponse<JsonNode> response = Unirest
                .get(TEST_CASE_URL + testCaseKey)
                .basicAuth(JIRA_LOGIN, JIRA_PASS)
                .queryString("fields", MINIMAL_FIELDS)
                .asJson();
        JiraConnect.jiraSessionLogout(response);
        if (response.getStatus() == HttpStatus.SC_NOT_FOUND) {
            throw new JiraConnectionException(String.format(
                    "Идентификатор '%s' не является валидным ID тест-кейса",
                    testCaseKey
            ));
        }
        if (!response.isSuccess()) {
            throw new JiraConnectionException(String.format("Не удалось загрузить тест-кейс %s\n", testCaseKey));
        }
        return response.getBody().getObject();
    }

    private JSONArray downloadMany(final TCQueryBuilder queryBuilder) {
        page = 0;
        queryBuilder.addMainField("maxResults", pageSize)
                .addMainField("sort", "id:asc")
                .addMainField("fields", ALL_FIELDS);
        final JSONArray nodeArray = new JSONArray();
        Timer.startTimer("downloadMany");
        while (true) {
            pageableDownload(queryBuilder.asQuery());
            if (pageArray.isEmpty()) {
                break;
            }
            pageArray.forEach(nodeArray::put);
            page++;
        }
        log.info("Данные из джиры загружены {}", Timer.getDelta("downloadMany", "Данные из джиры загружены"));
        return nodeArray;
    }

    private void pageableDownload(final Map<String, Object> query, final boolean... stopRecursion) {
        query.put("startAt", page * pageSize);
        final HttpResponse<JsonNode> response = request(query);
        JiraConnect.checkResponse(response, SEARCH_URL, query);
        if (!response.getBody().getObject().getJSONArray("results").isEmpty()) {
            pageArray = response.getBody().getObject().getJSONArray("results");
        } else {
            pageArray = new JSONArray();
        }
    }

    // запрос выполняется повторно один раз, если был сбой сети
    private static synchronized HttpResponse<JsonNode> request(final Map<String, Object> query, final boolean... stopRecursion) {
        try {
            HttpResponse<JsonNode> response = Unirest
                    .get(SEARCH_URL)
                    .basicAuth(JIRA_LOGIN, JIRA_PASS)
                    .queryString(query)
                    .asJson();
            JiraConnect.jiraSessionLogout(response);
            return response;
        } catch (final UnirestException e) {
            if (stopRecursion.length > 0 && stopRecursion[0]) {
                throw new ExternalServicesException(e);
            }
            log.error("{}", e.toString());
            ESUtils.freeze(10000);
            return request(query, true);
        }
    }

    private static String collectFields() {
        final List<Object> list = new ArrayList<>();
        collectFields(list, TestCaseModel.class);
        return generateFieldLine(list, new StringBuilder());
    }

    // Получаю набор используемых полей из модели тест-кейса
    private static void collectFields(final List<Object> list, final Class<?> clazz) {
        for (final Field field : clazz.getDeclaredFields()) {
            final Class<?> fClass = field.getType();
            if (EnumMap.class.isAssignableFrom(fClass)) {
                continue;
            }
            final boolean isString = String.class.isAssignableFrom(fClass);
            final boolean isDate = Date.class.isAssignableFrom(fClass);
            final boolean isNumber = Number.class.isAssignableFrom(fClass);
            final boolean isBoolean = Boolean.class.isAssignableFrom(fClass);
            final boolean isSetOfString = Set.class.isAssignableFrom(fClass)
                    && String.class.isAssignableFrom(
                    (Class<?>) ((ParameterizedType) field.getGenericType())
                            .getActualTypeArguments()[0]
            );
            final boolean isSelfClass = clazz.isAssignableFrom(fClass);
            if (isString || isBoolean || isDate || isNumber || isSetOfString || isSelfClass) {
                list.add(field.getName());
            } else {
                final Map<String, List<Object>> innerMap = new HashMap<>();
                final List<Object> innerList = new ArrayList<>();
                innerMap.put(field.getName(), innerList);
                final Class<?> innerClass = Set.class.isAssignableFrom(field.getType()) ?
                        (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0] : field.getType();
                collectFields(innerList, innerClass);
                list.add(innerMap);
            }
        }
    }

    // преобразую набор полей модели тест-кейса в список полей для передачи в запрос
    private static String generateFieldLine(final List<Object> fields, final StringBuilder fieldLine) {
        for (final Object field : fields) {
            if (fieldLine.length() != 0) {
                fieldLine.append(",");
            }
            if (String.class.isAssignableFrom(field.getClass())) {
                fieldLine.append(field);
            } else {
                final Map.Entry<String, List<Object>> mapEntry = ((Map<String, List<Object>>) field)
                        .entrySet()
                        .iterator()
                        .next();
                fieldLine.append(mapEntry.getKey());
                fieldLine.append("(");
                fieldLine.append(generateFieldLine(mapEntry.getValue(), new StringBuilder()));
                fieldLine.append(")");
            }
        }
        return fieldLine.toString();
    }
}
