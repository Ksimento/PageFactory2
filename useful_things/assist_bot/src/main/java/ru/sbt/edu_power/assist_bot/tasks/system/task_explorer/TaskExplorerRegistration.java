package ru.sbt.edu_power.assist_bot.tasks.system.task_explorer;

import com.slack.api.model.block.LayoutBlock;
import ru.sbt.edu_power.assist_bot.roles.roles.SlackRoles;
import ru.sbt.edu_power.assist_bot.task_flow.AbstractSlackRegistration;
import ru.sbt.edu_power.assist_bot.task_flow.StartSection;

public class TaskExplorerRegistration extends AbstractSlackRegistration {

    @Override
    public SlackRoles[] acceptedRoles() {
        return new SlackRoles[]{SlackRoles.ADMIN, SlackRoles.REGRESS};
    }

    @Override
    public LayoutBlock getStartButton() {
        return getStartButton("Диспетчер задач", "Поиск и остановка задач");
    }

    @Override
    public void registerStartButton() {
        registerStartButton(TaskExplorerView::new);
    }

    @Override
    public String getName() {
        return "Диспетчер задач";
    }

    @Override
    public StartSection getStartSection() {
        return StartSection.ADMIN;
    }

    @Override
    public boolean isDemon() {
        return false;
    }
}
