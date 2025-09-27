package ru.sbt.edu_power.external_services.jira.agile;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import ru.sbt.edu_power.external_services.jira.JiraConnect;
import ru.sbt.edu_power.external_services.jira.JiraConnectionException;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraVersionModel;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Класс выгружает из Jira версии в виде списков или выполняет поиск по названию или ID версии
 */
public class JiraVersion {

    public List<JiraVersionModel> getNotReleasedVersions(final Integer projectId) {
        final List<JiraVersionModel> list = getVersions(projectId)
                .stream()
                .filter(v -> !v.isArchived())
                .sorted()
                .collect(Collectors.toList());
        final int size = Math.min(40, list.size());
        return list.subList(list.size() - size, list.size());
    }

    public JiraVersionModel getJiraVersionById(final Integer projectId, final String versionId) {
        return getVersions(projectId)
                .stream()
                .filter(v -> v.getId().equals(versionId))
                .findFirst()
                .orElseThrow(() -> new JiraConnectionException("Не найдена версия с ID " + versionId));
    }

    public JiraVersionModel getJiraVersionByName(final Integer projectId, final String name) {
        return getVersions(projectId).stream()
                                     .filter(v -> name.equals(v.getName()))
                                     .findFirst()
                                     .orElse(null);
    }

    private List<JiraVersionModel> getVersions(final Integer projectId) {
        final HttpResponse<JsonNode> response = JiraConnect.getJiraVersions(projectId.toString());
        final List<JiraVersionModel> versions = new ArrayList<>();
        for (final Object object : response.getBody().getArray()) {
            final JiraVersionModel version = JiraConnect.GSON.fromJson(object.toString(), JiraVersionModel.class);
            versions.add(version);
        }
        return versions;
    }
}
