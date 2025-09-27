package ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.issue;

import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.jira.JiraConnect;
import ru.sbt.edu_power.external_services.jira.agile.JiraSearch;
import ru.sbt.edu_power.external_services.jira.agile.JiraVersion;
import ru.sbt.edu_power.external_services.jira.agile.model.IssueFields;
import ru.sbt.edu_power.external_services.jira.agile.model.IssueQuery;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraUser;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraVersionModel;
import ru.sbt.edu_power.external_services.jira.agile.model.enums.IssueStatus;
import ru.sbt.edu_power.external_services.jira.agile.model.enums.IssueType;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;
import ru.sbt.edu_power.assist_bot.services.slice.ISlice;
import ru.sbt.edu_power.assist_bot.services.slice.Slice;
import ru.sbt.edu_power.external_services.version_releases.JiraVersionTracker;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Срез данных по задачам в статусах IFT, Need Test и In QA по всем RC в указанной версии
@Slf4j
public class IssueSlice extends Slice<IssueFields> {
    private static final long serialVersionUID = 335676885194952880L;
    private final HashMap<String, IssueFields> keyToFieldsMap = new HashMap<>();
    private final Map<JiraUser, Map<String, List<IssueFields>>> userToVersionToIssueList = new HashMap<>();
    private final LocalDateTime dateTime = LocalDateTime.now(ZoneId.of("Europe/Moscow"));
    private final TCFields.ProjectId project;
    private final JiraVersionModel version;

    public IssueSlice(
            final TCFields.ProjectId project,
            final JiraVersionModel version
    ) {
        this.project = project;
        this.version = version;
    }

    @Override
    public void load() {
        final IssueQuery issueQuery = new IssueQuery(0, 100);
        // получаем данные по всем rc внутри основной версии приложения
        final String[] versionList = new JiraVersionTracker(project)
                .getAllRc(version)
                .stream()
                .map(JiraVersionModel::getName)
                .toArray(String[]::new);

        issueQuery
                .and(
                        IssueFields.Field.ISSUE_TYPE,
                        IssueQuery.Op.IN,
                        IssueType.BUG.getValue(),
                        IssueType.INCIDENT.getValue()
                )
                .and(IssueFields.Field.PROJECT, IssueQuery.Op.IN, project.name())
                .and(IssueFields.Field.FIX_VERSIONS, IssueQuery.Op.IN, versionList)
                .and(
                        IssueFields.Field.STATUS,
                        IssueQuery.Op.IN,
                        IssueStatus.NEED_TEST.getValue(),
                        IssueStatus.IN_QA.getValue(),
                        IssueStatus.IFT.getValue()
                );
        final JiraSearch jiraSearch = new JiraSearch(issueQuery);
        jiraSearch.search();
        keyToFieldsMap.putAll(jiraSearch.getIssueList());
        addAll(keyToFieldsMap.values());
    }

    @Override
    public void optimize(final ISlice<IssueFields> previous) {
        new HashMap<>(keyToFieldsMap).forEach((key, e) ->
                previous.forEach(pe -> {
                    if (e.equals(pe)) {
                        keyToFieldsMap.put(key, pe);
                    }
                })
        );
        clear();
        addAll(keyToFieldsMap.values());
    }

    public HashMap<String, IssueFields> getKeyToFieldsMap() {
        return keyToFieldsMap;
    }

    public IssueFields get(final String key) {
        return keyToFieldsMap.get(key);
    }

    public void sortByUser() {
        for (final IssueFields fields : keyToFieldsMap.values()) {
            final JiraUser user = JiraConnect.jiraGetUser(fields.get(IssueFields.Field.ASSIGNEE));
            if (!userToVersionToIssueList.containsKey(user)) {
                userToVersionToIssueList.put(user, new HashMap<>());
            }
            final String fixVersion = fields.get(IssueFields.Field.FIX_VERSIONS);
            final List<String> versions = Objects.isNull(fixVersion) ? new ArrayList<>() :
                    Stream.of(fixVersion.split(","))
                          .map(String::trim)
                          .collect(Collectors.toList());
            versions.forEach(v -> {
                if (!userToVersionToIssueList.get(user).containsKey(v)) {
                    userToVersionToIssueList.get(user).put(v, new ArrayList<>());
                }
                userToVersionToIssueList.get(user).get(v).add(fields);
            });
        }
    }

    public Map<JiraUser, List<IssueFields>> getUserToIssueListByVersions(final List<JiraVersionModel> versions) {
        final Map<JiraUser, List<IssueFields>> map = new HashMap<>();
        userToVersionToIssueList.forEach((user, data) -> {
            for (final JiraVersionModel version : versions) {
                if (Objects.nonNull(data.get(version.getName()))) {
                    map.put(user, data.get(version.getName()));
                }
            }
        });
        return map;
    }

    public List<JiraVersionModel> getVersionsBefore() {
        final List<String> versionsBefore = new JiraVersionTracker(project)
                .getVersionsBeforeRc(version)
                .stream()
                .map(JiraVersionModel::getName)
                .collect(Collectors.toList());
        return sortByVersions(versionsBefore);
    }

    public List<JiraVersionModel> getVersionsAfter() {
        final List<String> versionsAfter = new JiraVersionTracker(project)
                .getVersionsAfterRc(version)
                .stream()
                .map(JiraVersionModel::getName)
                .collect(Collectors.toList());
        return sortByVersions(versionsAfter);
    }

    private List<JiraVersionModel> sortByVersions(final List<String> versions) {
        final JiraVersion jiraVersion = new JiraVersion();
        return stream()
                       .map(isf -> isf.get(IssueFields.Field.FIX_VERSIONS))
                       .filter(Objects::nonNull)
                       .flatMap(v -> Stream.of(v.split(",")))
                       .map(String::trim)
                       .filter(versions::contains)
                       .distinct()
                       .map(v -> jiraVersion.getJiraVersionByName(project.id, v))
                       .collect(Collectors.toList());
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public JiraVersionModel getVersion() {
        return version;
    }

    @Override
    public void update(final Iterable<IssueFields> dataForUpdate) {
        dataForUpdate.forEach(issue -> keyToFieldsMap.put(issue.get(IssueFields.Field.KEY), issue));
        clear();
        addAll(keyToFieldsMap.values());
    }
}
