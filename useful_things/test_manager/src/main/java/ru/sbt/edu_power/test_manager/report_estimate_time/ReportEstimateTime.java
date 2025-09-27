package ru.sbt.edu_power.test_manager.report_estimate_time;

import kong.unirest.Unirest;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import ru.sbt.edu_power.external_services.confluence.ConfluenceDocument;
import ru.sbt.edu_power.external_services.confluence.TableHeaderInterface;
import ru.sbt.edu_power.external_services.jira.JiraConnect;
import ru.sbt.edu_power.external_services.jira.tc_verifier.NewJiraTCCollector;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCQueryBuilder;
import ru.sbt.edu_power.external_services.timer.Timer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class ReportEstimateTime {

    private static Map<String, Map<Integer, Integer>> groupTCtoTime = new HashMap<>();
    private static final TCQueryBuilder queryBuilder = new TCQueryBuilder();
    private static Map<String, Integer> countTestCaseInTeam = new HashMap<>();
    private static Map<String, Integer> timeTestCaseToTeam = new HashMap<>();
    private static AtomicInteger totalTime = new AtomicInteger();
    private static AtomicInteger totalTestCase = new AtomicInteger();

    public void createReport() {
        String projectKey = System.getProperty("jiraProjectKey");
        String pageID = ProjectId.valueOf(projectKey).projectId;
        loadTestCase(projectKey);
        createMap();
        countTotalCaseInTeam();
        new ReportEstimateTimeWorkConfluenceDocument(pageID).createTable();
    }

    /**
     * Выгружаем все кейсы из тестовой модели
     *
     * @param projectKey
     */
    private static void loadTestCase(String projectKey) {
        log.info("Начинаем выгрузку всех тест кейсов");
        Timer.startTimer("downloadMany");
        final TCFields.ProjectId projectId = TCFields.ProjectId.valueOf(projectKey);
        queryBuilder.addField(TCFields.ARCHIVED, false)
                .addField(TCFields.PROJECT_ID, projectId)
                .addField(TCFields.TEST_VIEW, TCFields.TestView.U_I)
                .addField(TCFields.TEST_TYPE, TCFields.TestType.REGRESS)
                .addField(TCFields.FOLDER, NewJiraTCCollector.getInstance().getFolderId("Регресс", projectId.id));
        NewJiraTCCollector.getInstance().collect(queryBuilder);
        log.info("Завершаем выгрузку всех тест кейсов");
    }

    /**
     * Выполняем группировку по Командам и времени выполнения
     */
    private static void createMap() {
        log.info("Начинаем группировку по команде и времени выполнения");
        NewJiraTCCollector.getInstance().asMap().forEach((k, testCase) -> {
            if (Objects.nonNull(testCase.getEstimatedTime())) {
                int tcTime = bringingTime(testCase.getEstimatedTime());
                if (groupTCtoTime.containsKey(testCase.getTeam())) {
                    Map<Integer, Integer> time = groupTCtoTime.get(testCase.getTeam());
                    if (time.containsKey(tcTime)) {
                        int number = time.get(tcTime) + 1;
                        time.put(tcTime, number);
                        groupTCtoTime.put(testCase.getTeam(), time);
                    } else {
                        time.put(tcTime, 1);
                        groupTCtoTime.put(testCase.getTeam(), time);
                    }
                } else {
                    Map<Integer, Integer> time = new HashMap<>();
                    time.put(tcTime, 1);
                    groupTCtoTime.put(testCase.getTeam(), time);
                }
            }
        });
        log.info("Завершаем группировку по команде и времени выполнения");
    }

    /**
     * Считаем в мапу countTestCaseInTeam количество тестов на команду
     * totalTime - общее время на все тесты по всем командам
     * totalTestCase - общее количество кейсов по всем командам
     */
    private void countTotalCaseInTeam() {
        groupTCtoTime.forEach((team, time) -> {
            AtomicInteger numberToTeam = new AtomicInteger();
            AtomicInteger timeToTeam = new AtomicInteger();
            time.forEach((k, v) -> {
                numberToTeam.addAndGet(v);
                totalTime.addAndGet(k * v);
                totalTestCase.addAndGet(v);
                timeToTeam.addAndGet(k * v);
            });
            countTestCaseInTeam.put(team, numberToTeam.get());
            timeTestCaseToTeam.put(team,timeToTeam.get());
        });
    }

    /**
     * Определяем количество целых затраченных минут на выполнение теста
     *
     * @param time - время в миллисекундах
     * @return
     */
    private static Integer bringingTime(Integer time) {
        int t = (time / 60000);
        if (t == 0){
            return 1;
        }
        return t;
    }

    /**
     * Страницы для загрузки отчета по проекту
     */
    private enum ProjectId {
        EDU("81952171"),
        S21("81952183"),
        B2C("81952175");
        private final String projectId;

        ProjectId(final String projectId) {
            this.projectId = projectId;
        }
    }

    private static class ReportEstimateTimeWorkConfluenceDocument extends ConfluenceDocument {

        protected ReportEstimateTimeWorkConfluenceDocument(String pageID) {
            super(pageID);
        }

        /**
         * Создаем таблицу для загрузки в конфлюенс
         */
        private void createTable() {
            log.info("Начинаем загрузку таблицы в конфлюенс");
            loadDocument();
            removeTableData();
            insertHeaderToDocument(
                    Stream.of(TableHeader.values())
                            .collect(Collectors.toList()), TableHeaderInterface::getColName);
            getDocument().selectFirst("table").appendChild(new Element("tbody"));

            groupTCtoTime.forEach((testCaseTeam, timeMap) -> {
                AtomicReference<String> teamCol = new AtomicReference<>("");
                createRow(testCaseTeam, "", countTestCaseInTeam.get(testCaseTeam).toString(), "", "", "", timeMap.size() + 2);
                timeMap.forEach((executionActual, tCases) -> {
                    createRow(
                            "",
                            bringingNormalTime(executionActual),
                            tCases.toString(),
                            bringingNormalTime(executionActual * tCases),
                            "",
                            "",
                            0
                    );
                });
                createRow("","","",bringingNormalTime(timeTestCaseToTeam.get(testCaseTeam)),"","",0);
            });
            createRow("Total Result", "", totalTestCase.toString(), bringingNormalTime(totalTime.get()), "", "", 1);
            saveDocument();
            log.info("Завершаем загрузку таблицы в конфлюенс");
        }

        /**
         * Создаем строку в таблице
         *
         * @param team      - команда
         * @param time      - время выполнения одного теста
         * @param number    - количество тестов
         * @param allTime   - общее время на выполнение всех тестов time * number
         * @param tiket     - опциональное поле тикет
         * @param comment   - опциональное поле Коммент
         * @param groupSize - используется для группировки ячеек, на вход приходит количество ячеек которые нужно сгруппировать
         */
        private void createRow(String team, String time, String number, String allTime, String tiket, String comment, int groupSize) {
            final Element row = getDocument().selectFirst("tbody").appendChild(new Element("tr")).children().last();
            final Element tdTeam = new Element("td");
            final Element tdTime = new Element("td");
            final Element tdNumber = new Element("td");
            final Element tdAllTime = new Element("td");
            final Element tdTiket = new Element("td");
            final Element tdComment = new Element("td");

            tdTeam.text(team);
            tdTime.text(time);
            tdNumber.text(number);
            tdAllTime.text(allTime);
            tdTiket.text(tiket);
            tdComment.text(comment);

            if (groupSize > 0) {
                tdTeam.attr("rowspan", String.valueOf(groupSize));
                tdTeam.text(team);
                row.appendChild(tdTeam);
            }

            row.appendChild(tdTime);
            row.appendChild(tdNumber);
            row.appendChild(tdAllTime);
            row.appendChild(tdTiket);
            row.appendChild(tdComment);
        }

        /**
         * Преобразовываем время из минут к нормальному виду 00:01:00
         *
         * @param timeMinutes - количество минут затраченное на один тест
         * @return
         */
        private String bringingNormalTime(int timeMinutes) {
            return String.format("%02d:%02d:00",
                    TimeUnit.MILLISECONDS.toHours(timeMinutes * 60000L),
                    TimeUnit.MILLISECONDS.toMinutes(timeMinutes * 60000L) -
                            TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(timeMinutes * 60000L)));
        }


        /**
         * Заголовки таблицы
         */
        private enum TableHeader implements TableHeaderInterface {
            TEAM("Test Case.Team"),
            TIME("Execution.Actual"),
            TOTAL_CASES("TCases"),
            TOTAL_TIME_CASES("Actual Time TCases"),
            TIKET("Тикет"),
            KOMMENT("Комментарий");

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
}