package ru.sbt.edu_power.external_services.confluence;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import ru.sbt.edu_power.external_services.PropReader;
import ru.sbt.edu_power.external_services.jira.JiraConnect;
import ru.sbt.edu_power.external_services.jira.JiraExporterUtils;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Класс выполняет подключение к confluence
 */
public class ConfluenceConnect {
    private static final String CONTENT_ENDPOINT = PropReader.get("conf.api.endpoint.content.by.id");
    private static final String EXPAND = "?expand=";
    private static final String SEARCH = PropReader.get("conf.api.search");
    private static final String CREATE = PropReader.get("conf.api.create");
    private static final String JIRA_LOGIN = JiraExporterUtils.decodeData(PropReader.get("jira.login"));
    private static final String JIRA_PASS = JiraExporterUtils.decodeData(PropReader.get("jira.pass"));

    public static synchronized HttpResponse<JsonNode> search(final Map<String, Object> params) {
        final HttpResponse<JsonNode> response = Unirest.get(SEARCH)
                .queryString(params)
                .basicAuth(JIRA_LOGIN,JIRA_PASS)
                .connectTimeout(1200000)
                .socketTimeout(600000)
                .asJson();
        JiraConnect.checkResponse(response, SEARCH, params);
        return response;
    }

    // Метод получает страницу по ID
    public static synchronized HttpResponse<JsonNode> getPage(final String id, final Expand... expandOptions) {
        final String options = expandOptions.length == 0 ? "" :
                EXPAND + Stream.of(expandOptions)
                        .map(Expand::getExpandName)
                        .collect(Collectors.joining(","));
        final String url = CONTENT_ENDPOINT.replace("{ID}", id) + options;
        final HttpResponse<JsonNode> response = Unirest.get(url)
                .basicAuth(JIRA_LOGIN,JIRA_PASS)
                .connectTimeout(1200000)
                .socketTimeout(600000)
                .asJson();
        JiraConnect.checkResponse(response, url);
        return response;
    }

    // Метод обновляет страницу по ID
    public static synchronized HttpResponse<JsonNode> updatePage(final String id, final ConfluenceContentModel model) {
        final String url = CONTENT_ENDPOINT.replace("{ID}", id);
        final HttpResponse<JsonNode> response = Unirest.put(url)
                .basicAuth(JIRA_LOGIN,JIRA_PASS)
                .body(model)
                .connectTimeout(1200000)
                .socketTimeout(600000)
                .asJson();
        JiraConnect.checkResponse(response, url, model);
        return response;
    }

    public static synchronized HttpResponse<JsonNode> createEmptyPage(final ConfluenceContentModel model) {
        final HttpResponse<JsonNode> response = Unirest.post(CREATE)
                .basicAuth(JIRA_LOGIN,JIRA_PASS)
                .body(model)
                .connectTimeout(1200000)
                .socketTimeout(600000)
                .asJson();
        JiraConnect.checkResponse(response, SEARCH, model);
        return response;
    }

    public enum Expand {
        BODY("body"),
        BODY_VIEW("body.view"),
        BODY_STORAGE("body.storage"),
        VERSION("version");

        private final String expandName;

        Expand(final String expandName) {
            this.expandName = expandName;
        }

        public String getExpandName() {
            return expandName;
        }
    }
}
