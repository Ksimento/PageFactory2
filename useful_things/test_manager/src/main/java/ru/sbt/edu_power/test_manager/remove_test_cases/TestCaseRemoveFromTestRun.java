package ru.sbt.edu_power.test_manager.remove_test_cases;

import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.jira.JiraConnectionException;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.BulkTestRunUpdate;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.TestRunSearch;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.Execution;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.ExecutionStatus;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.TestRunModel;
import ru.sbt.edu_power.test_manager.TestManagerException;
import ru.sbt.edu_power.test_manager.test_run_data.slice.TestRunSlice;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class TestCaseRemoveFromTestRun {
    private final TestRunModel testRunModel;
    private final int PAGE_SIZE;

    public TestCaseRemoveFromTestRun(final String testRunKey, final int pageSize) {
        PAGE_SIZE = pageSize;
        final TestRunSearch testRunSearch = new TestRunSearch(testRunKey);
        testRunModel = testRunSearch
                .getTestRunByKey()
                .orElseThrow(() -> new TestManagerException("Не найден тест-сет " + testRunKey));
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

        if (PAGE_SIZE < 3) {
            throw new TestManagerException(String.format("Не удалось выполнить удаление тест-кейсов: %s",
                    executionList.stream().map(Execution::getTestCaseKey).collect(Collectors.joining(", "))
                    )
            );
        }
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
            log.error(
                    "Ошибка при удалении тест-кейсов из тест-сета {}\n{}\n{}",
                    testRunModel.getKey(),
                    executionsPage.stream()
                                  .map(Execution::getTestCaseKey)
                                  .collect(Collectors.joining(", ")),
                    e
            );
        }
    }
}
