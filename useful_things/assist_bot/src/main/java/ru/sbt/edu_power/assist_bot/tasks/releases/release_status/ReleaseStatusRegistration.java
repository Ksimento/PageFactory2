package ru.sbt.edu_power.assist_bot.tasks.releases.release_status;

import com.slack.api.model.block.LayoutBlock;
import ru.sbt.edu_power.assist_bot.roles.roles.SlackRoles;
import ru.sbt.edu_power.assist_bot.task_flow.AbstractSlackRegistration;
import ru.sbt.edu_power.assist_bot.task_flow.StartSection;

public class ReleaseStatusRegistration extends AbstractSlackRegistration {

    @Override
    public SlackRoles[] acceptedRoles() {
        return new SlackRoles[]{SlackRoles.USER};
    }

    @Override
    public LayoutBlock getStartButton() {
        return getStartButton("Статус релиза", "Получить список дефектов в релизе, сгруппированный по приоритету и статусам");
    }

    @Override
    public void registerStartButton() {
        registerStartButton(ReleaseStatusView::new);
    }

    @Override
    public String getName() {
        return "Статус релиза";
    }

    @Override
    public StartSection getStartSection() {
        return StartSection.RELEASES;
    }
}
