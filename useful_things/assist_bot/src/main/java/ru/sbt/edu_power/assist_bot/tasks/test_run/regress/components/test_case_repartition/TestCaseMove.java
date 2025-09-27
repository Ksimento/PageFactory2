package ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.test_case_repartition;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.test_run.TestRunSlice;
import ru.sbt.edu_power.external_services.jira.JiraConnect;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraUser;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.Execution;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Класс выполняет перенос тест-кейсов в тест-сете от одного ответственного к другому (поле AssignedBy)
 */
@Slf4j
public class TestCaseMove {
    // последний срез тест-сета
    private final TestRunSlice testRunSlice;
    // пользователь, от кого тест-кейсы забираются
    private final JiraUser userFrom;
    // пользователь, кому тест-кейсы передаются
    private final JiraUser userTo;
    // набор экзекушенов, которые нужно передать
    private final List<Execution> testCaseList;
    private String movedTestCases;

    public TestCaseMove(
            final TestRunSlice testRunSlice,
            final String userFrom,
            final String userTo,
            final List<Execution> testCaseList
    ) {
        this.testRunSlice = testRunSlice;
        this.userFrom = JiraConnect.jiraGetUser(userFrom);
        this.userTo = JiraConnect.jiraGetUser(userTo);
        this.testCaseList = testCaseList;
    }

    public JiraUser getUserTo() {
        return userTo;
    }

    public JiraUser getUserFrom() {
        return userFrom;
    }

    public List<Execution> getExecutions() {
        return testCaseList;
    }

    public String getMovedTestCases() {
        return movedTestCases;
    }

    public void execute() {
        testCaseList.forEach(this::updateExecution);
        movedTestCases = testCaseList.stream().map(Execution::getTestCaseKey).collect(Collectors.joining("; "));
        log.info(
                "Тест кейсы перемещены на пользователя {}: {}",
                userTo.getDisplayName(),
                movedTestCases
        );
    }

    private void updateExecution(final Execution execution) {

        final Execution assignedField = new Execution();
        assignedField.setAssignedTo(userTo.getName().toLowerCase());
        final HttpResponse<JsonNode> response = JiraConnect.reportTestCaseToTestSet(
                execution.getTestCaseKey(),
                assignedField.toString(),
                testRunSlice.getTestRunKey()
        );
        if (!response.isSuccess()) {
            log.error(
                    "Ошибка при перемещении тест-кейса {} на пользователя {}\n{}",
                    execution.getTestCaseKey(),
                    userTo.getName(),
                    response
            );
        }
    }

    @Data
    private static class AssignedField {
        private final String assignedTo;
    }
}
