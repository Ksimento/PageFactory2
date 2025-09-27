package ru.sbt.edu_power.assist_bot.tasks.releases.old_branch_check.validations;

import com.slack.api.model.Attachment;
import com.slack.api.model.User;
import ru.sbt.edu_power.assist_bot.tasks.releases.old_branch_check.Branch;
import ru.sbt.edu_power.assist_bot.tasks.releases.old_branch_check.OldBranchCheckView;
import ru.sbt.edu_power.assist_bot.tasks.releases.old_branch_check.OldBranchDispatcher;
import ru.sbt.edu_power.external_services.jira.agile.model.IssueFields;
import ru.sbt.edu_power.external_services.jira.agile.model.enums.IssueResolution;
import ru.sbt.edu_power.assist_bot.slack.SlackClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

public class WithoutPullRequestAndTaskIsClosed {
    private final OldBranchCheckView view;
    private final Map<String, IssueFields> issues;
    private final List<Branch> branchList;
    private final LongAdder closedTaskCounter = new LongAdder();

    public WithoutPullRequestAndTaskIsClosed(
            final OldBranchCheckView view,
            final Map<String, IssueFields> issues,
            final List<Branch> branchList
    ) {
        this.view = view;
        this.issues = issues;
        this.branchList = branchList;
        execute();
    }

    public long getCounter() {
        return closedTaskCounter.sum();
    }

    private void execute() {
        final List<Branch> closedTaskBranches = branchList
                .stream()
                .filter(this::taskIsClosed)
                .peek(b -> closedTaskCounter.increment())
                .collect(Collectors.toList());

        branchList.removeIf(closedTaskBranches::contains);

        // персональные оповещения
        final Map<User, List<Branch>> personalMessaging = new HashMap<>();
        closedTaskBranches
                .stream()
                .filter(Branch::isUserInSlackByCommit)
                .forEach(b -> {
                    final User user = b.getSlackUserByCommit();
                    if (!personalMessaging.containsKey(user)) {
                        personalMessaging.put(user, new ArrayList<>());
                    }
                    personalMessaging.get(user).add(b);
                });
        // удаляем из списка closedTaskBranches ветки по которым выполнены персональные оповещения
        closedTaskBranches.removeIf(Branch::isUserInSlackByCommit);
        personalMessaging.forEach(this::sendMessageToUserIfTaskIsClosed);

        // общее оповещение в чат
        sendMessageToChannelIfTaskIsClosed(closedTaskBranches);
    }

    private boolean taskIsClosed(final Branch branch) {
        final String taskKey = branch.getBranchModel().getMetadata().getBranchListJiraIssuesModel().get(0).getKey();
        if (!issues.containsKey(taskKey)) {
            return false;
        }
        final IssueResolution resolution = IssueResolution.getByValue(issues
                .get(taskKey)
                .get(IssueFields.Field.RESOLUTION));
        return resolution != IssueResolution.UNRESOLVED;
    }

    private void sendMessageToUserIfTaskIsClosed(final User user, final List<Branch> branchList) {
        if (branchList.isEmpty()) {
            return;
        }
        final List<String> messages = getUserBranchMessages(branchList);
        final String message = String.format(
                "%s\n" +
                "Разбери эти ветки и удали ненужные. Если ветка нужна - обнови её из мастера, чтобы она не считалась устаревшей.\n" +
                "Если ветка не твоя - попытайся найти хозяина или команду. Это можно сделать по номеру задачи в названии ветки.",
                String.join("\n", messages)
        );
        final String chatId = OldBranchDispatcher.TEST_ENV ? view
                .getSlackChannelSelectSection()
                .getAccessory()
                .getValue() : user.getId();
        final List<Attachment> attachments = Collections.singletonList(Attachment
                .builder()
                .text(message)
                .fallback(message)
                .color("#9B3429")
                .build());
        SlackClient.sendMessageWithAttachments(
                String.format(
                        "Я нашёл ветки в репозитории *%s* по которой задача уже закрыта, а последний коммит был от тебя больше %s дней назад:",
                        branchList.get(0).getRepo().getRepoName(),
                        OldBranchDispatcher.MAX_DAYS
                ),
                attachments,
                chatId,
                view.getUserId()
        );
    }

    private void sendMessageToChannelIfTaskIsClosed(final List<Branch> branchList) {
        final List<String> messages = getUserBranchMessages(branchList);
        if (messages.isEmpty()) {
            return;
        }
        final String message = String.format(
                "%s\n" +
                "Здесь перечислены все авторы, у кого адрес почты в слаке не совпадает с почтой в Atlassian сервисах. " +
                "Иначе нотификация выполнена персонально.\n" +
                "Перечисленным здесь авторам нужно удалить эти ветки, либо обновить их из мастера, что бы они не считались устаревшими.\n" +
                "Если по вашей ветке открыта другая задача - нужно исправить имя ветки.\n" +
                "Если автор более не работает с нами, просьба разобрать такие ветки коллегам по команде.",

                String.join("\n", messages)
        );
        final List<Attachment> attachments = Collections.singletonList(Attachment
                .builder()
                .text(message)
                .fallback(message)
                .color("#9B3429")
                .build());
        SlackClient.sendMessageWithAttachments(
                String.format(
                        "Список авторов последних коммитов в репозитории *%s* в ветки старше %s дней, задачи по которым были закрыты:",
                        branchList.get(0).getRepo().getRepoName(),
                        OldBranchDispatcher.MAX_DAYS
                ),
                attachments,
                view.getSlackChannelSelectSection().getAccessory().getValue(),
                view.getUserId()
        );
    }

    private List<String> getUserBranchMessages(final List<Branch> branchList) {
        return branchList.stream()
                         .map(b ->
                                 String.format(
                                         "*%s*: %s - <%s|%s> %s",
                                         b.getUserNameByCommit(),
                                         b.getBranchModel().getDisplayId(),
                                         b.getFirstTaskLink(),
                                         b.getFirstTaskKey(),
                                         Objects.isNull(issues.get(b.getFirstTaskKey())) ? "Задача не найдена" : issues
                                                 .get(b.getFirstTaskKey())
                                                 .get(IssueFields.Field.STATUS)
                                 )
                         )
                         .collect(Collectors.toList());
    }
}
