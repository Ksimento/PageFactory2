package ru.sbt.edu_power.assist_bot.tasks.test_run.remove_tests;

import lombok.Getter;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractModal;
import ru.sbt.edu_power.assist_bot.slack.views.sections.JiraProjectSelectSection;
import ru.sbt.edu_power.assist_bot.slack.views.sections.JiraVersionSelectSection;
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces.IHasProjectAndVersions;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.TestRunSelectSection;
import ru.sbt.edu_power.external_services.jira.agile.JiraVersion;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraVersionModel;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;

@Getter
public class RemoveTestsView extends AbstractModal implements IHasProjectAndVersions {

    private final JiraProjectSelectSection jiraProjectSelectSection = new JiraProjectSelectSection();

    private final JiraVersionSelectSection jiraVersionSelectSection = new JiraVersionSelectSection(
            jiraProjectSelectSection,
            true,
            () -> jiraProjectSelectSection.getAccessory().isFilled()
    );

    private final TestRunSelectSection testRunSelectSection = new TestRunSelectSection(
            this,
            true,
            () -> jiraVersionSelectSection.getAccessory().isFilled()
    );

    @Override
    public String getName() {
        return "Удаление тест-кейсов";
    }

    @Override
    public void registerViewSubmit() {
        registerViewSubmit(new RemoveTestsDispatcher(this));
    }

    @Override
    public TCFields.ProjectId getJiraProjectId() {
        return TCFields.ProjectId.valueOf(jiraProjectSelectSection.getAccessory().getValue());
    }

    @Override
    public JiraVersionModel getDeployVersion() {
        return new JiraVersion().getJiraVersionById(getJiraProjectId().id, jiraVersionSelectSection.getAccessory().getValue());
    }

    @Override
    public JiraVersionModel getTestRunVersion() {
        return getDeployVersion();
    }
}
