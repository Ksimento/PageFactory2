package ru.sbt.edu_power.assist_bot.tasks.full_deploy;

import com.slack.api.model.block.LayoutBlock;
import ru.sbt.edu_power.assist_bot.roles.roles.SlackRoles;
import ru.sbt.edu_power.assist_bot.task_flow.AbstractSlackRegistration;
import ru.sbt.edu_power.assist_bot.task_flow.StartSection;

public class FullDeployRegistration extends AbstractSlackRegistration {
    @Override
    public SlackRoles[] acceptedRoles() {
        return new SlackRoles[]{SlackRoles.ADMIN};
    }

    @Override
    public String getName() {
        return "Деплой стенда";
    }

    @Override
    public StartSection getStartSection() {
        return StartSection.JENKINS;
    }

    @Override
    public LayoutBlock getStartButton() {
        return getStartButton(
                "Деплой стенда",
                "Деплой стенда из собранных ранее образов"
        );
    }

    @Override
    public void registerStartButton() {
        registerStartButton(FullDeployView::new);
    }
}
