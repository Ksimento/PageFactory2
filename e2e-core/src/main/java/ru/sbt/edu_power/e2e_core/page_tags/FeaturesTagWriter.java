package ru.sbt.edu_power.e2e_core.page_tags;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import ru.sbt.edu_power.e2e_core.features_utils.FeaturesParser;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FeaturesTagWriter {
    private final List<File> features = new ArrayList<>();
    private final PageChildren pageChildren;
    private final Map<String, Integer> tagCounter = new HashMap<>();

    public FeaturesTagWriter(final String[] globalTags, final List<PageChildren> pageChildren) {
        final FeaturesParser parser = new FeaturesParser();
        parser.filterFeaturesByTag(FeaturesParser.Operator.OR, globalTags);
        features.addAll(parser.asFileList());
        this.pageChildren = new PageChildren("", "", pageChildren);
    }

    public void writeTags() {
        removeOldTags();
        features.forEach(this::updateScenarios);
    }

    private void updateScenarios(final File feature) {
        final Map<String, List<String>> scenarios = getScenarios(feature);
        for (final String scenario : scenarios.keySet()) {
            final List<String> pagesList = getScenarioPagesList(scenarios.get(scenario));
            final List<String> tags = pagesList
                    .stream()
                    .map(pageChildren::getTagByPageName)
                    .filter(tag -> !tag.isEmpty())
                    .collect(Collectors.toList());
            if (!tags.isEmpty()) {
                writeTags(feature, scenario, tags);
            }
            tags.forEach(tag -> {
                if (!tagCounter.containsKey(tag)) {
                    tagCounter.put(tag, 0);
                }
                tagCounter.replace(tag, tagCounter.get(tag) + 1);
            });
        }
    }

    public Map<String, Integer> getTagCounter() {
        return tagCounter;
    }

    private Map<String, List<String>> getScenarios(final File feature) {
        final List<String> lines = readFeature(feature);
        final AtomicReference<String> scenarioName = new AtomicReference<>("");
        final Map<String, List<String>> scenarios = new HashMap<>();
        lines.stream()
             .map(String::trim)
             .filter(line -> !line.startsWith("@") && !line.isEmpty())
             .forEach(line -> {
                 if (scenarioName.get().isEmpty() && !line.startsWith("Сценарий")) {
                     return;
                 }
                 if (line.startsWith("Сценарий")) {
                     scenarioName.set(line.replace("Сценарий:", "").trim());
                     scenarios.put(scenarioName.get(), new ArrayList<>());
                 } else {
                     scenarios.get(scenarioName.get()).add(line);
                 }
             });
        return scenarios;
    }

    private void writeTags(final File feature, final String scenario, final List<String> tags) {
        final List<String> lines = readFeature(feature);
        final AtomicInteger scenarioLineIndex = new AtomicInteger(0);
        for (final String line : lines) {
            if (line.trim().replace("Сценарий: ", "").replace("Сценарий:", "").trim().equals(scenario)) {
                scenarioLineIndex.set(lines.indexOf(line));
            }
        }
        Assert.assertNotEquals(
                String.format("Сценарий \"%s\" не найден в файле \"%s\"", scenario, feature),
                0,
                scenarioLineIndex.get()
        );
        final String tagSpace = lines.get(scenarioLineIndex.get()).split("\\S", 2)[0];
        tags.forEach(tag -> lines.add(scenarioLineIndex.get(), tagSpace + tag));
        writeFeatureFile(feature, lines);
    }

    private List<String> getScenarioPagesList(final List<String> scenarioLines) {
        final Pattern pattern = Pattern.compile("^И (?:пользователь |он )?(?:находится на странице|открывается страница|открывается вкладка мастера) \"([^\"]*)\"$");

        return scenarioLines
                .stream()
                .filter(line -> pattern.matcher(line).find())
                .map(line -> pattern.matcher(line).replaceAll("$1"))
                .filter(FeaturesParser.distinctByKey(t -> t))
                .collect(Collectors.toList());
    }

    private void removeOldTags() {
        features.forEach(feature -> {
            final List<String> lines = readFeature(feature);
            if (lines.removeIf(line -> line.trim().startsWith("@PG_"))) {
                writeFeatureFile(feature, lines);
            }

        });
    }

    private List<String> readFeature(final File feature) {
        try {
            return Files.readAllLines(feature.toPath(), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new AutotestError(e);
        }
    }

    private void writeFeatureFile(final File feature, final List<String> data) {
        try {
            FileUtils.write(feature, String.join("\n", data), StandardCharsets.UTF_8, false);
        } catch (final IOException e) {
            throw new AutotestError(e);
        }
    }
}
