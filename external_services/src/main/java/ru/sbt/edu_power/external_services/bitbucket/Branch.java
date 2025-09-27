package ru.sbt.edu_power.external_services.bitbucket;

import lombok.extern.slf4j.Slf4j;
import net.bis5.mattermost.model.User;
import ru.sbt.edu_power.external_services.PropReader;
import ru.sbt.edu_power.external_services.bitbucket.models.BBRepos;
import ru.sbt.edu_power.external_services.bitbucket.models.BranchModel;
import ru.sbt.edu_power.external_services.bitbucket.models.GitUserModel;
import ru.sbt.edu_power.external_services.bitbucket.models.LatestCommitMetadata;
import ru.sbt.edu_power.external_services.bitbucket.models.OutgoingPullRequestMetadataModel;
import ru.sbt.edu_power.external_services.jira.JiraConnect;
import ru.sbt.edu_power.external_services.jira.JiraConnectionException;
import ru.sbt.edu_power.external_services.jira.agile.model.IssueFields;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraUser;
import ru.sbt.edu_power.external_services.mattermost.MattermostUsers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class Branch {
    private final BranchModel branch;
    private static final LocalDateTime NOW = LocalDateTime.now();
    private final long maxDays;
    private final BBRepos repo;
    private final LatestCommitMetadata commit;
    private final OutgoingPullRequestMetadataModel.PullRequest pr;
    private final GitUserModel commitUser;
    private final GitUserModel prUser;
    private final Map<String, IssueFields> issues;
    private JiraUser taskDeveloper;

    public Branch(final BranchModel branch, final BBRepos repo, final long maxDays, final Map<String, IssueFields> issues) {
        this.branch = branch;
        this.repo = repo;
        this.issues = issues;
        commit = branch.getMetadata().getLatestCommitMetadata();
        pr = Objects.isNull(branch.getMetadata().getOutgoingPullRequestMetadataModel()) ? null :
                branch.getMetadata()
                      .getOutgoingPullRequestMetadataModel()
                      .getPullRequest();
        commitUser = commit.getCommitter();
        prUser = Objects.nonNull(pr) ? pr.getAuthor().getUser() : null;
        this.maxDays = maxDays;
    }

    public BranchModel getBranchModel() {
        return branch;
    }

    public BBRepos getRepo() {
        return repo;
    }

    public boolean isUserInSlackByCommit() {
        final JiraUser jiraUser = getDeveloper();
        if (Objects.nonNull(jiraUser)) {
            return Objects.nonNull(MattermostUsers.getInstance().getUserByEmail(jiraUser.getEmailAddress()));
        }
        final User user = getSlackUserByCommit();
        return Objects.nonNull(user) && user.getDeleteAt() == 0;
    }

    private JiraUser getDeveloper() {
        if (Objects.nonNull(getTaskDeveloper())) {
            return getTaskDeveloper();
        }
        if (issues.containsKey(getFirstTaskKey())) {
            final String userName = issues.get(getFirstTaskKey()).get(IssueFields.Field.DEVELOPER);
            if (Objects.nonNull(userName)) {
                final JiraUser jiraUser = JiraConnect.jiraGetUser(userName);
                if (!jiraUser.isFakeUser()) {
                    setTaskDeveloper(jiraUser);
                    return jiraUser;
                }
            }
        }
        return null;
    }

    public boolean isUserInSlackByPullRequest() {
        final User user = getSlackUserByPullRequest();
        return Objects.nonNull(user) && user.getDeleteAt() == 0;
    }

    public User getSlackUserByCommit() {
        final JiraUser jiraUser = getDeveloper();
        if (Objects.nonNull(jiraUser)) {
            return MattermostUsers.getInstance().getUserByEmail(jiraUser.getEmailAddress());
        }
        return MattermostUsers.getInstance().getUserByEmail(
                commitUser.getEmailAddress()
        );
    }

    public User getSlackUserByPullRequest() {
        return MattermostUsers.getInstance().getUserByEmail(
                prUser.getEmailAddress()
        );
    }

    public String getUserNameByCommit() {
        final JiraUser developer = getDeveloper();
        if (Objects.nonNull(developer)) {
            return developer.getDisplayName();
        }
        return Objects.isNull(commitUser.getDisplayName()) ?
                commitUser.getName() :
                commitUser.getDisplayName();
    }

    public String getUserName() {
        final JiraUser developer = getDeveloper();
        if (Objects.nonNull(developer)) {
            return developer.getName();
        }
        if (Objects.nonNull(prUser)){
            return prUser.getName();
        }
        return Objects.isNull(commitUser.getName()) ?
                commitUser.getDisplayName() :
                commitUser.getName();
    }

    public String getEmailAddress(){
        final JiraUser jiraUser = getDeveloper();
        if (Objects.nonNull(jiraUser)) {
            return jiraUser.getEmailAddress();
        }
        if (Objects.nonNull(prUser)){
            prUser.getEmailAddress();
        }
        if (Objects.nonNull(commitUser)){
            return commitUser.getEmailAddress();
        }
        return null;
    }

    public String getPullRequestLink() {
        return PropReader.get("bb.api.pull-request.link")
                         .replace("{PROJECT}", repo.getProject().name())
                         .replace("{REPO}", repo.getRepoName())
                         .replace(
                                 "{PR_ID}",
                                 String.valueOf(pr.getId())
                         );
    }

    public String getUserStatus(){
        JiraUser user;
        try {
            user = JiraConnect.jiraGetUser(getUserName());
        } catch (JiraConnectionException e){
            return "(пользователь не найден в Jira)";
        }
        return user.isActive() ? "" : "(inactive)";
    }

    public String getPullRequestStatus() {
        return pr.getState();
    }

    public boolean isOld() {
        final LocalDateTime commitDate = LocalDateTime.ofEpochSecond(
                commit.getCommitterTimestamp() / 1000,
                0,
                ZoneOffset.UTC
        );
        return Duration.between(commitDate, NOW).toDays() > maxDays;
    }

    public boolean isVeryOld() {
        final LocalDateTime commitDate = LocalDateTime.ofEpochSecond(
                commit.getCommitterTimestamp() / 1000,
                0,
                ZoneOffset.UTC
        );
        return Duration.between(commitDate, NOW).toDays() > maxDays * 2;
    }

    public boolean hasPullRequest() {
        return Objects.nonNull(pr);
    }

    public boolean pullRequestIsClosed() {
        return pr.isClosed();
    }

    public String getUserNameByPullRequest() {
        return Objects.isNull(prUser.getDisplayName()) ?
                prUser.getName() :
                prUser.getDisplayName();
    }

    public boolean hasTask() {
        return !branch.getMetadata().getBranchListJiraIssuesModel().isEmpty();
    }

    public String getFirstTaskLink() {
        if (branch.getMetadata().getBranchListJiraIssuesModel().isEmpty()) {
            return "";
        }
        return branch.getMetadata().getBranchListJiraIssuesModel().get(0).getUrl();
    }

    public String getFirstTaskKey() {
        if (branch.getMetadata().getBranchListJiraIssuesModel().isEmpty()) {
            return "";
        }
        return branch.getMetadata().getBranchListJiraIssuesModel().get(0).getKey();
    }

    public JiraUser getTaskDeveloper() {
        return taskDeveloper;
    }

    public void setTaskDeveloper(final JiraUser taskDeveloper) {
        this.taskDeveloper = taskDeveloper;
    }
}
