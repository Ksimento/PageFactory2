package ru.sbt.edu_power.external_services.jira.tc_verifier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.UnirestParsingException;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import ru.sbt.edu_power.external_services.jira.JiraConnect;
import ru.sbt.edu_power.external_services.jira.JiraConnectionException;
import ru.sbt.edu_power.external_services.jira.test_manager.TMFields;
import ru.sbt.edu_power.external_services.jira.test_manager.model.TestCaseSearch;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbt.edu_power.external_services.validator.Validator;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Класс выполняет запрос данных из джиры и кеширует их, позволяя избежать повторных запросов
 */
@Slf4j
@Deprecated
public final class JiraTestCaseCollector {
    private final Map<String, JsonNode> testCasesByProjectKey = new ConcurrentHashMap<>();
    // сохраняет информацию о том, для каких видов тестов получены данные
    private final Map<String, List<TestView>> testViewMap = new HashMap<>();
    // projectKey -> fieldName -> testCaseKey -> fieldValue
    private final Map<String, Map<TMFields, Map<String, String>>> jiraFieldMap = new ConcurrentHashMap<>();
    private static final JiraTestCaseCollector INSTANCE = new JiraTestCaseCollector();
    private static String projectKey;

    private JiraTestCaseCollector() {
        projectKey = System.getProperty("jira.project.key");
    }

    public static JiraTestCaseCollector getInstanceUI() {
        INSTANCE.collectDataByTestView(TestView.UI);
        return INSTANCE;
    }

    public static JiraTestCaseCollector getInstanceAPI() {
        INSTANCE.collectDataByTestView(TestView.API);
        return INSTANCE;
    }

    public static void setProjectKey(final String projectKey) {
        JiraTestCaseCollector.projectKey = projectKey;
    }

    private synchronized void collectDataByTestView(final TestView testView) {
        Assert.assertNotNull("Не задан projectKey", projectKey);
        if (!testViewMap.containsKey(projectKey)) {
            testViewMap.put(projectKey, new ArrayList<>());
        }
        if (!testViewMap.get(projectKey).contains(testView)) {
            Timer.startTimer("tcLoader");
            for (final TMFields.Priority priority : TMFields.Priority.values()) {
                collectData(getQuery(testView, priority));
                final EnumMap<TMFields, String> query = new EnumMap<>(TMFields.class);
                query.put(TMFields.PRIORITY, priority.getPriorityName());
                log.info(
                        "Загружено кейсы с приоритетом {} за время {}",
                        priority.getPriorityName(),
                        Timer.getDelta("tcLoader", priority.getPriorityName())
                );
            }
            testViewMap.get(projectKey).add(testView);
            jiraFieldMap.remove(projectKey);
        }
    }

    public JsonNode getNode() {
        return testCasesByProjectKey.get(projectKey);
    }

    // получение значения поля из тест-кейса
    public String getFieldValue(final String testCaseKey, final TMFields fieldName) {
        final Map<String, String> map = getFieldMap(fieldName);
        Assert.assertTrue(String.format(
                "Тест-кейс \"%s\" отсутствует в РТМ в проектной области \"%s\"",
                testCaseKey,
                projectKey
        ), jiraFieldMap.get(projectKey).get(fieldName).containsKey(testCaseKey));
        return map.get(testCaseKey);
    }

    // получение списка значений поля из тест-кейса
    public List<String> getFieldValueList(final String testCaseKey, final TMFields fieldName) {
        return Stream.of(
                getFieldValue(testCaseKey, fieldName).split(fieldName.getSeparator())
        )
                     .map(value -> value.replaceAll("\"", ""))
                     .map(String::trim)
                     .filter(t -> !t.isEmpty())
                     .collect(Collectors.toList());
    }

    // получение мапы из номеров тест-кейсов и соответствующих значений нужного поля
    public Map<String, String> getFieldMap(final TMFields fieldName) {
        collectDataByField(fieldName);
        return jiraFieldMap.get(projectKey).get(fieldName);
    }

    // Метод устанавливает projectKey из входящей мапы и возвращает мапу для запроса
    public static EnumMap<TMFields, String> generateQueryWithProjectKey(final Map<String, String> query) {
        JiraTestCaseCollector.projectKey = query.get("projectKey");
        query.remove("projectKey");
        final EnumMap<TMFields, String> enumMap = new EnumMap<>(TMFields.class);
        query.forEach((k, v) -> enumMap.put(TMFields.getByFieldName(k), v));
        return enumMap;
    }

    public List<String> filterCase(final EnumMap<TMFields, String> query) {
        query.keySet().forEach(this::collectDataByField);
        final Set<String> testCaseKeyList = jiraFieldMap.get(projectKey).get(query.keySet().iterator().next()).keySet();
        return testCaseKeyList
                .stream()
                .filter(tag ->
                        query
                                .keySet()
                                .stream()
                                .allMatch(field -> {
                                    if (null != field.getSeparator()) {
                                        return Stream.of(
                                                jiraFieldMap
                                                        .get(projectKey)
                                                        .get(field)
                                                        .get(tag)
                                                        .split(field.getSeparator()))
                                                     .map(t -> t.replaceAll("\"", "").trim())
                                                     .collect(Collectors.toList())
                                                     .contains(query.get(field));
                                    } else {
                                        if (query.get(field).contains(" OR ")) {
                                            return Stream.of(query.get(field).split(" OR "))
                                                         .anyMatch(v -> Validator.matchValues(
                                                                 jiraFieldMap.get(projectKey).get(field).get(tag),
                                                                 v
                                                         ));
                                        }
                                        return Validator.matchValues(
                                                jiraFieldMap.get(projectKey).get(field).get(tag),
                                                query.get(field)
                                        );
                                    }
                                })
                )
                .collect(Collectors.toList());
    }

    private synchronized void collectDataByField(final TMFields jiraFieldName) {
        if (!jiraFieldMap.containsKey(projectKey)) {
            jiraFieldMap.put(projectKey, new EnumMap<>(TMFields.class));
        }
        if (jiraFieldMap.get(projectKey).containsKey(jiraFieldName)) {
            return;
        }
        jiraFieldMap.get(projectKey).put(jiraFieldName, new HashMap<>());
        for (final Object o : getNode().getArray()) {
            final JSONObject object = (JSONObject) o;
            String value;
            final JSONObject refinedObject = jiraFieldName.isCustomField() ? object.getJSONObject("customFields") : object;
            if (refinedObject.has(jiraFieldName.getFieldName())) {
                if (jiraFieldName.isMultiple()) {
                    value = refinedObject.getJSONArray(jiraFieldName.getFieldName()).join(jiraFieldName.getSeparator());
                } else {
                    try {
                        value = refinedObject.getString(jiraFieldName.getFieldName());
                    } catch (final UnsupportedOperationException e) {
                        value = refinedObject.getJSONObject(jiraFieldName.getFieldName()).toString();
                    }
                }
            } else {
                value = "";
            }
            jiraFieldMap.get(projectKey).get(jiraFieldName).put(object.getString("key"), value);
        }
    }

    private TestCaseSearch getQuery(final TestView testView, final TMFields.Priority priority) {
        final TestCaseSearch query = new TestCaseSearch();
        final String MAX_RESULTS_NUMBER = "10000";
        final String fields = Stream.of(TMFields.values())
                                    .filter(f -> !f.isCustomField())
                                    .map(TMFields::getFieldName)
                                    .collect(
                                            Collectors.joining(",")) + ",customFields,key";
        query.addParam("projectKey", projectKey)
             .addParam("maxResults", MAX_RESULTS_NUMBER)
             .addParam("fields", fields)
             .addParam("Вид теста", testView.getValue())
             .addParam(TMFields.PRIORITY.getFieldName(), priority.getPriorityName())
             .build();
        return query;
    }

    private void collectData(final TestCaseSearch query) {
        final HttpResponse<JsonNode> httpResponse = JiraConnect.testCaseSearch(query);
        if (!httpResponse.isSuccess()) {
            final Optional<UnirestParsingException> unirestParsingException = httpResponse.getParsingError();
            final String message = Objects.nonNull(httpResponse.getBody()) ? httpResponse.getBody().toPrettyString() : "";
            final Gson gson = new GsonBuilder().setPrettyPrinting().create();
            final String json = gson.toJson(query);
            throw new JiraConnectionException(String.format(
                    "Ошибка при выполнении запроса: %d\n%s\n%s\n%s",
                    httpResponse.getStatus(),
                    message,
                    json,
                    unirestParsingException.orElse(new UnirestParsingException("", null)).getOriginalBody()
            ));
        }
        if (!testCasesByProjectKey.containsKey(projectKey)) {
            testCasesByProjectKey.put(projectKey, new JsonNode(null));
        }
        final JSONArray currentData = testCasesByProjectKey.get(projectKey).getArray();
        if (currentData.length() == 1) {
            if ("{}".equals(currentData.get(0).toString())) {
                currentData.remove(0);
            }
        }
        final JSONArray responseData = httpResponse.getBody().getArray();
        for (final Object object : responseData) {
            currentData.put(object);
        }
        testCasesByProjectKey.replace(
                projectKey,
                new JsonNode(
                        currentData.toString()
                )
        );

    }

    public synchronized void updateFieldMap(
            final TMFields field,
            final String testCaseKey,
            final String newValue
    ) {
        Assert.assertNotNull("Нет данных по проекту " + projectKey, jiraFieldMap.get(projectKey));
        Assert.assertNotNull("Нет данных по полю " + field, jiraFieldMap.get(projectKey).get(field));
        jiraFieldMap.get(projectKey).get(field).replace(testCaseKey, newValue);
    }

    public static String getProjectKey() {
        if (projectKey == null) {
            projectKey = System.getProperty("jira.project.key");
        }
        return projectKey;
    }

    public enum TestView {
        UI("UI;Верстка"),
        API("API");

        private final String value;

        TestView(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
