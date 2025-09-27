package ru.sbt.edu_power.assist_bot.tasks.releases.branches_for_cherry_pick;

import lombok.Getter;
import ru.sbt.edu_power.external_services.jira.agile.JiraVersion;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraVersionModel;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractModal;
import ru.sbt.edu_power.assist_bot.slack.views.sections.BBRepoSelectSection;
import ru.sbt.edu_power.assist_bot.slack.views.sections.JiraProjectSelectSection;
import ru.sbt.edu_power.assist_bot.slack.views.sections.JiraVersionSelectSection;
import ru.sbt.edu_power.assist_bot.slack.views.sections.MarkdownTextSection;

@Getter
public class BranchesForCherryPickView extends AbstractModal {

    private final BBRepoSelectSection bbRepoSelectSection = new BBRepoSelectSection();

    private final JiraProjectSelectSection jiraProjectSelectSection = new JiraProjectSelectSection(
            true,
            () -> bbRepoSelectSection.getAccessory().isFilled()
    );

    private final JiraVersionSelectSection jiraVersionSelectSection =
            new JiraVersionSelectSection(jiraProjectSelectSection, "Jira fix version");

    private final MarkdownTextSection completionText = new MarkdownTextSection(
            () -> "Все данные введены, можно начать поиск",
            true,
            () -> jiraVersionSelectSection.getAccessory().isFilled()
    );

    @Override
    public String getName() {
        return "Ветки для черипика";
    }

    @Override
    public void registerViewSubmit() {
        registerViewSubmit(new BranchesForCherryPickDispatcher(this));
    }

    public TCFields.ProjectId getProject() {
        return TCFields.ProjectId.valueOf(jiraProjectSelectSection.getAccessory().getValue());
    }

    public JiraVersionModel getJiraVersion() {
        return new JiraVersion().getJiraVersionById(
                getProject().id,
                jiraVersionSelectSection.getAccessory().getValue()
        );
    }
}
