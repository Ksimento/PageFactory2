package ru.sbt.edu_power.assist_bot.tasks.system.memory_usage;

import ru.sbt.edu_power.assist_bot.services.slice.SliceStorage;

public class MemoryUsageStorage extends SliceStorage<Long, MemoryUsageSlice> {
    private static final long serialVersionUID = -6123475041868225965L;

    @Override
    public void slice() {
        final MemoryUsageSlice memoryUsageSlice = new MemoryUsageSlice();
        memoryUsageSlice.load();
        if (!isEmpty()) {
            memoryUsageSlice.optimize(getLastSlice());
        }
        add(memoryUsageSlice);
    }
}
