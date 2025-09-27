package ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.test_run;

import ru.sbt.edu_power.assist_bot.AssistBotException;
import ru.sbt.edu_power.assist_bot.roles.roles.UserRolesRepository;
import ru.sbt.edu_power.assist_bot.slack.SlackClient;
import ru.sbt.edu_power.external_services.jira.JiraConnectionException;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.BulkTestRunUpdate;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.TestRunSearch;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.Execution;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.ExecutionStatus;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.TestRunModel;

import java.util.List;
import java.util.stream.Collectors;

public class TestCaseRemoveFromTestRun {
    private final TestRunModel testRunModel;
    private final int PAGE_SIZE = 20;

    public TestCaseRemoveFromTestRun(final TestRunModel testRunModel) {
        this.testRunModel = testRunModel;
    }

    public TestCaseRemoveFromTestRun(final String testRunKey) {
        final TestRunSearch testRunSearch = new TestRunSearch(testRunKey);
        testRunModel = testRunSearch
                .getTestRunByKey()
                .orElseThrow(() -> new AssistBotException("Не найден тест-сет " + testRunKey));
    }

    public void execute() {
        final TestRunSlice testRunSlice = new TestRunSlice(testRunModel.getKey());
        testRunSlice.load();
        execute(testRunSlice);
    }

    public void execute(final TestRunSlice testRunSlice) {
        final List<Execution> executionList = testRunSlice
                .stream()
                .filter(e -> ExecutionStatus.NOT_EXECUTED
                        .getStatusName()
                        .equals(e.getStatus()))
                .collect(Collectors.toList());

        for (int i = 0; i <= executionList.size() / PAGE_SIZE; i++) {
            executePageable(i, executionList);
        }
    }

    private void executePageable(final int page, final List<Execution> executions) {
        final int lastElement = Math.min(executions.size(), (page + 1) * PAGE_SIZE);
        final List<Execution> executionsPage = executions.subList(page * PAGE_SIZE, lastElement);
        final BulkTestRunUpdate bulkTestRunUpdate = new BulkTestRunUpdate(testRunModel);
        try {
            bulkTestRunUpdate.removeItems(executionsPage).execute();
        } catch (final JiraConnectionException e) {
            SlackClient.sendText(
                    String.format(
                            "Ошибка при удалении тест-кейсов из тест-сета %s\n%s\n%s",
                            testRunModel.getKey(),
                            executionsPage.stream()
                                    .map(Execution::getTestCaseKey)
                                    .collect(Collectors.joining(", ")),
                            e
                    ),
                    UserRolesRepository.getAdminUser());
        }
    }
}
