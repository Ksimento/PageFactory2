package ru.sbt.edu_power.confluence_reporting.RTM_metrics;

import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.confluence_reporting.RTM_metrics.applied_metrics.ApplyMetrics;
import ru.sbt.edu_power.confluence_reporting.RTM_metrics.applied_metrics.LowAutomatePercent;
import ru.sbt.edu_power.external_services.confluence.ConfluenceConnectException;
import ru.sbt.edu_power.external_services.confluence.ConfluenceDocument;
import ru.sbt.edu_power.external_services.confluence.TableHeaderInterface;
import ru.sbt.edu_power.external_services.jira.tc_verifier.NewJiraTCCollector;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFilter;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCQueryBuilder;
import ru.sbt.edu_power.external_services.nexus.NexusConnect;
import ru.sbt.edu_power.external_services.nexus.NexusRepo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Класс выполняет агрегацию данных из тест-кейсов и собирает таблицу для confluence
 */
@Slf4j
public class GenerateRtmTable extends ConfluenceDocument {
    private final RtmCollector rtmCollector;
    private final RtmMetricsCalc calc;
    // Все данные для таблицы
    // Заголовки таблицы -> Название команды -> числовые данные
    private final EnumMap<TableHeader, Map<String, Integer>> tableData = new EnumMap<>(TableHeader.class);
    private final TCFields.ProjectId project;
    private final boolean withNF;
    private final TCQueryBuilder queryBuilder = new TCQueryBuilder();
    private final int minAutomate;

    public GenerateRtmTable(final String pageId, final String projectKey, final boolean withNF, final int minAutomate) {
        super(pageId);
        this.minAutomate = minAutomate;
        this.withNF = withNF;
        for (final TableHeader tableHeader : TableHeader.values()) {
            tableData.put(tableHeader, new HashMap<>());
        }
        // Задаём фильтры по которым будем отбирать все подходящие тест-кейсы

        queryBuilder.addField(TCFields.TEST_TYPE, TCFields.TestType.REGRESS)
                .addField(TCFields.STATUS, TCFields.Status.APPROVED)
                .addField(TCFields.TEST_VIEW, TCFields.TestView.U_I)
                .addField(TCFields.ARCHIVED, false);
        project = TCFields.ProjectId.valueOf(projectKey);
        queryBuilder.addField(TCFields.PROJECT_ID, project);
        final Integer regressFolder = NewJiraTCCollector.getInstance().getFolderId("Регресс", project.id);
        if (withNF) {
            final Integer nfFolder = NewJiraTCCollector.getInstance().getFolderId("НФ", project.id);
            queryBuilder.addField(TCFields.FOLDER, nfFolder);
            queryBuilder.addField(TCFields.TEST_TYPE, TCFields.TestType.N_F);
        }
        queryBuilder.addField(TCFields.FOLDER, regressFolder);
        rtmCollector = new RtmCollector(queryBuilder);
        calc = new RtmMetricsCalc(rtmCollector);
    }

    public RtmCollector getRtmCollector() {
        return rtmCollector;
    }

    public void generate() {
        loadDocument();
        removeTableData();
        calculateData();
        try {
            uploadDataToNexus();
        } catch (final Throwable e) {
            log.error("не удалось выгрузить данные в NEXUS: {}", e.getLocalizedMessage());
        }
        insertHeaderToDocument(Stream
                .of(TableHeader.values())
                .collect(Collectors.toList()), this::formatHeader);
        insertDataToTable();
        insertTotalRowToTable();
        if (!withNF) {
            final LowAutomatePercent lowAutomatePercent = new LowAutomatePercent(
                    calc,
                    minAutomate,
                    tableData.get(TableHeader.CRIT_AUT_PERCENT_EXCL_DONT_AUT)
            );
            final ApplyMetrics applyMetrics = new ApplyMetrics(getDocument(), new HashMap<>(this.tableData));
                    applyMetrics.applyNeedAutomate(lowAutomatePercent, TableHeader.NEED_AUTOMATE_FOR_KPI);
        }
        saveDocument();
    }

    private void uploadDataToNexus() {
        if (withNF) {
            return;
        }
        final String data = GSON.toJson(tableData);
        final SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmmss");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        final String fileName = format.format(new Date()) + "_rtm.json";
        final String folder = "RTM/" + project.name();
        NexusConnect
                .getInstance()
                .uploadData(NexusRepo.JAVA_E2E_STATS, data.getBytes(), folder, fileName);
    }

    private String formatHeader(final TableHeaderInterface header) {
        switch ((TableHeader) header) {
            case DESCRIPTION:
                return formatHeader(header.getColName(), 120);
            case TEAM:
                return formatHeader(header.getColName(), 45);
            default:
                return header.getColName();
        }
    }

    private void calculateData() {
        Stream.of(TableHeader.values()).forEach(this::calculateData);
    }

    // подсчёт покрытия автотестами по каждой команде
    private void calculateData(final TableHeader header) {
        final Map<String, Integer> map;
        switch (header) {
            // Считаем сколько всего кейсов по командам
            case CASE_ALL:
                map = calc.caseAllMap();
                break;

            // Считаем сколько всего критичных кейсов по командам
            case CRITICAL_CASES:
                map = calc.criticalCaseMap();
                break;

            // Считаем сколько автоматизировано только критичных кейсов по командам
            case CRITICAL_CASES_AUTOMATED:
                map = calc.automatedCriticalCaseMap();
                break;

            // Считаем сколько всего API тестов по командам
            case API:
                map = calc.caseAllApiMap();
                break;

            // Считаем сколько всего тестов автоматизировано по командам (по статусу Автоматизирован - Да)
            case AUTOMATED_ALL:
                map = calc.automatedAllMap();
                break;

            // Считаем сколько кейсов в статусе На автоматизации
            case ON_AUTOMATE:
                map = calc.onAutomateMap();
                break;

            // Считаем сколько всего ручных тестов
            case HANDLE:
                map = calc.handleTestsMap();
                break;


            // Считаем сколько кейсов исключено из прогона
            case SKIPPED:
                map = calc.skippedMap();
                break;

            // Считаем сколько тестов не подлежит автоматизации
            case DONT_AUTOMATE:
                map = calc.dontAutomateMap();
                break;

            // Считаем сколько тестов не подлежит автоматизации из критичных
            case CRITICAL_DONT_AUTOMATE:
                map = calc.criticalDontAutomateMap();
                break;

            // Считаем сколько тест-кейсов с полем Framework JS или SELENIUM_JS
            case AUTO_JS:
                map = calc.frameworkJs();
                break;

            // Считаем сколько тест-кейсов с полем Framework SELENIUM или SELENIUM_JS
            case AUTO_JAVA:
                map = calc.frameworkSelenium();
                break;

            // Считаем процент автоматизации только критичных кейсов - ИСКЛЮЧАЯ не подлежащие автоматизации
            case CRIT_AUT_PERCENT_EXCL_DONT_AUT:
                map = calc.automatedPercentMapExclDontAut(true);
                break;

            // Считаем процент автоматизации только критичных кейсов - ВКЛЮЧАЯ не подлежащие автоматизации
            case CRITICAL_AUTOMATED_PERCENT:
                map = calc.automatedPercentMap(true);
                break;

            // Считаем общий процент автоматизации - ИСКЛЮЧАЯ не подлежащие автоматизации и исключая Тесты отчетности
            case AUT_PERCENT_EXCLUDE_DONT_AUT:
                map = calc.automatedPercentMapExclDontAut(false);
                break;

            // Считаем, сколько нужно автоматизировать критичных кейсов для достижения KPI
            case NEED_AUTOMATE_FOR_KPI:
                map = calc.needAutomateForKpi(minAutomate);
                break;

            // Считаем общий процент автоматизации - ВКЛЮЧАЯ не подлежащие автоматизации
            case AUTOMATED_PERCENT:
                map = calc.automatedPercentMap(false);
                break;

            // Считаем выполнение KPI. В таблицу записываем 0 если не выполнен и 1 если выполнен
            case IS_KPI_COMPLETE:
                map = calc.isKpiComplete(tableData.get(TableHeader.CRIT_AUT_PERCENT_EXCL_DONT_AUT), minAutomate);
                break;
            case AUTOMATE_KPI:
                map = tableData.get(TableHeader.CRIT_AUT_PERCENT_EXCL_DONT_AUT)
                        .keySet()
                        .stream()
                        .collect(Collectors.toMap(t -> t, t -> minAutomate));
                break;
            case DESCRIPTION:
            case TEAM:
                return;
            default:
                throw new ConfluenceConnectException("Не реализована калькуляция для заголовка " + header);
        }
        tableData.put(header, map);
    }

    // заполнение таблицы данными
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
                default:
                    result = tableData.get(header).get(team).toString();
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
            final Integer totalValue;
            switch ((TableHeader) header) {
                case AUTOMATED_PERCENT:
                    final int caseAllTotal = getTotalFromCol(TableHeader.CASE_ALL);
                    totalValue = caseAllTotal == 0 ? 0 : getTotalFromCol(TableHeader.AUTOMATED_ALL) * 100 /
                                                         caseAllTotal;
                    break;
                case CRITICAL_AUTOMATED_PERCENT:
                    final int critical = getTotalFromCol(TableHeader.CRITICAL_CASES);
                    totalValue = critical == 0 ? 0 : getTotalFromCol(TableHeader.CRITICAL_CASES_AUTOMATED) * 100 /
                                                     critical;
                    break;
                case AUT_PERCENT_EXCLUDE_DONT_AUT:
                    final int dontAutomate = (getTotalFromCol(TableHeader.CASE_ALL) -
                                              getTotalFromCol(TableHeader.DONT_AUTOMATE));
                    totalValue = dontAutomate == 0 ? 0 : getTotalFromCol(TableHeader.AUTOMATED_ALL) * 100 /
                                                         dontAutomate;
                    break;
                case CRIT_AUT_PERCENT_EXCL_DONT_AUT:
                    final int dontAutomatedByCritPriority = TCFilter.of(rtmCollector.getCollector().asMap())
                                                                    .filter(t -> t.getAutomatedStatus() ==
                                                                                 TCFields.AutomatedStatus.NOT_REQUIRED)
                                                                    .filter(t -> t.getNotAutomatedReason() != null &&
                                                                                 !t.getNotAutomatedReason().isEmpty())
                                                                    .filter(t -> t
                                                                            .getPriority()
                                                                            .getName()
                                                                            .equals(TCFields.Priority.HIGH.value))
                                                                    .toMap()
                                                                    .size();
                    final int criticalDontAutomate = (getTotalFromCol(TableHeader.CRITICAL_CASES) -
                                                      dontAutomatedByCritPriority);
                    totalValue = criticalDontAutomate == 0 ? 0 : getTotalFromCol(TableHeader.CRITICAL_CASES_AUTOMATED) *
                                                                 100 /
                                                                 criticalDontAutomate;
                    break;
                case CRITICAL_CASES:
                case CASE_ALL:
                case API:
                case HANDLE:
                case AUTO_JS:
                case SKIPPED:
                case AUTO_JAVA:
                case ON_AUTOMATE:
                case AUTOMATED_ALL:
                case DONT_AUTOMATE:
                case CRITICAL_DONT_AUTOMATE:
                case CRITICAL_CASES_AUTOMATED:
                case NEED_AUTOMATE_FOR_KPI:
                    totalValue = getTotalFromCol((TableHeader) header);
                    break;
                case TEAM:
                case DESCRIPTION:
                    totalValue = null;
                    break;
                default:
                    throw new ConfluenceConnectException("Не реализована калькуляция для заголовка " + header);
            }
            return totalValue == null ? "" : totalValue.toString();
        };
        insertTotalRowToTable(
                Stream.of(TableHeader.values()).collect(Collectors.toList()),
                converterFunction,
                TableHeader.TEAM
        );
    }

    // метод получает сумму всех значений колонки
    private int getTotalFromCol(final TableHeader header) {
        return tableData.get(header).values().stream().reduce(Integer::sum).orElse(0);
    }

    public enum TableHeader implements TableHeaderInterface {
        TEAM("Команда", false),
        CASE_ALL("Всего", false),
        AUTOMATED_ALL("E2E всего", false),
        AUTO_JS("E2E-JS", false),
        AUTO_JAVA("E2E-Selenium", false),
        API("API", false),
        HANDLE("Ручных", false),
        CRITICAL_CASES("Критичных", false),
        CRITICAL_CASES_AUTOMATED("E2E критичных", false),
        ON_AUTOMATE("На автоматизации", false),
        SKIPPED("Исключённые из прогона", false),
        DONT_AUTOMATE("Не подлежат автоматизации", false),
        CRITICAL_DONT_AUTOMATE("Критичные не подлежат автоматизации", false),
        CRITICAL_AUTOMATED_PERCENT("% критичных E2E", false),
        AUTOMATED_PERCENT("% автоматизированных E2E", false),
        CRIT_AUT_PERCENT_EXCL_DONT_AUT("% крит АФТ исключая не автоматизируемые", false),
        AUT_PERCENT_EXCLUDE_DONT_AUT("% автотестов исключая не автоматизируемые", false),
        NEED_AUTOMATE_FOR_KPI("Необходимо автоматизировать для достижения KPI", false),
        AUTOMATE_KPI("KPI по автоматизации", true),
        IS_KPI_COMPLETE("Показатель KPI выполнен", true),
        DESCRIPTION("Примечания", false);

        private final String colName;
        private final boolean doNotRender;

        @Override
        public String getColName() {
            return colName;
        }

        @Override
        public boolean doNotRender() {
            return doNotRender;
        }

        TableHeader(final String colName, final boolean doNotRender) {
            this.colName = colName;
            this.doNotRender = doNotRender;
        }
    }
}
