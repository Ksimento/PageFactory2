package ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.issue;

import ru.sbt.edu_power.external_services.jira.test_manager.test_run.HasExecutableStatus;
import ru.sbt.edu_power.external_services.shared.TaskExecutionStatus;
import ru.sbt.edu_power.external_services.jira.agile.model.IssueFields;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraVersionModel;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;
import ru.sbt.edu_power.assist_bot.services.slice.SliceStorage;

public class IssueStorage extends SliceStorage<IssueFields, IssueSlice> implements HasExecutableStatus {
    private static final long serialVersionUID = -5218610087021914173L;
    private TaskExecutionStatus status = TaskExecutionStatus.NOT_STARTED;
    private final TCFields.ProjectId project;
    private final JiraVersionModel iftVersion;

    public IssueStorage(
            final TCFields.ProjectId project,
            final JiraVersionModel iftVersion
    ) {
        this.project = project;
        this.iftVersion = iftVersion;
    }

    @Override
    public void slice() {
        final IssueSlice issueSlice = new IssueSlice(project, iftVersion);
        issueSlice.load();
        if (!isEmpty()) {
            issueSlice.optimize(getLastSlice());
        }
        issueSlice.sortByUser();
        add(issueSlice);
        if (size() == 1) {
            status = TaskExecutionStatus.REPEATABLE;
        }
        if (issueSlice.isEmpty()) {
            status = TaskExecutionStatus.SUCCESS;
        }
    }

    @Override
    public void updateStatus() {

    }

    @Override
    public TaskExecutionStatus getStatus() {
        return status;
    }
}
