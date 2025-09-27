package ru.sbt.edu_power.assist_bot.tasks.test_run.test_run_creation;

import lombok.Getter;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractModal;
import ru.sbt.edu_power.assist_bot.slack.views.sections.JiraProjectSelectSection;
import ru.sbt.edu_power.assist_bot.slack.views.sections.PresetSelectSection;
import ru.sbt.edu_power.assist_bot.slack.views.sections.JiraVersionSelectSection;
import ru.sbt.edu_power.assist_bot.slack.views.sections.SlackChannelSelectSection;
import ru.sbt.edu_power.assist_bot.slack.views.sections.TestRunRiskDependencySelect;

@Getter
public class TestRunCreationView extends AbstractModal {
    private final SlackChannelSelectSection slackChannelSelectSection = new SlackChannelSelectSection();

    private final PresetSelectSection presetSelectSection = new PresetSelectSection(
            true,
            () -> slackChannelSelectSection.getAccessory().isFilled()
    );

    private final TestRunRiskDependencySelect testRunRiskDependencySelect = new TestRunRiskDependencySelect(
            true,
            () -> presetSelectSection.getAccessory().isFilled()
    );

    private final JiraProjectSelectSection jiraProjectSelectSection = new JiraProjectSelectSection(
            true,
            () -> testRunRiskDependencySelect.getAccessory().isFilled()
    );

    private final JiraVersionSelectSection jiraVersionSelectSection = new JiraVersionSelectSection(
            jiraProjectSelectSection
    );

    @Override
    public String getName() {
        return "Создание тест-сета";
    }

    @Override
    public void registerViewSubmit() {
        registerViewSubmit(new TestRunCreationDispatcher(this));
    }
}
