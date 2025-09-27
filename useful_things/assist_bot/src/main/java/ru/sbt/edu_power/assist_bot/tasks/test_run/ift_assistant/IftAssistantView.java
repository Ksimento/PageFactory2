package ru.sbt.edu_power.assist_bot.tasks.test_run.ift_assistant;

import lombok.Getter;
import ru.sbt.edu_power.external_services.jira.agile.JiraVersion;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraVersionModel;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractModal;
import ru.sbt.edu_power.assist_bot.slack.views.sections.JiraProjectSelectSection;
import ru.sbt.edu_power.assist_bot.slack.views.sections.JiraVersionSelectSection;
import ru.sbt.edu_power.assist_bot.slack.views.sections.SlackChannelSelectSection;
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces.ISlackChannel;

/**
 * Класс реализует модуль запуска ассистента IFT
 * Ассистент выполняет поиск незакрытых задач в статусах IFT, Need Test и In QA
 * относительно версии проводимого регресса и информирует ответственных, а так же предоставляет отчёт
 */
@Getter
public class IftAssistantView extends AbstractModal implements ISlackChannel {

    private final SlackChannelSelectSection slackChannelSelectSection = new SlackChannelSelectSection();

    private final JiraProjectSelectSection jiraProjectSelectSection = new JiraProjectSelectSection(
            true,
            () -> slackChannelSelectSection.getAccessory().isFilled()
    );

    private final JiraVersionSelectSection jiraVersionSelectSection =
            new JiraVersionSelectSection(jiraProjectSelectSection);

    @Override
    public String getName() {
        return "IFT ассистент";
    }

    @Override
    public void registerViewSubmit() {
        registerViewSubmit(new IftDispatcher(this));
    }

    public TCFields.ProjectId getProject() {
        return TCFields.ProjectId.valueOf(jiraProjectSelectSection.getAccessory().getValue());
    }

    public JiraVersionModel getVersion() {
        return new JiraVersion().getJiraVersionById(
                getProject().id,
                jiraVersionSelectSection.getAccessory().getValue()
        );
    }

    @Override
    public String getSlackChannel() {
        return slackChannelSelectSection.getAccessory().getValue();
    }
}
