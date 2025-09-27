package ru.sbt.edu_power.external_services.confluence;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
  Модель данных для страницы Confluence
  !!! Модель не подходит для парсинга ответа новой создаваемой страницы
  Можно из ответа получить ID после чего загрузить страницу по ID
 */
@Data
public class ConfluenceContentModel {
    private Map<String, Integer> version = new HashMap<>();
    private String title;
    private String id;
    private String type = "page";
    private Map<String, Map<String, String>> body = new HashMap<>();
    private Space space;
    private List<Ancestors> ancestors = new ArrayList<>();

    public ConfluenceContentModel() {

    }

    public ConfluenceContentModel(final int version, final String title, final String body) {
        this.version.put("number", version);
        this.title = title;
        this.body.put("storage", new HashMap<>());
        this.body.get("storage").put("value", body);
        this.body.get("storage").put("representation", "storage");
    }

    @Data
    @AllArgsConstructor
    public static class Space {
        private String key;
    }

    @Data
    @AllArgsConstructor
    public static class Ancestors {
        private Integer id;
    }
}
