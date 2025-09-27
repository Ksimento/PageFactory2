package ru.sbt.edu_power.assist_bot.tasks.releases.old_branch_check;

import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.bitbucket.BBConnection;
import ru.sbt.edu_power.external_services.bitbucket.models.BBRepos;
import ru.sbt.edu_power.external_services.bitbucket.models.BranchListJiraIssuesModel;
import ru.sbt.edu_power.external_services.bitbucket.models.BranchModel;
import ru.sbt.edu_power.external_services.jira.agile.JiraSearch;
import ru.sbt.edu_power.external_services.jira.agile.model.IssueFields;
import ru.sbt.edu_power.external_services.jira.agile.model.IssueQuery;
import ru.sbt.edu_power.assist_bot.slack.SlackClient;
import ru.sbt.edu_power.assist_bot.task_flow.IDispatcher;
import ru.sbt.edu_power.assist_bot.tasks.releases.old_branch_check.validations.ClosedPullRequests;
import ru.sbt.edu_power.assist_bot.tasks.releases.old_branch_check.validations.VeryOldBranches;
import ru.sbt.edu_power.assist_bot.tasks.releases.old_branch_check.validations.WithoutPullRequestAndTaskIsClosed;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class OldBranchDispatcher implements IDispatcher {
    private final OldBranchCheckView view;
    private final Map<String, IssueFields> issues = new HashMap<>();
    public static final boolean TEST_ENV = false;
    public static final long MAX_DAYS = 30;

    public OldBranchDispatcher(final OldBranchCheckView view) {
        this.view = view;
    }

    @Override
    public void dispatch() {
        try {
            evaluate();
        } catch (final Throwable e) {
            log.error("Возникла ошибка", e);
            SlackClient.sendText(
                    "Что-то пошло не так, я не справился с задачей. Передай разработчику это сообщение: \n" + e,
                    view.getUserId()
            );
        }
    }

    private void evaluate() {
        SlackClient.sendText("Приступил к работе, нужно немного подождать", view.getUserId());
        final BBRepos repo = BBRepos.getByRepoName(view.getBbRepoSelectSection().getAccessory().getValue());
        final List<Branch> branchList = BBConnection.getBranches(repo, "")
                .stream()
                .map(bm -> {
                    try {
                        return new Branch(bm, repo, MAX_DAYS, issues);
                    } catch (final Throwable e) {
                        SlackClient.sendText("Ошибка при получении данных из ветки " + bm.getDisplayId(), view.getUserId());
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        log.info("Очень старых веток {}", branchList.stream().filter(Branch::isVeryOld).count());
        final int totalBranches = branchList.size();
        log.info("Получил все ветки из репозитория");
        final List<Branch> oldBranches = branchList
                .stream()
                .filter(Branch::hasTask)
                .filter(Branch::isOld)
                .collect(Collectors.toList());

        final ClosedPullRequests closedPullRequests = new ClosedPullRequests(view, oldBranches);
        log.info("Выполнена нотификация по смерженным PR");

        collectIssues(oldBranches);
        log.info("Собрал информацию по таскам: {} записей", issues.size());

        final WithoutPullRequestAndTaskIsClosed withoutPullRequestAndTaskIsClosed = new WithoutPullRequestAndTaskIsClosed(
                view,
                issues,
                oldBranches
        );
        log.info("Выполнена нотификация по закрытым задачам");

        final VeryOldBranches veryOldBranches = new VeryOldBranches(view, issues, oldBranches);
        log.info("Выполнена нотификация по очень старым веткам");

        final String message = String.format(
                "<!here> Всего получено веток из репозитория *%s*: %s\n" +
                        "Ветки с закрытыми PR: %s\n" +
                        "Ветки с закрытыми задачами: %s\n" +
                        "Очень старые ветки: %s",
                branchList.get(0).getRepo().getRepoName(),
                totalBranches,
                closedPullRequests.getCounter(),
                withoutPullRequestAndTaskIsClosed.getCounter(),
                veryOldBranches.getCounter()
        );
        SlackClient.sendText(message, view.getSlackChannelSelectSection().getAccessory().getValue());
    }


    private void collectIssues(final List<Branch> branchList) {
        final String[] keys = branchList
                .stream()
                .filter(Branch::hasTask)
                .map(Branch::getBranchModel)
                .map(BranchModel::getMetadata)
                .map(BranchModel.Metadata::getBranchListJiraIssuesModel)
                .flatMap(List::stream)
                .map(BranchListJiraIssuesModel.JiraIssue::getKey)
                .collect(Collectors.toList())
                .toArray(new String[]{});
        if (keys.length == 0) {
            return;
        }
        final IssueQuery issueQuery = new IssueQuery(0, 100);
        issueQuery.and(IssueFields.Field.ID, IssueQuery.Op.IN, keys)
                .setFields(
                        IssueFields.Field.ID.getForQuery(),
                        IssueFields.Field.PROJECT.getForQuery(),
                        IssueFields.Field.RESOLUTION.getForQuery(),
                        IssueFields.Field.STATUS.getForQuery(),
                        IssueFields.Field.DEVELOPER.getFieldName()
                )
        ;
        final JiraSearch search = new JiraSearch(issueQuery);
        search.search();
        issues.putAll(search.getIssueList());
    }
}
