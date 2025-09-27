package ru.sbt.edu_power.e2e_core.auxiliary;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.qameta.allure.Allure;
import ru.sbtqa.tag.datajack.Stash;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StandConfigurationDiff {
    private final String standKeyA;
    private final String standKeyB;
    private final List<Map<String, String>> configA = new ArrayList<>();
    private final List<Map<String, String>> configB = new ArrayList<>();
    private final List<Map<String, String>> diff = new ArrayList<>();
    private final List<String> exclude = new ArrayList<>();

    public StandConfigurationDiff(final List<String> data) {
        this.standKeyA = data.get(0);
        this.standKeyB = data.get(1);
        data.remove(0);
        data.remove(1);
        exclude.addAll(data);
    }

    public void restore() {
        setConfigMapList(standKeyA, configA);
        setConfigMapList(standKeyB, configB);
    }

    public void excludeConfigWithFileLinks() {
        configA.forEach(conf -> {
            if (conf.get("value").contains("/public_any/")) {
                if (!exclude.contains(conf.get("propertyCode"))) {
                    exclude.add(conf.get("propertyCode"));
                }
            }
        });
        configB.forEach(conf -> {
            if (conf.get("value").contains("/public_any/")) {
                if (!exclude.contains(conf.get("propertyCode"))) {
                    exclude.add(conf.get("propertyCode"));
                }
            }
        });
        final List<Map<String, String>> tempConfigA = new ArrayList<>(configA);
        final List<Map<String, String>> tempConfigB = new ArrayList<>(configB);
        exclude.forEach(code -> {
            tempConfigA.forEach(conf -> {
                if (code.equals(conf.get("propertyCode"))) {
                    configA.remove(conf);
                }
            });
            tempConfigB.forEach(conf -> {
                if (code.equals(conf.get("propertyCode"))) {
                    configB.remove(conf);
                }
            });
        });
    }

    public void compare() {
        filterConfig();
        makeDiff();
        printConfigToAllure("Разница конфигураций", diff);
        printConfigToAllure(
                String.format(
                        "Конфигурация есть на стенде %s но нет на стенде %s",
                        standKeyA,
                        standKeyB
                ),
                configA
        );
        printConfigToAllure(
                String.format(
                        "Конфигурация есть на стенде %s но нет на стенде %s",
                        standKeyB,
                        standKeyA
                ),
                configB
        );
    }

    public void generateSQLScript() {
        if (diff.isEmpty()) {
            return;
        }
        final String sql = diff.stream()
                               .map(values -> generateSQLString(standKeyA, values))
                               .collect(Collectors.joining("\n"));
        Allure.addAttachment("Скрипт для выполнения на стенде", sql);
    }

    private String generateSQLString(final String standKey, final Map<String, String> values) {
        return String.format(
                "UPDATE edu_power_configuration.configurations\n" +
                "SET value = '%s'\n" +
                "WHERE property_code = '%s'\n" +
                "AND entity_id = '99999999-9999-9999-9999-999999999999';",
                values.get(standKey),
                values.get("propertyCode")
        );
    }

    private void setConfigMapList(final String key, final List<Map<String, String>> config) {
        final String json = Stash.getValue(key);
        final JsonElement element = new JsonParser().parse(json);
        final JsonArray jsonArray = element
                .getAsJsonObject()
                .getAsJsonObject("data")
                .getAsJsonObject("systemAdmin")
                .getAsJsonArray("getAllBackendConfigurations");
        jsonArray.forEach(conf -> {
            final Map<String, String> configMap = new HashMap<>();
            configMap.put("propertyCode", conf.getAsJsonObject().getAsJsonPrimitive("propertyCode").getAsString());
            configMap.put("value", conf.getAsJsonObject().getAsJsonPrimitive("value").getAsString());
            configMap.put("description",
                    conf
                            .getAsJsonObject()
                            .getAsJsonObject("configurationMeta")
                            .getAsJsonPrimitive("displayableName")
                            .getAsString()
            );
            config.add(configMap);
        });
    }

    private void filterConfig() {
        final List<Map<String, String>> tempConfigA = new ArrayList<>(configA);
        final List<Map<String, String>> tempConfigB = new ArrayList<>(configB);
        tempConfigA.forEach(cA ->
                tempConfigB.forEach(cB -> {
                    if (
                            cA.get("propertyCode").equals(cB.get("propertyCode"))
                            && cA.get("value").equals(cB.get("value"))
                    ) {
                        configA.remove(cA);
                        configB.remove(cB);
                    }
                })
        );
    }

    private void makeDiff() {
        final List<Map<String, String>> tempConfigA = new ArrayList<>(configA);
        final List<Map<String, String>> tempConfigB = new ArrayList<>(configB);
        tempConfigA.forEach(cA ->
                tempConfigB.forEach(cB -> {
                    if (
                            cA.get("propertyCode").equals(cB.get("propertyCode"))
                            && !cA.get("value").equals(cB.get("value"))
                    ) {
                        final Map<String, String> diffMap = new HashMap<>();
                        diffMap.put("propertyCode", cA.get("propertyCode"));
                        diffMap.put(standKeyA, cA.get("value"));
                        diffMap.put(standKeyB, cB.get("value"));
                        diffMap.put("description", cA.get("description"));
                        diff.add(diffMap);
                        configA.remove(cA);
                        configB.remove(cB);
                    }
                })
        );
    }

    private void printConfigToAllure(final String printName, final List<Map<String, String>> config) {
        if (config.isEmpty()) {
            return;
        }
        Allure.addAttachment(printName, getFormattedValues(config));
    }

    private String getFormattedValues(final List<Map<String, String>> values) {
        return values.stream()
                     .map(this::getFormattedValue)
                     .collect(Collectors.joining("\n\n"));
    }

    private String getFormattedValue(final Map<String, String> values) {
        return values.keySet()
                     .stream()
                     .map(key -> String.format("%s: %s", key, values.get(key)))
                     .collect(Collectors.joining("\n"));
    }
}
