package ru.sbt.edu_power.external_services.jira.test_manager.test_run;

import com.google.gson.Gson;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import org.apache.http.HttpStatus;
import ru.sbt.edu_power.external_services.PropReader;
import ru.sbt.edu_power.external_services.jira.JiraConnect;
import ru.sbt.edu_power.external_services.jira.JiraExporterUtils;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraVersionModel;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.TestRunModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class TestRunSearch {
    private JiraVersionModel jiraVersion;
    private TCFields.ProjectId project;
    private String testRunKey;
    private static final String JIRA_LOGIN = JiraExporterUtils.decodeData(PropReader.get("jira.login"));
    private static final String JIRA_PASS = JiraExporterUtils.decodeData(PropReader.get("jira.pass"));
    private static final String SEARCH_STRING = "https://jira.pcbltools.ru/jira/rest/tests/1.0/testrun/search?" +
                                                "fields=id,key,name,folderId,projectVersionId,testCaseCount&" +
                                                "maxResults=40&query=testRun.projectKey+IN+(%27{PROJECT_NAME}%27)+" +
                                                "AND+testRun.versionName+IN+(%27{JIRA_VERSION}%27)&startAt=0";
    private final Gson gson = new Gson();

    public TestRunSearch(
            final JiraVersionModel jiraVersion,
            final TCFields.ProjectId project
    ) {
        this.jiraVersion = jiraVersion;
        this.project = project;
    }

    public TestRunSearch(final String testRunKey) {
        this.testRunKey = testRunKey;
    }

    public List<TestRunModel> getTestRunList() {
        Objects.requireNonNull(jiraVersion);
        Objects.requireNonNull(project);
        final String url = SEARCH_STRING
                .replace("{PROJECT_NAME}", project.name())
                .replace("{JIRA_VERSION}", jiraVersion.getName());
        HttpResponse<JsonNode> response = Unirest
                .get(url)
                .basicAuth(JIRA_LOGIN, JIRA_PASS)
                .asJson();
        JiraConnect.jiraSessionLogout(response);
        final List<TestRunModel> testRunList = new ArrayList<>();
        if (response.getStatus() == HttpStatus.SC_NOT_FOUND) {
            return testRunList;
        }
        JiraConnect.checkResponse(response, url);
        if (response.getBody().getObject().getInt("total") == 0) {
            return testRunList;
        }
        for (final Object o : response.getBody().getObject().getJSONArray("results")) {
            final TestRunModel model = gson.fromJson(o.toString(), TestRunModel.class);
            testRunList.add(model);
        }
        Unirest.shutDown(true);
        return testRunList;
    }

    public Optional<TestRunModel> getTestRunByKey() {
        Objects.requireNonNull(testRunKey);
        final HttpResponse<JsonNode> response = JiraConnect.getTestSet(
                testRunKey,
                "name,key,id,projectId,version,projectVersionId,plannedStartDate,plannedEndDate,owner,testCaseCount,statusId"
        );
        if (response.getStatus() == HttpStatus.SC_NOT_FOUND) {
            return Optional.empty();
        }
        final TestRunModel testRunModel = gson.fromJson(response.getBody().getObject().toString(), TestRunModel.class);
        return Optional.of(testRunModel);
    }
}
