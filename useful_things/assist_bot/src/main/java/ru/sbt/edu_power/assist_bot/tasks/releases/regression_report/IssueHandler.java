package ru.sbt.edu_power.assist_bot.tasks.releases.regression_report;

import ru.sbt.edu_power.external_services.version_releases.JiraVersionTracker;
import ru.sbt.edu_power.external_services.jira.agile.JiraSearch;
import ru.sbt.edu_power.external_services.jira.agile.JiraVersion;
import ru.sbt.edu_power.external_services.jira.agile.model.IssueFields;
import ru.sbt.edu_power.external_services.jira.agile.model.IssueQuery;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraVersionModel;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IssueHandler {
    private static final String JQL = "project = {PROJECT} AND ((issuetype = Story AND status = IFT) OR (issuetype in (Incident, Bug) AND resolution = unresolved)) AND fixVersion in ({VERSIONS}) order by updated DESC";
    private final TCFields.ProjectId projectId;
    private final JiraVersionModel jiraVersion;
    private final StringBuilder stringBuilder = new StringBuilder();
    private final IssueFilter issueFilter;
    
    public IssueHandler(final String projectName, final String jiraVersionId) {
        projectId = TCFields.ProjectId.valueOf(projectName);
        jiraVersion = new JiraVersion().getJiraVersionById(projectId.id, jiraVersionId);
        issueFilter = new IssueFilter(getIssues(), projectId, jiraVersion);
    }
    

    private Map<String, IssueFields> getIssues() {
        final IssueQuery issueQuery = new IssueQuery(0, 20);
        final JiraVersionTracker jiraVersionTracker = new JiraVersionTracker(projectId);

        final String versions = jiraVersionTracker.getAllRc(jiraVersion)
                                                  .stream()
                                                  .map(JiraVersionModel::getName)
                                                  .map(v -> "'" + v + "'")
                                                  .collect(Collectors.joining(","));

        issueQuery.setFields(
                IssueFields.Field.ISSUE_TYPE.getFieldName(),
                IssueFields.Field.STATUS.getFieldName(),
                IssueFields.Field.FIX_VERSIONS.getFieldName(),
                IssueFields.Field.PRIORITY.getFieldName()
        )
                  .setJql(
                          JQL.replace("{PROJECT}", projectId.name()).replace("{VERSIONS}", versions)
                  );
        final JiraSearch jiraSearch = new JiraSearch(issueQuery);
        jiraSearch.search();
        return jiraSearch.getIssueList();
    }

    public List<String> formatIssues() {

        final List<String> formattedData = new ArrayList<>();
        formattedData.add(String.format("*Проект: %s; Версия: %s*\n", projectId.name(), jiraVersion.getName()));

        // Story в IFT
        appendStoryInIft();
        formattedData.add(stringBuilder.toString());

        // Блокеры и криты
        stringBuilder.delete(0, stringBuilder.length());
        stringBuilder.append("\n*Количество блокеров / критов:*\n");
        appendOpenBlockerCritical();
        appendIftBlockerCritical();
        appendWaitRcBlockerCritical();
        appendWrongBlockerCritical();
        formattedData.add(stringBuilder.toString());
        stringBuilder.delete(0, stringBuilder.length());

        // High инциденты и дефекты
        stringBuilder.append("\n*Количество high инцидентов / дефектов:*\n");
        appendOpenHigh();
        appendIftHigh();
        appendWaitRcHigh();
        appendWrongFixVersionHigh();
        formattedData.add(stringBuilder.toString());

        // Низкоприоритетные дефекты
        stringBuilder.delete(0, stringBuilder.length());
        stringBuilder.append("\n*Дефекты с низким приоритетом:*\n");
        appendLowPriority();
        appendWaitRcLow();
        formattedData.add(stringBuilder.toString());

        return formattedData;
    }

    private void appendOpenBlockerCritical() {
        final int openBlocker = issueFilter.clear()
                                           .inOpenStatus()
                                           .incidentsAndBugs()
                                           .blocker()
                                           .allRc()
                                           .getFilteredDataSize();
        final String openBlockerLink = issueFilter.getJqlLink();

        final int openCritical = issueFilter.clear()
                                            .inOpenStatus()
                                            .incidentsAndBugs()
                                            .critical()
                                            .allRc()
                                            .getFilteredDataSize();
        final String openCriticalLink = issueFilter.getJqlLink();
        if (openBlocker == 0 && openCritical == 0) {
            stringBuilder.append("Открытых нет\n");
            return;
        }
        stringBuilder.append("Открытых: ")
                     .append(openBlockerLink)
                     .append(" / ")
                     .append(openCriticalLink)
                     .append("\n");
    }

    private void appendIftBlockerCritical() {
        final int iftBlocker = issueFilter.clear()
                                          .inIftStatus()
                                          .incidentsAndBugs()
                                          .blocker()
                                          .beforeWithCurrentRc()
                                          .getFilteredDataSize();
        final String iftBlockerLink = issueFilter.getJqlLink();

        final int iftCritical = issueFilter.clear()
                                           .inIftStatus()
                                           .incidentsAndBugs()
                                           .critical()
                                           .beforeWithCurrentRc()
                                           .getFilteredDataSize();
        final String iftCriticalLink = issueFilter.getJqlLink();
        if (iftBlocker == 0 && iftCritical == 0) {
            stringBuilder.append("В тестировании нет\n");
            return;
        }
        stringBuilder.append("В тестировании: ")
                     .append(iftBlockerLink)
                     .append(" / ")
                     .append(iftCriticalLink)
                     .append("\n");
    }

    private void appendWaitRcBlockerCritical() {
        final int waitRcBlocker = issueFilter.clear()
                                             .inIftStatus()
                                             .incidentsAndBugs()
                                             .blocker()
                                             .afterRc()
                                             .getFilteredDataSize();
        final String waitRcBlockerLink = issueFilter.getJqlLink();

        final int waitRcCritical = issueFilter.clear()
                                              .inIftStatus()
                                              .incidentsAndBugs()
                                              .critical()
                                              .afterRc()
                                              .getFilteredDataSize();
        final String waitRcCriticalLink = issueFilter.getJqlLink();
        if (waitRcBlocker == 0 && waitRcCritical == 0) {
            stringBuilder.append("Ожидающих отведения ветки нет\n");
            return;
        }
        stringBuilder.append("Ожидают отведения ветки: ")
                     .append(waitRcBlockerLink)
                     .append(" / ")
                     .append(waitRcCriticalLink)
                     .append("\n");
    }

    private void appendWrongBlockerCritical() {
        final int wrongBlocker = issueFilter.clear()
                                            .inOpenStatus()
                                            .incidentsAndBugs()
                                            .blocker()
                                            .beforeWithCurrentRc()
                                            .getFilteredDataSize();
        final String wrongBlockerLink = issueFilter.getJqlLink();

        final int wrongCritical = issueFilter.clear()
                                             .inOpenStatus()
                                             .incidentsAndBugs()
                                             .critical()
                                             .beforeWithCurrentRc()
                                             .getFilteredDataSize();
        final String wrongCriticalLink = issueFilter.getJqlLink();
        if (wrongBlocker == 0 && wrongCritical == 0) {
            stringBuilder.append("С некорректной fix-version нет\n");
            return;
        }
        stringBuilder.append("С некорректной fix-version: ")
                     .append(wrongBlockerLink)
                     .append(" / ")
                     .append(wrongCriticalLink)
                     .append("\n");
    }

    private void appendOpenHigh() {
        final List<IssueFields> incidentOpened = issueFilter.clear()
                                                            .high()
                                                            .incidents()
                                                            .inOpenStatus()
                                                            .allRc()
                                                            .getFilteredData();
        final String incidentOpenedLink = issueFilter.getJqlLink();

        final List<IssueFields> bugOpened = issueFilter.clear()
                                                       .high()
                                                       .bugs()
                                                       .inOpenStatus()
                                                       .allRc()
                                                       .getFilteredData();
        final String bugOpenedLink = issueFilter.getJqlLink();

        if (bugOpened.isEmpty() && incidentOpened.isEmpty()) {
            stringBuilder.append("Открытых нет\n");
            return;
        }
        stringBuilder.append("Открытых: ");

        stringBuilder.append(incidentOpenedLink)
                     .append(" / ")
                     .append(bugOpenedLink)
                     .append("\n");
    }

    private void appendIftHigh() {
        final int incidentInIft = issueFilter.clear()
                                             .high()
                                             .incidents()
                                             .inIftStatus()
                                             .beforeWithCurrentRc()
                                             .getFilteredDataSize();
        final String incidentInIftLink = issueFilter.getJqlLink();

        final int bugInIft = issueFilter.clear()
                                        .high()
                                        .bugs()
                                        .inIftStatus()
                                        .beforeWithCurrentRc()
                                        .getFilteredDataSize();
        final String bugInIftLink = issueFilter.getJqlLink();

        if (incidentInIft == 0 && bugInIft == 0) {
            stringBuilder.append("В тестировании нет\n");
            return;
        }

        stringBuilder.append("В тестировании: ")
                     .append(incidentInIftLink)
                     .append(" / ")
                     .append(bugInIftLink)
                     .append("\n");
    }

    private void appendWaitRcHigh() {
        final List<IssueFields> incidentWaitRc = issueFilter.clear()
                                                            .high()
                                                            .incidents()
                                                            .inIftStatus()
                                                            .afterRc()
                                                            .getFilteredData();
        final String incidentWaitRcLink = issueFilter.getJqlLink();

        final List<IssueFields> bugWaitRc = issueFilter
                .clear()
                .high()
                .bugs()
                .inIftStatus()
                .afterRc()
                .getFilteredData();
        final String bugWaitRcLink = issueFilter.getJqlLink();

        if (incidentWaitRc.isEmpty() && bugWaitRc.isEmpty()) {
            stringBuilder.append("Ожидающих отведения ветки нет\n");
            return;
        }
        stringBuilder.append("Ожидают отведения ветки: ")
                     .append(incidentWaitRcLink)
                     .append(" / ")
                     .append(bugWaitRcLink)
                     .append("\n");
    }

    private void appendWrongFixVersionHigh() {
        final List<IssueFields> wrongIncident = issueFilter.clear()
                                                           .high()
                                                           .incidents()
                                                           .inOpenStatus()
                                                           .beforeWithCurrentRc()
                                                           .getFilteredData();
        final String wrongIncidentLink = issueFilter.getJqlLink();


        final List<IssueFields> wrongBug = issueFilter.clear()
                                                      .high()
                                                      .bugs()
                                                      .inOpenStatus()
                                                      .beforeWithCurrentRc()
                                                      .getFilteredData();
        final String wrongBugLink = issueFilter.getJqlLink();

        if (wrongIncident.isEmpty() && wrongBug.isEmpty()) {
            stringBuilder.append("С некорректной fix version нет\n");
        } else {
            stringBuilder.append("С некорректной fix version: ")
                         .append(wrongIncidentLink)
                         .append(" / ")
                         .append(wrongBugLink)
                         .append("\n");
        }
    }

    private void appendStoryInIft() {
        final int storyInIft = issueFilter.clear()
                                          .story()
                                          .inIftStatus()
                                          .allRc()
                                          .getFilteredDataSize();
        final String storyInIftLink = issueFilter.getJqlLink();

        if (storyInIft == 0) {
            stringBuilder.append("Story в IFT нет\n");
        } else {
            stringBuilder.append("Story в IFT: ").append(storyInIftLink);
        }
    }

    private void appendLowPriority() {
        final int inIft = issueFilter.clear()
                                     .belowHighPriority()
                                     .incidentsAndBugs()
                                     .inIftStatus()
                                     .beforeWithCurrentRc()
                                     .getFilteredDataSize();
        final String inIftLink = issueFilter.getJqlLink();

        if (inIft == 0) {
            stringBuilder.append("В тестировании нет\n");
        } else {
            stringBuilder.append("В тестировании: ")
                         .append(inIftLink)
                         .append("\n");
        }
    }

    private void appendWaitRcLow() {
        final int waitLow = issueFilter.clear()
                                       .belowHighPriority()
                                       .incidentsAndBugs()
                                       .inIftStatus()
                                       .afterRc()
                                       .getFilteredDataSize();
        final String waitLowLink = issueFilter.getJqlLink();

        if (waitLow == 0) {
            stringBuilder.append("Ожидающих отведения ветки нет\n");
        } else {
            stringBuilder.append("Ожидающих отведения ветки: ")
                         .append(waitLowLink)
                         .append("\n");
        }
    }
}
