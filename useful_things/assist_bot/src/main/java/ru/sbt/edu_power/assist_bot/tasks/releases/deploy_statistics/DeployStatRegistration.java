package ru.sbt.edu_power.assist_bot.tasks.releases.deploy_statistics;

import com.slack.api.model.block.LayoutBlock;
import ru.sbt.edu_power.assist_bot.roles.roles.SlackRoles;
import ru.sbt.edu_power.assist_bot.task_flow.AbstractSlackRegistration;
import ru.sbt.edu_power.assist_bot.task_flow.StartSection;

public class DeployStatRegistration extends AbstractSlackRegistration {
    @Override
    public LayoutBlock getStartButton() {
        return getStartButton(
                "Статистика установок",
                "Получение статистики успешных и неуспешных установок сервисов из каналов #maintenance и #releases"
        );
    }

    @Override
    public void registerStartButton() {
        registerStartButton(DeployStatView::new);
    }

    @Override
    public SlackRoles[] acceptedRoles() {
        return new SlackRoles[]{SlackRoles.USER};
    }

    @Override
    public String getName() {
        return "Статистика установок";
    }

    @Override
    public StartSection getStartSection() {
        return StartSection.RELEASES;
    }
}
