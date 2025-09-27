package ru.sbt.edu_power.external_services.timer;

import java.util.LinkedHashMap;
import java.util.Map;

class TimerRepository {
    private final long timeoutMillis;
    private final long startTimer;
    private final Map<String, Long> deltaList = new LinkedHashMap<>();

    TimerRepository() {
        timeoutMillis = -1;
        startTimer = System.currentTimeMillis();
    }

    TimerRepository(final long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
        startTimer = System.currentTimeMillis();
    }

    boolean isTimeoutMillis() {
        if (timeoutMillis == -1) {
            throw new TimerException("Время таймаута не установлено");
        }
        return System.currentTimeMillis() > (startTimer + timeoutMillis);
    }

    long getExpiredTime() {
        return System.currentTimeMillis() - startTimer;
    }

    long getLastDelta(final String name) {
        final long currentDelta = System.currentTimeMillis() - (startTimer + getDeltaSum());
        setDelta(name, currentDelta);
        return currentDelta;
    }

    private void setDelta(final String name, final long executionTime) {
        deltaList.put(name, executionTime);
    }

    private long getDeltaSum() {
        return deltaList.values().stream().reduce(Long::sum).orElse(0L);
    }
}
