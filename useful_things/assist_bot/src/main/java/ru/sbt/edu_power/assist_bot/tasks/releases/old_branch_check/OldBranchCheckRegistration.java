package ru.sbt.edu_power.assist_bot.tasks.releases.old_branch_check;

import com.slack.api.model.block.LayoutBlock;
import ru.sbt.edu_power.assist_bot.roles.roles.SlackRoles;
import ru.sbt.edu_power.assist_bot.task_flow.AbstractSlackRegistration;
import ru.sbt.edu_power.assist_bot.task_flow.StartSection;

public class OldBranchCheckRegistration extends AbstractSlackRegistration {
    @Override
    public SlackRoles[] acceptedRoles() {
        return new SlackRoles[]{SlackRoles.ADMIN, SlackRoles.USER};
    }

    @Override
    public LayoutBlock getStartButton() {
        return getStartButton(
                "Найти ветки",
                "Поиск устаревших веток, по которым мердж выполнен или задача закрыта. " +
                "Информация будет отправлена в личку авторам коммитов и в выбранный канал");
    }

    @Override
    public void registerStartButton() {
        registerStartButton(OldBranchCheckView::new);
    }

    @Override
    public String getName() {
        return "Поиск устаревших веток";
    }

    @Override
    public StartSection getStartSection() {
        return StartSection.USER;
    }
}
