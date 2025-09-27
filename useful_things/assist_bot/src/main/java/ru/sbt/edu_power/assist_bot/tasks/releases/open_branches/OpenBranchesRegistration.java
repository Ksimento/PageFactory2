package ru.sbt.edu_power.assist_bot.tasks.releases.open_branches;

import com.slack.api.model.block.LayoutBlock;
import ru.sbt.edu_power.assist_bot.roles.roles.SlackRoles;
import ru.sbt.edu_power.assist_bot.task_flow.AbstractSlackRegistration;
import ru.sbt.edu_power.assist_bot.task_flow.StartSection;

public class OpenBranchesRegistration extends AbstractSlackRegistration {
    @Override
    public LayoutBlock getStartButton() {
        return getStartButton("Открытые ветки", "Получить список открытых для мерджа веток");
    }

    @Override
    public void registerStartButton() {
        registerStartButton(OpenBranchesView::new);
    }

    @Override
    public SlackRoles[] acceptedRoles() {
        return new SlackRoles[]{SlackRoles.USER};
    }

    @Override
    public String getName() {
        return "Открытые ветки";
    }

    @Override
    public StartSection getStartSection() {
        return StartSection.RELEASES;
    }
}
