package ru.sbt.edu_power.risk_assessment;

import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.ExternalServicesException;
import ru.sbt.edu_power.external_services.confluence.ConfluenceDocument;
import ru.sbt.edu_power.external_services.confluence.TableHeaderInterface;
import ru.sbt.edu_power.external_services.jira.agile.JiraSearch;
import ru.sbt.edu_power.external_services.jira.agile.model.IssueFields;
import ru.sbt.edu_power.external_services.jira.agile.model.IssueQuery;
import ru.sbt.edu_power.external_services.jira.agile.model.enums.IssuePriority;
import ru.sbt.edu_power.external_services.jira.agile.model.enums.IssueType;
import ru.sbt.edu_power.external_services.jira.tc_verifier.NewJiraTCCollector;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFilter;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCQueryBuilder;
import ru.sbt.edu_power.external_services.jira.test_manager.model.TestCaseModel;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class TCDefectStat extends ConfluenceDocument {
    // тест-кейсы к мапе количества дефектов по приоритетам
    private final Map<TestCaseModel, EnumMap<IssuePriority, Integer>> testCaseToIssuePriority = new HashMap<>();
    private int allTestCaseNumber;
    private final TCQueryBuilder queryBuilder = new TCQueryBuilder();
    private final TCFields.ProjectId project;
    private final EnumMap<TableHeader, EnumMap<Metrics, Integer>> data = new EnumMap<>(TableHeader.class);

    public TCDefectStat(final String pageId, final String projectKey) {
        super(pageId);
        project = TCFields.ProjectId.valueOf(projectKey);
    }

    public void generate() {
        loadDocument();
        log.info("Документ загружен");
        collectRtm();
        log.info("Данные из РТМ получены");
        calculate();
        log.info("Рассчёты выполнены");
        removeTableData();
        insertHeaderToDocument(Stream
                .of(TableHeader.values())
                .collect(Collectors.toList()), TableHeaderInterface::getColName);
        insertDataToTable();
        log.info("Таблица сформирована");
        saveDocument();
        log.info("Документ сохранён");
    }

    // заполнение таблицы данными
    private void insertDataToTable() {
        final BiFunction<String, TableHeaderInterface, String> converterFunction = (metric, header) -> {
            if (header == TableHeader.EMPTY) {
                return Metrics.valueOf(metric).getDesc();
            }
            return data.get(header)
                       .get(Metrics.valueOf(metric))
                       .toString();
        };
        insertDataToTable(
                Stream.of(Metrics.values()).map(Metrics::name).collect(Collectors.toList()),
                Stream.of(TableHeader.values()).collect(Collectors.toList()),
                converterFunction
        );
    }

    private void collectRtm() {
        queryBuilder.addField(TCFields.ARCHIVED, false)
                    .addField(TCFields.PROJECT_ID, TCFields.ProjectId.valueOf(project.name()))
                    .addField(TCFields.TEST_VIEW, TCFields.TestView.U_I)
                    .addField(TCFields.TEST_TYPE, TCFields.TestType.REGRESS)
                    .addField(TCFields.FOLDER, NewJiraTCCollector.getInstance().getFolderId("Регресс", project.id));
        NewJiraTCCollector.getInstance().collect(queryBuilder);
    }

    private void calculate() {
        for (final TableHeader header : TableHeader.values()) {
            if (header == TableHeader.EMPTY) {
                continue;
            }
            collectStat(header);
            addTableMetrics(header);
        }
    }

    private void addTableMetrics(final TableHeader header) {
        data.put(header, new EnumMap<>(Metrics.class));
        for (final Metrics metric : Metrics.values()) {
            final int size;
            switch (metric) {
                case ALL_TC:
                    size = allTestCaseNumber;
                    break;
                case TC_HIGH_WITH_DEFECTS:
                    size = getDefectCountByCasePriority(TCFields.Priority.HIGH);
                    break;
                case TC_MEDIUM_WITH_DEFECTS:
                    size = getDefectCountByCasePriority(TCFields.Priority.MEDIUM);
                    break;
                case TC_LOW_WITH_DEFECTS:
                    size = getDefectCountByCasePriority(TCFields.Priority.LOW);
                    break;
                case HAS_LOW:
                    size = getByDefectPriority(IssuePriority.LOW, 1);
                    break;
                case TWO_LOW:
                    size = getByDefectPriority(IssuePriority.LOW, 2);
                    break;
                case HAS_HIGH:
                    size = getByDefectPriority(IssuePriority.HIGH, 1);
                    break;
                case TWO_HIGH:
                    size = getByDefectPriority(IssuePriority.HIGH, 2);
                    break;
                case HAS_LOWEST:
                    size = getByDefectPriority(IssuePriority.LOWEST, 1);
                    break;
                case TWO_LOWEST:
                    size = getByDefectPriority(IssuePriority.LOWEST, 2);
                    break;
                case HAS_MEDIUM:
                    size = getByDefectPriority(IssuePriority.MEDIUM, 1);
                    break;
                case TWO_MEDIUM:
                    size = getByDefectPriority(IssuePriority.MEDIUM, 2);
                    break;
                case HAS_BLOCKERS:
                    size = getByDefectPriority(IssuePriority.BLOCKER, 1);
                    break;
                case TWO_BLOCKERS:
                    size = getByDefectPriority(IssuePriority.BLOCKER, 2);
                    break;
                case HAS_CRITICAL:
                    size = getByDefectPriority(IssuePriority.CRITICAL, 1);
                    break;
                case TWO_CRITICAL:
                    size = getByDefectPriority(IssuePriority.CRITICAL, 2);
                    break;
                case WITH_DEFECTS:
                    size = getAllIssuedTestCasesSize();
                    break;
                case WITHOUT_DEFECTS:
                    size = getAllTestCaseNumber() - getAllIssuedTestCasesSize();
                    break;
                case HAS_MORE_THAN_5:
                    size = getByDefectsNumber(5);
                    break;
                case HAS_MORE_THAN_4:
                    size = getByDefectsNumber(4);
                    break;
                case HAS_MORE_THAN_3:
                    size = getByDefectsNumber(3);
                    break;
                case HAS_MORE_THAN_2:
                    size = getByDefectsNumber(2);
                    break;
                case HAS_MORE_THAN_1:
                    size = getByDefectsNumber(1);
                    break;
                case HAS_MORE_THAN_1_TC_HIGH:
                    size = getByDefectsNumberAndCasePriority(1, TCFields.Priority.HIGH);
                    break;
                case HAS_MORE_THAN_1_TC_MEDIUM:
                    size = getByDefectsNumberAndCasePriority(1, TCFields.Priority.MEDIUM);
                    break;
                case HAS_MORE_THAN_1_TC_LOW:
                    size = getByDefectsNumberAndCasePriority(1, TCFields.Priority.LOW);
                    break;
                default:
                    throw new ExternalServicesException("Нет обработчика для метрики " + metric.name());
            }
            data.get(header).put(metric, size);
        }
    }

    public void collectStat(final TableHeader header) {
        testCaseToIssuePriority.clear();
        allTestCaseNumber = (int) getFilteredStream(header).count();
        final IssueQuery issueQuery = new IssueQuery(0, 100);
        final String[] issueIds = getFilteredStream(header)
                .filter(t -> !t.getIssueLinks().isEmpty())
                .peek(t -> testCaseToIssuePriority.put(t, new EnumMap<>(IssuePriority.class)))
                .flatMap(t -> t.getIssueLinks().stream())
                .map(TestCaseModel.IssueLinks::getIssueId)
                .collect(Collectors.toList())
                .toArray(new String[]{});
        issueQuery.and(IssueFields.Field.ID, IssueQuery.Op.IN, issueIds)
                  .and(
                          IssueFields.Field.ISSUE_TYPE,
                          IssueQuery.Op.IN,
                          IssueType.BUG.getValue(),
                          IssueType.INCIDENT.getValue()
                  )
                  .and(
                          IssueFields.Field.CREATED.getFieldName(),
                          IssueQuery.Op.MORE_OR_EQUALS,
                          "-" + header.getIssueCreatedLastDays() + "d"
                  );
        final JiraSearch jiraSearch = new JiraSearch(issueQuery);
        jiraSearch.search();
        final Map<String, IssueFields> issues = jiraSearch.getIssueList();
        testCaseToIssuePriority.keySet()
                               .forEach(t -> t.getIssueLinks()
                                              .forEach(issueLinks -> sortTestCaseByPriority(issues, t, issueLinks)));
    }

    private Stream<TestCaseModel> getFilteredStream(final TableHeader header) {
        final LocalDate monthOlderThan = LocalDate.now().minusMonths(header.getTcMonthOlderThan());
        return TCFilter.of(NewJiraTCCollector.getInstance().asMap())
                       .filter(queryBuilder)
                       .toMap()
                       .values()
                       .stream()
                       .filter(t -> t
                               .getCreatedOn()
                               .toInstant()
                               .atZone(ZoneId.of("UTC"))
                               .toLocalDate()
                               .isBefore(monthOlderThan));
    }

    private void sortTestCaseByPriority(
            final Map<String, IssueFields> issues,
            final TestCaseModel testCaseModel,
            final TestCaseModel.IssueLinks issueLinks
    ) {
        final IssueFields issue = getIssueById(issues, issueLinks.getIssueId());
        if (Objects.isNull(issue)) {
            return;
        }

        final IssuePriority priority = IssuePriority.getByName(issue.get(IssueFields.Field.PRIORITY));
        if (priority == IssuePriority.UNDEFINED) {
            log.error(
                    "Не установлен приоритет для дефекта {}",
                    getIssueKeyByLinkedIssueId(issues, issueLinks.getIssueId())
            );
            return;
        }
        if (!testCaseToIssuePriority.get(testCaseModel).containsKey(priority)) {
            testCaseToIssuePriority.get(testCaseModel).put(priority, 0);
        }
        testCaseToIssuePriority
                .get(testCaseModel)
                .put(priority, testCaseToIssuePriority.get(testCaseModel).get(priority) + 1);
    }

    // метод находит issue в списке по его ID и возвращает его ключ
    private String getIssueKeyByLinkedIssueId(final Map<String, IssueFields> issues, final String issueId) {
        return issues.entrySet()
                     .stream()
                     .filter(e -> e.getValue().get(
                             IssueFields.Field.ID).equals(issueId))
                     .map(Map.Entry::getKey)
                     .findFirst()
                     .orElse("");
    }

    private int getAllTestCaseNumber() {
        return allTestCaseNumber;
    }

    // метод возвращает количество тестов с дефектами
    public Integer getAllIssuedTestCasesSize() {
        return (int) testCaseToIssuePriority.values()
                                            .stream()
                                            .filter(m -> !m.isEmpty())
                                            .count();
    }

    // метод возвращает количество кейсов с дефектами выбранного приоритета и
    // количеством дефектов больше или равно выбранному количеству
    public Integer getByDefectPriority(final IssuePriority priority, final int count) {
        return (int) testCaseToIssuePriority.values()
                                            .stream()
                                            .filter(m -> m.containsKey(priority))
                                            .filter(m -> m.get(priority) >= count)
                                            .count();
    }

    // метод возвращает количество кейсов в которых количество дефектов больше выбранного количества
    public Integer getByDefectsNumber(final int defectNumbers) {
        return (int) testCaseToIssuePriority.values()
                                            .stream()
                                            .filter(m -> !m.isEmpty())
                                            .filter(m -> m.values().stream().reduce(Integer::sum).orElse(0) >
                                                         defectNumbers)
                                            .count();
    }

    private Integer getByDefectsNumberAndCasePriority(final int defectNumbers, final TCFields.Priority priority) {
        return (int) testCaseToIssuePriority.keySet()
                                            .stream()
                                            .filter(t -> t.getPriority().getName().equals(priority.value))
                                            .map(testCaseToIssuePriority::get)
                                            .filter(m -> !m.isEmpty())
                                            .filter(m -> m.values().stream().reduce(Integer::sum).orElse(0) >
                                                         defectNumbers)
                                            .count();
    }

    public Integer getDefectCountByCasePriority(final TCFields.Priority priority) {
        return (int) testCaseToIssuePriority.keySet()
                                            .stream()
                                            .filter(t -> t.getPriority().getName().equals(priority.value))
                                            .filter(t -> !testCaseToIssuePriority.get(t).isEmpty())
                                            .count();
    }

    private IssueFields getIssueById(final Map<String, IssueFields> issues, final String id) {
        return issues.values().stream()
                     .filter(i -> i.get(IssueFields.Field.ID).equals(id))
                     .findFirst()
                     .orElse(null);
    }

    private enum Metrics {
        ALL_TC("Всего тест-кейсов"),
        WITHOUT_DEFECTS("Тест-кейсов без дефектов"),
        WITH_DEFECTS("Тест-кейсы с дефектами"),
        TC_HIGH_WITH_DEFECTS("Тест-кейсы HIGH с дефектами"),
        TC_MEDIUM_WITH_DEFECTS("Тест-кейсы MEDIUM с дефектами"),
        TC_LOW_WITH_DEFECTS("Тест-кейсы LOW с дефектами"),
        TWO_BLOCKERS("Два и более блокера"),
        HAS_BLOCKERS("Есть блокеры"),
        TWO_CRITICAL("Два и более крита"),
        HAS_CRITICAL("Есть криты"),
        TWO_HIGH("Два и более хай"),
        HAS_HIGH("Есть хай"),
        TWO_MEDIUM("Два и более медиум"),
        HAS_MEDIUM("Есть медиум"),
        TWO_LOW("Два и более лоу"),
        HAS_LOW("Есть лоу"),
        TWO_LOWEST("Два и более лоуэст"),
        HAS_LOWEST("Есть лоуэст"),
        HAS_MORE_THAN_5("Больше 5 дефектов"),
        HAS_MORE_THAN_4("Больше 4 дефектов"),
        HAS_MORE_THAN_3("Больше 3 дефектов"),
        HAS_MORE_THAN_2("Больше 2 дефектов"),
        HAS_MORE_THAN_1("Больше 1 дефектов"),
        HAS_MORE_THAN_1_TC_HIGH("Больше 1 дефектов в тест-кейсах HIGH"),
        HAS_MORE_THAN_1_TC_MEDIUM("Больше 1 дефектов в тест-кейсах MEDIUM"),
        HAS_MORE_THAN_1_TC_LOW("Больше 1 дефектов в тест-кейсах LOW");

        private final String desc;

        Metrics(final String desc) {
            this.desc = desc;
        }

        public String getDesc() {
            return desc;
        }

        public static Metrics getByDesc(final String desc) {
            return Stream.of(Metrics.values())
                         .filter(m -> desc.equals(m.desc))
                         .findFirst()
                         .orElseThrow(() -> new ExternalServicesException("Не найдено значения для " + desc));
        }
    }

    private enum TableHeader implements TableHeaderInterface {
        EMPTY("Метрики", 0, 0),
        TC_OLDER_3M_WITH_DEF_IN_LAST_3M("Тесты старше 3 месяцев, дефекты в последние 3 месяца", 3, 90),
        TC_OLDER_3M_WITH_DEF_IN_LAST_6M("Тесты старше 3 месяцев, дефекты в последние 6 месяцев", 3, 180),
        TC_OLDER_1M_WITH_DEF_IN_LAST_3M("Тесты старше 1 месяца, дефекты в последние 3 месяца", 1, 90),
        TC_OLDER_1M_WITH_DEF_IN_LAST_6M("Тесты старше 1 месяца, дефекты в последние 6 месяцев", 1, 180);

        private final String desc;
        private final int tcMonthOlderThan;
        private final int issueCreatedLastDays;


        TableHeader(final String desc, final int tcMonthOlderThan, final int issueCreatedLastDays) {
            this.desc = desc;
            this.tcMonthOlderThan = tcMonthOlderThan;
            this.issueCreatedLastDays = issueCreatedLastDays;
        }

        @Override
        public String getColName() {
            return desc;
        }

        @Override
        public boolean doNotRender() {
            return false;
        }

        public int getTcMonthOlderThan() {
            return tcMonthOlderThan;
        }

        public int getIssueCreatedLastDays() {
            return issueCreatedLastDays;
        }
    }
}
