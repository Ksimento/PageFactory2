package ru.sbt.edu_power.e2e_core.smoke_layout;

import com.google.gson.JsonElement;
import ru.sbt.edu_power.e2e_core.features_utils.Feature;
import ru.sbt.edu_power.e2e_core.features_utils.ScenariosParser;
import ru.sbt.edu_power.e2e_core.layout.enums.DimensionEnum;
import ru.sbt.edu_power.e2e_core.smoke.Roles;
import ru.sbt.edu_power.e2e_core.smoke_layout.misc.DataVolume;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class LayoutScenarioParser {
    private final ScenariosParser parser;
    private final Feature feature;
    private String uuid;
    // Тег роли -> Тег полноты данных -> Набор шагов для фильтрации данных
    private final Map<PageConfiguration.Variant, JsonElement> variantToScenarioMap = new HashMap<>();
    private final Map<PageConfiguration.Variant, List<String>> routingParams = new HashMap<>();
    private final Map<PageConfiguration.Variant, String> authProfiles = new HashMap<>();
    // Вариант -> Название элемента -> Предварительные шаги
    private final Map<PageConfiguration.Variant, Map<String, String>> filterSteps = new HashMap<>();


    public LayoutScenarioParser(final Path path) {
        parser = new ScenariosParser(path);
        parser.parse();
        feature = parser.getFeature();
        parse();
    }

    public Map<PageConfiguration.Variant, List<String>> getRoutingParams() {
        return routingParams;
    }

    public Map<PageConfiguration.Variant, String> getAuthProfiles() {
        return authProfiles;
    }

    private void parse() {
        parseUuid();
        parseScenarios();
        variantToScenarioMap.keySet().forEach(this::parseRouteParams);
        variantToScenarioMap.keySet().forEach(this::parseAuthParams);
    }

    private void parseUuid() {
        uuid = parser.getFeature()
                     .getSectionData(Feature.Section.HEADER)
                     .stream()
                     .filter(r -> r.startsWith("#uuid"))
                     .findFirst()
                     .orElse("")
                     .replace("#uuid=", "");
    }

    private void parseScenarios() {
        feature.getScenarios()
               .forEach(this::parseScenario);
    }

    private void parseScenario(final JsonElement scenario) {
        Objects.requireNonNull(scenario);
        final List<String> tags = feature.getScenarioData(scenario, Feature.ScenarioSection.TAGS);
        final AtomicReference<Roles> role = new AtomicReference<>();
        final AtomicReference<DataVolume> dataVolume = new AtomicReference<>();
        final List<DimensionEnum> dimensions = new ArrayList<>();
        tags.forEach(tag -> {
            if (tag.startsWith("@R_")) {
                role.set(Roles.getRoleByTagName(tag));
            } else if (tag.startsWith("@LDV_")) {
                dataVolume.set(DataVolume.valueOf(tag.replace("@LDV_", "")));
            } else if (tag.startsWith("@LTD_")) {
                dimensions.add(DimensionEnum.valueOf(tag.replace("@LTD_", "")));
            }
        });
        if (Objects.nonNull(role.get()) && Objects.nonNull(dataVolume.get()) && !dimensions.isEmpty()) {
            dimensions.forEach(dimension -> {
                final PageConfiguration.Variant variant = new PageConfiguration.Variant(
                        role.get(),
                        dimension,
                        dataVolume.get()
                );
                variantToScenarioMap.put(variant, scenario);
            });

        }
    }

    private void parseRouteParams(final PageConfiguration.Variant variant) {
        final List<String> steps = feature.getScenarioData(
                variantToScenarioMap.get(variant),
                Feature.ScenarioSection.STEPS
        );
        for (int i = 0; i < steps.size() - 1; i++) {
            if (steps.get(i).startsWith("И проверяет роут")) {
                if (steps.get(i + 1).startsWith("|")) {
                    final List<String> params = Stream.of(steps.get(i + 1).split("\\|"))
                            .filter(t -> !t.isEmpty())
                            .map(String::trim)
                            .collect(Collectors.toList());
                    routingParams.put(variant, params);
                }
                break;
            }
        }
    }

    private void parseAuthParams(final PageConfiguration.Variant variant) {
        final List<String> steps = feature.getScenarioData(
                variantToScenarioMap.get(variant),
                Feature.ScenarioSection.STEPS
        );
        for (final String step : steps) {
            if (step.contains("авториз")) {
                final String[] stepParts = step.split("\"");
                authProfiles.put(variant, stepParts.length > 1 ? stepParts[1] : "");
                break;
            }
        }
    }

    public Map<String, String> getFilterStep(final PageConfiguration.Variant variant) {
        if (filterSteps.containsKey(variant)) {
            return filterSteps.get(variant);
        }
        if (!variantToScenarioMap.containsKey(variant)) {
            filterSteps.put(variant, new HashMap<>());
            return filterSteps.get(variant);
        }
        final List<String> steps = feature.getScenarioData(
                variantToScenarioMap.get(variant),
                Feature.ScenarioSection.STEPS
        );
        final Map<String, List<String>> filterStepsMap = new TreeMap<>();
        String elementName = "";
        boolean isFilterSteps = false;
        for (final String step : steps) {
            if (step.contains("#!filter_End")) {
                isFilterSteps = false;
                continue;
            }
            if (isFilterSteps) {
                filterStepsMap.get(elementName).add(step);
            }
            if (step.contains("#!filter_Start")) {
                if (isFilterSteps) {
                    throw new AutotestError(
                            "Обнаружен маркер '#!filter_Start' два раза подряд без промежуточного '#!filter_End'");
                }
                isFilterSteps = true;
                elementName = step.replace("#!filter_Start_", "");
                filterStepsMap.put(elementName, new ArrayList<>());
            }
        }
        final Map<String, String> nameToSteps = filterStepsMap.entrySet()
                             .stream()
                             .collect(Collectors.toMap(
                                     Map.Entry::getKey,
                                     e -> String.join("\n", e.getValue()),
                                     (a, b) -> a
                                     )
                             );
        filterSteps.put(variant, nameToSteps);
        return nameToSteps;
    }

    public String getUuid() {
        return uuid;
    }
}
