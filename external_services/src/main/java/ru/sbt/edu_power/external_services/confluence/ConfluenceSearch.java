package ru.sbt.edu_power.external_services.confluence;

import com.google.gson.Gson;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import lombok.Getter;
import lombok.Setter;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Setter
@Getter
public class ConfluenceSearch {
    private TCFields.ProjectId space;
    private String title;
    private List<ConfluenceContentModel> results = new ArrayList<>();

    public void search() {
        final HttpResponse<JsonNode> response = ConfluenceConnect.search(getMap());
        final Gson gson = new Gson();
        if (response.getBody().getObject().has("size") && response.getBody().getObject().getInt("size") > 0) {
            for (final Object o : response.getBody().getObject().getJSONArray("results")) {
                results.add(gson.fromJson(o.toString(), ConfluenceContentModel.class));
            }
        }
    }

    public List<ConfluenceContentModel> getResults() {
        return results;
    }

    private Map<String, Object> getMap() {
        final List<String> request = new ArrayList<>();
        if (Objects.nonNull(space)) {
            request.add("space = \"" + space.confluenceKey + "\"");
        }
        if (Objects.nonNull(title)) {
            request.add("title = \"" + title + "\"");
        }
        final Map<String, Object> map = new HashMap<>();
        map.put("cql", String.join(" and ", request));
        return map;
    }
}
