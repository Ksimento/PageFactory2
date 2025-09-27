package ru.sbt.edu_power.assist_bot.tasks.system.user_roles_config;

import com.slack.api.model.block.LayoutBlock;
import ru.sbt.edu_power.assist_bot.roles.roles.SlackRoles;
import ru.sbt.edu_power.assist_bot.task_flow.AbstractSlackRegistration;
import ru.sbt.edu_power.assist_bot.task_flow.StartSection;

public class UserRolesConfigRegistration extends AbstractSlackRegistration {
    @Override
    public SlackRoles[] acceptedRoles() {
        return new SlackRoles[]{SlackRoles.ADMIN};
    }

    @Override
    public String getName() {
        return "Роли";
    }

    @Override
    public StartSection getStartSection() {
        return StartSection.ADMIN;
    }

    @Override
    public boolean isDemon() {
        return false;
    }

    @Override
    public LayoutBlock getStartButton() {
        return getStartButton("Настроить роли", "Добавление и удаление ролей пользователей");
    }

    @Override
    public void registerStartButton() {
        registerStartButton(UserRolesConfigView::new);
    }
}
