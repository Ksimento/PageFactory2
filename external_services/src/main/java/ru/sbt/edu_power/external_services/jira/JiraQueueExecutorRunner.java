package ru.sbt.edu_power.external_services.jira;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.timer.Timer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
public class JiraQueueExecutorRunner implements Callable<Boolean> {
    private boolean isQueueActive = true;
    private final Map<Consumer<Map<String, ?>>, Map<String, Object>> queue = new ConcurrentHashMap<>();
    private static final JiraQueueExecutorRunner INSTANCE = new JiraQueueExecutorRunner();

    public static JiraQueueExecutorRunner getInstance() {
        return INSTANCE;
    }

    public void stopQueue() {
        isQueueActive = false;
    }
    @SneakyThrows
    @Override
    public Boolean call() {
        do {
            for (final Consumer<Map<String, ?>> function : queue.keySet()) {
                function.accept(queue.get(function));
            }
            Thread.sleep(500);
        } while (isQueueActive());
        return true;
    }

    public void putToQueue(final Consumer<Map<String, ?>> function, final Map<String, ?> data) {
        if (!queue.containsKey(function)) {
            queue.put(function, new ConcurrentHashMap<>());
        }
        data.forEach((k, v) ->
            queue.get(function).put(encodeTag(k), v)
        );
    }

    private boolean isQueueEmpty() {
        return queue.keySet().stream().allMatch(function -> queue.get(function).isEmpty());
    }

    private boolean isQueueActive() {
        if (isQueueActive) {
            return true;
        }
        if (isQueueEmpty()) {
            return false;
        }
        return !Timer.isTimeout("queueThreadTimeout", 30);
    }

    private static String encodeTag(final String tag) {
        return tag + "&" + UUID.randomUUID();
    }

    public static String decodeTag(final String tag) {
        return tag.split("&")[0];
    }
}
