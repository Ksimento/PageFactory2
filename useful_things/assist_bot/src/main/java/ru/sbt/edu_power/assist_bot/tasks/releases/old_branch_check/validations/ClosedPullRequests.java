package ru.sbt.edu_power.assist_bot.tasks.releases.old_branch_check.validations;

import com.slack.api.model.Attachment;
import com.slack.api.model.User;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.assist_bot.slack.SlackClient;
import ru.sbt.edu_power.assist_bot.tasks.releases.old_branch_check.Branch;
import ru.sbt.edu_power.assist_bot.tasks.releases.old_branch_check.OldBranchCheckView;
import ru.sbt.edu_power.assist_bot.tasks.releases.old_branch_check.OldBranchDispatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

@Slf4j
public class ClosedPullRequests {
    private final OldBranchCheckView view;
    private final List<Branch> branchList;
    private final LongAdder closedPullRequestCounter = new LongAdder();

    public ClosedPullRequests(
            final OldBranchCheckView view,
            final List<Branch> branchList
    ) {
        this.view = view;
        this.branchList = branchList;
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

        // Отправка персональных оповещений
        final Map<User, List<Branch>> personalMessaging = new HashMap<>();
        closedPullRequestBranches
                .stream()
                // удаляем из oldBranches ветки, в которых закрыт PR
                .filter(Branch::isUserInSlackByPullRequest)
                .forEach(b -> {
                    if (!personalMessaging.containsKey(b.getSlackUserByPullRequest())) {
                        personalMessaging.put(b.getSlackUserByPullRequest(), new ArrayList<>());
                    }
                    personalMessaging.get(b.getSlackUserByPullRequest()).add(b);
                });

        personalMessaging.forEach(this::sendMessageToUserIfPullRequestBeenClosed);

        // удаляем из closedPullRequestBranches ветки, по которым выполнено персональное оповещение
        closedPullRequestBranches.removeIf(Branch::isUserInSlackByPullRequest);

        // Отправка оповещений в общий чат
        sendMessageToChannelIfPullRequestBeenClosed(closedPullRequestBranches);
    }

    @SneakyThrows
    private void sendMessageToUserIfPullRequestBeenClosed(final User user, final List<Branch> branchList) {
        if (branchList.isEmpty()) {
            return;
        }
        final List<String> messages = branchList.stream()
                                                .map(branch ->
                                                        String.format(
                                                                "*%s*: <%s|PR %s>, последний коммит в ветку от имени %s.",
                                                                branch.getUserNameByPullRequest(),
                                                                branch.getPullRequestLink(),
                                                                branch.getPullRequestStatus(),
                                                                branch.getUserNameByCommit()
                                                        )
                                                )
                                                .collect(Collectors.toList());
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
                        "Я нашёл ветки в репозитории *%s* PR по которым закрыт, но ветки не удалены более %s дней:",
                        branchList.get(0).getRepo().getRepoName(),
                        OldBranchDispatcher.MAX_DAYS
                ),
                attachments,
                chatId,
                view.getUserId()
        );
    }

    private void sendMessageToChannelIfPullRequestBeenClosed(final List<Branch> branchList) {
        final List<String> messages = new ArrayList<>();
        branchList.forEach(b -> {
            final String message = String.format(
                    "*%s*: <%s|PR>. Статус: %s",
                    b.getUserNameByCommit(),
                    b.getPullRequestLink(),
                    b.getPullRequestStatus()
            );
            messages.add(message);
        });
        if (messages.isEmpty()) {
            return;
        }
        final String message = String.format(
                "%s\n" +
                "Здесь перечислены все авторы, у кого адрес почты в слаке не совпадает с почтой в Atlassian сервисах. " +
                "Иначе нотификация выполнена персонально.\n" +
                "Перечисленным здесь авторам нужно удалить эти ветки, либо обновить их из мастера, что бы они не считались устаревшими.\n" +
                "Если автор PR более не работает с нами, просьба разобрать такие ветки коллегам по команде.",
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
                        "Список авторов PR в репозитории *%s*, которые уже закрыты, но ветки не были удалены более %s дней:",
                        branchList.get(0).getRepo().getRepoName(),
                        OldBranchDispatcher.MAX_DAYS
                ),
                attachments,
                view.getSlackChannelSelectSection().getAccessory().getValue(),
                view.getUserId()
        );
    }
}
