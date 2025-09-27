package ru.sbt.edu_power.assist_bot.tasks.releases.release_status;

import lombok.Getter;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractModal;
import ru.sbt.edu_power.assist_bot.slack.views.sections.JiraProjectSelectSection;
import ru.sbt.edu_power.assist_bot.slack.views.sections.JiraVersionSelectSection;

@Getter
public class ReleaseStatusView extends AbstractModal {

    private final JiraProjectSelectSection jiraProjectSelectSection =
            new JiraProjectSelectSection(true);

    private final JiraVersionSelectSection jiraVersionSelectSection =
            new JiraVersionSelectSection(jiraProjectSelectSection);

    @Override
    public String getName() {
        return "Статус релиза";
    }

    @Override
    public void registerViewSubmit() {
        registerViewSubmit(new ReleaseStatusDispatcher(this));
    }
}
