package ru.sbt.edu_power.external_services.version_releases;

import ru.sbt.edu_power.external_services.jira.agile.JiraSearch;
import ru.sbt.edu_power.external_services.jira.agile.model.IssueFields;
import ru.sbt.edu_power.external_services.jira.agile.model.IssueQuery;
import ru.sbt.edu_power.external_services.jira.agile.model.enums.IssueStatus;
import ru.sbt.edu_power.external_services.jira.agile.model.enums.IssueType;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

// Загрузчик релизных задач из джиры.
public class Releases {
    private final List<Release> releases = new ArrayList<>();
    // Релизные задачи разбитые по проектам
    private final Map<String, List<Release>> releaseByServiceMap = new HashMap<>();

    public Releases() {
        load();
        splitReleases();
    }

    public List<Release> getReleases() {
        return releases;
    }

    public Map<String, List<Release>> getReleaseByServiceMap() {
        return releaseByServiceMap;
    }

    public Optional<Release> searchRelease(final ReleaseProjectId projectId, final String version) {
        return releases.stream()
                .filter(r -> r.getReleaseProjectId() == projectId)
                .filter(r -> Objects.nonNull(r.getReleaseVersion()))
                .filter(r -> r.getReleaseVersion().toString().startsWith(version))
                .findFirst();
    }

    private void load() {
        final IssueQuery issueQuery = new IssueQuery(0, 20);
        issueQuery.and(IssueFields.Field.ISSUE_TYPE, IssueQuery.Op.EQUAL, IssueType.RELEASE_2_0.getValue())
                .setFields(
                        IssueFields.Field.CREATED.getFieldName(),
                        IssueFields.Field.STATUS.getFieldName(),
                        IssueFields.Field.FEATURE_FREEZE.getFieldName(),
                        IssueFields.Field.FIX_VERSIONS.getFieldName(),
                        IssueFields.Field.INSTALL_PLANE_DATE.getFieldName(),
                        IssueFields.Field.RELEASE_OBJECT.getFieldName(),
                        IssueFields.Field.SUMMARY.getFieldName(),
                        IssueFields.Field.RELEASE_TYPE.getFieldName(),
                        IssueFields.Field.RESOLVED.getFieldName()
                )
                .and(IssueFields.Field.PROJECT.getFieldName(), IssueQuery.Op.EQUAL, "SM")
                .and(IssueFields.Field.CREATED.getFieldName(), IssueQuery.Op.MORE_OR_EQUALS, "-120d");
        final JiraSearch search = new JiraSearch(issueQuery);
        search.search();
        final Date current = new Date();
        search.getIssueList()
                .forEach((key, data) -> {
                    final Release release = new Release(key, data);
                    // Убираем все отменённые релизы и релизы с неустановленным сервисом
                    if ("Отмена".equals(release.getStatus())) {
                        return;
                    }
                    // Убираем все релизы, которые были установлены более 7 дней назад
                    final Date resolved = release.getDateTime(IssueFields.Field.RESOLVED);

                    if (
                            Objects.nonNull(resolved)
                                    && release.getStatus().equals(IssueStatus.RELEASE_INSTALLED.getValue())
                                    && TimeUnit.MILLISECONDS.toDays(current.getTime() - resolved.getTime()) > 7
                    ) {
                        return;
                    }
                    releases.add(release);
                });
    }


    private void splitReleases() {
        releases.forEach(r -> {
            final String projectName = r.getProjectName();
            if (!releaseByServiceMap.containsKey(projectName)) {
                releaseByServiceMap.put(projectName, new ArrayList<>());
            }
            releaseByServiceMap.get(projectName).add(r);
        });
    }
}
