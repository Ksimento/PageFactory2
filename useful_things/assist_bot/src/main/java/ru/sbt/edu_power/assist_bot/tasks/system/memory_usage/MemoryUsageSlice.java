package ru.sbt.edu_power.assist_bot.tasks.system.memory_usage;

import ru.sbt.edu_power.assist_bot.services.slice.Slice;

public class MemoryUsageSlice extends Slice<Long> {
    private static final long serialVersionUID = 270281713502564434L;

    @Override
    public void load() {
        add(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        add(Runtime.getRuntime().totalMemory());
    }

    @Override
    public void update(final Iterable<Long> dataForUpdate) {
        dataForUpdate.forEach(this::add);
    }
}
