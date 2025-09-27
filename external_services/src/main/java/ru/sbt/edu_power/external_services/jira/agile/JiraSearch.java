package ru.sbt.edu_power.external_services.jira.agile;

import kong.unirest.json.JSONObject;
import ru.sbt.edu_power.external_services.jira.JiraConnect;
import ru.sbt.edu_power.external_services.jira.agile.model.IssueFields;
import ru.sbt.edu_power.external_services.jira.agile.model.IssueQuery;

import java.util.HashMap;
import java.util.Map;

/**
 * Класс выполняет поиск issue в Jira по JQL запросу
 */
public class JiraSearch {
    private int searchResults;
    private final IssueQuery query;
    private final Map<String, IssueFields> issueList = new HashMap<>();

    public JiraSearch(final IssueQuery query) {
        this.query = query;
    }

    public void search() {
        final JSONObject node = JiraConnect.jiraIssueSearch(query);

        searchResults = node.getInt("total");
        for (final Object object : node.getJSONArray("issues")) {
            final String key = ((JSONObject) object).getString("key");
            issueList.put(
                    key,
                    IssueFields.of(object.toString())
            );
            issueList.get(key).setField(IssueFields.Field.ID, ((JSONObject) object).getString("id"));
            issueList.get(key).setField(IssueFields.Field.KEY, key);
        }
    }

    public int getSearchResults() {
        return searchResults;
    }

    public Map<String, IssueFields> getIssueList() {
        return issueList;
    }
}
