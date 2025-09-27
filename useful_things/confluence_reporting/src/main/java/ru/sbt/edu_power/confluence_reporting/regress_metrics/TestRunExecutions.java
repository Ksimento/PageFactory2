package ru.sbt.edu_power.confluence_reporting.regress_metrics;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.jira.JiraConnect;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.Execution;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.TestResultLatestModel;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.TestRunModel;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static ru.sbt.edu_power.external_services.jira.JiraConnect.GSON;

@Slf4j
public class TestRunExecutions {
    private static List<Execution> executionList = new ArrayList<>();

    public static List<Execution> load(String testSetId) {
        if (executionList.isEmpty()) {
            HttpResponse<JsonNode> response = JiraConnect.getTestSetResult(testSetId);
            final TestRunModel testRunModel =
                    GSON.fromJson(response.getBody().getObject().toString(), TestRunModel.class);
            final Date current = new Date();
            for (final Execution element : testRunModel.getItems()) {
                final Date endDate = element.getActualEndDate();
                if (Objects.nonNull(endDate)) {
                    element.setExecutionTime(
                            (int) (element.getActualEndDate().getTime() - element.getActualStartDate().getTime())
                    );
                }

                if (element.getStatus().equals("Fail") || element.getStatus().equals("Blocked")) {
                    HttpResponse<JsonNode> responseLatest = JiraConnect.getTestResultLatest(element.getTestCaseKey());
                    TestResultLatestModel testResultLatestModel =
                            GSON.fromJson(responseLatest.getBody().getObject().toString(), TestResultLatestModel.class);
                    if (Objects.nonNull(testResultLatestModel.getIssueLinks()) &&
                            Objects.equals(testResultLatestModel.getId(), element.getId())) {
                        element.setIssueLinks(testResultLatestModel.getIssueLinks());
                    }
                }
                // если дата завершения тест-кейса превышает текущую, то заменяем её на текущую
                if (Objects.nonNull(endDate) && endDate.after(current)) {
                    log.warn(
                            "Дата завершения тест-кейса {} превышает текущую: {}",
                            element.getTestCaseKey(),
                            new SimpleDateFormat("d-M-yyyy H:m:s").format(endDate)
                    );
                    element.setActualEndDate(current);
                }
                executionList.add(element);
            }
            return executionList;
        }
        return executionList;
    }
}
