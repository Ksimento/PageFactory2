package ru.sbt.edu_power.confluence_reporting.regress_metrics;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Evaluator;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.Execution;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.ExecutionStatus;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
public class MetricsMethods {
    // Считаем количество тест-кейсов
    public static Map<String, Long> totalCasesCalc(final Map<String, List<Execution>> executionMap) {
        final Map<String, Long> totalMap = new HashMap<>();
        executionMap.forEach((member, list) -> totalMap.put(member, (long) list.size()));
        return totalMap;
    }

    // Вычисляем время старта
    public static Map<String, Long> startDateCalc(final Map<String, List<Execution>> executionMap) {
        final Map<String, Long> timeMap = new HashMap<>();
        executionMap.forEach((member, list) -> {
            long startDate = 0;
            for (final Execution e : list) {
                final ExecutionStatus status = ExecutionStatus.getStatusByName(e.getStatus());
                if (isHandleStatus(status) || status == ExecutionStatus.IN_PROGRESS) {
                    if (e.getActualStartDate() != null) {
                        if (startDate == 0 || e.getActualStartDate().getTime() < startDate) {
                            startDate = e.getActualStartDate().getTime();
                        }
                    }
                }
            }
            timeMap.put(member, startDate);
        });
        return timeMap;
    }

    // Вычисляем время окончания
    public static Map<String, Long> endDateCalc(final Map<String, List<Execution>> executionMap) {
        final Map<String, Long> timeMap = new HashMap<>();
        executionMap.forEach((team, list) -> {
            long endDate = 0;
            for (final Execution e : list) {
                final ExecutionStatus status = ExecutionStatus.getStatusByName(e.getStatus());
                if (isHandleStatus(status)) {
                    if (e.getActualEndDate() != null) {
                        if (e.getActualEndDate().getTime() > System.currentTimeMillis()) {
                            log.info("В тест-кейсе {} дата завершения превышает текущую дату", e.getTestCaseKey());
                        }
                        if (endDate == 0 || e.getActualEndDate().getTime() > endDate) {
                            endDate = e.getActualEndDate().getTime();
                        }
                    }
                }
            }
            timeMap.put(team, endDate);
        });
        return timeMap;
    }

    // Собираем количество проваленных тестов с привязанными дефектами
    public static Map<String, Long> brokenWithBugCalc(final Map<String, List<Execution>> executionMap) {
        final Map<String, Long> totalMap = new HashMap<>();
        executionMap.forEach((team, list) -> {
            final long total = list.stream()
                    .filter(e -> {
                        final ExecutionStatus status = ExecutionStatus.getStatusByName(e.getStatus());
                        final boolean isIssueLinksEmpty = Objects.isNull(e.getIssueLinks()) ||
                                                          e.getIssueLinks().isEmpty();
                        return (status == ExecutionStatus.BLOCKED || status == ExecutionStatus.FAIL) && !isIssueLinksEmpty;
                    })
                    .count();
            totalMap.put(team, total);
        });
        return totalMap;
    }

    // Собираем количество не запущенных тестов в разрезе статусов
    public static Map<String, Long> notExecutedCalcInContextStatuses(final Map<String, List<Execution>> executionMap, ExecutionStatus... status) {
        final Map<String, Long> totalMap = new HashMap<>();
        executionMap.forEach((team, list) -> {
            final long total = list.stream()
                    .map(Execution::getStatus)
                    .map(ExecutionStatus::getStatusByName)
                    .filter(s -> Arrays.stream(status).collect(Collectors.toList()).contains(s))
                    .count();
            totalMap.put(team, total);
        });
        return totalMap;
    }

    public static List<String> getNotExecutedTestCaseListByTeam(final String team, final Map<String, List<Execution>> executionMap) {
        return executionMap.get(team).stream()
                    .filter(e -> MetricsMethods.isNotStartedStatus(ExecutionStatus.getStatusByName(e.getStatus())))
                    .map(Execution::getTestCaseKey)
                    .collect(Collectors.toList());
    }

    public static String getExecutedBy(final Execution e) {
        if ("aft.integration".equals(e.getExecutedBy()) || "".equals(e.getExecutedBy()) || e.getExecutedBy() == null) {
            return e.getAssignedTo();
        }
        return e.getExecutedBy();
    }

    public static void updateTestSetId(final Document document, final String testSetId) {
        updateNamedField(document, "Тест-сет", testSetId);
        final SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy HH:mm");
        final String date = format.format(new Date());
        updateNamedField(document, "Дата сбора статистики", date);
    }

    public static void updateAutotestLinks(final Document document, final Map<String, String> links) {
        links.forEach((name, value) -> updateNamedField(document, name, value));
    }

    private static void updateNamedField(final Document document, final String name, final String value) {
        final Element testSetElement = document
                .select(new Evaluator.Tag("p"))
                .stream().filter(e -> e.text().contains(name))
                .findFirst()
                .orElse(null);
        if (testSetElement != null) {
            testSetElement.text(String.format("%s: %s", name, value));
        } else {
            final Element emptyTestSetElement = new Element("p");
            emptyTestSetElement.text(String.format("%s: %s", name, value));
            document.selectFirst("body").appendChild(emptyTestSetElement);
        }
    }

    public static String formatDuration(long duration, final boolean withHour) {
        if (duration == 0) {
            return String.valueOf(0);
        }
        final int ONE_HOUR = 60 * 60 * 1000;
        final int hours = (int) Math.floor((float) duration / ONE_HOUR);
        if (hours > 0) {
            duration -= hours * ONE_HOUR;
        }
        final int ONE_MINUTE = 60 * 1000;
        final int minutes = (int) Math.floor((float) duration / ONE_MINUTE);
        if (minutes > 0) {
            duration -= minutes * ONE_MINUTE;
        }
        final int ONE_SECOND = 1000;
        final int seconds = (int) Math.floor((float) duration / ONE_SECOND);
        final StringBuilder builder = new StringBuilder();
        if (hours > 0 || withHour) {
            builder.append(hours).append(" ч. ");
        }
        builder.append(minutes).append(" м. ").append(seconds).append(" с. ");
        return builder.toString();
    }

    private static boolean isHandleStatus(final ExecutionStatus status) {
        return status == ExecutionStatus.BLOCKED ||
                status == ExecutionStatus.FAIL ||
                status == ExecutionStatus.PASS ||
                status == ExecutionStatus.N_A;
    }

    private static boolean isNotStartedStatus(final ExecutionStatus status) {
        return status == ExecutionStatus.AUTO_FAIL ||
                status == ExecutionStatus.NOT_EXECUTED ||
                status == ExecutionStatus.IN_PROGRESS;
    }
}
