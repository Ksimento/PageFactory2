package ru.sbt.edu_power.test_manager.old_branch_notify.validations;

import net.bis5.mattermost.model.PostType;
import ru.sbt.edu_power.external_services.bitbucket.Branch;
import ru.sbt.edu_power.external_services.jira.agile.model.IssueFields;
import ru.sbt.edu_power.external_services.mattermost.Regressman;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

public class VeryOldBranches {
    private final Map<String, IssueFields> issues;
    private final List<Branch> branchList;
    private final LongAdder veryOldBranchCounter = new LongAdder();
    private final int maxDays;
    private final String channelName;

    public VeryOldBranches(
            final Map<String, IssueFields> issues,
            final List<Branch> branchList,
            final int maxDays,
            final String channelName
    ) {
        this.issues = issues;
        this.branchList = branchList;
        this.maxDays = maxDays;
        this.channelName = channelName;
        execute();
    }

    public long getCounter() {
        return veryOldBranchCounter.sum();
    }

    private void execute() {
        final List<Branch> veryOldBranches = branchList
                .stream()
                .filter(Branch::isVeryOld)
                .peek(b -> veryOldBranchCounter.increment())
                .collect(Collectors.toList());

        branchList.removeIf(veryOldBranches::contains);

        sendMessageToChannelIfBranchIsVeryOld(veryOldBranches);
    }

    private void sendMessageToChannelIfBranchIsVeryOld(final List<Branch> branchList) {
        final List<String> messages = getUserBranchMessages(branchList);

        if (messages.isEmpty()) {
            return;
        }
        sendPersonalMessage(
                branchList,
                String.format(
                        "Вам необходимо удалить указанные ветки, либо обновить их из мастера, что бы они не считались устаревшими \n" +
                                "Перечисленные ветки находятся в репозитории %s, ветки старше %s дней (очень старые ветки):\n",
                        branchList.get(0).getRepo().getRepoName(),
                        maxDays * 2
                ));
        final String branchMessage = String.format(
                "%s\n" +
                        "Перечисленным здесь авторам нужно удалить эти ветки, либо обновить их из мастера, что бы они не считались устаревшими.\n" +
                        "Если автор более не работает с нами, просьба разобрать такие ветки коллегам по команде.",
                String.join("\n", messages)
        );
        final String message = String.format(
                "Список авторов последних коммитов в репозитории *%s* в ветки старше *%s* дней (очень старые ветки):",
                branchList.get(0).getRepo().getRepoName(),
                maxDays * 2
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
        return String.format("\n%s - [%s](%s) %s",
                branch.getBranchModel().getDisplayId(),
                branch.getFirstTaskKey(),
                branch.getFirstTaskLink(),
                Objects.isNull(issues.get(branch.getFirstTaskKey())) ? "Задача не найдена" : issues
                        .get(branch.getFirstTaskKey())
                        .get(IssueFields.Field.STATUS));
    }

    private String createMessage(Branch branch) {
        return String.format(
                "\n%s @%s : %s - [%s](%s) %s",
                branch.getUserStatus(),
                branch.getUserName(),
                branch.getBranchModel().getDisplayId(),
                branch.getFirstTaskKey(),
                branch.getFirstTaskLink(),
                Objects.isNull(issues.get(branch.getFirstTaskKey())) ? "Задача не найдена" : issues
                        .get(branch.getFirstTaskKey())
                        .get(IssueFields.Field.STATUS)
        );
    }
}
