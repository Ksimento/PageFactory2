package ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.user_data;

import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.AnalyticUtils;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.test_run.TestRunSlice;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.test_run.TestRunStorage;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraUser;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.Execution;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.RegressTestRunCreation;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

// Класс реализует рассчёт времени прохождения регресса вручную.
// Скорость - максимальное время, которое допустимо затратить на прохождение одного кейса
public class TestRunProgressSpeed {
    // Максимальное время выделенное на регресс
    private static final int MAX_REGRESS_TIME = 8 * 60 * 60;
    // Максимальное время прохождения одного тест-кейса
    private static final long MAX_TEST_TIME = 20 * 60;
    private final WorkTime workTime;
    private final TestRunStorage testRunStorage;
    private final JiraUser user;
    private long effectiveSpeed;
    private long currentSpeed;
    private final RegressTestRunCreation regressTestRunCreation;

    public TestRunProgressSpeed(
            final WorkTime workTime,
            final TestRunStorage testRunStorage,
            final JiraUser user,
            final RegressTestRunCreation regressTestRunCreation
    ) {
        this.workTime = workTime;
        this.testRunStorage = testRunStorage;
        this.user = user;
        calcEffectiveSpeed();
        this.regressTestRunCreation = regressTestRunCreation;
    }

    // обновление текущей скорости прохождения по обновлённым данным из testRunAnalytic и workTime
    public void updateCurrentSpeed() {
        final TestRunSlice firstState = testRunStorage.get(0);
        final TestRunSlice lastState = testRunStorage.getLastSlice();
        final int initialExecutionCount = getExecutionsInProgress(
                firstState.getUserToExecutionStatusToExecutionsMap().get(user));
        final int currentExecutionCount = getExecutionsInProgress(
                lastState.getUserToExecutionStatusToExecutionsMap().get(user));
        final int closedExecution = initialExecutionCount - currentExecutionCount;
        currentSpeed = closedExecution == 0 ? 0 : workTime.getWorkTime().getSeconds() / closedExecution;
    }

    // метод возвращает true если тестировщик укладывается в ожидаемое время прохождения регресса
    public boolean isCurrentSpeedEffective() {
        return currentSpeed <= effectiveSpeed;
    }

    // метод возвращает остаточную ожидаемую длительность прохождения регресса на основании текущей скорости тестировщика
    public Duration getEndTestRunPrognosis() {
        final int currentExecutionsInProgress = getExecutionsInProgress(
                testRunStorage.getLastSlice().getUserToExecutionStatusToExecutionsMap().get(user));
        return Duration.ofSeconds(currentExecutionsInProgress * currentSpeed);
    }

    // метод рассчитывает необходимую скорость прохождения, достаточную для завершения регресса вовремя
    private void calcEffectiveSpeed() {
        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.S'Z'");
        dateTimeFormatter.withZone(ZoneId.of("Europe/Moscow"));
        final String startRegress = regressTestRunCreation
                .getBlankTestRunCreation()
                .getTestRunModel()
                .getPlannedStartDate();
        final LocalDateTime testRunStarted = LocalDateTime.from(dateTimeFormatter.parse(startRegress));
        final LocalDateTime handleRegressStarted = workTime.getStartRegression();
        final Duration minusRegressionTime = Duration.between(testRunStarted, handleRegressStarted);
        final Duration handleRegressMax = Duration.ofSeconds(MAX_REGRESS_TIME).minus(minusRegressionTime);
        final long speed = handleRegressMax.getSeconds() /
                          getExecutionsInProgress(testRunStorage
                                  .get(0)
                                  .getUserToExecutionStatusToExecutionsMap()
                                  .get(user));
        effectiveSpeed = Math.min(MAX_TEST_TIME, speed);
    }

    // метод получает общее количество тестов, прохождение которых не завершено
    private int getExecutionsInProgress(final Map<String, List<Execution>> map) {
        return map.keySet()
                  .stream()
                  .filter(AnalyticUtils::isInNotCompleteStatus)
                  .map(map::get)
                  .mapToInt(List::size)
                  .reduce(Integer::sum)
                  .orElse(0);
    }
}
