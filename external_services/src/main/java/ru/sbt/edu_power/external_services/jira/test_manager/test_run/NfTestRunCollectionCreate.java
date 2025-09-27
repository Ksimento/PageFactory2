package ru.sbt.edu_power.external_services.jira.test_manager.test_run;


import ru.sbt.edu_power.external_services.jira.agile.JiraSearch;
import ru.sbt.edu_power.external_services.jira.agile.model.IssueFields;
import ru.sbt.edu_power.external_services.jira.agile.model.IssueQuery;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraVersionModel;
import ru.sbt.edu_power.external_services.jira.agile.model.enums.IssueStatus;
import ru.sbt.edu_power.external_services.jira.agile.model.enums.IssueType;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.TestRunModel;

import java.util.HashMap;
import java.util.Map;

public class NfTestRunCollectionCreate {
    private static final String RELEASE = "Релиз ";
    private static final String TEMP_REGRESS_FOLDER = "Автоматизация";
    private static final String BASE_FOLDER = "ИФТ (STAGE)";
    private final Map<IssueFields, TestRunModel> testRunCollection = new HashMap<>();
    private final Map<String, IssueFields> storiesInIft = new HashMap<>();
    private final TCFields.ProjectId project;
    private final JiraVersionModel version;

    public NfTestRunCollectionCreate(
            final TCFields.ProjectId project,
            final JiraVersionModel version
    ) {
        this.project = project;
        this.version = version;
    }

    public void create() {
        collectIssue();
        storiesInIft.forEach((k, v) -> {
            final BlankTestRunCreation blankTestRunCreation = new BlankTestRunCreation(project, version, TEMP_REGRESS_FOLDER, BASE_FOLDER, getTestSetDirectory());
            final TestRunModel testRunSaved = blankTestRunCreation.create(testRunNameGenerator(k, v));
            testRunCollection.put(v, testRunSaved);
        });
    }

    public Map<IssueFields, TestRunModel> getTestRunCollection() {
        return testRunCollection;
    }

    private void collectIssue() {
        final IssueQuery issueQuery = new IssueQuery(0, 100);
        issueQuery.and(IssueFields.Field.ISSUE_TYPE, IssueQuery.Op.EQUAL, IssueType.STORY.getValue())
                  .and("Без изменения исходного кода", IssueQuery.Op.IS, "")
                  .and(IssueFields.Field.STATUS, IssueQuery.Op.EQUAL, IssueStatus.IFT.getValue())
                  .and(IssueFields.Field.PROJECT, IssueQuery.Op.EQUAL, project.name())
                  .and(IssueFields.Field.FIX_VERSIONS, IssueQuery.Op.IN, version.getName());
        final JiraSearch jiraSearch = new JiraSearch(issueQuery);
        jiraSearch.search();
        storiesInIft.putAll(jiraSearch.getIssueList());
    }

    // генерируем название поддиректории для создания там регресса
    private String getTestSetDirectory() {
        return RELEASE + version.getName().replace("r/", "").split("-")[0];
    }

    private String testRunNameGenerator(final String issueKey, final IssueFields issue) {
        return String.format(
                "[%s] [%s]: %s",
                issueKey,
                issue.get(IssueFields.Field.TEAM),
                issue.get(IssueFields.Field.SUMMARY)
        );
    }
}
