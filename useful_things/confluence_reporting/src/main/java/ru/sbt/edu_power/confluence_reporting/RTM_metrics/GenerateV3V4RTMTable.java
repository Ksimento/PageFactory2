package ru.sbt.edu_power.confluence_reporting.RTM_metrics;

import ru.sbt.edu_power.external_services.confluence.ConfluenceConnectException;
import ru.sbt.edu_power.external_services.confluence.ConfluenceDocument;
import ru.sbt.edu_power.external_services.confluence.TableHeaderInterface;
import ru.sbt.edu_power.external_services.jira.tc_verifier.NewJiraTCCollector;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCQueryBuilder;
import ru.sbt.edu_power.external_services.jira.test_manager.model.TestCaseModel;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GenerateV3V4RTMTable extends ConfluenceDocument {
    private final RtmCollector rtmCollector;
    private final RtmMetricsCalc calc;
    // Все данные для таблицы
    // Заголовки таблицы -> Название команды -> числовые данные
    private final EnumMap<TableHeader, Map<String, Integer>> tableData = new EnumMap<>(TableHeader.class);

    public GenerateV3V4RTMTable(final String pageId, final String projectKey) {
        super(pageId);
        final TCQueryBuilder queryBuilder = new TCQueryBuilder();
        final TCFields.ProjectId project = TCFields.ProjectId.valueOf(projectKey);
        for (final TableHeader tableHeader : TableHeader.values()) {
            tableData.put(tableHeader, new HashMap<>());
        }
        final Integer regressFolder = NewJiraTCCollector.getInstance().getFolderId("Регресс", project.id);
        final Integer nfFolder = NewJiraTCCollector.getInstance().getFolderId("НФ", project.id);
        queryBuilder.addField(TCFields.TEST_TYPE, TCFields.TestType.REGRESS, TCFields.TestType.N_F)
                    .addField(TCFields.ARCHIVED, false)
                    .addField(TCFields.PROJECT_ID, project)
                    .addField(TCFields.STATUS, TCFields.Status.APPROVED, TCFields.Status.NEED_REFACTORING)
                    .addField(TCFields.TEST_VIEW, TCFields.TestView.U_I)
                    .addField(TCFields.FOLDER, regressFolder, nfFolder);

        rtmCollector = new RtmCollector(queryBuilder);
        calc = new RtmMetricsCalc(rtmCollector);
    }

    public void generate() {
        loadDocument();
        removeTableData();
        calculateData();

        insertHeaderToDocument(Stream
                .of(TableHeader.values())
                .collect(Collectors.toList()), TableHeaderInterface::getColName);
        insertDataToTable();
        insertTotalRowToTable();
        saveDocument();
    }

    private void calculateData() {
        Stream.of(TableHeader.values()).forEach(this::calculateData);
    }

    // подсчёт покрытия автотестами по каждой команде
    private void calculateData(final TableHeader header) {
        final Map<String, Integer> map;
        switch (header) {
            case TEAM:
                return;
            case LBL_V3:
                map = calc.getTestNumberByLabelFunction(this::isV3);
                break;
            case LBL_V3_E2E:
                map = calc.getAutomatedTestNumberByLabelFunction(this::isV3);
                break;
            case LBL_V3_CRIT:
                map = calc.getCriticalTestNumberByLabelFunction(this::isV3);
                break;
            case LBL_V3_CRIT_E2E:
                map = calc.getCriticalAutomatedTestNumberByLabelFunction(this::isV3);
                break;
            case LBL_V3_CRIT_E2E_PERCENT:
                map = calc.getCriticalAutomatedPercentTestNumberByLabelFunction(this::isV3);
                break;
            case LBL_V3_CRT_E2E_PRC_EXCL_DONT_AUT:
                map = calc.getCriticalAutomatedPercentExcludeDontAutomateTestNumberByLabelFunction(this::isV3);
                break;
            case LBL_V4:
                map = calc.getTestNumberByLabelFunction(this::isV4);
                break;
            case LBL_V4_E2E:
                map = calc.getAutomatedTestNumberByLabelFunction(this::isV4);
                break;
            case LBL_V4_CRIT:
                map = calc.getCriticalTestNumberByLabelFunction(this::isV4);
                break;
            case LBL_V4_CRIT_E2E:
                map = calc.getCriticalAutomatedTestNumberByLabelFunction(this::isV4);
                break;
            case LBL_V4_CRIT_E2E_PERCENT:
                map = calc.getCriticalAutomatedPercentTestNumberByLabelFunction(this::isV4);
                break;
            case LBL_V4_CRT_E2E_PRC_EXCL_DNT_AUT:
                map = calc.getCriticalAutomatedPercentExcludeDontAutomateTestNumberByLabelFunction(this::isV4);
                break;
            case LBL_LIGHT:
                map = calc.getTestNumberByLabelFunction(this::isLight);
                break;
            case LBL_LIGHT_E2E:
                map = calc.getAutomatedTestNumberByLabelFunction(this::isLight);
                break;
            case LBL_LIGHT_CRIT:
                map = calc.getCriticalTestNumberByLabelFunction(this::isLight);
                break;
            case LBL_LIGHT_CRIT_E2E:
                map = calc.getCriticalAutomatedTestNumberByLabelFunction(this::isLight);
                break;
            case LBL_LIGHT_CRIT_E2E_PERCENT:
                map = calc.getCriticalAutomatedPercentTestNumberByLabelFunction(this::isLight);
                break;
            case LBL_LT_CRT_E2E_PRC_EXCL_DNT_AUT:
                map = calc.getCriticalAutomatedPercentExcludeDontAutomateTestNumberByLabelFunction(this::isLight);
                break;
            case LBL_PMO:
                map = calc.getTestNumberByLabelFunction(this::isPmo);
                break;
            case LBL_PMO_E2E:
                map = calc.getAutomatedTestNumberByLabelFunction(this::isPmo);
                break;
            case LBL_PMO_CRIT:
                map = calc.getCriticalTestNumberByLabelFunction(this::isPmo);
                break;
            case LBL_PMO_CRIT_E2E:
                map = calc.getCriticalAutomatedTestNumberByLabelFunction(this::isPmo);
                break;
            case LBL_PMO_CRIT_E2E_PERCENT:
                map = calc.getCriticalAutomatedPercentTestNumberByLabelFunction(this::isPmo);
                break;
            case LBL_PMO_CRT_E2E_PRC_EXCL_DNT_AUT:
                map = calc.getCriticalAutomatedPercentExcludeDontAutomateTestNumberByLabelFunction(this::isPmo);
                break;
            default:
                throw new ConfluenceConnectException("Не реализована калькуляция для заголовка " + header);
        }
        tableData.put(header, map);
    }

    private boolean isV3(final TestCaseModel tc) {
        return tc.getLabels()
                 .stream()
                 .filter(l -> !"v4".equalsIgnoreCase(l))
                 .filter(l -> !"ЛАЙТ".equalsIgnoreCase(l))
                 .noneMatch("ПМО"::equalsIgnoreCase);
    }

    private boolean isV4(final TestCaseModel tc) {
        return tc.getLabels()
                 .stream()
                 .anyMatch("v4"::equalsIgnoreCase);
    }

    private boolean isLight(final TestCaseModel tc) {
        return tc.getLabels()
                 .stream()
                 .anyMatch("ЛАЙТ"::equalsIgnoreCase);
    }

    private boolean isPmo(final TestCaseModel tc) {
        return tc.getLabels()
                 .stream()
                 .anyMatch("ПМО"::equalsIgnoreCase);
    }

    // заполнение таблицы данными
    private void insertDataToTable() {
        final BiFunction<String, TableHeaderInterface, String> converterFunction = (team, header) -> {
            final String result;
            if (header == TableHeader.TEAM) {
                result = team;
            } else {
                result = tableData.get((TableHeader) header).get(team).toString();
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
                case TEAM:
                    totalValue = null;
                    break;
                case LBL_V3:
                case LBL_V3_E2E:
                case LBL_V3_CRIT:
                case LBL_V3_CRIT_E2E:
                case LBL_V4:
                case LBL_V4_E2E:
                case LBL_V4_CRIT:
                case LBL_V4_CRIT_E2E:
                case LBL_LIGHT:
                case LBL_LIGHT_E2E:
                case LBL_LIGHT_CRIT:
                case LBL_LIGHT_CRIT_E2E:
                case LBL_PMO:
                case LBL_PMO_E2E:
                case LBL_PMO_CRIT:
                case LBL_PMO_CRIT_E2E:
                    totalValue = getTotalFromCol((TableHeader) header);
                    break;
                case LBL_V3_CRIT_E2E_PERCENT:
                    final int v3TotalCrit = getTotalFromCol(TableHeader.LBL_V3_CRIT);
                    totalValue = v3TotalCrit == 0 ? 0 : getTotalFromCol(TableHeader.LBL_V3_CRIT_E2E) * 100 /
                                                        v3TotalCrit;
                    break;
                case LBL_V4_CRIT_E2E_PERCENT:
                    final int v4TotalCrit = getTotalFromCol(TableHeader.LBL_V4_CRIT);
                    totalValue = v4TotalCrit == 0 ? 0 : getTotalFromCol(TableHeader.LBL_V4_CRIT_E2E) * 100 /
                                                        v4TotalCrit;
                    break;
                case LBL_LIGHT_CRIT_E2E_PERCENT:
                    final int lightTotalCrit = getTotalFromCol(TableHeader.LBL_LIGHT_CRIT);
                    totalValue = lightTotalCrit == 0 ? 0 : getTotalFromCol(TableHeader.LBL_LIGHT_CRIT_E2E) * 100 /
                                                           lightTotalCrit;
                    break;
                case LBL_PMO_CRIT_E2E_PERCENT:
                    final int pmoTotalCrit = getTotalFromCol(TableHeader.LBL_PMO_CRIT);
                    totalValue = pmoTotalCrit == 0 ? 0 : getTotalFromCol(TableHeader.LBL_PMO_CRIT_E2E) * 100 /
                                                         pmoTotalCrit;
                    break;
                case LBL_V3_CRT_E2E_PRC_EXCL_DONT_AUT:
                    final int v3TotalExclDontAutomate = calc.getTestNumberExcludeDontAutomateByLabelFunction(this::isV3);
                    totalValue = v3TotalExclDontAutomate == 0 ? 0 : getTotalFromCol(TableHeader.LBL_V3_CRIT_E2E) * 100 /
                                                                    v3TotalExclDontAutomate;
                    break;
                case LBL_V4_CRT_E2E_PRC_EXCL_DNT_AUT:
                    final int v4TotalExclDontAutomate = calc.getTestNumberExcludeDontAutomateByLabelFunction(this::isV4);
                    totalValue = v4TotalExclDontAutomate == 0 ? 0 : getTotalFromCol(TableHeader.LBL_V4_CRIT_E2E) * 100 /
                                                                    v4TotalExclDontAutomate;
                    break;
                case LBL_LT_CRT_E2E_PRC_EXCL_DNT_AUT:
                    final int lightTotalExclDontAutomate = calc.getTestNumberExcludeDontAutomateByLabelFunction(this::isLight);
                    totalValue = lightTotalExclDontAutomate == 0 ? 0 : getTotalFromCol(TableHeader.LBL_LIGHT_CRIT_E2E) * 100 /
                                                                       lightTotalExclDontAutomate;
                    break;
                case LBL_PMO_CRT_E2E_PRC_EXCL_DNT_AUT:
                    final int pmoTotalExclDontAutomate = calc.getTestNumberExcludeDontAutomateByLabelFunction(this::isPmo);
                    totalValue = pmoTotalExclDontAutomate == 0 ? 0 : getTotalFromCol(TableHeader.LBL_PMO_CRIT_E2E) * 100 /
                                                                     pmoTotalExclDontAutomate;
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
        LBL_V3("V3", false),
        LBL_V3_E2E("V3 E2E", false),
        LBL_V3_CRIT("V3 Критичных", false),
        LBL_V3_CRIT_E2E("V3 Критичных E2E", false),
        LBL_V3_CRIT_E2E_PERCENT("V3 Критичных E2E %", false),
        LBL_V3_CRT_E2E_PRC_EXCL_DONT_AUT("V3 Критичных E2E % исключая не автоматизируемые", false),
        LBL_V4("V4", false),
        LBL_V4_E2E("V4 E2E", false),
        LBL_V4_CRIT("V4 Критичных", false),
        LBL_V4_CRIT_E2E("V4 Критичных E2E", false),
        LBL_V4_CRIT_E2E_PERCENT("V4 Критичных E2E %", false),
        LBL_V4_CRT_E2E_PRC_EXCL_DNT_AUT("V4 Критичных E2E % исключая не автоматизируемые", false),
        LBL_LIGHT("Лайт", false),
        LBL_LIGHT_E2E("Лайт E2E", false),
        LBL_LIGHT_CRIT("Лайт Критичных", false),
        LBL_LIGHT_CRIT_E2E("Лайт Критичных E2E", false),
        LBL_LIGHT_CRIT_E2E_PERCENT("Лайт Критичных E2E %", false),
        LBL_LT_CRT_E2E_PRC_EXCL_DNT_AUT("Лайт Критичных E2E % исключая не автоматизируемые", false),
        LBL_PMO("ПМО", false),
        LBL_PMO_E2E("ПМО E2E", false),
        LBL_PMO_CRIT("ПМО Критичных", false),
        LBL_PMO_CRIT_E2E("ПМО Критичных E2E", false),
        LBL_PMO_CRIT_E2E_PERCENT("ПМО Критичных E2E %", false),
        LBL_PMO_CRT_E2E_PRC_EXCL_DNT_AUT("ПМО Критичных E2E % исключая не автоматизируемые", false),
        ;

        private final String colName;
        private final boolean doNotRender;

        TableHeader(final String colName, final boolean doNotRender) {
            this.colName = colName;
            this.doNotRender = doNotRender;
        }

        @Override
        public String getColName() {
            return colName;
        }

        @Override
        public boolean doNotRender() {
            return doNotRender;
        }
    }
}
