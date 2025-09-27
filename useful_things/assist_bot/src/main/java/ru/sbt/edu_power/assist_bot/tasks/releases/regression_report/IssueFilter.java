package ru.sbt.edu_power.assist_bot.tasks.releases.regression_report;

import ru.sbt.edu_power.external_services.version_releases.JiraVersionTracker;
import ru.sbt.edu_power.external_services.jira.agile.model.IssueFields;
import ru.sbt.edu_power.external_services.jira.agile.model.IssueQuery;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraVersionModel;
import ru.sbt.edu_power.external_services.jira.agile.model.enums.IssuePriority;
import ru.sbt.edu_power.external_services.jira.agile.model.enums.IssueResolution;
import ru.sbt.edu_power.external_services.jira.agile.model.enums.IssueStatus;
import ru.sbt.edu_power.external_services.jira.agile.model.enums.IssueType;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class IssueFilter {
    private final Map<String, IssueFields> issues;
    private final Collection<IssueFields> filteredData = new ArrayList<>();
    private final List<JiraVersionModel> beforeWithCurrentRc;
    private final List<JiraVersionModel> afterRc;
    private IssueQuery issueQuery;
    private final TCFields.ProjectId projectId;

    public IssueFilter(
            final Map<String, IssueFields> issues,
            final TCFields.ProjectId projectId,
            final JiraVersionModel jiraVersionModel
    ) {
        this.projectId = projectId;
        final JiraVersionTracker jiraVersionTracker = new JiraVersionTracker(projectId);
        this.issues = issues;
        beforeWithCurrentRc = jiraVersionTracker.getVersionsBeforeRc(jiraVersionModel);
        beforeWithCurrentRc.add(jiraVersionModel);
        afterRc = jiraVersionTracker.getVersionsAfterRc(jiraVersionModel);
    }

    public List<IssueFields> getFilteredData() {
        return new ArrayList<>(filteredData);
    }

    public int getFilteredDataSize() {
        return filteredData.size();
    }

    public IssueFilter clear() {
        filteredData.clear();
        filteredData.addAll(issues.values());
        issueQuery = new IssueQuery(0, 20);
        issueQuery.and(IssueFields.Field.PROJECT, IssueQuery.Op.EQUAL, projectId.name());
        return this;
    }

    public IssueFilter blocker() {
        filter(IssuePriority.BLOCKER);
        issueQuery.and(
                IssueFields.Field.PRIORITY,
                IssueQuery.Op.EQUAL,
                IssuePriority.BLOCKER.getValue()
        );
        return this;
    }

    public IssueFilter critical() {
        filter(IssuePriority.CRITICAL);
        issueQuery.and(
                IssueFields.Field.PRIORITY,
                IssueQuery.Op.EQUAL,
                IssuePriority.CRITICAL.getValue()
        );
        return this;
    }

    public IssueFilter high() {
        filter(IssuePriority.HIGH);
        issueQuery.and(
                IssueFields.Field.PRIORITY,
                IssueQuery.Op.EQUAL,
                IssuePriority.HIGH.getValue()
        );
        return this;
    }

    // оставляет все кейсы с приоритетом ниже High (medium, low, lowest)
    public IssueFilter belowHighPriority() {
        filter(IssuePriority.MEDIUM, IssuePriority.LOW, IssuePriority.LOWEST);
        issueQuery.and(
                IssueFields.Field.PRIORITY,
                IssueQuery.Op.IN,
                IssuePriority.MEDIUM.getValue(),
                IssuePriority.LOW.getValue(),
                IssuePriority.LOWEST.getValue()
        );
        return this;
    }

    public IssueFilter blockerAndCritical() {
        filter(IssuePriority.BLOCKER, IssuePriority.CRITICAL);
        issueQuery.and(
                IssueFields.Field.PRIORITY,
                IssueQuery.Op.EQUAL,
                IssuePriority.BLOCKER.getValue(),
                IssuePriority.CRITICAL.getValue()
        );
        return this;
    }

    // Фильтр для получения всех кейсов с приоритетом выше среднего (high, critical, blocker)
    public IssueFilter aboveMediumPriority() {
        filter(IssuePriority.HIGH, IssuePriority.CRITICAL, IssuePriority.BLOCKER);
        issueQuery.and(
                IssueFields.Field.PRIORITY,
                IssueQuery.Op.IN,
                IssuePriority.BLOCKER.getValue(),
                IssuePriority.CRITICAL.getValue(),
                IssuePriority.HIGH.getValue()
        );
        return this;
    }

    public IssueFilter incidents() {
        filter(IssueType.INCIDENT);
        issueQuery.and(IssueFields.Field.ISSUE_TYPE, IssueQuery.Op.IN, IssueType.INCIDENT.getValue());
        return this;
    }

    public IssueFilter bugs() {
        filter(IssueType.BUG);
        issueQuery.and(IssueFields.Field.ISSUE_TYPE, IssueQuery.Op.IN, IssueType.BUG.getValue());
        return this;
    }

    public IssueFilter incidentsAndBugs() {
        filter(IssueType.INCIDENT, IssueType.BUG);
        issueQuery.and(
                IssueFields.Field.ISSUE_TYPE,
                IssueQuery.Op.IN,
                IssueType.INCIDENT.getValue(),
                IssueType.BUG.getValue()
        );
        return this;
    }

    public IssueFilter story() {
        filter(IssueType.STORY);
        issueQuery.and(
                IssueFields.Field.ISSUE_TYPE,
                IssueQuery.Op.EQUAL,
                IssueType.STORY.getValue()
        );
        return this;
    }

    public IssueFilter beforeWithCurrentRc() {
        filter(beforeWithCurrentRc);
        issueQuery.and(
                IssueFields.Field.FIX_VERSIONS,
                IssueQuery.Op.IN,
                beforeWithCurrentRc.stream().map(JiraVersionModel::getName).toArray(String[]::new)
        );
        return this;
    }

    public IssueFilter afterRc() {
        if (afterRc.isEmpty()) {
            filteredData.clear();
            return this;
        }
        filter(afterRc);
        issueQuery.and(
                IssueFields.Field.FIX_VERSIONS,
                IssueQuery.Op.IN,
                afterRc.stream().map(JiraVersionModel::getName).toArray(String[]::new)
        );
        return this;
    }

    public IssueFilter allRc() {
        final String[] allVersions = Stream.of(beforeWithCurrentRc, afterRc)
                .flatMap(List::stream)
                .map(JiraVersionModel::getName)
                .toArray(String[]::new);
        issueQuery.and(
                IssueFields.Field.FIX_VERSIONS,
                IssueQuery.Op.IN,
                allVersions
        );
        return this;
    }

    public IssueFilter allIssueType() {
        issueQuery.and(
                IssueFields.Field.ISSUE_TYPE,
                IssueQuery.Op.IN,
                IssueType.INCIDENT.getValue(),
                IssueType.BUG.getValue(),
                IssueType.STORY.getValue()
        );
        return this;
    }

    public IssueFilter inOpenStatus() {
        filteredData.removeIf(is -> IssueStatus.IFT.getValue().equals(is.get(IssueFields.Field.STATUS)));
        issueQuery.and(IssueFields.Field.STATUS, IssueQuery.Op.NOT_EQUAL, IssueStatus.IFT.getValue())
                  .and(IssueFields.Field.RESOLUTION, IssueQuery.Op.EQUAL, IssueResolution.UNRESOLVED.getValue());
        return this;
    }

    public IssueFilter inIftStatus() {
        filteredData.removeIf(is -> !IssueStatus.IFT.getValue().equals(is.get(IssueFields.Field.STATUS)));
        issueQuery.and(IssueFields.Field.STATUS, IssueQuery.Op.EQUAL, IssueStatus.IFT.getValue());
        return this;
    }

    public String getJqlLink() {
        if (filteredData.isEmpty()) {
            return "-";
        }
        final String link = String.format(
                "https://jira.pcbltools.ru/jira/issues/?filter=-1&jql=%s order by updated DESC",
                issueQuery.getJql()
        );
        return String.format("<%s|%d>", link, filteredData.size());
    }

    private void filter(final IssueType... issueType) {
        final String[] issueTypes = Stream.of(issueType)
                                          .map(IssueType::getValue)
                                          .toArray(String[]::new);
        Arrays.sort(issueTypes);
        filteredData.removeIf(is -> Arrays.binarySearch(issueTypes, is.get(IssueFields.Field.ISSUE_TYPE)) < 0);
    }

    private void filter(final IssuePriority... priority) {
        final String[] priorities = Stream.of(priority)
                .map(IssuePriority::getValue)
                .toArray(String[]::new);
        Arrays.sort(priorities);
        filteredData.removeIf(is -> Arrays.binarySearch(priorities, is.get(IssueFields.Field.PRIORITY)) < 0);
    }

    private void filter(final List<JiraVersionModel> jiraVersionModelList) {
        final String[] versions = jiraVersionModelList
                .stream()
                .map(JiraVersionModel::getName)
                .toArray(String[]::new);
        filteredData.removeIf(is ->
                is.getValues(IssueFields.Field.FIX_VERSIONS)
                  .stream()
                  .noneMatch(v -> Arrays.binarySearch(versions, v) > -1)
        );
    }
}
