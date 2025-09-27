package ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs;

import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.ExecutionStatus;

import java.time.Duration;

public class AnalyticUtils {

    public static boolean isInNotCompleteStatus(final String status) {
        return status.equals(ExecutionStatus.AUTO_FAIL.getStatusName()) ||
               status.equals(ExecutionStatus.IN_PROGRESS.getStatusName()) ||
               status.equals(ExecutionStatus.NOT_EXECUTED.getStatusName());
    }

    public static boolean isInNotStartedStatus(final String status) {
        return status.equals(ExecutionStatus.AUTO_FAIL.getStatusName()) ||
               status.equals(ExecutionStatus.NOT_EXECUTED.getStatusName());
    }

    public static boolean isHandleTestCase(final String status) {
        return status.equals(ExecutionStatus.NOT_EXECUTED.getStatusName()) ||
               status.equals(ExecutionStatus.IN_PROGRESS.getStatusName()) ||
               status.equals(ExecutionStatus.BLOCKED.getStatusName()) ||
               status.equals(ExecutionStatus.FAIL.getStatusName()) ||
               status.equals(ExecutionStatus.N_A.getStatusName()) ||
               status.equals(ExecutionStatus.PASS.getStatusName());
    }

    public static boolean isInCompleteStatus(final String status) {
        return !isInNotCompleteStatus(status);
    }

    public static String timeToString(final long time) {
        final Duration duration = Duration.ofMillis(time);
        return String.format("%d ч. %d м.", duration.toHours(), duration.toMinutes());
    }
}
