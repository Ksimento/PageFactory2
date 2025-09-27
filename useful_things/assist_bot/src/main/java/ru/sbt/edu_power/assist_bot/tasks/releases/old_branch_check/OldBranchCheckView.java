package ru.sbt.edu_power.assist_bot.tasks.releases.old_branch_check;

import lombok.Getter;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractModal;
import ru.sbt.edu_power.assist_bot.slack.views.sections.MarkdownTextSection;
import ru.sbt.edu_power.assist_bot.slack.views.sections.SlackChannelSelectSection;
import ru.sbt.edu_power.assist_bot.slack.views.sections.BBRepoSelectSection;

// модуль выполняет поиск устаревших веток, по которым закрыты PR или задачи, ну или ветка просто очень старая без движения
@Getter
public class OldBranchCheckView extends AbstractModal {

    private final SlackChannelSelectSection slackChannelSelectSection = new SlackChannelSelectSection();

    private final BBRepoSelectSection bbRepoSelectSection = new BBRepoSelectSection(
            true,
            () -> slackChannelSelectSection.getAccessory().isFilled()
    );

    private final MarkdownTextSection completionText = new MarkdownTextSection(
            () -> "Все данные введены, можно начать поиск",
            true,
            () -> bbRepoSelectSection.getAccessory().isFilled()
    );

    @Override
    public String getName() {
        return "Поиск устаревших веток";
    }

    @Override
    public void registerViewSubmit() {
        registerViewSubmit(new OldBranchDispatcher(this));
    }
}
