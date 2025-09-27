package ru.sbt.edu_power.e2e_core.features_utils;

import com.google.gson.JsonObject;
import org.junit.Assert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Класс выполняет парсинг feature файла, пытаясь идентифицировать в нём базовые составляющие:
 * Заголовок (#language:ru)
 * Глобальные теги
 * Функционал (может быть многострочным)
 * Набор сценариев
 *
 * В каждом сценарии идентифицируются:
 * Набор тегов
 * Строка с названием сценария (может быть многострочной)
 * Набор шагов
 *
 * После парсинга файла мы получаем объект Feature в котором можем искать сценарии по названию или тегу,
 * добавлять или удалять составляющие объекта, а так же преобразовывать его в список строк для записи в feature файл
 */
public class ScenariosParser {
    private final Path path;
    private final Feature feature;
    private final List<String> rows;
    private int index;
    private boolean isHeaderAdded;
    private boolean isGlobalTagsAdded;
    private boolean isFunctionalAdded;

    public ScenariosParser(final Path path) {
        this.path = path;
        feature = new Feature(path);
        rows = read();
    }

    // выполнение парсинга
    public void parse() {
        while (index < rows.size()) {
            if (!isHeaderAdded) {
                addHeader();
            }
            if (!isGlobalTagsAdded) {
                addGlobalTags();
            }
            if (!isFunctionalAdded) {
                addFunctional();
            }
            addScenario();
        }

    }

    public Feature getFeature() {
        return feature;
    }

    // поиск и запись в Feature хедера фичи
    private void addHeader() {
        final int functionalSpecifiedIndex = getSpecifiedRowIndex(rows, index, Feature::isFunctional);
        final int globalTagsSpecifiedIndex = getSpecifiedRowIndex(rows,index, Feature::isTag);
        final int specifiedIndex = Math.min(functionalSpecifiedIndex, globalTagsSpecifiedIndex);
        if (specifiedIndex != index) {
            final List<String> headers = new ArrayList<>(rows.subList(index, specifiedIndex));
            headers.removeIf(String::isEmpty);
            headers.forEach(row -> feature.addFeatureSection(Feature.Section.HEADER, row));
        }
        isHeaderAdded = true;
        index = specifiedIndex;
    }

    // поиск и запись в Feature глобальных тегов
    private void addGlobalTags() {
        final int functionalSpecifiedIndex = getSpecifiedRowIndex(rows, index, Feature::isFunctional);
        final int globalTagsSpecifiedIndex = getSpecifiedRowIndex(rows,index, Feature::isTag);
        if (functionalSpecifiedIndex >= globalTagsSpecifiedIndex) {
            final List<String> globalTags = new ArrayList<>(rows.subList(globalTagsSpecifiedIndex, functionalSpecifiedIndex));
            globalTags.removeIf(String::isEmpty);
            globalTags.forEach(row -> feature.addFeatureSection(Feature.Section.GLOBAL_TAGS, row));
            index = functionalSpecifiedIndex;
        }
        isGlobalTagsAdded = true;
    }

    // поиск и запись в Feature строки Функционал
    private void addFunctional() {
        final int specifiedIndex = getSpecifiedRowIndex(rows, index, (row) ->
                row.trim().isEmpty() ||
                row.trim().startsWith("#") ||
                Feature.isTag(row) ||
                Feature.isScenario(row)
        );
        Assert.assertNotEquals("Строки с названием функционала не обнаружены", index, specifiedIndex);
        final List<String> functionalRows = new ArrayList<>(rows.subList(index, specifiedIndex));
        feature.addFeatureSection(Feature.Section.FUNCTIONAL, String.join("\n", functionalRows));
        index = specifiedIndex;
        isFunctionalAdded = true;
    }

    // поиск и запись в Feature сценария
    private void addScenario() {
        final int specifiedScenarioIndex = getSpecifiedRowIndex(rows, index, Feature::isScenario);
        // проверяем что доступна строка с названием сценария
        if (Feature.isScenario(rows.get(specifiedScenarioIndex))) {
            final JsonObject scenario = feature.createScenario();
            feature.addFeatureSection(Feature.Section.SCENARIOS, scenario);
            // если есть теги - сохраняем их
            if (index < specifiedScenarioIndex) {
                final List<String> tags = new ArrayList<>(rows.subList(index, specifiedScenarioIndex));
                tags.removeIf(String::isEmpty);
                tags.forEach(row -> feature.addScenarioSection(scenario, Feature.ScenarioSection.TAGS, row));
            }
            index = specifiedScenarioIndex;
            final int specifiedStepIndex = getSpecifiedRowIndex(rows, index, (row) ->
                    Feature.isStep(row) || row.trim().startsWith("#")
            );
            final List<String> scenarioNameRows = new ArrayList<>(rows.subList(index, specifiedStepIndex));
            scenarioNameRows.removeIf(String::isEmpty);
            // Сохраняем название сценария
            feature.addScenarioSection(scenario, Feature.ScenarioSection.SCENARIO_NAME, String.join("\n", scenarioNameRows));
            index = specifiedStepIndex;
            if (rows.size() < index) {
                return;
            }
            // находим все строки до следующего сцения или до конца файла и сохраняем их в качестве набора шагов
            int endScenarioIndex = getSpecifiedRowIndex(rows, index, (row) ->
                    Feature.isTag(row) || Feature.isScenario(row)
            );
            if (endScenarioIndex == index) {
                endScenarioIndex = rows.size();
            }
            final List<String> steps = new ArrayList<>(rows.subList(index, endScenarioIndex));
            trimList(steps);
            steps.forEach(step -> feature.addScenarioSection(scenario, Feature.ScenarioSection.STEPS, step));
            index = endScenarioIndex;
        }
    }

    // метод удаляет пустые строки в начале и в конце списка
    private void trimList(final List<String> list) {
        if (list.get(list.size() - 1).trim().isEmpty()) {
            list.remove(list.size() - 1);
            trimList(list);
        }
        if (list.get(0).trim().isEmpty()) {
            list.remove(0);
            trimList(list);
        }
    }

    /**
     * Метод находит индекс строки в списке по условию, переданному в виде функции
     *
     * @param rows          список для поиска строк
     * @param startIndex    начальный индекс для поиска строки
     * @param function      функция должна принимать текущую строку и возвращать булево значение
     * @return              индекс первой найденной строки либо начальный индекс, если строка не найдена
     */
    private int getSpecifiedRowIndex(final List<String> rows, final int startIndex, final Function<String, Boolean> function) {
        return IntStream
                .range(startIndex, rows.size())
                .filter(i -> function.apply(rows.get(i)))
                .findFirst()
                .orElse(startIndex);
    }

    // возвращает список строк из файла
    private List<String> read() {
        try {
            return Files.readAllLines(path);
        } catch (final IOException e) {
            throw new FeaturesParserException(e);
        }
    }
}
