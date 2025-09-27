package ru.sbt.edu_power.assist_bot.tasks.releases.releases_tags;

import com.slack.api.model.block.LayoutBlock;
import ru.sbt.edu_power.assist_bot.roles.roles.SlackRoles;
import ru.sbt.edu_power.assist_bot.task_flow.AbstractSlackRegistration;
import ru.sbt.edu_power.assist_bot.task_flow.StartSection;

public class ReleasesTagsRegistration extends AbstractSlackRegistration {

    @Override
    public SlackRoles[] acceptedRoles() {
        return new SlackRoles[]{SlackRoles.USER};
    }

    @Override
    public LayoutBlock getStartButton() {
        return getStartButton("Найти теги", "Поиск тегов сервисов в канале Releases по названию сервиса и стенду");
    }

    @Override
    public void registerStartButton() {
        registerStartButton(ReleasesTagsView::new);
    }

    @Override
    public String getName() {
        return "Поиск тегов в releases";
    }

    @Override
    public StartSection getStartSection() {
        return StartSection.RELEASES;
    }
}
