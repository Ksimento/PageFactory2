package ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.test_run;

import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.AnalyticUtils;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraUser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class TestAllocationCalc {
    private TestRunSlice testRunSlice;
    private final double overloadK = 2.0;
    private final double underloadK = 0.5;
    private final Map<JiraUser, Integer> userToTestSetSize = new HashMap<>();
    // Среднее количество тестов на одного тестировщика
    private double averageTestByMember;
    private final int negligible = 10;

    public void load(final TestRunSlice testRunSlice) {
        this.testRunSlice = testRunSlice;
        collect();
        calcAverage();
    }

    public String mapToString(final Map<JiraUser, Integer> map) {
        return map.entrySet()
                  .stream()
                  .map(e -> e.getKey().getDisplayName() + ": " + e.getValue())
                  .sorted()
                  .collect(Collectors.joining("\n"));
    }

    public Map<JiraUser, Integer> getOverloadedUsers() {
        final int MIN_TEST_CASE_FOR_OVERLOAD_STATUS = 20;
        return userToTestSetSize.entrySet()
                                .stream()
                                .filter(e -> e.getValue() > MIN_TEST_CASE_FOR_OVERLOAD_STATUS)
                                .filter(e -> e.getValue() >= averageTestByMember * overloadK)
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Map<JiraUser, Integer> getUnderloadedUsers() {
        return userToTestSetSize.entrySet()
                                .stream()
                                .filter(e -> e.getValue() <= averageTestByMember * underloadK)
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public boolean isAllocationUneven() {
        return userToTestSetSize.values()
                                .stream()
                                .anyMatch(i -> i <= averageTestByMember * underloadK ||
                                               i >= averageTestByMember * overloadK);
    }

    // собираем информацию по текущему количеству тестов, которое необходимо пройти руками по каждому тестировщику
    private void collect() {
        testRunSlice.getUserToExecutionStatusToExecutionsMap().forEach((user, map) -> {
            final int total = map.entrySet()
                                 .stream()
                                 .filter(e -> AnalyticUtils.isInNotCompleteStatus(e.getKey()))
                                 .map(Map.Entry::getValue)
                                 .mapToInt(List::size)
                                 .sum();
            userToTestSetSize.put(user, total);
        });
    }

    private void calcAverage() {
        // всего тестов, которые необходимо пройти руками
        final int total = userToTestSetSize.values()
                                           .stream()
                                           .reduce(Integer::sum)
                                           .orElse(0);
        // исключаем из общего числа тестировщиков погрешность (если у него количество тестов меньше минимального)
        final long users = userToTestSetSize
                .values()
                .stream()
                .filter(d -> d >= negligible)
                .count();
        averageTestByMember = users == 0 ? 0 : (double) total / users;
    }
}
