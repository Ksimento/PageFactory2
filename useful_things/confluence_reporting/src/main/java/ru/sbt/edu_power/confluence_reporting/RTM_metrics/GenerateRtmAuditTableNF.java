package ru.sbt.edu_power.confluence_reporting.RTM_metrics;

import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.confluence_reporting.RTM_metrics.applied_metrics.*;
import ru.sbt.edu_power.external_services.confluence.ConfluenceConnectException;
import ru.sbt.edu_power.external_services.confluence.ConfluenceDocument;
import ru.sbt.edu_power.external_services.confluence.TableHeaderInterface;
import ru.sbt.edu_power.external_services.jira.tc_verifier.NewJiraTCCollector;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCQueryBuilder;
import ru.sbt.edu_power.external_services.timer.Timer;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class GenerateRtmAuditTableNF extends ConfluenceDocument {
    private final RtmCollector rtmCollector;
    private final RtmMetricsCalc calc;
    // Все данные для таблицы
    // Заголовки таблицы -> Название команды -> числовые данные
    private final EnumMap<TableHeader, Map<String, Integer>> sizeTableData = new EnumMap<>(TableHeader.class);
    private final EnumMap<TableHeader, Map<String, List<String>>> textTableData = new EnumMap<>(TableHeader.class);

    private final Map<Class<? extends AbstractMetrics>, AbstractMetrics> metricsMap = new HashMap<>();

    public GenerateRtmAuditTableNF(final String pageID, final String projectKey) {
        super(pageID);
        for (final TableHeader tableHeader : TableHeader.values()) {
            sizeTableData.put(tableHeader, new HashMap<>());
        }
        // Задаём фильтры по которым будем отбирать все подходящие тест-кейсы
        final TCQueryBuilder queryBuilder = new TCQueryBuilder();
        queryBuilder.addField(TCFields.TEST_VIEW, TCFields.TestView.U_I)
                    .addField(TCFields.ARCHIVED, false);
        final TCFields.ProjectId project = TCFields.ProjectId.valueOf(projectKey);
        queryBuilder.addField(TCFields.FOLDER, NewJiraTCCollector.getInstance().getFolderId("НФ", project.id));
        queryBuilder.addField(TCFields.PROJECT_ID, project);
        rtmCollector = new RtmCollector(queryBuilder);
        calc = new RtmMetricsCalc(rtmCollector);
    }

    public void generate() {
        Timer.startTimer("Steps");
        loadDocument();
        log.info("Выполнена загрузка документа: {}", Timer.getDelta("Steps","loadDocument") / 1000d);
        removeTableData();
        calculateData();
        log.info("Выполнен расчёт данных: {}", Timer.getDelta("Steps","calculateData") / 1000d);
        insertHeaderToDocument(Stream
                .of(TableHeader.values())
                .collect(Collectors.toList()), this::formatHeader);
        insertDataToTable();
        insertTotalRowToTable();
        log.info("Данные вставлены в таблицу: {}", Timer.getDelta("Steps","insertTotalRowToTable") / 1000d);
        final ApplyMetrics applyMetrics = new ApplyMetrics(getDocument(), new HashMap<>(sizeTableData));
        applyMetrics.apply(metricsMap.get(WithoutAutomator.class), TableHeader.WITHOUT_AUTOMATOR);
        applyMetrics.apply(metricsMap.get(WithoutFramework.class), TableHeader.WITHOUT_FRAMEWORK);
        applyMetrics.apply(metricsMap.get(WithoutStoryLinks.class), TableHeader.WITHOUT_STORY_LINKS);
        applyMetrics.apply(metricsMap.get(TestCaseInRootFolder.class), TableHeader.TEST_CASE_IN_ROOT_FOLDER);
        applyMetrics.apply(
                metricsMap.get(WithoutSystemRequirementsLink.class),
                TableHeader.WITHOUT_SYSTEM_REQUIREMENTS_LINK
        );
        applyMetrics.apply(metricsMap.get(WithoutSteps.class), TableHeader.CASES_WITHOUT_STEPS);
        applyMetrics.apply(metricsMap.get(TestCaseNotExecutionOverHalfYear.class), TableHeader.NOT_EXECUTED_TESTS);
        log.info("Рассчитаны метрики: {}", Timer.getDelta("Steps","applyMetrics") / 1000d);
        saveDocument();
        log.info("Документ сохранён: {}", Timer.getDelta("Steps","saveDocument") / 1000d);
    }

    private void calculateData() {
        Timer.startTimer("calculateData");
        Stream.of(TableHeader.values()).forEach(this::calculateData);
    }

    private void calculateData(final TableHeader header) {
        switch (header) {
            case TEAM:
            case DESCRIPTION:
                return;
            case CASES_WITHOUT_STEPS:
                metricsMap.put(WithoutSteps.class, new WithoutSteps(calc));
                sizeTableData.put(
                        header,
                        metricsMap.get(WithoutSteps.class).getTeamToSizeMap()
                );
                break;
            case WITHOUT_AUTOMATOR:
                metricsMap.put(WithoutAutomator.class, new WithoutAutomator(calc));
                sizeTableData.put(
                        header,
                        metricsMap.get(WithoutAutomator.class).getTeamToSizeMap()
                );
                break;
            case WITHOUT_FRAMEWORK:
                metricsMap.put(WithoutFramework.class, new WithoutFramework(calc));
                sizeTableData.put(
                        header,
                        metricsMap.get(WithoutFramework.class).getTeamToSizeMap()
                );
                break;
            case WITHOUT_SYSTEM_REQUIREMENTS_LINK:
                metricsMap.put(WithoutSystemRequirementsLink.class, new WithoutSystemRequirementsLink(calc));
                sizeTableData.put(
                        header,
                        metricsMap.get(WithoutSystemRequirementsLink.class).getTeamToSizeMap()
                );
                break;
            case WITHOUT_STORY_LINKS:
                metricsMap.put(WithoutStoryLinks.class, new WithoutStoryLinks(calc));
                sizeTableData.put(
                        header,
                        metricsMap.get(WithoutStoryLinks.class).getTeamToSizeMap()
                );
                break;
            case TEST_CASE_IN_ROOT_FOLDER:
                metricsMap.put(TestCaseInRootFolder.class, new TestCaseInRootFolder(calc));
                sizeTableData.put(
                        header,
                        metricsMap.get(TestCaseInRootFolder.class).getTeamToSizeMap()
                );
                break;
            case TEST_OWNERS:
                textTableData.put(
                        header,
                        calc.testCaseOwners()
                );
                break;
            case TEST_STATUS:
                metricsMap.put(TestCaseStatusNotApproved.class, new TestCaseStatusNotApproved(calc));
                textTableData.put(
                        header,
                        ((TestCaseStatusNotApproved) metricsMap.get(TestCaseStatusNotApproved.class)).testCaseStatus()
                );
                sizeTableData.put(
                        header,
                        metricsMap.get(TestCaseStatusNotApproved.class).getTeamToSizeMap()
                );
                break;
            case TEST_TYPE:
                metricsMap.put(TestTypeNotRegress.class, new TestTypeNotRegress(calc));
                textTableData.put(
                        header,
                        ((TestTypeNotRegress) metricsMap.get(TestTypeNotRegress.class)).testCaseTestType()
                );
                sizeTableData.put(
                        header,
                        metricsMap.get(TestTypeNotRegress.class).getTeamToSizeMap()
                );
                break;
            case NOT_EXECUTED_TESTS:
                metricsMap.put(TestCaseNotExecutionOverHalfYear.class, new TestCaseNotExecutionOverHalfYear(calc));
                sizeTableData.put(
                        header,
                        metricsMap.get(TestCaseNotExecutionOverHalfYear.class).getTeamToSizeMap()
                );
                break;
            default:
                throw new ConfluenceConnectException("Не реализована калькуляция для заголовка " + header);
        }
        log.info("Данные для '{}' рассчитаны: {}", header.name(), Timer.getDelta("calculateData", header.name()) / 1000d);
    }

    private void insertDataToTable() {
        final BiFunction<String, TableHeaderInterface, String> converterFunction = (team, header) -> {
            final String result;
            switch ((TableHeader) header) {
                case TEAM:
                    result = team;
                    break;
                case DESCRIPTION:
                    result = "";
                    break;
                case TEST_OWNERS:
                case TEST_STATUS:
                case TEST_TYPE:
                    result = String.join("\n", textTableData.get(header).get(team));
                    break;
                default:
                    result = sizeTableData.get(header).get(team).toString();
                    break;
            }
            return result;
        };
        insertDataToTable(
                new ArrayList<>(rtmCollector.getUiCaseListByTeam().keySet()),
                Stream.of(TableHeader.values()).collect(Collectors.toList()),
                converterFunction
        );
    }

    private void insertTotalRowToTable() {
        final Function<TableHeaderInterface, String> converterFunction = (header) -> {
            final Integer total;
            switch ((TableHeader) header) {
                case TEAM:
                case DESCRIPTION:
                case TEST_OWNERS:
                case TEST_STATUS:
                case TEST_TYPE:
                    total = null;
                    break;
                default:
                    total = getTotalFromCol((TableHeader) header);
            }
            return total == null ? "" : total.toString();
        };
        insertTotalRowToTable(
                Stream.of(TableHeader.values()).collect(Collectors.toList()),
                converterFunction,
                TableHeader.TEAM
        );
    }

    // метод получает сумму всех значений колонки
    private int getTotalFromCol(final TableHeader header) {
        return sizeTableData.get(header).values().stream().reduce(Integer::sum).orElse(0);
    }

    private String formatHeader(final TableHeaderInterface header) {
        switch ((TableHeader) header) {
            case DESCRIPTION:
                return formatHeader(header.getColName(), 150);
            case TEAM:
                return formatHeader(header.getColName(), 45);
            case TEST_OWNERS:
                return formatHeader(header.getColName(), 2);
            case TEST_STATUS:
            case TEST_TYPE:
                return formatHeader(header.getColName(), 20);
            default:
                return header.getColName();
        }
    }

    public enum TableHeader implements TableHeaderInterface {
        TEAM("Команда"),
        CASES_WITHOUT_STEPS("Не указаны шаги"),
        WITHOUT_AUTOMATOR("Не указан автоматизатор"),
        WITHOUT_FRAMEWORK("Не указан фреймворк"),
        WITHOUT_SYSTEM_REQUIREMENTS_LINK("Нет ссылки на системные требования"),
        WITHOUT_STORY_LINKS("Нет ссылки на стори по которому выполнено тестирование НФ"),
        TEST_CASE_IN_ROOT_FOLDER("ТК в корневой директории"),
        TEST_OWNERS("Владельцы ТК и автотестов"),
        TEST_STATUS("Статистика в разрезе статусов ТК"),
        TEST_TYPE("Статистика в разрезе типов ТК"),
        NOT_EXECUTED_TESTS("Не запускаемые тесты"),
        DESCRIPTION("Примечания");

        private final String colName;

        @Override
        public String getColName() {
            return colName;
        }

        @Override
        public boolean doNotRender() {
            return false;
        }

        TableHeader(final String colName) {
            this.colName = colName;
        }
    }
}
