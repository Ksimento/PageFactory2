package ru.sbt.edu_power.assist_bot.tasks.system.memory_usage;

import com.slack.api.model.block.LayoutBlock;
import ru.sbt.edu_power.assist_bot.roles.roles.SlackRoles;
import ru.sbt.edu_power.assist_bot.task_flow.AbstractSlackRegistration;
import ru.sbt.edu_power.assist_bot.task_flow.StartSection;

public class MemoryUsageRegistration extends AbstractSlackRegistration {
    @Override
    public LayoutBlock getStartButton() {
        return getStartButton("Использование памяти", "Предоставляет информацию об использовании памяти ботом");
    }

    @Override
    public void registerStartButton() {
        registerStartButton(MemoryUsageView::new);
    }

    @Override
    public SlackRoles[] acceptedRoles() {
        return new SlackRoles[]{SlackRoles.ADMIN};
    }

    @Override
    public String getName() {
        return "Использование памяти";
    }

    @Override
    public StartSection getStartSection() {
        return StartSection.ADMIN;
    }
}
