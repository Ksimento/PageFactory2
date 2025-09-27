package ru.sbt.edu_power.risk_assessment;

import com.google.gson.Gson;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.UnirestException;
import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.ESUtils;
import ru.sbt.edu_power.external_services.jira.JiraConnect;
import ru.sbt.edu_power.external_services.jira.agile.JiraSearch;
import ru.sbt.edu_power.external_services.jira.agile.model.IssueFields;
import ru.sbt.edu_power.external_services.jira.agile.model.IssueLinks;
import ru.sbt.edu_power.external_services.jira.agile.model.IssueQuery;
import ru.sbt.edu_power.external_services.jira.agile.model.enums.IssueStatus;
import ru.sbt.edu_power.external_services.jira.agile.model.enums.IssueType;
import ru.sbt.edu_power.external_services.jira.tc_verifier.NewJiraTCCollector;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFilter;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCQueryBuilder;
import ru.sbt.edu_power.external_services.jira.test_manager.model.TestCaseModel;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.MassExecution;
import ru.sbt.edu_power.external_services.timer.Timer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class LinkedIssueCollector {
    private final ConcurrentHashMap<String, List<String>> testCaseToAllIssuesList = new ConcurrentHashMap<>();
    private final Map<String, List<IssueFields>> testCaseToIssueList = new HashMap<>();
    private final TCQueryBuilder queryBuilder;
    private int maxExecutionsCount;

    public LinkedIssueCollector(final TCQueryBuilder queryBuilder) {
        this.queryBuilder = queryBuilder;
    }

    public void collectIssues() {
        Timer.startTimer("collectIssues");
        final List<String> issueKeyList = Collections.synchronizedList(new ArrayList<>());
        final Map<String, TestCaseModel> testCaseMap = TCFilter.of(NewJiraTCCollector.getInstance().asMap())
                                                               .filter(queryBuilder)
                                                               .toMap();

        // получаю все дефекты привязанные к тест-кейсам и к экзекюшенам этих тест-кейсов
        collectIssueFromTestCasesAndExecutions(issueKeyList, testCaseMap);
        log.info(
                "получение всех дефектов привязанных к тест-кейсам и к экзекюшенам этих тест-кейсов: {}",
                Timer.getDelta("collectIssues", "collectIssues") / 1000d
        );

        final TCFields.ProjectId projectId = TCFields.ProjectId.getById(
                testCaseMap.entrySet().iterator().next().getValue().getProjectId()
        );

        final Map<String, IssueFields> issues = loadDefects(projectId, issueKeyList);
        log.info(
                "получение всех дефектов из джиры: {}",
                Timer.getDelta("collectIssues", "collectIssues") / 1000d
        );

        log.info("Всего дефектов: {}", issues.size());

        testCaseMap.values().forEach(tc -> {
            testCaseToAllIssuesList.get(tc.getKey()).forEach(l -> {
                // заполняем testCaseToIssueList
                if (issues.containsKey(l)) {
                    if (!testCaseToIssueList.containsKey(tc.getKey())) {
                        testCaseToIssueList.put(tc.getKey(), new ArrayList<>());
                    }
                    testCaseToIssueList.get(tc.getKey()).add(issues.get(l));
                }

            });
        });
        log.info("Всего тест-кейсов с дефектами: {}", testCaseToIssueList.size());
    }

    private Map<String, IssueFields> loadDefects(final TCFields.ProjectId projectId, final List<String> linkedIssues) {
        final IssueQuery query = new IssueQuery(0, 100);
        query
                .setFields(IssueFields.Field.ISSUE_TYPE.getFieldName(),
                        IssueFields.Field.STATUS.getFieldName(),
                        IssueFields.Field.CREATED.getFieldName(),
                        IssueFields.Field.ISSUELINKS.getFieldName()
                )
                .and(
                        IssueFields.Field.ISSUE_TYPE,
                        IssueQuery.Op.IN,
                        IssueType.BUG.getValue(),
                        IssueType.INCIDENT.getValue()
                )
                .and(IssueFields.Field.PROJECT, IssueQuery.Op.EQUAL, projectId.name())
                .and(IssueFields.Field.CREATED, IssueQuery.Op.MORE_OR_EQUALS, "-365d");
        final JiraSearch search = new JiraSearch(query);
        search.search();
        return search.getIssueList()
                     .values()
                     .stream()
                     .filter(isf -> linkedIssues.contains(isf.get(IssueFields.Field.ID)))
                     .filter(isf -> {
                         if (IssueType.BUG.getValue().equals(isf.get(IssueFields.Field.ISSUE_TYPE))) {
                             return !IssueStatus.CANCELLED.getValue().equals(isf.get(IssueFields.Field.STATUS));
                         }
                         if (IssueStatus.CANCELLED
                                 .getValue()
                                 .equals(isf.get(IssueFields.Field.STATUS))) {
                             final Set<IssueLinks> issueLinks = isf.getObjectList(
                                     IssueFields.Field.ISSUELINKS);
                             if (issueLinks != null) {
                                 return issueLinks
                                         .stream()
                                         .flatMap(i -> Stream.of(
                                                 i.getInwardIssue(),
                                                 i.getOutwardIssue()
                                         ))
                                         .filter(Objects::nonNull)
                                         .map(o -> o.getFields().getIssuetype().getName())
                                         .anyMatch(n -> IssueType.RISK.getValue().equals(n));
                             }
                         }
                         return true;
                     })
                     .collect(Collectors.toMap(isf -> isf.get(IssueFields.Field.ID), isf -> isf, (a, b) -> b));
    }

    private void collectIssueFromTestCasesAndExecutions(
            final List<String> issueKeyList,
            final Map<String, TestCaseModel> testCaseMap
    ) {
        testCaseMap.values()
                   .parallelStream()
                   .forEach(tc -> {
                       testCaseToAllIssuesList.put(tc.getKey(), new ArrayList<>());
                       tc.getIssueLinks().forEach(l -> {
                           issueKeyList.add(l.getIssueId());
                           testCaseToAllIssuesList.get(tc.getKey()).add(l.getIssueId());
                       });
                       final List<String> issuesFromExecutions = getIssueIdListFromExecutions(tc.getId().toString());
                       issueKeyList.addAll(issuesFromExecutions);
                       testCaseToAllIssuesList.get(tc.getKey()).addAll(issuesFromExecutions);
                   });
        log.info("Всего получено тест-кейсов: {}", testCaseToAllIssuesList.size());
        log.info(
                "Всего дефектов: {}",
                testCaseToAllIssuesList.values().stream().mapToInt(List::size).reduce(Integer::sum).orElse(0)
        );
    }

    private List<String> getIssueIdListFromExecutions(final String testCaseId) {
        final List<String> issues = new ArrayList<>();
        final AtomicReference<Throwable> exception = new AtomicReference<>();
        final BooleanSupplier waitWhenDataBeLoad = () -> {
            try {
                issues.clear();
                final HttpResponse<JsonNode> response = JiraConnect.testCaseExecutionsGet(testCaseId);
                // иногда получаем 500 при вызове response.getBody(), пока обрабатываем путем NullPointerException, нужно разобратться почему Jira шелет 500
                if(!response.isSuccess()){
                    return false;
                }
                if (response.getBody().getObject().getInt("totalCount") > maxExecutionsCount) {
                    maxExecutionsCount = response.getBody().getObject().getInt("totalCount");
                }
                final Gson gson = new Gson();
                for (final Object o : response.getBody().getObject().getJSONArray("data")) {
                    final MassExecution execution = gson.fromJson(o.toString(), MassExecution.class);
                    execution.getIssueLinks().forEach(il -> issues.add(il.getIssueId()));
                }
                return true;
            } catch (final UnirestException e) {
                exception.set(e);
                log.error("Запрос {} провален: {}", testCaseId, e.getMessage());
                ESUtils.freeze(3000);
                return false;
            }
        };

        if (!Timer.executeTimer(30, waitWhenDataBeLoad)) {
            throw new RiskAssessmentException(exception.get());
        }
        return issues;
    }

    public boolean checkIfTestHasDefect(final String testCaseKey) {
        return testCaseToIssueList.containsKey(testCaseKey);
    }

    // проверяем у тест-кейса наличие дефектов с датой создания от (минус месяцев) до (минус месяцев)
    // Отсчёт от текущей даты
    public boolean checkIfTestHasDefectInMonthRange(
            final int minusMonthFrom,
            final int minusMonthTo,
            final String testCaseKey
    ) {
        final LocalDateTime localDateFrom = LocalDateTime.now().minusMonths(minusMonthFrom);
        final LocalDateTime localDateTo = LocalDateTime.now().minusMonths(minusMonthTo);
        if (!testCaseToIssueList.containsKey(testCaseKey)) {
            return false;
        }
        return testCaseToIssueList.get(testCaseKey)
                                  .stream()
                                  .map(i -> i.get(IssueFields.Field.CREATED))
                                  .map(text -> LocalDateTime.parse(
                                          text,
                                          DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSx")
                                  ))
                                  .anyMatch(date -> date.isAfter(localDateFrom) && date.isBefore(localDateTo));

    }
}
