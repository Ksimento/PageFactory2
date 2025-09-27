package ru.sbt.edu_power.assist_bot.tasks.system.memory_usage;

import ru.sbt.edu_power.assist_bot.slack.views.AbstractModal;

public class MemoryUsageView extends AbstractModal {
    @Override
    public String getName() {
        return "Контроль памяти";
    }

    @Override
    public void registerViewSubmit() {
        registerViewSubmit(new MemoryUsageDispatcher(this));
    }
}
