package ru.sbt.edu_power.assist_bot.tasks.system.memory_usage;

import ru.sbt.edu_power.assist_bot.task_flow.AbstractSlackDemonRegistration;
import ru.sbt.edu_power.assist_bot.task_flow.IDemon;

public class MemoryUsageDemonRegistration extends AbstractSlackDemonRegistration {

    @Override
    public String getName() {
        return "Memory control";
    }

    @Override
    public boolean isDemon() {
        return true;
    }

    @Override
    public IDemon getDemon() {
        return MemoryUsageDemon.getInstance();
    }
}
