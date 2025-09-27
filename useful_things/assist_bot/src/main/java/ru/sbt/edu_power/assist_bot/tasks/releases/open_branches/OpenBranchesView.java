package ru.sbt.edu_power.assist_bot.tasks.releases.open_branches;

import lombok.Getter;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractModal;
import ru.sbt.edu_power.assist_bot.slack.views.sections.BBRepoSelectSection;
import ru.sbt.edu_power.assist_bot.slack.views.sections.MarkdownTextSection;

@Getter
public class OpenBranchesView extends AbstractModal {

    private final BBRepoSelectSection bbRepoSelectSection = new BBRepoSelectSection();

    private final MarkdownTextSection completionText = new MarkdownTextSection(
            () -> "Все данные введены, можно начать поиск",
            true,
            () -> bbRepoSelectSection.getAccessory().isFilled()
    );

    @Override
    public String getName() {
        return "Открытые ветки";
    }

    @Override
    public void registerViewSubmit() {
        registerViewSubmit(new OpenBranchesDispatcher(this));
    }
}
