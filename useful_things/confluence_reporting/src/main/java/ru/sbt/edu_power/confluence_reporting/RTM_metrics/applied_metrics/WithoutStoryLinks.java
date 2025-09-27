package ru.sbt.edu_power.confluence_reporting.RTM_metrics.applied_metrics;

import ru.sbt.edu_power.confluence_reporting.RTM_metrics.RtmMetricsCalc;
import ru.sbt.edu_power.confluence_reporting.TableTune;
import ru.sbt.edu_power.external_services.jira.agile.JiraSearch;
import ru.sbt.edu_power.external_services.jira.agile.model.IssueFields;
import ru.sbt.edu_power.external_services.jira.agile.model.IssueQuery;
import ru.sbt.edu_power.external_services.jira.agile.model.enums.IssueType;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFilter;
import ru.sbt.edu_power.external_services.jira.test_manager.model.TestCaseModel;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class WithoutStoryLinks extends AbstractMetrics {
    private final int expected = 0;
    private final LocalDateTime expectedDate;
    private final TableTune.BgColor bgColor = TableTune.BgColor.BLUE;
    private final Map<String, Map<String, TestCaseModel>> teamToCaseListMap = new HashMap<>();

    public WithoutStoryLinks(final RtmMetricsCalc calc) {
        super(calc);
        expectedDate = LocalDateTime.now().minusYears(2L);
    }

    @Override
    public Function<Integer, Boolean> predicate() {
        return (actual) -> actual > expected;
    }

    @Override
    public Function<Integer, Boolean> predicateNeedAutomate() {
        return null;
    }

    @Override
    public BiFunction<String, Integer, String> descriptionFunction() {
        return (team, size) -> String.format(
                "Не прилинкованы стори или подзадачи, в рамках которых выполнено тестирование НФ в кейсах: %s",
                joinListWithLimit(teamToCaseListMap.get(team).keySet())
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
            final String[] issueList = TCFilter.of(list)
                    .filterByDate(expectedDate, TestCaseModel::getCreatedOn, false)
                    .toMap()
                    .values()
                    .stream()
                    .flatMap(t -> t
                            .getIssueLinks()
                            .stream()
                            .map(TestCaseModel.IssueLinks::getIssueId))
                    .collect(Collectors.toList())
                    .toArray(new String[]{});
            final Map<String, IssueFields> issueMap = new HashMap<>();
            if (issueList.length > 0) {
                final IssueQuery query = new IssueQuery(0, 20);
                query.and(
                                "id",
                                IssueQuery.Op.IN,
                                issueList
                        )
                        .and(
                                IssueFields.Field.ISSUE_TYPE.getFieldName(),
                                IssueQuery.Op.IN,
                                IssueType.STORY.getValue(),
                                IssueType.SUB_TASK.getValue(),
                                IssueType.TASK.getValue(),
                                IssueType.EPIC.getValue(),
                                IssueType.CHANGE_REQUEST.getValue()
                        )
                        // в ответе нам нужно только id и key, но они приходят отдельно от остальных полей,
                        // поэтому запрашиваем одно обязательное поле, что бы сократить объём данных
                        .setFields(IssueFields.Field.PROJECT.getFieldName());
                final JiraSearch search = new JiraSearch(query);
                search.search();
                issueMap.putAll(
                        search.getIssueList()
                                .entrySet()
                                .stream()
                                .collect(Collectors.toMap(e -> e
                                        .getValue()
                                        .asMap()
                                        .get("id")
                                        .toString(), Map.Entry::getValue))
                );
            }
            teamToCaseListMap.put(
                    team,
                    TCFilter.of(list)
                            .filterByDate(expectedDate, TestCaseModel::getCreatedOn, false)
                            .filter(t -> t.getIssueLinks().isEmpty() ||
                                    t.getIssueLinks()
                                            .stream()
                                            .map(TestCaseModel.IssueLinks::getIssueId)
                                            .noneMatch(issueMap::containsKey)
                            )
                            .toMap()

            );
            map.put(team, teamToCaseListMap.get(team).size());
        });
        return map;
    }
}
