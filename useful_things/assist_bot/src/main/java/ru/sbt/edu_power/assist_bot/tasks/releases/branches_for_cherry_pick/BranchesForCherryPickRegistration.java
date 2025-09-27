package ru.sbt.edu_power.assist_bot.tasks.releases.branches_for_cherry_pick;

import com.slack.api.model.block.LayoutBlock;
import ru.sbt.edu_power.assist_bot.roles.roles.SlackRoles;
import ru.sbt.edu_power.assist_bot.task_flow.AbstractSlackRegistration;
import ru.sbt.edu_power.assist_bot.task_flow.StartSection;

public class BranchesForCherryPickRegistration extends AbstractSlackRegistration {
    @Override
    public LayoutBlock getStartButton() {
        return getStartButton(
                "Ветки для черипика",
                "Показывает все ветки, в которые нужно сделать черипик изменений относительно выбранной"
        );
    }

    @Override
    public void registerStartButton() {
        registerStartButton(BranchesForCherryPickView::new);
    }

    @Override
    public SlackRoles[] acceptedRoles() {
        return new SlackRoles[]{SlackRoles.USER};
    }

    @Override
    public String getName() {
        return "Ветки для черипика";
    }

    @Override
    public StartSection getStartSection() {
        return StartSection.RELEASES;
    }
}
