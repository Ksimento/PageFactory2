package ru.sbt.edu_power.assist_bot.tasks.releases.master_version;

import com.slack.api.model.block.LayoutBlock;
import ru.sbt.edu_power.assist_bot.roles.roles.SlackRoles;
import ru.sbt.edu_power.assist_bot.task_flow.AbstractSlackRegistration;
import ru.sbt.edu_power.assist_bot.task_flow.StartSection;

public class MasterVersionRegistration extends AbstractSlackRegistration {
    @Override
    public LayoutBlock getStartButton() {
        return getStartButton("Версия мастера", "Поможет узнать текущую версию мастера");
    }

    @Override
    public void registerStartButton() {
        registerStartButton(MasterVersionView::new);
    }

    @Override
    public SlackRoles[] acceptedRoles() {
        return new SlackRoles[]{SlackRoles.USER};
    }

    @Override
    public String getName() {
        return "Версия мастера";
    }

    @Override
    public StartSection getStartSection() {
        return StartSection.RELEASES;
    }
}
