package ru.sbt.edu_power.e2e_core.features_utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.Assert;
import ru.sbt.edu_power.external_services.validator.Validator;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Класс реализует модель .feature файла
 * Модель хранится в виде JSON массива
 */
public class Feature {
    private final JsonObject featureMap = new JsonObject();
    private final Path path;

    public Feature(final Path path) {
        this.path = path;
    }

    public JsonObject createScenario() {
        return new JsonObject();
    }

    // метод добавляет данные в секцию сценария
    public void addScenarioSection(final JsonObject scenario, final ScenarioSection scenarioSection, final String row) {
        switch (scenarioSection) {
            case TAGS:
            case STEPS:
                if (!scenario.has(scenarioSection.name())) {
                    final JsonElement jsonArray = new JsonArray();
                    scenario.add(scenarioSection.name(), jsonArray);
                }
                scenario.getAsJsonArray(scenarioSection.name()).add(row.trim());
                break;
            case SCENARIO_NAME:
                Assert.assertFalse(
                        "Сценарий уже содержит название. В сценарий нельзя добавить два названия",
                        scenario.has(scenarioSection.name())
                );
                scenario.addProperty(scenarioSection.name(), row.trim());
                break;
            default:
                throw new FeaturesParserException("Значение не поддерживается " + scenarioSection.name());
        }
    }

    public String getFunctional() {
        return featureMap.getAsJsonPrimitive(Section.FUNCTIONAL.name()).getAsString();
    }

    // метод добавляет строку в секцию фичи
    public void addFeatureSection(final Section section, final String row) {
        switch (section) {
            case HEADER:
            case GLOBAL_TAGS:
                if (!featureMap.has(section.name())) {
                    final JsonElement jsonArray = new JsonArray();
                    featureMap.add(section.name(), jsonArray);
                }
                featureMap.getAsJsonArray(section.name()).add(row.trim());
                break;
            case FUNCTIONAL:
                if (featureMap.has(section.name())) {
                    featureMap.remove(section.name());
                }
                featureMap.addProperty(section.name(), row.trim());
                break;
            case SCENARIOS:
                throw new FeaturesParserException(
                        "Сценарий можно добавить только в виде JsonObject, но вместо этого передана строка");
            default:
                throw new FeaturesParserException("Значение не поддерживается " + section.name());
        }
    }

    // метод добавляет объект сценария в фичу
    public void addFeatureSection(final Section section, final JsonElement scenario) {
        Assert.assertEquals(
                "В виде JsonObject можно добавлять только секцию scenario. Но вы пытаетесь его добавить в секцию " +
                section.name(),
                section,
                Section.SCENARIOS
        );
        if (!featureMap.has(section.name())) {
            final JsonElement jsonArray = new JsonArray();
            featureMap.add(section.name(), jsonArray);
        }
        featureMap.getAsJsonArray(section.name()).add(scenario);
    }

    // метод возвращает данные из JSON представления фичи в виде набора строк, пригодного для записи в файл
    public List<String> asFileRows() {
        final List<String> rows = new ArrayList<>();
        if (featureMap.has(Section.HEADER.name())) {
            featureMap.getAsJsonArray(Section.HEADER.name())
                      .forEach(object -> rows.add(object.getAsString()));
            rows.add("");
        }
        if (featureMap.has(Section.GLOBAL_TAGS.name())) {
            featureMap.getAsJsonArray(Section.GLOBAL_TAGS.name())
                      .forEach(object -> rows.add(object.getAsString()));
        }
        rows.addAll(Arrays.asList(featureMap.getAsJsonPrimitive(Section.FUNCTIONAL.name()).getAsString().split("\n")));
        if (featureMap.has(Section.SCENARIOS.name())) {
            featureMap.getAsJsonArray(Section.SCENARIOS.name())
                      .forEach(scenario -> {
                          rows.add("");
                          if (scenario.getAsJsonObject().has(ScenarioSection.TAGS.name())) {
                              scenario.getAsJsonObject()
                                      .getAsJsonArray(
                                              ScenarioSection.TAGS.name())
                                      .forEach(tags -> rows.add(formatRow(ScenarioSection.TAGS, tags.getAsString()))
                                      );
                          }
                          rows.addAll(Arrays.stream(scenario.getAsJsonObject()
                                                            .getAsJsonPrimitive(ScenarioSection.SCENARIO_NAME.name())
                                                            .getAsString()
                                                            .split("\n"))
                                            .map(r -> formatRow(ScenarioSection.SCENARIO_NAME, r))
                                            .collect(Collectors.toList())

                          );
                          scenario.getAsJsonObject()
                                  .getAsJsonArray(ScenarioSection.STEPS.name())
                                  .forEach(step -> rows.add(formatRow(ScenarioSection.STEPS, step.getAsString())));
                      });
        }
        return rows;
    }

    // метод находит в сценарии данные и удаляет их
    public void removeScenarioData(final JsonObject scenario, final ScenarioSection section, final String value) {
        switch (section) {
            case TAGS:
            case STEPS:
                if (scenario.has(section.name())) {
                    final JsonArray array = scenario.getAsJsonArray(section.name());
                    final Optional<JsonElement> element = find(array, value);
                    element.ifPresent(array::remove);
                }
                break;
            default:
                throw new FeaturesParserException("Значение не поддерживается " + section.name());
        }
    }

    // метод находит в фиче данные и удаляет их
    public void removeFeatureData(final Section section, final String value) {
        switch (section) {
            case HEADER:
            case GLOBAL_TAGS:
                if (featureMap.has(section.name())) {
                    final JsonArray array = featureMap.getAsJsonArray(section.name());
                    final Optional<JsonElement> element = find(array, value);
                    element.ifPresent(array::remove);
                }
                break;
            default:
                throw new FeaturesParserException("Значение не поддерживается " + section.name());
        }
    }

    // метод удаляет целиком секцию из сценария
    public void removeScenarioData(final JsonObject scenario, final ScenarioSection section) {
        switch (section) {
            case SCENARIO_NAME:
            case STEPS:
            case TAGS:
                if (scenario.has(section.name())) {
                    scenario.remove(section.name());
                }
                break;
            default:
                throw new FeaturesParserException("Значение не поддерживается " + section.name());
        }
    }

    // получаем список элементов секции фичи
    public List<String> getSectionData(final Section section) {
        if (section == Section.SCENARIOS) {
            throw new AutotestError("Метод не позволяет получать строки сценариев");
        }
        final List<String> rows = new ArrayList<>();
        if (featureMap.has(section.name())) {
            featureMap.getAsJsonArray(section.name()).forEach(r -> {
                rows.add(r.getAsString().trim());
            });
        }
        return rows;
    }

    // метод удаляет целиком секцию из фичи
    public void removeFeatureData(final Section section) {
        switch (section) {
            case GLOBAL_TAGS:
            case HEADER:
            case SCENARIOS:
            case FUNCTIONAL:
                if (featureMap.has(section.name())) {
                    featureMap.remove(section.name());
                }
                break;
            default:
                throw new FeaturesParserException("Значение не поддерживается " + section.name());
        }
    }

    private String formatRow(final ScenarioSection scenarioSection, final String row) {
        final char[] spaces = new char[scenarioSection.getSpaces()];
        Arrays.fill(spaces, ' ');
        final String tab = new String(spaces);
        return Stream.of(row.split("\\n"))
                     .map(String::trim)
                     .map(r -> {
                         if (r.isEmpty()) {
                             return "";
                         }
                         if (r.startsWith("|")) {
                             return tab + "  " + r;
                         }
                         return tab + r;
                     })
                     .collect(Collectors.joining("\n"));
    }

    // Поиск строки в списке JsonArray по значению (поддерживается маска)
    private Optional<JsonElement> find(final JsonArray array, final String value) {
        for (int i = 0; i < array.size(); i++) {
            final boolean result = Validator.matchValues(array.get(i).getAsString().trim(), value);
            if (result) {
                return Optional.of(array.get(i));
            }
        }
        return Optional.empty();
    }

    // метод возвращает JSON объект сценария по названию (доступен поиск по маске)
    public Optional<JsonElement> getScenarioByName(final String scenarioName) {
        for (final JsonElement scenario : featureMap.getAsJsonArray(Section.SCENARIOS.name())) {
            final boolean hasScenarioName = Validator.matchValues(
                    scenario.getAsJsonObject().getAsJsonPrimitive(ScenarioSection.SCENARIO_NAME.name()).getAsString(),
                    scenarioName
            );
            if (hasScenarioName) {
                return Optional.of(scenario);
            }
        }
        return Optional.empty();
    }

    // метод возвращает JSON объект сценария по тегу (доступен поиск по маске)
    public Optional<JsonElement> getScenarioByTag(final String tag) {
        for (final JsonElement scenario : featureMap.getAsJsonArray(Section.SCENARIOS.name())) {
            final boolean hasTag = Validator.matchValueInList(getScenarioData(scenario, ScenarioSection.TAGS), tag);
            if (hasTag) {
                return Optional.of(scenario);
            }
        }
        return Optional.empty();
    }

    // Метод возвращает список строк соответствующей секции сценария
    public List<String> getScenarioData(final JsonElement scenario, final ScenarioSection section) {
        final List<String> rows = new ArrayList<>();
        if (scenario.getAsJsonObject().has(section.name())) {
            switch (section) {
                case SCENARIO_NAME:
                    rows.add(scenario.getAsJsonObject().getAsJsonPrimitive(section.name()).getAsString().trim());
                    break;
                case TAGS:
                case STEPS:
                    scenario.getAsJsonObject()
                            .getAsJsonArray(section.name())
                            .forEach(row -> rows.add(row.getAsString().trim()));
                    break;
            }
        }
        return rows;
    }

    public List<String> getGlobalTags() {
        final List<String> tags = new ArrayList<>();
        if (featureMap.has(Section.GLOBAL_TAGS.name())) {
            featureMap.getAsJsonArray(Section.GLOBAL_TAGS.name())
                      .forEach(tag -> tags.add(tag.getAsString().trim()));
        }
        return tags;
    }

    // метод определяет является ли строка шагом сценария
    public static boolean isStep(final String row) {
        return row.trim().startsWith("И ") || row.trim().startsWith("Когда ") || row.trim().startsWith("Тогда ");
    }

    // метод определяет является ли строка названием функционала фичи
    public static boolean isFunctional(final String row) {
        return row.trim().startsWith("Функционал:");
    }

    // метод определяет является ли строка название сценария
    public static boolean isScenario(final String row) {
        return row.trim().startsWith("Сценарий:");
    }

    // метод определяет является ли строка тегом
    public static boolean isTag(final String row) {
        return row.trim().startsWith("@");
    }

    public List<JsonElement> getScenarios() {
        final List<JsonElement> scenarios = new ArrayList<>();
        if (featureMap.has(Section.SCENARIOS.name())) {
            featureMap.getAsJsonArray(Section.SCENARIOS.name()).forEach(scenarios::add);
        }
        return scenarios;
    }

    // Возвращает объект фичи если есть сценарий с тегом
    public Optional<Feature> ifScenarioHasTag(final JsonElement scenario, final String tag) {
        final boolean isTagPresent = Validator.matchValueInList(
                getScenarioData(scenario, ScenarioSection.TAGS).stream().map(String::trim).collect(Collectors.toList()),
                tag
        );
        return isTagPresent ? Optional.of(this) : Optional.empty();
    }

    // Возвращает объект фичи если есть глобальный тег
    public Optional<Feature> ifHasGlobalTag(final String tag) {
        return getGlobalTags().contains(tag) ? Optional.of(this) : Optional.empty();
    }

    // Возвращает объект фичи если нет глобального тега
    public Optional<Feature> ifNotHasGlobalTag(final String tag) {
        return getGlobalTags().contains(tag) ? Optional.empty() : Optional.of(this);
    }

    // Перезапись фичи с новыми данными
    public void saveToDisk() {
        saveToDisk(path);
    }

    // Запись фичи в произвольный файл
    public void saveToDisk(final Path path) {
        try {
            final byte[] bytes = String.join("\n", asFileRows()).getBytes();
            Files.createDirectories(path.getParent());
            Files.write(path, bytes);
        } catch (final IOException e) {
            throw new FeaturesParserException(e);
        }
    }

    // секции фичи
    public enum Section {
        HEADER, // заголовк типа #language:ru, хранится в массиве
        GLOBAL_TAGS, // глобальные теги над функционалом, хранится в массиве
        FUNCTIONAL, // название функционала, хранится в строке
        SCENARIOS // набор объектов сценариев, хранится в массиве
    }

    // секции сценария
    public enum ScenarioSection {
        TAGS(3), // набор тегов сценария, хранится в массиве
        SCENARIO_NAME(2), // название сценария, хранится в строке
        STEPS(4); // набор шагов сценария, хранится в массиве

        private final int spaces;

        ScenarioSection(final int spaces) {
            this.spaces = spaces;
        }

        public int getSpaces() {
            return spaces;
        }
    }
}
