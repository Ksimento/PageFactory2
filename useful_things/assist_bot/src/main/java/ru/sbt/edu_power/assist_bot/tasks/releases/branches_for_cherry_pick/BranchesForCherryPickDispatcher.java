package ru.sbt.edu_power.assist_bot.tasks.releases.branches_for_cherry_pick;

import ru.sbt.edu_power.external_services.bitbucket.models.BBRepos;
import ru.sbt.edu_power.external_services.version_releases.ReleaseBranchHelper;
import ru.sbt.edu_power.assist_bot.slack.SlackClient;
import ru.sbt.edu_power.assist_bot.task_flow.IDispatcher;

public class BranchesForCherryPickDispatcher implements IDispatcher {
    private final BranchesForCherryPickView view;

    public BranchesForCherryPickDispatcher(final BranchesForCherryPickView view) {
        this.view = view;
    }

    @Override
    public void dispatch() {
        final ReleaseBranchHelper helper = new ReleaseBranchHelper(
                BBRepos.getByRepoName(view.getBbRepoSelectSection().getAccessory().getValue())
        );

        final String branchesForCherryPick = helper.getBranchesForCherryPick(
                view.getJiraVersion().getName()
        );

        SlackClient.sendText(
                String.format(
                        "Ветки для черипика в репозиторий *%s*:\n%s",
                        view.getBbRepoSelectSection().getAccessory().getValue(),
                        branchesForCherryPick
                ),
                view.getUserId()
        );
    }
}
