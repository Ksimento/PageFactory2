package ru.sbt.edu_power.assist_bot.tasks.releases.last_releases;

import com.slack.api.model.block.LayoutBlock;
import ru.sbt.edu_power.assist_bot.roles.roles.SlackRoles;
import ru.sbt.edu_power.assist_bot.task_flow.AbstractSlackRegistration;
import ru.sbt.edu_power.assist_bot.task_flow.StartSection;

public class LastReleasesRegistration extends AbstractSlackRegistration {
    @Override
    public LayoutBlock getStartButton() {
        return getStartButton(
                "Последние релизы",
                "Получить список последних выполненных и запланированных релизов"
        );
    }

    @Override
    public void registerStartButton() {
        registerStartButton(LastReleasesView::new);
    }

    @Override
    public SlackRoles[] acceptedRoles() {
        return new SlackRoles[]{SlackRoles.USER};
    }

    @Override
    public String getName() {
        return "Последние релизы";
    }

    @Override
    public StartSection getStartSection() {
        return StartSection.RELEASES;
    }
}
