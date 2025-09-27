package ru.sbt.edu_power.assist_bot.tasks.system.memory_usage;

import ru.sbt.edu_power.assist_bot.task_flow.IDemon;
import ru.sbt.edu_power.assist_bot.task_flow.TaskGlue;

public final class MemoryUsageDemon implements IDemon {
    private static final MemoryUsageDemon INSTANCE = new MemoryUsageDemon();
    private final MemoryUsageTask task = new MemoryUsageTask();

    private MemoryUsageDemon() {}

    public static MemoryUsageDemon getInstance() {
        return INSTANCE;
    }

    @Override
    public void init() {
        new TaskGlue("Memory control", "", true)
                .addQueueCompleteCondition(() -> false)
                .add(task)
                .execute();
    }

    public MemoryUsageTask getTask() {
        return task;
    }
}
