package ru.sbt.edu_power.confluence_reporting.regress_metrics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.json.JSONArray;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import org.junit.Assert;
import ru.sbt.edu_power.external_services.confluence.ConfluenceDocument;
import ru.sbt.edu_power.external_services.confluence.TableHeaderInterface;
import ru.sbt.edu_power.external_services.jira.JiraConnect;
import ru.sbt.edu_power.external_services.jira.tc_verifier.NewJiraTCCollector;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.Execution;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.ExecutionStatus;
import ru.sbt.edu_power.external_services.nexus.NexusConnect;
import ru.sbt.edu_power.external_services.nexus.NexusRepo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Класс выполняет генерацию отчёта по регрессу в разрезе команд
 */
@Slf4j
public class MeasureHandleRegressTime extends ConfluenceDocument {
    private final String testSetId;
    private static final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").create();
    private final Map<String, List<Execution>> teamToExecutionsMap = new HashMap<>();
    private final EnumMap<TableHeader, Map<String, Long>> teamToDataMap = new EnumMap<>(TableHeader.class);
    private final Map<String, String> regressAutotestLinks = new HashMap<>();

    public MeasureHandleRegressTime(final String pageID) {
        super(pageID);
        testSetId = System.getProperty("jira.testset.id");
    }

    public void addLink(final String name, final String link) {
        regressAutotestLinks.put(name, link);
    }

    public MeasureHandleRegressTime(final String pageID, final String testSetId) {
        super(pageID);
        this.testSetId = testSetId;
    }

    public void generate() {
        Assert.assertNotNull("Не заполнено поле тест-сета", testSetId);
        loadDocument();
        log.info("Документ загружен");
        loadTestRunExecutions();
        log.info("Загружены данные из тест-сета {}", testSetId);
        calculateData();
        log.info("Рассчёты для таблицы выполнены");
        removeTableData();
        MetricsMethods.updateTestSetId(getDocument(), testSetId);
        MetricsMethods.updateAutotestLinks(getDocument(), regressAutotestLinks);
        insertHeaderToDocument(Stream
                .of(TableHeader.values())
                .collect(Collectors.toList()), this::formatHeader);
        try {
            uploadDataToNexus();
            log.info("Данные выгружены в Nexus");
        } catch (final Throwable e) {
            log.error("не удалось выгрузить данные в NEXUS: {}", e.getLocalizedMessage());
        }
        insertDataToTable();
        insertTotalRowToTable();
        log.info("Таблица построена");
        saveDocument();
        log.info("Таблица загружена в Confluence");
    }

    private String formatHeader(final TableHeaderInterface header) {
        switch ((TableHeader) header) {
            case DESCRIPTION:
                return formatHeader(header.getColName(), 150);
            case TEAM:
                return formatHeader(header.getColName(), 45);
            default:
                return header.getColName();
        }
    }

    private void uploadDataToNexus() {
        final JsonObject jo = gson.toJsonTree(teamToDataMap).getAsJsonObject();
        jo.addProperty("test-set-key", testSetId);
        // добавляю в json ссылки на прогоны автотестов
        regressAutotestLinks.forEach(jo::addProperty);
        final SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmmss");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        final String fileName = format.format(new Date()) + "_regress_by_teams.json";
        final String folder = "REGRESS/" + testSetId.split("-")[0];
        NexusConnect.getInstance().uploadData(NexusRepo.JAVA_E2E_STATS, jo.toString().getBytes(), folder, fileName);
    }

    private void loadTestRunExecutions() {
        final List<Execution> executionList = TestRunExecutions.load(testSetId);
        for (final Execution element : executionList) {
            try {
                final String team = NewJiraTCCollector.getInstance().getById(element.getTestCaseKey()).getTeam();
                if (Objects.isNull(team) || team.isEmpty()) {
                    continue;
                }
                if (!teamToExecutionsMap.containsKey(team)) {
                    teamToExecutionsMap.put(team, new ArrayList<>());
                }
                teamToExecutionsMap.get(team).add(element);
            } catch (final AssertionError ignored) {
            }
        }
    }

    private void calculateData() {
        // Собираем общее количество тестов по командам
        teamToDataMap.put(TableHeader.TOTAL_CASES, MetricsMethods.totalCasesCalc(teamToExecutionsMap));

        // Собираем количество исполнителей в команде
        calcMembers();

        // Собираем общее количество тестов в статусе AutoPass
        teamToDataMap.put(TableHeader.AUTO_PASSED, MetricsMethods.notExecutedCalcInContextStatuses(teamToExecutionsMap,
                ExecutionStatus.AUTO_PASS));

        // Собираем количество тестов пройденных руками
        teamToDataMap.put(TableHeader.HANDLE, MetricsMethods.notExecutedCalcInContextStatuses(teamToExecutionsMap,
                ExecutionStatus.BLOCKED, ExecutionStatus.FAIL, ExecutionStatus.PASS, ExecutionStatus.N_A));

        // Собираем время старта регресса
        teamToDataMap.put(TableHeader.START_DATE, MetricsMethods.startDateCalc(teamToExecutionsMap));

        // Собираем время окончания регресса
        teamToDataMap.put(TableHeader.END_DATE, MetricsMethods.endDateCalc(teamToExecutionsMap));

        // Собираем количество проваленных тестов
        teamToDataMap.put(TableHeader.BROKEN, MetricsMethods.notExecutedCalcInContextStatuses(teamToExecutionsMap,
                ExecutionStatus.BLOCKED, ExecutionStatus.FAIL));

        // Собираем количество проваленных тестов с привязанными дефектами
        teamToDataMap.put(TableHeader.BROKEN_WITH_BUG, MetricsMethods.brokenWithBugCalc(teamToExecutionsMap));

        // Собираем количество не актуальных тестов
        teamToDataMap.put(TableHeader.NOT_ACTUAL, MetricsMethods.notExecutedCalcInContextStatuses(teamToExecutionsMap,
                ExecutionStatus.N_A));

        // Собираем количество не запущенных тестов
        teamToDataMap.put(TableHeader.NOT_EXECUTED, MetricsMethods.notExecutedCalcInContextStatuses(teamToExecutionsMap,
                ExecutionStatus.AUTO_FAIL, ExecutionStatus.NOT_EXECUTED, ExecutionStatus.IN_PROGRESS));

        // Собираем время ручного прохождения
        calcHandleTime();

        // вычисляем трудоёмкость команды в человекочасах
        // вычисляем среднее время прохождения кейса в человекочасах
        calcHumanHours();

        // вычисляем среднее время, требуемое для прохождения одного теста
        calcSingleTestTime();
    }

    // Собираем количество исполнителей в команде
    private void calcMembers() {
        final Map<String, Long> membersMap = new HashMap<>();
        teamToExecutionsMap.forEach((team, list) -> {
            final Set<String> members = new HashSet<>();
            list.stream()
                .map(MetricsMethods::getExecutedBy)
                .forEach(members::add);
            membersMap.put(team, (long) members.size());
        });
        teamToDataMap.put(TableHeader.MEMBERS, membersMap);
    }

    // Собираем время ручного прохождения
    private void calcHandleTime() {
        final Map<String, Long> handleTimeMap = new HashMap<>();
        teamToExecutionsMap.forEach((team, list) -> {
            Date start = null;
            Date end = null;
            for (final Execution execution : list) {
                final ExecutionStatus status = ExecutionStatus.getStatusByName(execution.getStatus());
                if (isHandleStatus(status) || status == ExecutionStatus.IN_PROGRESS) {
                    if (execution.getActualStartDate() != null) {
                        if (start == null || execution.getActualStartDate().before(start)) {
                            start = execution.getActualStartDate();
                        }
                    }
                    if (execution.getActualEndDate() != null) {
                        if (end == null || execution.getActualEndDate().after(end)) {
                            end = execution.getActualEndDate();
                        }
                    }
                }
            }
            final long range;
            if (end != null && start != null) {
                range = end.getTime() - start.getTime();
            } else {
                range = 0L;
            }
            handleTimeMap.put(team, range);
        });
        teamToDataMap.put(TableHeader.HANDLE_TIME, handleTimeMap);
    }

    // вычисляем трудоёмкость команды в человекочасах
    // вычисляем среднее время прохождения кейса в человекочасах
    private void calcHumanHours() {
        // разбиваем все экзекушены по командам, в каждой команде по сотрудникам, проходившим регресс в команде
        final Map<String, Map<String, List<Execution>>> teamToMemberToExecutionList = new HashMap<>();
        teamToExecutionsMap.forEach((team, list) -> {
            teamToMemberToExecutionList.put(team, new HashMap<>());
            list.stream()
                .filter(e -> isHandleStatus(ExecutionStatus.getStatusByName(e.getStatus())))
                .forEach(e -> {
                    if (!teamToMemberToExecutionList.get(team).containsKey(e.getAssignedTo())) {
                        teamToMemberToExecutionList.get(team).put(e.getAssignedTo(), new ArrayList<>());
                    }
                    teamToMemberToExecutionList.get(team).get(e.getAssignedTo()).add(e);
                });
        });
        // массив команд, сотрудников и времени выполнения регресса каждым сотрудником
        final Map<String, Map<String, Long>> teamToHumansMap = new HashMap<>();
        teamToMemberToExecutionList.forEach((team, memberToExec) -> {
            teamToHumansMap.put(team, new HashMap<>());
            teamToMemberToExecutionList.get(team).forEach((member, list) -> {
                final long start = list.stream()
                                       .map(Execution::getActualStartDate)
                                       .filter(Objects::nonNull)
                                       .map(Date::getTime)
                                       .min(Long::compareTo)
                                       .orElse(0L);
                final long end = list.stream()
                                     .map(Execution::getActualStartDate)
                                     .filter(Objects::nonNull)
                                     .map(Date::getTime)
                                     .max(Long::compareTo)
                                     .orElse(0L);
                teamToHumansMap.get(team).put(member, end - start);
            });
        });
        // массив команд, и суммарного выполнения регресса всеми сотрудниками команды (человекочасы)
        final Map<String, Long> teamToHumanHoursMap = new HashMap<>();
        teamToHumansMap.forEach((team, map) -> {
            final long sum = map.values().stream()
                                    .reduce(Long::sum)
                                    .orElse(0L);
            teamToHumanHoursMap.put(team, sum);
        });
        // массив команд и среднего выполнения одного тест-кейса изходя из их количества и рассчитанных человекочасов
        final Map<String, Long> teamToAverageTime = new HashMap<>();
        teamToExecutionsMap.forEach((team, list) -> {
            final long totalCases = list.stream()
                    .map(Execution::getStatus)
                    .map(ExecutionStatus::getStatusByName)
                    .filter(this::isHandleStatus)
                    .count();
            final long average = totalCases == 0 ? 0 : teamToHumanHoursMap.get(team) / totalCases;
            teamToAverageTime.put(team, average);
        });
        teamToDataMap.put(TableHeader.HUMAN_HOURS, teamToHumanHoursMap);
        teamToDataMap.put(TableHeader.HUMAN_HOURS_BY_SINGLE_TEST, teamToAverageTime);
    }

    // вычисляем среднее время, требуемое для прохождения одного теста
    // вычисляем общее время прохождения регресса при отсутствии автотестов
    private void calcSingleTestTime() {
        final Map<String, Long> singleTestTimeMap = new HashMap<>();
        teamToExecutionsMap.keySet().forEach(team -> {
            final long handle = teamToDataMap.get(TableHeader.HANDLE).get(team);
            final long handleTime = teamToDataMap.get(TableHeader.HANDLE_TIME).get(team);
            final long averageTime = handle == 0 ? 0 : handleTime / handle;
            singleTestTimeMap.put(team, averageTime);
        });
        final Map<String, Long> regressTimeWithoutAFTMap = new HashMap<>();
        teamToExecutionsMap.forEach((team, list) -> regressTimeWithoutAFTMap.put(team, singleTestTimeMap.get(team) * list.size()));
        teamToDataMap.put(TableHeader.PREDICTED_TIME_ONLY_HANDLE, regressTimeWithoutAFTMap);
        teamToDataMap.put(TableHeader.SINGLE_TEST_TIME, singleTestTimeMap);
    }

    private boolean isHandleStatus(final ExecutionStatus status) {
        return status == ExecutionStatus.BLOCKED ||
               status == ExecutionStatus.FAIL ||
               status == ExecutionStatus.PASS ||
               status == ExecutionStatus.N_A;
    }

    // заполнение таблицы данными
    private void insertDataToTable() {
        getDocument().selectFirst("table").appendChild(new Element("tbody"));
        for (final String team : teamToExecutionsMap.keySet()) {
            final Element row = getDocument().selectFirst("tbody").appendChild(new Element("tr")).children().last();
            for (final TableHeader tableHeader : TableHeader.values()) {
                final Element td = new Element("td");
                if (tableHeader == TableHeader.TEAM) {
                    td.text(team);
                } else if (tableHeader == TableHeader.START_DATE || tableHeader == TableHeader.END_DATE) {
                    if (teamToDataMap.get(tableHeader).get(team) == 0) {
                        td.text("-");
                    } else {
                        final SimpleDateFormat format = new SimpleDateFormat("dd MMM - HH:mm");
                        td.text(format.format(new Date(teamToDataMap.get(tableHeader).get(team))));
                    }
                } else if (
                        tableHeader == TableHeader.HANDLE_TIME ||
                        tableHeader == TableHeader.HUMAN_HOURS ||
                        tableHeader == TableHeader.PREDICTED_TIME_ONLY_HANDLE
                ) {
                    if (teamToDataMap.get(tableHeader).get(team) == 0) {
                        td.text("-");
                    } else {
                        td.text(MetricsMethods.formatDuration(teamToDataMap.get(tableHeader).get(team), true));
                    }
                } else if (
                        tableHeader == TableHeader.SINGLE_TEST_TIME ||
                        tableHeader == TableHeader.HUMAN_HOURS_BY_SINGLE_TEST
                ) {
                    if (teamToDataMap.get(tableHeader).get(team) == 0) {
                        td.text("-");
                    } else {
                        td.text(MetricsMethods.formatDuration(teamToDataMap.get(tableHeader).get(team), false));
                    }
                } else if (tableHeader == TableHeader.DESCRIPTION) {
                    final List<String> notExecutedTestKeyList = MetricsMethods.getNotExecutedTestCaseListByTeam(team, teamToExecutionsMap);
                    if (notExecutedTestKeyList.size() < 6 && !notExecutedTestKeyList.isEmpty()) {
                        td.text("Осталось пройти кейсы " + String.join(", ", notExecutedTestKeyList));
                    }
                } else {
                    td.text(String.valueOf(teamToDataMap.get(tableHeader).get(team).intValue()));
                }
                row.appendChild(td);
            }
        }
    }

//    Генерация строки Итого
    private void insertTotalRowToTable() {
        final Element row = getDocument().selectFirst("tbody").appendChild(new Element("tr")).children().last();
        row.appendChild(new Element("td")).children().last().text("Итого");
        for (final TableHeader header : TableHeader.values()) {
            final long totalValue;
            if (header == TableHeader.TEAM || header == TableHeader.DESCRIPTION) {
                continue;
            } else if (header == TableHeader.HANDLE_TIME) {
                long min = 0;
                for (final long value : teamToDataMap.get(TableHeader.START_DATE).values()) {
                    if ((min == 0 && value > 0) || (min > value && value != 0)) {
                        min = value;
                    }
                }
                final long max = teamToDataMap.get(TableHeader.END_DATE).values().stream().max(Long::compareTo).orElse(0L);
                totalValue = max - min;
            } else if (header == TableHeader.SINGLE_TEST_TIME) {
                final long totalHandleTime = getTotalFromCol(TableHeader.HANDLE_TIME);
                final long totalHandleTest = getTotalFromCol(TableHeader.HANDLE);
                totalValue = totalHandleTest == 0 ? 0 : totalHandleTime / totalHandleTest;
            } else if (header == TableHeader.START_DATE) {
                long min = 0;
                for (final long value : teamToDataMap.get(header).values()) {
                    if ((min == 0 && value > 0) || (min > value && value != 0)) {
                        min = value;
                    }
                }
                totalValue = min;
            } else if (header == TableHeader.END_DATE) {
                totalValue = teamToDataMap.get(header).values().stream().max(Long::compareTo).orElse(0L);
            } else if (header == TableHeader.HUMAN_HOURS_BY_SINGLE_TEST) {
                final long handleTests = getTotalFromCol(TableHeader.HANDLE);
                final long humanHours = getTotalFromCol(TableHeader.HUMAN_HOURS);
                totalValue = handleTests == 0 ? 0 : humanHours / handleTests;
            } else if (header == TableHeader.PREDICTED_TIME_ONLY_HANDLE) {
                final long tests = getTotalFromCol(TableHeader.TOTAL_CASES);long min = 0;
                for (final long value : teamToDataMap.get(TableHeader.START_DATE).values()) {
                    if ((min == 0 && value > 0) || (min > value && value != 0)) {
                        min = value;
                    }
                }
                final long max = teamToDataMap.get(TableHeader.END_DATE).values().stream().max(Long::compareTo).orElse(0L);
                final long totalHandleTime = max - min;
                final long totalHandleTest = getTotalFromCol(TableHeader.HANDLE);
                final long averageHandleTime = totalHandleTest == 0 ? 0 : totalHandleTime / totalHandleTest;
                totalValue = tests * averageHandleTime;
            } else {
                totalValue = getTotalFromCol(header);
            }
            final Element b = new Element("b");
            final Element td = new Element("td");
            if (
                    header == TableHeader.HANDLE_TIME ||
                    header == TableHeader.HUMAN_HOURS ||
                    header == TableHeader.PREDICTED_TIME_ONLY_HANDLE
            ) {
                b.text(MetricsMethods.formatDuration(totalValue, true));
            } else if (
                    header == TableHeader.SINGLE_TEST_TIME ||
                    header == TableHeader.HUMAN_HOURS_BY_SINGLE_TEST
            ) {
                b.text(MetricsMethods.formatDuration(totalValue, false));
            } else if (header == TableHeader.START_DATE || header == TableHeader.END_DATE) {
                if (totalValue == 0) {
                    b.text("-");
                } else {
                    final SimpleDateFormat format = new SimpleDateFormat("dd MMM - HH:mm");
                    b.text(format.format(new Date(totalValue)));
                }
            } else {
                b.text(String.valueOf(totalValue));
            }
            td.appendChild(b);
            row.appendChild(td);
        }
    }

    // метод получает сумму всех значений колонки
    private long getTotalFromCol(final TableHeader header) {
        return teamToDataMap.get(header).values().stream().reduce(Long::sum).orElse(0L);
    }

    private enum TableHeader implements TableHeaderInterface {
        TEAM("Команда"),
        MEMBERS("Участников"),
        TOTAL_CASES("Всего тест-кейсов"),
        AUTO_PASSED("Пройдены автотестами"),
        HANDLE("Пройдены руками"),
        START_DATE("Время старта"),
        END_DATE("Время завершения"),
        BROKEN("Проваленные тесты"),
        BROKEN_WITH_BUG("Тесты с привязанными дефектами"),
        NOT_ACTUAL("Не актуальные"),
        NOT_EXECUTED("Не пройденные"),
        HANDLE_TIME("Время ручного прохождения команды"),
        SINGLE_TEST_TIME("Среднее время на 1 ручной тест"),
        HUMAN_HOURS("Трудоемкость"),
        HUMAN_HOURS_BY_SINGLE_TEST("Трудоемкость на 1 ручной тест"),
        PREDICTED_TIME_ONLY_HANDLE("Прогнозируемое время без автотестов"),
        DESCRIPTION("Инфо");

        private final String colName;

        TableHeader(final String colName) {
            this.colName = colName;
        }

        @Override
        public String getColName() {
            return colName;
        }

        @Override
        public boolean doNotRender() {
            return false;
        }
    }
}
