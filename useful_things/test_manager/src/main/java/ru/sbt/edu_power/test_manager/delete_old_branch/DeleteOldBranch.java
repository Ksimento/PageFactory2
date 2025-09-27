package ru.sbt.edu_power.test_manager.delete_old_branch;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.bitbucket.BBConnection;
import ru.sbt.edu_power.external_services.bitbucket.Branch;
import ru.sbt.edu_power.external_services.bitbucket.models.BBRepos;
import ru.sbt.edu_power.external_services.bitbucket.models.BranchListJiraIssuesModel;
import ru.sbt.edu_power.external_services.bitbucket.models.BranchModel;
import ru.sbt.edu_power.external_services.jira.agile.JiraSearch;
import ru.sbt.edu_power.external_services.jira.agile.model.IssueFields;
import ru.sbt.edu_power.external_services.jira.agile.model.IssueQuery;
import ru.sbt.edu_power.external_services.jira.agile.model.enums.IssueStatus;
import ru.sbt.edu_power.external_services.mattermost.Regressman;
import ru.sbt.edu_power.test_manager.old_branch_notify.BBReposToChannelEnum;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class DeleteOldBranch {
    private static final int MAX_DAYS = 40;
    private static final int MAX_PERIOD_LIFE_DAYS = 70;
    private final Map<String, IssueFields> issues = new HashMap<>();
    private final Pattern pattern = Pattern.compile("^(f/|bug/)?[a-zA-Z][a-zA-Z0-9]{1,4}-\\d{1,6}");
    private static final LocalDateTime NOW = LocalDateTime.now();


    /**
     * Перебираем все доступные репозитории
     */
    public void clearOldBranchRepository() {
        Arrays.stream(BBReposToChannelEnum.values()).forEach(
                bbReposToChannelEnum -> {
                    loadBranch(bbReposToChannelEnum.getRepo(), bbReposToChannelEnum.getChannel());
                }
        );
    }

    /**
     * Находим в репозитории ветки в статусе MERGED или DECLINE,
     * проверяем что у привязанных к ним задач статус Resolved, и отправляем запрос на удаление
     *
     * @param repos - репозиторий в котором ищем ветки подходящие под условия удаления
     */
    private void loadBranch(final BBRepos repos, final String channel) {
        final List<BranchModel> branchModelList = BBConnection.getBranches(repos, "");

        final List<Branch> branchList = branchModelList.stream()
                                                       .filter(bm -> pattern.matcher(bm.getDisplayId()).find())
                                                       .map(bm -> {
                                                           try {
                                                               return new Branch(bm, repos, MAX_DAYS, issues);
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
                                                       .filter(branch ->
                                                               {
                                                                   if (Objects.nonNull(branch.getBranchModel().getMetadata())) {
                                                                       if (Objects.nonNull(branch
                                                                               .getBranchModel()
                                                                               .getMetadata()
                                                                               .getOutgoingPullRequestMetadataModel())) {
                                                                           if (Objects.nonNull(branch
                                                                                   .getBranchModel()
                                                                                   .getMetadata()
                                                                                   .getOutgoingPullRequestMetadataModel()
                                                                                   .getPullRequest())) {
                                                                               if (branch
                                                                                       .getBranchModel()
                                                                                       .getMetadata()
                                                                                       .getOutgoingPullRequestMetadataModel()
                                                                                       .getPullRequest()
                                                                                       .isClosed()) {
                                                                                   return true;
                                                                               }
                                                                           }
                                                                       }
                                                                   }
                                                                   return false;
                                                               }
                                                       )
                                                       .collect(Collectors.toList());

        collectIssues(branchList);
        final List<Branch> willDeleteInFuture = new ArrayList<>();
        final List<Branch> deleteTodayBranch = new ArrayList<>();

        branchList.stream()
                  .filter(branch -> Objects.nonNull(branch.getFirstTaskKey()))
                  .filter(branch -> {
                              try {
                                  return issues.get(branch.getFirstTaskKey())
                                               .get(IssueFields.Field.STATUS).equals(IssueStatus.RESOLVED.getValue());
                              } catch (final Exception e) {
                                  return false;
                              }
                          }
                  )
                  .forEach(branch ->
                          {
                              if (deleteBranch(repos, branch)) {
                                  deleteTodayBranch.add(branch);
                              } else {
                                  willDeleteInFuture.add(branch);
                              }
                          }
                  );
        sendMessage(
                repos,
                getUserBranchMessages(willDeleteInFuture, true),
                getUserBranchMessages(deleteTodayBranch, false),
                channel
        );
    }

    /**
     * Выгружаем информацию по задачам из переданного списка веток
     *
     * @param branchList
     */
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
                  );
        final JiraSearch search = new JiraSearch(issueQuery);
        search.search();
        issues.putAll(search.getIssueList());
    }

    /**
     * Выполняем отправку уведомлений о сроках удаления веток по тематическим каналам
     */
    private void sendMessage(
            final BBRepos repos,
            final List<String> willDeleteInFuture,
            final List<String> deleteTodayBranch,
            final String channel
    ) {
        if (!willDeleteInFuture.isEmpty()) {
            final String headerMessageWillDeleteInFuture =
                    String.format("### **Список веток из репозитория %s с последним коммитом старше %s дней:**",
                            repos.getRepoName(), MAX_DAYS
                    );
            Regressman
                    .getInstance()
                    .sendPostByChannelName(
                            channel,
                            headerMessageWillDeleteInFuture
                    );
            Regressman
                    .getInstance()
                    .sendPostByChannelName(
                            channel,
                            String.join("\n", willDeleteInFuture)
                    );
        }

        if (!deleteTodayBranch.isEmpty()) {
            final String headerMessageDeleteTodayBranch =
                    String.format(
                            "### **Данные ветки были удалены из репозитория %s:**",
                            repos.getRepoName()
                    );
            Regressman
                    .getInstance()
                    .sendPostByChannelName(
                            channel,
                            headerMessageDeleteTodayBranch
                    );
            Regressman
                    .getInstance()
                    .sendPostByChannelName(
                            channel,
                            String.join("\n", deleteTodayBranch)
                    );
        }
    }

    private boolean deleteBranch(final BBRepos repos, final Branch branch) {
        final LocalDateTime commitDate = LocalDateTime.ofEpochSecond(
                branch.getBranchModel().getMetadata().getLatestCommitMetadata().getCommitterTimestamp() / 1000,
                0,
                ZoneOffset.UTC
        );
        if (Duration.between(commitDate, NOW).toDays() >= MAX_PERIOD_LIFE_DAYS) {
            HttpResponse<JsonNode> response = BBConnection.deleteBranch(
                    repos,
                    branch.getBranchModel().getDisplayId()
            );
            if (response.getStatus() != 204){
                log.error(String.format(
                        "Не удалось удалить ветку %s из репозитория %s",
                        branch.getBranchModel().getDisplayId(),
                        repos.getRepoName()
                ));
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Генерируем список строк с сообщениями по каждой ветке с разбивкой по командам
     *
     * @param branchList
     * @return
     */
    private List<String> getUserBranchMessages(final List<Branch> branchList, boolean flagDelete) {
        final Map<String, String> messageByTeam = new HashMap<>();
        branchList.forEach(branch -> {
            String team = Regressman.getInstance().getUserTeamByUserName(branch.getUserName());
            if (messageByTeam.containsKey(team)) {
                messageByTeam.put(team, messageByTeam.get(team) + createMessage(branch, flagDelete));
            } else {
                messageByTeam.put(team, "\n" + team + ":" + createMessage(branch, flagDelete));
            }
        });
        return new ArrayList<>(messageByTeam.values());
    }

    /**
     * Формируем текст сообщения по конкретной ветке
     *
     * @param branch
     * @param flagDelete
     * @return
     */
    private String createMessage(Branch branch, boolean flagDelete) {
        calculatingDateDeleteBranch(branch);
        return String.format(
                "\n%s @%s : [PR](%s). Статус: %s %s - ветка будет удалена после указанной даты",
                branch.getUserStatus(),
                branch.getUserName(),
                branch.getPullRequestLink(),
                branch.getPullRequestStatus(),
                calculatingDateDeleteBranch(branch)
        );
    }

    /**
     * Рассчитываем примерную дату удаления, по истечении 70 дней с последнего коммита
     *
     * @param branch
     * @return
     */
    private String calculatingDateDeleteBranch(Branch branch) {
        final LocalDateTime commitDate = LocalDateTime.ofEpochSecond(
                branch.getBranchModel().getMetadata().getLatestCommitMetadata().getCommitterTimestamp() / 1000,
                0,
                ZoneOffset.UTC
        );
        return commitDate.plusDays(MAX_PERIOD_LIFE_DAYS).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }
}
