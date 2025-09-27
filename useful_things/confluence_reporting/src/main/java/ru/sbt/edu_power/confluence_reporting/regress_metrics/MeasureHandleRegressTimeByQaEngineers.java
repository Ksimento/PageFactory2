package ru.sbt.edu_power.confluence_reporting.regress_metrics;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import org.junit.Assert;
import ru.sbt.edu_power.external_services.PropReader;
import ru.sbt.edu_power.external_services.confluence.ConfluenceDocument;
import ru.sbt.edu_power.external_services.confluence.TableHeaderInterface;
import ru.sbt.edu_power.external_services.jira.tc_verifier.NewJiraTCCollector;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.Execution;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.ExecutionStatus;
import ru.sbt.edu_power.external_services.mattermost.Regressman;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class MeasureHandleRegressTimeByQaEngineers extends ConfluenceDocument {
    private final String testSetId;
    private String channel;
    private final Map<String, List<Execution>> memberToExecutionsMap = new HashMap<>();
    private final EnumMap<TableHeader, Map<String, Long>> memberToDataMap = new EnumMap<>(TableHeader.class);
    private final Map<String, Map<String, Integer>> memberToTeamsListMap = new HashMap<>();
    private static final String LINK_TEST_PLAYER = PropReader.get("jira.ui.testrun.endpoint");

    public MeasureHandleRegressTimeByQaEngineers(final String pageID) {
        super(pageID);
        this.testSetId = System.getProperty("jira.testset.id");
        this.channel = System.getProperty("sendMessageToChannel");
    }

    public MeasureHandleRegressTimeByQaEngineers(final String pageID, final String testSetId) {
        super(pageID);
        this.testSetId = testSetId;
        this.channel = System.getProperty("sendMessageToChannel");
    }

    public void generate() {
        Assert.assertNotNull("Не заполнено поле тест-сета", testSetId);
        loadDocument();
        log.info("Документ загружен");
        loadTestRunExecutions();
        log.info("Загружены данные из тест-сета {}", testSetId);
        calculateData();
        log.info("Рассчёты для таблицы выполнены");
        if (channel.length() > 0){
            sendMessageNoExecutedTest(channel);
            log.info("Рассылка о непройденных тестах выполнена");
        }
        removeTableData();
        MetricsMethods.updateTestSetId(getDocument(), testSetId);
        insertHeaderToDocument(
                Stream.of(TableHeader.values())
                      .collect(Collectors.toList()), TableHeaderInterface::getColName);
        insertDataToTable();
        insertTotalRowToTable();
        log.info("Таблица построена");
        saveDocument();
        log.info("Таблица загружена в Confluence");
    }

    private void loadTestRunExecutions() {
        final List<Execution> executionList = TestRunExecutions.load(testSetId);
        for (final Execution element : executionList) {
            try {
                final String member = MetricsMethods.getExecutedBy(element);
                if (!memberToExecutionsMap.containsKey(member)) {
                    memberToExecutionsMap.put(member, new ArrayList<>());
                }
                memberToExecutionsMap.get(member).add(element);
                final String team = NewJiraTCCollector.getInstance().getById(element.getTestCaseKey()).getTeam();
                if (!memberToTeamsListMap.containsKey(member)) {
                    memberToTeamsListMap.put(member, new HashMap<>());
                }
                if (!memberToTeamsListMap.get(member).containsKey(team)) {
                    memberToTeamsListMap.get(member).put(team, 1);
                } else {
                    memberToTeamsListMap.get(member).put(team, memberToTeamsListMap.get(member).get(team) + 1);
                }

            } catch (final AssertionError ignored) {
            }
        }
    }

    private void calculateData() {
        // Добавляем пустой столбец команды
        memberToDataMap.put(TableHeader.TEAM, new HashMap<>());

        // Считаем количество тест-кейсов на сотруднике
        memberToDataMap.put(TableHeader.TOTAL_CASES, MetricsMethods.totalCasesCalc(memberToExecutionsMap));

        // Считаем количество тестов пройденных автоматически
        memberToDataMap.put(TableHeader.AUTO_PASSED, MetricsMethods.notExecutedCalcInContextStatuses(memberToExecutionsMap,
                ExecutionStatus.AUTO_PASS));

        // Считаем количество тестов пройденных руками
        memberToDataMap.put(TableHeader.HANDLE, MetricsMethods.notExecutedCalcInContextStatuses(memberToExecutionsMap,
                ExecutionStatus.BLOCKED, ExecutionStatus.FAIL, ExecutionStatus.PASS, ExecutionStatus.N_A));

        // Вычисляем время старта
        memberToDataMap.put(TableHeader.START_DATE, MetricsMethods.startDateCalc(memberToExecutionsMap));

        // Вычисляем время окончания
        memberToDataMap.put(TableHeader.END_DATE, MetricsMethods.endDateCalc(memberToExecutionsMap));

        // Считаем количество проваленных тестов
        memberToDataMap.put(TableHeader.BROKEN, MetricsMethods.notExecutedCalcInContextStatuses(memberToExecutionsMap,
                ExecutionStatus.BLOCKED, ExecutionStatus.FAIL));

        // Считаем количество тестов с привязанными дефектами
        memberToDataMap.put(TableHeader.BROKEN_WITH_BUG, MetricsMethods.brokenWithBugCalc(memberToExecutionsMap));

        // Считаем количество не актуальных тестов
        memberToDataMap.put(TableHeader.NOT_ACTUAL, MetricsMethods.notExecutedCalcInContextStatuses(memberToExecutionsMap,
                ExecutionStatus.N_A));

        // Считаем количество не запущенных тестов
        memberToDataMap.put(TableHeader.NOT_EXECUTED, MetricsMethods.notExecutedCalcInContextStatuses(memberToExecutionsMap,
                ExecutionStatus.AUTO_FAIL, ExecutionStatus.NOT_EXECUTED, ExecutionStatus.IN_PROGRESS));

        // Считаем количество в прогрессе
        memberToDataMap.put(TableHeader.IN_PROGRESS, MetricsMethods.notExecutedCalcInContextStatuses(memberToExecutionsMap,
                ExecutionStatus.IN_PROGRESS));

        // Считаем количество в статусе AUTO_FAIL
        memberToDataMap.put(TableHeader.AUTO_FAIL, MetricsMethods.notExecutedCalcInContextStatuses(memberToExecutionsMap,
                ExecutionStatus.AUTO_FAIL));

        // Считаем количество только в статусе Не пройденные
        memberToDataMap.put(TableHeader.ONLY_NOT_EXECUTED, MetricsMethods.notExecutedCalcInContextStatuses(memberToExecutionsMap,
                ExecutionStatus.NOT_EXECUTED));

        // Считаем время ручного прохождения
        handleTimeCalc();

        // Считаем среднее время прохождения 1 теста
        singleTestTimeCalc();
    }

    //Выполняем отправку списка пользователей с незапущенными тестами
    public void sendMessageNoExecutedTest(String channel){
        String header = String.format("Не пройденные тест-кейсы в прогоне [" + testSetId + "](%s):", LINK_TEST_PLAYER + testSetId);
        StringBuilder message =
                new StringBuilder(header + "\n");
        if (Objects.nonNull(memberToDataMap.get(TableHeader.NOT_EXECUTED))){
            for (Map.Entry map : memberToDataMap.get(TableHeader.NOT_EXECUTED).entrySet()){
                if ((long)map.getValue() > 0){
                    message.append("@" + getUserName((String) map.getKey()) + " : " + map.getValue() + "\n");
                }
            }
            long onlyNotExecutedNumber = 0;
            if (Objects.nonNull(memberToDataMap.get(TableHeader.ONLY_NOT_EXECUTED))){
                for (Map.Entry map : memberToDataMap.get(TableHeader.ONLY_NOT_EXECUTED).entrySet()){
                    if ((long)map.getValue() > 0){
                        onlyNotExecutedNumber += (long)map.getValue();
                    }
                }
            }
            long inProgressNumber = 0;
            if (Objects.nonNull(memberToDataMap.get(TableHeader.IN_PROGRESS))){
                for (Map.Entry map : memberToDataMap.get(TableHeader.IN_PROGRESS).entrySet()){
                    if ((long)map.getValue() > 0){
                        inProgressNumber += (long)map.getValue();
                    }
                }
            }
            long autoFailNumber = 0;
            if (Objects.nonNull(memberToDataMap.get(TableHeader.AUTO_FAIL))){
                for (Map.Entry map : memberToDataMap.get(TableHeader.AUTO_FAIL).entrySet()){
                    if ((long)map.getValue() > 0){
                        autoFailNumber += (long)map.getValue();
                    }
                }
            }
            message.append("\n--------------------");
            if (onlyNotExecutedNumber > 0){
                message.append("\n").append("Не пройденных: ").append(onlyNotExecutedNumber);
            }
            if (inProgressNumber > 0){
                message.append("\n").append("В прогрессе: ").append(inProgressNumber);
            }
            if (autoFailNumber > 0){
                message.append("\n").append("Авто фейл: ").append(autoFailNumber);
            }
            Regressman.getInstance().sendPostByChannelName(channel, message.toString());
        }
    }

    // Считаем время ручного прохождения
    private void handleTimeCalc() {
        final Map<String, Long> handleTimeMap = new HashMap<>();
        memberToExecutionsMap.keySet().forEach(member -> {
            final long totalTime;
            final Long start = memberToDataMap.get(TableHeader.START_DATE).get(member);
            final Long end = memberToDataMap.get(TableHeader.END_DATE).get(member);
            if (Objects.isNull(start) || start == 0) {
                totalTime = 0;
            } else if (Objects.isNull(end) || end == 0) {
                totalTime = 0;
            } else {
                totalTime = end - start;
            }
            handleTimeMap.put(member, totalTime);
        });
        memberToDataMap.put(TableHeader.HANDLE_TIME, handleTimeMap);
    }

    // Считаем среднее время прохождения 1 теста
    private void singleTestTimeCalc() {
        final Map<String, Long> singleTesTimeMap = new HashMap<>();
        memberToExecutionsMap.keySet().forEach(member -> {
            final long totalTime = memberToDataMap.get(TableHeader.HANDLE_TIME).get(member);
            final long totalCases = memberToDataMap.get(TableHeader.TOTAL_CASES).get(member);
            final long average = totalCases == 0 ? 0 : totalTime / totalCases;
            singleTesTimeMap.put(member, average);
        });
        memberToDataMap.put(TableHeader.SINGLE_TEST_TIME, singleTesTimeMap);
    }

    // заполнение таблицы данными
    private void insertDataToTable() {
        getDocument().selectFirst("table").appendChild(new Element("tbody"));
        for (final String member : memberToExecutionsMap.keySet()) {
            final Element row = getDocument().selectFirst("tbody").appendChild(new Element("tr")).children().last();
            for (final TableHeader tableHeader : TableHeader.values()) {
                final Element td = new Element("td");
                if (tableHeader == TableHeader.MEMBER) {
                    td.text(getUserDisplayName(member));
                } else if (tableHeader == TableHeader.TEAM) {
                    getTeamList(memberToTeamsListMap.get(member)).forEach(team -> {
                        final Element p = new Element("p");
                        p.text(team);
                        td.appendChild(p);
                    });
                } else if (tableHeader == TableHeader.START_DATE || tableHeader == TableHeader.END_DATE) {
                    if (memberToDataMap.get(tableHeader).get(member) == 0) {
                        td.text("-");
                    } else {
                        final SimpleDateFormat format = new SimpleDateFormat("dd MMM - HH:mm");
                        td.text(format.format(new Date(memberToDataMap.get(tableHeader).get(member))));
                    }
                } else if (tableHeader == TableHeader.HANDLE_TIME) {
                    if (memberToDataMap.get(tableHeader).get(member) == 0) {
                        td.text("-");
                    } else {
                        td.text(MetricsMethods.formatDuration(memberToDataMap.get(tableHeader).get(member), true));
                    }
                } else if (tableHeader == TableHeader.SINGLE_TEST_TIME) {
                    if (memberToDataMap.get(tableHeader).get(member) == 0) {
                        td.text("-");
                    } else {
                        td.text(MetricsMethods.formatDuration(memberToDataMap.get(tableHeader).get(member), false));
                    }
                } else {
                    td.text(String.valueOf(memberToDataMap.get(tableHeader).get(member).intValue()));
                }
                row.appendChild(td);
            }
        }
    }

    private List<String> getTeamList(final Map<String, Integer> teams) {
        return teams.keySet().stream()
                    .map(t -> t + " (" + teams.get(t) + ")")
                    .collect(Collectors.toList());
    }

    //    Генерация строки Итого
    private void insertTotalRowToTable() {
        final Element row = getDocument().selectFirst("tbody").appendChild(new Element("tr")).children().last();
        row.appendChild(new Element("td")).children().last().text("Итого");
        // добавляем пустую ячейку для столбца команд (там никаких суммарных значений не должно быть)
        row.appendChild(new Element("td"));
        for (final TableHeader header : TableHeader.values()) {
            final long totalValue;
            if (header == TableHeader.TEAM || header == TableHeader.MEMBER) {
                continue;
            } else if (header == TableHeader.HANDLE_TIME) {
                long min = 0;
                for (final long value : memberToDataMap.get(TableHeader.START_DATE).values()) {
                    if ((min == 0 && value > 0) || (min > value && value != 0)) {
                        min = value;
                    }
                }
                final long max = memberToDataMap
                        .get(TableHeader.END_DATE)
                        .values()
                        .stream()
                        .max(Long::compareTo)
                        .orElse(0L);
                totalValue = max - min;
            } else if (header == TableHeader.SINGLE_TEST_TIME) {
                final long totalHandleTime = getTotalFromCol(TableHeader.HANDLE_TIME);
                final long totalHandleTest = getTotalFromCol(TableHeader.HANDLE);
                totalValue = totalHandleTest == 0 ? 0 : totalHandleTime / totalHandleTest;
            } else if (header == TableHeader.START_DATE) {
                long min = 0;
                for (final long value : memberToDataMap.get(header).values()) {
                    if ((min == 0 && value > 0) || (min > value && value != 0)) {
                        min = value;
                    }
                }
                totalValue = min;
            } else if (header == TableHeader.END_DATE) {
                totalValue = memberToDataMap.get(header).values().stream().max(Long::compareTo).orElse(0L);
            } else {
                totalValue = getTotalFromCol(header);
            }
            final Element b = new Element("b");
            final Element td = new Element("td");
            if (header == TableHeader.HANDLE_TIME) {
                b.text(MetricsMethods.formatDuration(totalValue, true));
            } else if (header == TableHeader.SINGLE_TEST_TIME) {
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
        return memberToDataMap.get(header).values().stream().reduce(Long::sum).orElse(0L);
    }

    private enum TableHeader implements TableHeaderInterface {
        MEMBER("Сотрудник"),
        TEAM("Команда"),
        TOTAL_CASES("Всего тест-кейсов"),
        AUTO_PASSED("Пройдены автотестами"),
        HANDLE("Пройдены руками"),
        START_DATE("Время старта"),
        END_DATE("Время завершения"),
        BROKEN("Проваленные тесты"),
        BROKEN_WITH_BUG("Тесты с привязанными дефектами"),
        NOT_ACTUAL("Не актуальные"),
        NOT_EXECUTED("Не пройденные"),
        IN_PROGRESS("В прогрессе"),
        AUTO_FAIL("Авто фейл"),
        ONLY_NOT_EXECUTED("Только не пройденные"),
        HANDLE_TIME("Время ручного прохождения"),
        SINGLE_TEST_TIME("Среднее время на 1 ручной тест");

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
