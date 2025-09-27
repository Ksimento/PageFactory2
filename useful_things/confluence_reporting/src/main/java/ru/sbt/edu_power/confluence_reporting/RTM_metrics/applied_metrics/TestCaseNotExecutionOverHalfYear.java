package ru.sbt.edu_power.confluence_reporting.RTM_metrics.applied_metrics;

import com.google.gson.Gson;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import ru.sbt.edu_power.confluence_reporting.RTM_metrics.RtmMetricsCalc;
import ru.sbt.edu_power.confluence_reporting.TableTune;
import ru.sbt.edu_power.external_services.jira.JiraConnect;
import ru.sbt.edu_power.external_services.jira.JiraConnectionException;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;
import ru.sbt.edu_power.external_services.jira.test_manager.model.TestCaseModel;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.MassExecution;
import ru.sbt.edu_power.external_services.timer.Timer;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.stream.Collectors;

// Метрика находит тест-кейсы, которые долгое время не запускаются
public class TestCaseNotExecutionOverHalfYear extends AbstractMetrics {
    // задаём максимальное время которое регрессионный тест не запускался - полгода
    private static final int MAX_NOT_EXECUTION_DAYS = 365 / 2;
    private final int expected = 0;
    private final TableTune.BgColor bgColor = TableTune.BgColor.RED;
    private final Map<String, List<String>> teamToTestCaseList = new HashMap<>();
    private final Gson gson = new Gson();
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private final LocalDateTime current = LocalDateTime.now();

    public TestCaseNotExecutionOverHalfYear(final RtmMetricsCalc calc) {
        super(calc);
    }

    @Override
    public Function<Integer, Boolean> predicate() {
        return actual -> actual > expected;
    }

    @Override
    public Function<Integer, Boolean> predicateNeedAutomate() {
        return null;
    }

    @Override
    public BiFunction<String, Integer, String> descriptionFunction() {
        return (team, size) -> String.format(
                "Тест-кейсы не запускались более полугода: %s",
                String.join(", ", teamToTestCaseList.get(team))
        );
    }

    @Override
    public TableTune.BgColor getBgColor() {
        return bgColor;
    }

    @Override
    public Map<String, Integer> getTeamToSizeMap() {
        final Map<String, Integer> map = new HashMap<>();
        calc.getRtmCollector().getUiCaseListByTeam().forEach((team, list) -> {
            final Map<TestCaseModel, MassExecution> executions = new HashMap<>();
            list.values()
                .parallelStream()
                .filter(tc -> !tc.getTestType().value.equals(TCFields.TestType.NOT_FOR_REGRESS.value))
                .forEach(tc -> {
                    if (TCFields.Status.DISABLED.value.equals(tc.getStatus().getName())) {
                        return;
                    }
                    AtomicReference<HttpResponse<JsonNode>> atomicReference = new AtomicReference<>();
                    BooleanSupplier getResponse = () -> {
                        try {
                            Thread.sleep(800);
                        } catch (InterruptedException ignored) {
                        }
                        HttpResponse<JsonNode> tempResponse = JiraConnect.testCaseExecutionsGet(
                                tc.getId().toString(),
                                10,
                                0
                        );
                        try {
                            if (tempResponse.getBody().getObject() != null) {
                                atomicReference.set(tempResponse);
                                return true;
                            } else {
                                return false;
                            }
                        } catch (NullPointerException e) {
                            return false;
                        }
                    };
                    try {
                        Timer.executeTimer(6, getResponse);
                    } catch (final JiraConnectionException e) {
                        return;
                    }
                    HttpResponse<JsonNode> response = atomicReference.get();
                    if (response.getBody().getObject().has("totalCount") &&
                        response.getBody().getObject().getInt("totalCount") != 0) {
                        for (final Object o : response.getBody().getObject().getJSONArray("data")) {
                            final MassExecution me = gson.fromJson(o.toString(), MassExecution.class);
                            if (Objects.isNull(me.getExecutionDate())) {
                                // если дата выполнения экзекушена не задана, проверяем следующий
                                continue;
                            }
                            final LocalDateTime date = LocalDateTime.parse(me.getExecutionDate(), dateTimeFormatter);
                            if (Duration.between(date, current).toDays() > MAX_NOT_EXECUTION_DAYS) {
                                executions.put(tc, me);
                            }
                            // если первый попавшийся экзекушн не старее полугода, переходим к следующему тест-кейсу
                            return;
                        }
                    }
                    // если экзекушенов нет или если первые 10 экзекушенов не имеют даты
                    // запуска - считаем что тест не выполняется, при условии что время
                    // создания теста превышает максимально допустимое
                    final Duration testCaseOld = Duration.between(LocalDateTime.ofInstant(
                            tc.getCreatedOn().toInstant(),
                            ZoneId.systemDefault()
                    ), current);
                    if (testCaseOld.toDays() > MAX_NOT_EXECUTION_DAYS) {
                        executions.put(tc, null);
                    }
                });
            map.put(team, executions.size());
            teamToTestCaseList.put(
                    team,
                    executions.entrySet()
                              .stream()
                              .map(e -> e.getKey().getKey() + formatExecution(e.getValue()))
                              .collect(Collectors.toList())
            );
        });
        return map;
    }

    private String formatExecution(final MassExecution execution) {
        if (Objects.isNull(execution)) {
            return " (тест кейс никогда не добавлялся в тест-сет или не был пройден в течении последних 10 регрессов)";
        }
        return " (последний запуск " +
               Duration.between(LocalDateTime.parse(execution.getExecutionDate(), dateTimeFormatter), current)
                       .toDays() +
               " дней назад)";
    }
}
