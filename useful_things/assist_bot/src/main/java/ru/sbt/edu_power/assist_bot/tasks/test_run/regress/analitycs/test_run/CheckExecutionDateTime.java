package ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.test_run;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.jira.JiraConnect;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.Execution;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.TestRunModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class CheckExecutionDateTime {
    private final TestRunModel testRunModel;
    private final TestRunSlice testRunSlice;
    private final Date currentTime = new Date();
    private final Date regressStartTime;
    private final Map<Execution, TimeType> executionToWrongTimeType = new HashMap<>();

    @SneakyThrows
    public CheckExecutionDateTime(
            final TestRunModel testRunModel,
            final TestRunSlice testRunSlice
    ) {
        this.testRunModel = testRunModel;
        this.testRunSlice = testRunSlice;
        final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        regressStartTime = dateTimeFormatter.parse(testRunModel.getPlannedStartDate());
    }

    public void check() {
        log.info("Выполняю проверку корректности дат");
        testRunSlice.forEach(e -> {
            final TimeType timeType = getTimeType(e);
            if (timeType != TimeType.NORMAL) {
                executionToWrongTimeType.put(e, timeType);
            }
        });
        update();
    }

    private void update() {
        executionToWrongTimeType.forEach(this::update);
    }

    private void update(final Execution actual, final TimeType timeType) {
        final Execution update = new Execution();
        switch (timeType) {
            case WRONG_START:
                log.info("{} Неверное время старта теста: {}", actual.getTestCaseKey(), actual.getActualStartDate());
                update.setActualStartDate(currentTime);
                if (Objects.nonNull(actual.getActualEndDate())) {
                    update.setExecutionTime(0);
                }
                break;
            case WRONG_END:
                log.info("{} Неверное время завершения теста: {}", actual.getTestCaseKey(), actual.getActualEndDate());
                update.setActualEndDate(currentTime);
                update.setExecutionTime((int) (currentTime.getTime() - actual.getActualStartDate().getTime()));
                break;
            case WRONG_START_END:
                log.info("{} Неверное время старта и завершения теста: {} - {}", actual.getTestCaseKey(), actual.getActualStartDate(), actual.getActualEndDate());
                update.setActualStartDate(currentTime);
                update.setActualEndDate(currentTime);
                update.setExecutionTime(0);
                break;
            default:
        }
        final HttpResponse<JsonNode> response = JiraConnect.reportTestCaseToTestSet(
                actual.getTestCaseKey(),
                update.toString(),
                testRunModel.getKey()
        );
        JiraConnect.checkResponse(response, "reportTestCaseToTestSet", update);
    }

    private TimeType getTimeType(final Execution execution) {
        final boolean isStartWrong = isWrongDate(execution.getActualStartDate());
        final boolean isEndWrong = isWrongDate(execution.getActualEndDate());
        if (isStartWrong) {
            if (isEndWrong || Objects.nonNull(execution.getActualEndDate())) {
                return TimeType.WRONG_START_END;
            } else {
                return TimeType.WRONG_START;
            }
        }
        if (isEndWrong) {
            return TimeType.WRONG_END;
        }
        if (
                Objects.nonNull(execution.getActualStartDate()) &&
                Objects.nonNull(execution.getActualEndDate()) &&
                execution.getActualEndDate().before(execution.getActualStartDate())
        ) {
            return TimeType.WRONG_START_END;
        }
        return TimeType.NORMAL;
    }

    private boolean isWrongDate(final Date date) {
        return Objects.nonNull(date) &&
               (date.before(regressStartTime) || date.after(currentTime));
    }

    private enum TimeType {
        WRONG_START,
        WRONG_END,
        WRONG_START_END,
        NORMAL
    }
}
