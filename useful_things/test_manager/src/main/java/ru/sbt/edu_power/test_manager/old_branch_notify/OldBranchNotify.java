package ru.sbt.edu_power.test_manager.old_branch_notify;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import ru.sbt.edu_power.external_services.bitbucket.BBConnection;
import ru.sbt.edu_power.external_services.bitbucket.Branch;
import ru.sbt.edu_power.external_services.bitbucket.models.BranchListJiraIssuesModel;
import ru.sbt.edu_power.external_services.bitbucket.models.BranchModel;
import ru.sbt.edu_power.external_services.jira.agile.JiraSearch;
import ru.sbt.edu_power.external_services.jira.agile.model.IssueFields;
import ru.sbt.edu_power.external_services.jira.agile.model.IssueQuery;
import ru.sbt.edu_power.external_services.mattermost.Regressman;
import ru.sbt.edu_power.test_manager.Environment;
import ru.sbt.edu_power.test_manager.old_branch_notify.validations.ClosedPullRequests;
import ru.sbt.edu_power.test_manager.old_branch_notify.validations.VeryOldBranches;
import ru.sbt.edu_power.test_manager.old_branch_notify.validations.WithoutPullRequestAndTaskIsClosed;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class OldBranchNotify {
    private final Map<String, IssueFields> issues = new HashMap<>();
    public static final int MAX_DAYS = 30;

    public OldBranchNotify() {
        Assert.assertNotNull("Не указан репозиторий", Environment.REPOSITORY);
    }

    public void evaluate() {
        final Pattern pattern = Pattern.compile("^(f/|bug/)?[a-zA-Z][a-zA-Z0-9]{1,4}-\\d{1,6}");
        final List<Branch> branchList = BBConnection.getBranches(Environment.REPOSITORY.getRepo(), "")
                                                    .stream()
                                                    .filter(bm -> pattern.matcher(bm.getDisplayId()).find())
                                                    .map(bm -> {
                                                        try {
                                                            return new Branch(bm, Environment.REPOSITORY.getRepo(), MAX_DAYS, issues);
                                                        } catch (final Throwable e) {
                                                            log.error(
                                                                    "Ошибка при получении данных из ветки {}",
                                                                    bm.getDisplayId(),
                                                                    e
                                                            );
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

        final ClosedPullRequests closedPullRequests = new ClosedPullRequests(
                oldBranches,
                MAX_DAYS,
                Environment.REPOSITORY.getChannel()
        );
        log.info("Выполнена нотификация по смерженным PR");

        collectIssues(oldBranches);
        log.info("Собрал информацию по таскам: {} записей", issues.size());

        Regressman.getInstance().sendPostByChannelName(Environment.REPOSITORY.getChannel(), "-----");

        final WithoutPullRequestAndTaskIsClosed withoutPullRequestAndTaskIsClosed = new WithoutPullRequestAndTaskIsClosed(
                issues,
                branchList,
                MAX_DAYS,
                Environment.REPOSITORY.getChannel()
        );
        log.info("Выполнена нотификация по закрытым задачам");

        Regressman.getInstance().sendPostByChannelName(Environment.REPOSITORY.getChannel(), "-----");

        final VeryOldBranches veryOldBranches = new VeryOldBranches(issues, oldBranches, MAX_DAYS, Environment.REPOSITORY.getChannel());
        log.info("Выполнена нотификация по очень старым веткам");

        Regressman.getInstance().sendPostByChannelName(Environment.REPOSITORY.getChannel(), "-----");

        final String message = String.format(
                "Всего получено веток из репозитория *%s*: %s\n" +
                "Ветки с закрытыми PR: %s\n" +
                "Ветки с закрытыми задачами: %s\n" +
                "Очень старые ветки: %s",
                branchList.get(0).getRepo().getRepoName(),
                totalBranches,
                closedPullRequests.getCounter(),
                withoutPullRequestAndTaskIsClosed.getCounter(),
                veryOldBranches.getCounter()
        );
        Regressman.getInstance().sendPostByChannelName(Environment.REPOSITORY.getChannel(), message);
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
