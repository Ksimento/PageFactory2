package ru.sbt.edu_power.external_services.jira.test_manager.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Модель создания запроса в джиру
// Правила формирования запроса описаны на странице https://support.smartbear.com/tm4j-server/api-docs/v1/
// в разделе /testcase/search GET
@Data
@JsonIgnoreProperties(value = {"queryConstructor"})
@Deprecated
public class TestCaseSearch {
    private String query;
    private int maxResults;
    private int startAt;
    private String fields;
    private final Map<String, String> queryConstructor = new HashMap<>();

    public TestCaseSearch() {
    }

    public TestCaseSearch addParam(final String name, final String value) {
        queryConstructor.put(name, value);
        return this;
    }

    public void build() {
        buildQuery();
    }

    private void buildQuery() {
        final List<String> queryList = new ArrayList<>();
        for (final String param : queryConstructor.keySet()) {
            switch (param) {
                case "maxResults":
                    final Integer maxResults = Integer.valueOf(queryConstructor.get(param));
                    Assert.assertNotNull("Значение для maxResults должно быть числом", maxResults);
                    this.maxResults = maxResults;
                    break;
                case "startAt":
                    final Integer startAt = Integer.valueOf(queryConstructor.get(param));
                    Assert.assertNotNull("Значение для startAt должно быть числом", startAt);
                    this.startAt = startAt;
                    break;
                case "fields":
                    this.fields = queryConstructor.get(param);
                    break;
                case "projectKey":
                case "key":
                case "name":
                case "status":
                case "priority":
                case "component":
                case "folder":
                case "estimatedTime":
                case "labels":
                case "owner":
                    final String operator = queryConstructor.get(param).contains(";") ? " IN " : " = ";
                    if (" IN ".equals(operator)) {
                        final String values = Stream.of(queryConstructor.get(param).split(";"))
                                                    .map(String::trim)
                                                    .map(s -> "\"" + s + "\"")
                                                    .collect(Collectors.joining(","));
                        queryList.add(param + operator + "(" + values + ")");
                    } else {
                        queryList.add(param + " = \"" + queryConstructor.get(param) + "\"");
                    }
                    break;
                default:
                    final String values = Stream.of(queryConstructor.get(param).split(";"))
                                                .map(String::trim)
                                                .map(s -> "\"" + s + "\"")
                                                .collect(Collectors.joining(","));
                    queryList.add("\"" + param + "\" IN (" + values + ")");
            }
        }
        Assert.assertFalse(
                "Запрос не может быть пустым. Минимальный запрос должен содержать projectKey",
                queryList.isEmpty()
        );
        this.query = String.join(" AND ", queryList);
    }
}
