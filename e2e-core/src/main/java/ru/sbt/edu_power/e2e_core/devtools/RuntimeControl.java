package ru.sbt.edu_power.e2e_core.devtools;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RuntimeControl {
    private static final Map<Integer, RuntimeConsoleErrorTracking> RUNTIME_CONSOLE_ERROR_TRACKING_MAP = new ConcurrentHashMap<>();

    public static Map<String, String> getErrors() {
        return getRuntimeConsoleErrorTrackingInstance().getErrors();
    }

    public static void clearErrors() {
        getRuntimeConsoleErrorTrackingInstance().clearErrors();
    }

    // Возвращает инстанс RuntimeConsoleErrorTracking для текущего потока исполнения
    public static RuntimeConsoleErrorTracking getRuntimeConsoleErrorTrackingInstance() {
        if (!RUNTIME_CONSOLE_ERROR_TRACKING_MAP.containsKey(DevTools.getId())) {
            RUNTIME_CONSOLE_ERROR_TRACKING_MAP.put(DevTools.getId(), new RuntimeConsoleErrorTracking(DevTools.getRuntime()));
        }
        return RUNTIME_CONSOLE_ERROR_TRACKING_MAP.get(DevTools.getId());
    }
}
