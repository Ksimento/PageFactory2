package ru.sbt.edu_power.external_services.timer;

import ru.sbtqa.tag.datajack.Stash;

import java.util.UUID;
import java.util.function.BooleanSupplier;

// класс реализует возможность исполнения произвольного кода в цикле до
// тех пор, пока не будет достигнут ожидаемый результат, либо не завершится
// время, отведённое для выполнения
public class Timer {
    private static TimerRepository getTimer(final String timerName) {
        if (!Stash.asMap().containsKey(timerName)) {
            Stash.put(timerName, new TimerRepository());
        }
        return Stash.getValue(timerName);
    }

    private static TimerRepository getTimer(final String timerName, final long timeoutMillis) {
        if (!Stash.asMap().containsKey(timerName)) {
            Stash.put(timerName, new TimerRepository(timeoutMillis));
        }
        return Stash.getValue(timerName);
    }

    public static void startTimer(final String timerName) {
        removeTimer(timerName);
        getTimer(timerName);
    }

    public static boolean isTimeoutMillis(final String timerName, final long timeoutMillis) {
        return getTimer(timerName, timeoutMillis).isTimeoutMillis();
    }

    public static boolean isTimeout(final String timerName, final int timeout) {
        final boolean result = isTimeoutMillis(timerName, timeout * 1000L);
        if (result) {
            removeTimer(timerName);
        }
        return result;
    }

    public static boolean isTimeoutThrowable(final String timerName, final int timeout) {
        final String message = "Вышло время исполнения кода";
        return isTimeoutThrowable(timerName, timeout, message);
    }

    public static boolean isTimeoutThrowable(final String timerName, final int timeout, final String message) {
        if (isTimeout(timerName, timeout)) {
            throw new TimerException(String.format("\"%s\"\nТаймаут: \"%s\" секунд", message, timeout));
        }
        return false;
    }

    public static long getExpiredTime(final String timerName) {
        return getTimer(timerName).getExpiredTime();
    }

    public static void removeTimer(final String timerName) {
        if (Stash.asMap().containsKey(timerName)) {
            Stash.remove(timerName);
        }
    }

    public static long getDelta(final String timerName, final String executionOperationName) {
        return getTimer(timerName).getLastDelta(executionOperationName);
    }

    public static void executeTimerThrowable(final String timerName, final int timeout, final BooleanSupplier function) {
        final String message = String.format("Задача не выполнена в течении \"%s\" секунд", timeout);
        whileExecutor(timerName, timeout, message, function);
    }

    public static void executeTimerThrowable(final int timeout, final String message, final BooleanSupplier function) {
        final String timerName = generateTimerName();
        whileExecutor(timerName, timeout, message, function);
    }

    public static void executeTimerThrowable(final String timerName, final int timeout, final String message, final BooleanSupplier function) {
        whileExecutor(timerName, timeout, message, function);
    }

    public static boolean executeTimer(final int timeout, final BooleanSupplier function) {
        return executeTimerMillis(generateTimerName(), timeout * 1000L, function);
    }

    public static boolean executeTimer(final String timerName, final int timeout, final BooleanSupplier function) {
        return executeTimerMillis(timerName, timeout * 1000L, function);
    }

    public static boolean executeTimerMillis(final long timeout, final BooleanSupplier function) {
        return executeTimerMillis(generateTimerName(), timeout, function);
    }

    public static boolean executeTimerMillis(final String timerName, final long timeout, final BooleanSupplier function) {
        while (!isTimeoutMillis(timerName, timeout)) {
            if (function.getAsBoolean()) {
                return true;
            }
            freeze(50L);
        }
        return false;
    }

    private static void whileExecutor(final String timerName, final int timeout, final String message, final BooleanSupplier function) {
        while (!isTimeoutThrowable(timerName, timeout, message)) {
            if (function.getAsBoolean()) {
                break;
            }
            freeze(250L);
        }
    }

    private static void freeze(final long ms) {
        try {
            Thread.sleep(ms);
        } catch (final InterruptedException e) {
            throw new TimerException(e);
        }
    }

    private static String generateTimerName() {
        return UUID.randomUUID().toString();
    }
}
