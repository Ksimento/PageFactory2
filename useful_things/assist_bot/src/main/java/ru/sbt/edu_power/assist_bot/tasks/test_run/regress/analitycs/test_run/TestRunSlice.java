package ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.test_run;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.json.JSONArray;
import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.jira.JiraConnect;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraUser;
import ru.sbt.edu_power.external_services.jira.tc_verifier.NewJiraTCCollector;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.Execution;
import ru.sbt.edu_power.external_services.jira.test_manager.model.TestCaseModel;
import ru.sbt.edu_power.assist_bot.services.slice.Slice;
import ru.sbt.edu_power.assist_bot.slack.SlackClient;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// срез текущего состояния тест-сета
@Slf4j
public class TestRunSlice extends Slice<Execution> {
    private static final long serialVersionUID = -5148629892146220126L;
    private final String testRunKey;
    // дата и время получения среза
    private final LocalDateTime dateTime = LocalDateTime.now(ZoneId.of("Europe/Moscow"));
    // пользователь -> статус прохождения тест-кейса -> экзекюшены
    private final Map<JiraUser, Map<String, List<Execution>>> userToExecutionStatusToExecutionsMap = new HashMap<>();
    // команда -> статус прохождения тест-кейса -> экзекюшены
    private final Map<String, Map<String, List<Execution>>> teamToExecutionStatusToToExecutionsMap = new HashMap<>();
    // статус прохождения тест-кейс -> экзекюшены
    private final Map<String, List<Execution>> statusToExecutionMap = new HashMap<>();
    private String initialUser;

    public TestRunSlice(final String testRunKey) {
        this.testRunKey = testRunKey;
    }

    public TestRunSlice(final String testRunKey, final String initialUser) {
        this.testRunKey = testRunKey;
        this.initialUser = initialUser;
    }

    public String getTestRunKey() {
        return testRunKey;
    }

    @Override
    public void load() {
        final HttpResponse<JsonNode> response = JiraConnect.testSetExecutionsGet(testRunKey);
        final JSONArray jsonArray = response.getBody().getArray();
        int errorCounter = 0;
        for (final Object element : jsonArray) {
            try {
                final Execution execution = Execution.GSON.fromJson(element.toString(), Execution.class);
                add(execution);
            } catch (final Throwable e) {
                log.error("{}", e.toString());
                log.info("{}", element);
                errorCounter++;
            }
        }
        if (errorCounter > 0 && Objects.nonNull(initialUser)) {
            SlackClient.sendText("При сборе данных из тест-сета возникли ошибки", initialUser);
        }
    }

    @Override
    public void update(final Iterable<Execution> dataForUpdate) {
        dataForUpdate.forEach(this::add);
    }

    // метод распределяет все полученные экзекюшены на два массива по пользователям и по командам - для удобства последующей аналитики
    public void splitExecutions() {
        userToExecutionStatusToExecutionsMap.clear();
        teamToExecutionStatusToToExecutionsMap.clear();
        statusToExecutionMap.clear();
        this.forEach(e -> {
                final String owner = e.getExecutedBy() == null ||
                                     e.getExecutedBy().isEmpty() ||
                                     "aft.integration".equals(e.getExecutedBy()) ? e.getAssignedTo() : e.getExecutedBy();
                final JiraUser user = JiraConnect.jiraGetUser(owner);
                final String status = e.getStatus();
                if (!statusToExecutionMap.containsKey(status)) {
                    statusToExecutionMap.put(status, new ArrayList<>());
                }
                statusToExecutionMap.get(status).add(e);
                updateMap(userToExecutionStatusToExecutionsMap, user, status, e);
                final TestCaseModel testCaseModel = NewJiraTCCollector.getInstance().getById(e.getTestCaseKey());
                final String team = testCaseModel.getTeam();
                updateMap(teamToExecutionStatusToToExecutionsMap, team, status, e);
            });

    }

    // метод добавляет данные в массивы
    private <T> void updateMap(
            final Map<T, Map<String, List<Execution>>> map,
            final T o,
            final String status,
            final Execution e
    ) {
        if (!map.containsKey(o)) {
            map.put(o, new HashMap<>());
        }
        if (!map.get(o).containsKey(status)) {
            map.get(o).put(status, new ArrayList<>());
        }
        map.get(o).get(status).add(e);
    }

    public Map<JiraUser, Map<String, List<Execution>>> getUserToExecutionStatusToExecutionsMap() {
        return userToExecutionStatusToExecutionsMap;
    }

    public Map<String, Map<String, List<Execution>>> getTeamToExecutionStatusToToExecutionsMap() {
        return teamToExecutionStatusToToExecutionsMap;
    }

    public Map<String, List<Execution>> getStatusToExecutionMap() {
        return statusToExecutionMap;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }
}
