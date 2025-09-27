package ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.user_data;

import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.AnalyticUtils;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.test_run.TestRunSlice;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraUser;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.Execution;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class WorkTimeRepository {
    private final Map<JiraUser, WorkTime> userWorkTimeMap = new HashMap<>();

    public Map<JiraUser, WorkTime> getUserWorkTimeMap() {
        return userWorkTimeMap;
    }

    public void update(final TestRunSlice testRunSlice) {
        testRunSlice.getUserToExecutionStatusToExecutionsMap().forEach((user, map) -> {
            final List<Execution> executionList = map.keySet().stream()
                                                     .filter(AnalyticUtils::isHandleTestCase)
                                                     .map(map::get)
                                                     .flatMap(List::stream)
                                                     .collect(Collectors.toList());
            update(user, executionList);
        });
    }

    // Обновляем время работы сотрудника
    private void update(final JiraUser user, final List<Execution> executionList) {
        // добавляем сотрудника если его нет
        if (!userWorkTimeMap.containsKey(user)) {
            final Date startRegression = getMin(executionList);
            if (startRegression == null) {
                // Если ручные кейсы не начинались - выход без добавления сотрудника
                return;
            }
            // считаем начало ручного регресса со времени первого запуска ручного кейса
            userWorkTimeMap.put(
                    user,
                    new WorkTime(LocalDateTime.ofInstant(startRegression.toInstant(), ZoneId.of("UTC")))
            );
        }
        // последнее зарегистированное рабочее время, от которого искать ручные кейсы
        final Date last = userWorkTimeMap.get(user).getLastDate();
        // если рабочее время ещё не зарегистировано, то за время начала берём первый ручной кейс, иначе
        // ищем первый кейс, который был запущен после последнего рабочего времени - это будет начало нового периода работы
        final Date start = last == null ? getMin(executionList) : getMin(executionList, last);
        // берём самое последнее время взаимодействия с кейсом (старт или завершение) и считаем его временем окончания
        // нового периода работы
        final Date end = getLastExecutionTime(executionList);
        // в каждом отрезке времени может получиться так, что новых кейсов не начинали, но старые закончили, или наоборот
        // поэтому обрабатываем каждый из возможных вариантов
        if (start == null) {
            if (end != null) {
                userWorkTimeMap.get(user).join(end, end);
            }
        } else {
            userWorkTimeMap.get(user).join(start, end == null ? start : end);
        }
    }

    // получаем минимальное время старта кейса
    private Date getMin(final List<Execution> executionList) {
        return executionList.stream()
                            .map(Execution::getActualStartDate)
                            .filter(Objects::nonNull)
                            .min(Date::compareTo)
                            .orElse(null);
    }

    // получаем максимальное время между максимальным временем старта и максимальным временем завершения
    // то-есть это самое крайнее из всего доступного времени взаимодействия с кейсами
    private Date getLastExecutionTime(final List<Execution> executionList) {
        final Date maxEnd = getMaxEnd(executionList);
        final Date maxStart = getMaxStart(executionList);
        if (maxEnd == null) {
            return maxStart;
        }
        if (maxStart == null) {
            return maxEnd;
        }
        return maxStart.after(maxEnd) ? maxStart : maxEnd;
    }

    // получаем максимальное время завершения
    private Date getMaxEnd(final List<Execution> executionList) {
        return executionList.stream()
                            .map(Execution::getActualEndDate)
                            .filter(Objects::nonNull)
                            .max(Date::compareTo)
                            .orElse(null);
    }

    // получаем максимальное время старта
    private Date getMaxStart(final List<Execution> executionList) {
        return executionList.stream()
                            .map(Execution::getActualStartDate)
                            .filter(Objects::nonNull)
                            .max(Date::compareTo)
                            .orElse(null);
    }

    // получаем минимальное время старта начиная с определённого времени
    private Date getMin(final List<Execution> executionList, final Date afterDate) {
        return executionList.stream()
                            .map(Execution::getActualStartDate)
                            .filter(Objects::nonNull)
                            .filter(afterDate::after)
                            .min(Date::compareTo)
                            .orElse(null);
    }
}
