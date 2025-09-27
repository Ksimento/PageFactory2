package ru.sbt.edu_power.test_manager.old_branch_notify.validations;

import lombok.extern.slf4j.Slf4j;
import net.bis5.mattermost.model.PostType;
import ru.sbt.edu_power.external_services.bitbucket.Branch;
import ru.sbt.edu_power.external_services.mattermost.Regressman;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

@Slf4j
public class ClosedPullRequests {
    private final List<Branch> branchList;
    private final LongAdder closedPullRequestCounter = new LongAdder();
    private final int maxDays;
    private final String channelName;

    public ClosedPullRequests(
            final List<Branch> branchList,
            final int maxDays,
            final String channelName
    ) {
        this.branchList = branchList;
        this.maxDays = maxDays;
        this.channelName = channelName;
        check();
    }

    public long getCounter() {
        return closedPullRequestCounter.sum();
    }

    private void check() {
        final List<Branch> closedPullRequestBranches = branchList
                .stream()
                .filter(Branch::hasPullRequest)
                .filter(Branch::pullRequestIsClosed)
                .peek(b -> closedPullRequestCounter.increment())
                .collect(Collectors.toList());

        // удаляем из branchList ветки, которые сейчас будут обработаны
        branchList.removeIf(closedPullRequestBranches::contains);

        // Отправка оповещений в общий чат
        sendMessageToChannelIfPullRequestBeenClosed(closedPullRequestBranches);
    }

    private void sendMessageToChannelIfPullRequestBeenClosed(final List<Branch> branchList) {
        final List<String> messages = getUserBranchMessages(branchList);
        if (messages.isEmpty()) {
            return;
        }
        sendPersonalMessage(
                branchList,
                String.format(
                        "Вам необходимо удалить указанные ветки, либо обновить их из мастера, что бы они не считались устаревшими \n" +
                                "Указанные PR находятся в репозитории %s, они уже закрыты, но ветки не были удалены более %s дней:\n",
                        branchList.get(0).getRepo().getRepoName(),
                        maxDays
                ));
        final String branchMessage = String.format(
                "%s\n" +
                        "Перечисленным здесь авторам нужно удалить эти ветки, либо обновить их из мастера, что бы они не считались устаревшими.\n" +
                        "Если автор PR более не работает с нами, просьба разобрать такие ветки коллегам по команде.",
                String.join("\n", messages)
        );
        final String message = String.format(
                "Список авторов PR в репозитории *%s*, которые уже закрыты, но ветки не были удалены более %s дней:",
                branchList.get(0).getRepo().getRepoName(),
                maxDays
        );
        Regressman.getInstance().sendPostByChannelName(channelName, message);
        Regressman
                .getInstance()
                .sendPost(Regressman.getInstance().getChannelByName(channelName).getId(),
                        branchMessage,
                        PostType.SLACK_ATTACHMENT
                );
    }

    private void sendPersonalMessage(final List<Branch> branchList, String header) {
        Map<String, String> mailingList = new HashMap<>();
        branchList.forEach(branch -> {
            mailingList.put(
                    branch.getUserName(),
                    mailingList.getOrDefault(branch.getUserName(), header) + createPersonalMessage(branch)
            );
        });
        mailingList.forEach((key, value) -> {
            Regressman.getInstance().sendMessageToMmByUserName(key, value);
        });
    }

    private List<String> getUserBranchMessages(final List<Branch> branchList) {
        Map<String, String> messageByTeam = new HashMap<>();
        branchList.forEach(b -> {
            String team = Regressman.getInstance().getUserTeamByUserName(b.getUserName());
            if (messageByTeam.containsKey(team)) {
                messageByTeam.put(team, messageByTeam.get(team) + createMessage(b));
            } else {
                messageByTeam.put(team, "\n" + team + ":" + createMessage(b));
            }
        });
        return new ArrayList<>(messageByTeam.values());
    }

    private String createPersonalMessage(Branch branch) {
        return String.format("\n[PR](%s). Статус: %s",
                branch.getPullRequestLink(),
                branch.getPullRequestStatus());
    }

    private String createMessage(Branch branch) {
        return String.format(
                "\n%s @%s : [PR](%s). Статус: %s",
                branch.getUserStatus(),
                branch.getUserName(),
                branch.getPullRequestLink(),
                branch.getPullRequestStatus());
    }
}
