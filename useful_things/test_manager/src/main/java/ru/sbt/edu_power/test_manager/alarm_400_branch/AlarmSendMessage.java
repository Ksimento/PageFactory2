package ru.sbt.edu_power.test_manager.alarm_400_branch;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.sbt.edu_power.external_services.bitbucket.BBConnection;
import ru.sbt.edu_power.external_services.bitbucket.Branch;
import ru.sbt.edu_power.external_services.bitbucket.models.BBRepos;
import ru.sbt.edu_power.external_services.bitbucket.models.BranchListJiraIssuesModel;
import ru.sbt.edu_power.external_services.bitbucket.models.BranchModel;
import ru.sbt.edu_power.external_services.confluence.ConfluenceDocument;
import ru.sbt.edu_power.external_services.jira.agile.JiraSearch;
import ru.sbt.edu_power.external_services.jira.agile.model.IssueFields;
import ru.sbt.edu_power.external_services.jira.agile.model.IssueQuery;
import ru.sbt.edu_power.external_services.mattermost.Regressman;
import ru.sbt.edu_power.test_manager.old_branch_notify.BBReposToChannelEnum;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class AlarmSendMessage extends ConfluenceDocument {
    private static final String fileName = "branchesNotDeleted";
    private final Map<String, IssueFields> issues = new HashMap<>();
    private final Map<String, IssueFields> issuesTeams = new HashMap<>();
    private static final int MAX_DAYS = 30;
    private final Pattern pattern = Pattern.compile("^(f/|bug/)?[a-zA-Z][a-zA-Z0-9]{1,4}-\\d{1,6}");
    private final String channel = "teamleads";
    private int countBranch = 450;
    private final Map<String, String> teamAndLead = new HashMap<>();
    private final Map<String, List<String>> linkBranchByTeam = new TreeMap<>();

    public AlarmSendMessage() {
        super("67307271");
    }

    public void sendMessage() {
        Arrays.stream(BBReposToChannelEnum.values()).forEach(
                bbReposToChannelEnum -> {
                    final String message = checkNumberBranch(bbReposToChannelEnum.getRepo());
                    final String nameRepo = bbReposToChannelEnum.getRepo().getRepoName();
                    final String fileName = this.fileName + "(" + nameRepo + ")" + ".txt";
                    if (Objects.nonNull(message)) {
                        String repoMessage = "Внимание, в репозитории " +
                                             nameRepo +
                                             " открыто более " + countBranch + " веток";
                        createCsvFile(modifyMapToFile(linkBranchByTeam), fileName);
                        Regressman
                                .getInstance()
                                .sendPostMessageInThread(channel, repoMessage, message, new File(fileName).toPath());
                        log.info("Отправлено сообщение по репозиторию " + nameRepo);
                        linkBranchByTeam.clear();
                    }
                }
        );
    }

    /**
     * Если в репе больше 450 открытых веток, то выполняем оповещение об этом
     *
     * @param repos - репозиторий для выгрузки данных
     * @return
     */
    private String checkNumberBranch(BBRepos repos) {
        final List<BranchModel> branchModelList = BBConnection.getBranches(repos, "");
        if (branchModelList.size() >= countBranch) {
            final List<Branch> branchList = branchModelList
                    .stream()
                    /**
                     * TODO временно отключаем фильтр фича веток, т.к. у коллег возникают вопросы,
                     * почему количество веток в рассылках отличается от 450 штук
                     */
//                    .filter(bm -> pattern.matcher(bm.getDisplayId()).find())
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
                    .collect(Collectors.toList());
            createMapIssueAndTeam(branchList);
            return getUserBranchMessages(branchList).toString();
        }
        return null;
    }

    /**
     * Группируем ветки по командам и добавляем к ним теги тим лидов,
     * и формируем сообщение для отправки в канал
     *
     * @param branchList
     * @return
     */
    private StringBuilder getUserBranchMessages(final List<Branch> branchList) {
        final Map<String, Integer> branchByTeam = new TreeMap<>();
        branchList.forEach(b -> {
            String team;
            try {
                team = issuesTeams
                        .get(b.getBranchModel().getMetadata().getBranchListJiraIssuesModel().get(0).getKey())
                        .get(IssueFields.Field.TEAM);
            } catch (Exception e) {
                team = Regressman.getInstance().getUserTeamByUserName(b.getUserName());
            }

            if (Objects.isNull(team)) {
                team = Regressman.getInstance().getUserTeamByUserName(b.getUserName());
            }

            if (branchByTeam.containsKey(team)) {
                branchByTeam.put(team, branchByTeam.get(team) + 1);
            } else {
                branchByTeam.put(team, 1);
            }

            List<String> branchLink = new ArrayList<>();
            if (linkBranchByTeam.containsKey(team)) {
                branchLink.addAll(linkBranchByTeam.get(team));
            }
            branchLink.add(b.getBranchModel().getDisplayId() + "\n");
            linkBranchByTeam.put(team, branchLink);
        });
        createMapTeamAndLead();
        StringBuilder message = new StringBuilder();
        branchByTeam.forEach((k, v) -> {
            String lead = "";
            if (Objects.nonNull(teamAndLead.get(k))) {
                lead = "@" + teamAndLead.get(k);
            }
            message.append(k).append(" = ").append(v).append(" ").append(lead).append("\n");
        });
        return message;
    }

    /**
     * Формируем мапу со страницы https://confluence.pcbltools.ru/confluence/pages/viewpage.action?pageId=67307271
     * Ключ это команда, значение это лид команды
     */
    private void createMapTeamAndLead() {
        loadDocument();
        final Elements elementsTr = getDocument().body().getElementsByTag("tr");
        elementsTr.remove(0);
        for (Element element : elementsTr) {
            teamAndLead.put(
                    element.getElementsByTag("td").get(1).text(),
                    element.getElementsByTag("td").get(3).text()
            );
        }
    }

    /**
     * Из списка веток получаем список задач привязанных к этим веткам,
     * и по списку задач выполняем запрос на получение команды из задачи
     *
     * @param branchList - список веток
     */
    private void createMapIssueAndTeam(final List<Branch> branchList) {
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
        final IssueQuery issueQuery = new IssueQuery(0, 500);
        issueQuery.and(IssueFields.Field.ID, IssueQuery.Op.IN, keys)
                  .setFields(
                          IssueFields.Field.ID.getForQuery(),
                          IssueFields.Field.PROJECT.getForQuery(),
                          IssueFields.Field.RESOLUTION.getForQuery(),
                          IssueFields.Field.STATUS.getForQuery(),
                          IssueFields.Field.DEVELOPER.getFieldName(),
                          IssueFields.Field.TEAM.getFieldName()
                  )
        ;
        final JiraSearch search = new JiraSearch(issueQuery);
        search.search();
        issuesTeams.putAll(search.getIssueList());
    }

    private String modifyMapToFile(final Map<String, List<String>> linkBranchByTeam) {
        StringBuilder rez = new StringBuilder();
        for (String key : linkBranchByTeam.keySet()) {
            rez.append(key).append("\n");
            rez.append(linkBranchByTeam.get(key).toString()
                                       .replace("[", "")
                                       .replace("]", "")
                                       .replace(", ", "")).append("\n");
        }
        return String.valueOf(rez);
    }

    private void createCsvFile(final String commitNoBranch, final String fileName) {
        try (PrintWriter write = new PrintWriter(fileName)) {
            write.write(commitNoBranch);
        } catch (final IOException ignored) {

        }
    }
}
