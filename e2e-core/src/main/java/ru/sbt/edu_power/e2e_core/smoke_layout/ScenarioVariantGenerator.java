package ru.sbt.edu_power.e2e_core.smoke_layout;

import com.google.gson.JsonObject;
import ru.sbt.edu_power.e2e_core.features_utils.Feature;
import ru.sbt.edu_power.e2e_core.layout.enums.DimensionEnum;
import ru.sbt.edu_power.e2e_core.layout.enums.MeasuringTypes;
import ru.sbt.edu_power.e2e_core.resource_repository.ResourceRepository;
import ru.sbt.edu_power.e2e_core.smoke.RoutFeatureStep;
import ru.sbt.edu_power.external_services.jira.tc_verifier.NewJiraTCCollector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ScenarioVariantGenerator {
    private final Feature feature;
    private final PageConfiguration pageConfiguration;
    private final String env;
    private final SLTFeaturesSteps steps = new SLTFeaturesSteps();
    private final Map<String, Map<String, Map<String, String>>> layoutUsers = ResourceRepository
            .getResource(ResourceRepository.AvailableResource.SMOKE_LAYOUT_USERS);
    private Map<PageConfiguration.Variant, List<LayoutElement>> variants;
    private String dataSetPath;
    private final LayoutScenarioParser layoutScenarioParser;
    private final NewJiraTCCollector collector;

    public ScenarioVariantGenerator(
            final Feature feature,
            final PageConfiguration pageConfiguration,
            final boolean forProd,
            final LayoutScenarioParser layoutScenarioParser,
            final NewJiraTCCollector collector
    ) {
        this.feature = feature;
        this.pageConfiguration = pageConfiguration;
        env = forProd ? "PROD" : "DEV";
        this.layoutScenarioParser = layoutScenarioParser;
        this.collector = collector;
    }

    public void generate() {
        variants = pageConfiguration.getVariants();
        final List<String> dataSetPathList = new ArrayList<>();
        dataSetPathList.add("Smoke-" + env);
        dataSetPathList.add(upperFirstChar(pageConfiguration.getPage().getName().split("pages.")[1].split("\\.")[0]));
        dataSetPathList.add(pageConfiguration.getPage().getSimpleName());
        dataSetPath = String.join("; ", dataSetPathList);
        final List<VariantGroup> variantGroups = new ArrayList<>();
        variants.forEach((variant, layoutElementList) ->
                VariantGroup.updateCollection(variantGroups, variant, layoutElementList)
        );
        variantGroups.forEach(this::createScenario);
    }

    private void createScenario(final VariantGroup variantGroup) {
        final JsonObject scenario = feature.createScenario();
        feature.addFeatureSection(Feature.Section.SCENARIOS, scenario);
        final TestCaseGenerator testCaseGenerator = new TestCaseGenerator(
                pageConfiguration,
                variantGroup.getVariants(),
                collector
        );
        final String testCaseTag = "@" + testCaseGenerator.getTestCase().getKey();
        final String roleTag = variantGroup.getVariants().get(0).getRole().getRoleTag();
        final List<String> dimensionTags = variantGroup.getVariants()
                                                       .stream()
                                                       .map(PageConfiguration.Variant::getDimensionEnum)
                                                       // layout testing dimension
                                                       .map(d -> "@LTD_" + d)
                                                       .collect(Collectors.toList());

        feature.addScenarioSection(scenario, Feature.ScenarioSection.TAGS, roleTag);
        // layout data volume
        feature.addScenarioSection(
                scenario,
                Feature.ScenarioSection.TAGS,
                variantGroup.getVariants().get(0).getDataVolume().getTag()
        );
        dimensionTags.forEach(tag -> feature.addScenarioSection(scenario, Feature.ScenarioSection.TAGS, tag));
        feature.addScenarioSection(scenario, Feature.ScenarioSection.TAGS, testCaseTag);
        final String scenarioName = steps.getScenario().replace("%s", String.join(
                ". ",
                pageConfiguration.getPageEntry(),
                variantGroup.getVariants().get(0).getDataVolume().name(),
                variantGroup.getVariants().stream()
                            .map(PageConfiguration.Variant::getDimensionEnum)
                            .map(DimensionEnum::name)
                            .collect(Collectors.joining(". "))
        ));
        feature.addScenarioSection(scenario, Feature.ScenarioSection.SCENARIO_NAME, scenarioName);
        // Авторизация
        addAuthStep(scenario, variantGroup.getVariants().get(0));
        // Переход на страницу
        addRouteStep(scenario, variantGroup.getVariants().get(0));
        // Генерация шагов тестирования верстки
        ltStepImpl(scenario, variantGroup.getVariants().get(0));
    }

    private void ltStepImpl(final JsonObject scenario, final PageConfiguration.Variant variant) {
        final List<LayoutElement> elements = variants.get(variant);
        // Собираем все элементы с проверкой POSITION без использования фильтрации
        final List<LayoutElement> positionTestElementsWithoutFilter = elements
                .stream()
                .filter(element -> element.getMeasuringTypes() == MeasuringTypes.POSITION)
                .filter(element -> !element.isFiltered())
                .collect(Collectors.toList());
        if (!positionTestElementsWithoutFilter.isEmpty()) {
            addPositionTestingStep(scenario, positionTestElementsWithoutFilter, variant, false);
        }

        // Собираем все элементы POSITION с использованием фильтрации
        final List<LayoutElement> positionTestElementsWithFilter = elements
                .stream()
                .filter(element -> element.getMeasuringTypes() == MeasuringTypes.POSITION)
                .filter(LayoutElement::isFiltered)
                .collect(Collectors.toList());
        if (!positionTestElementsWithFilter.isEmpty()) {
            final String filterSteps;
            if (
                    hasPrevScenario() &&
                    Objects.nonNull(layoutScenarioParser.getFilterStep(variant)) &&
                    layoutScenarioParser.getFilterStep(variant).containsKey("POSITION")
            ) {
                filterSteps = "\n" + layoutScenarioParser.getFilterStep(variant).get("POSITION") + "\n";
            } else {
                filterSteps = "\n\n";
            }
            feature.addScenarioSection(
                    scenario,
                    Feature.ScenarioSection.STEPS,
                    steps.getFilterStep()
                         .replaceAll("%s", "POSITION")
                         .replaceAll("\\n\\n", filterSteps)
            );
            addPositionTestingStep(scenario, positionTestElementsWithFilter, variant, true);
            feature.addScenarioSection(scenario, Feature.ScenarioSection.STEPS, steps.getRefreshPage());
        }

        // Собираем все остальные элементы не требующие фильтрацию
        final List<LayoutElement> otherTestElementsWithoutFilter = elements
                .stream()
                .filter(element -> element.getMeasuringTypes() != MeasuringTypes.POSITION)
                .filter(element -> !element.isFiltered())
                .collect(Collectors.toList());
        if (!otherTestElementsWithoutFilter.isEmpty()) {
            addSelfContainerTestingStep(scenario, otherTestElementsWithoutFilter, variant, false);
        }

        // Собираем все остальные элементы требующие фильтрацию
        final List<LayoutElement> otherTestElementsWithFilter = elements
                .stream()
                .filter(element -> element.getMeasuringTypes() != MeasuringTypes.POSITION)
                .filter(LayoutElement::isFiltered)
                .collect(Collectors.toList());
        if (!otherTestElementsWithoutFilter.isEmpty()) {
            addSelfContainerTestingStep(scenario, otherTestElementsWithFilter, variant, true);
        }
    }

    private void addAuthStep(final JsonObject scenario, final PageConfiguration.Variant variant) {
        final String profile;
        if (hasPrevScenario() && layoutScenarioParser.getAuthProfiles().containsKey(variant)) {
            profile = layoutScenarioParser.getAuthProfiles().get(variant);
        } else {
            profile = layoutUsers
                    .get(env)
                    .get(variant.getDataVolume().name())
                    .get(variant.getRole().getEndPointName());
        }
        final String authStep = steps.getAuthStep(variant.getRole())
                                     .replace("%s", profile);
        feature.addScenarioSection(
                scenario,
                Feature.ScenarioSection.STEPS,
                authStep
        );
    }

    private void addRouteStep(final JsonObject scenario, final PageConfiguration.Variant variant) {
        final RoutFeatureStep routFeatureStep;
        if (hasPrevScenario() && layoutScenarioParser.getRoutingParams().containsKey(variant)) {
            routFeatureStep = new RoutFeatureStep(
                    pageConfiguration.getPage().getSimpleName(),
                    layoutScenarioParser.getRoutingParams().get(variant),""
            );
        } else {
            routFeatureStep = RoutFeatureStep.generateStep(
                    pageConfiguration.getPage().getSimpleName(),
                    variant.getRole(),""
            );
        }
        feature.addScenarioSection(
                scenario,
                Feature.ScenarioSection.STEPS,
                routFeatureStep.get()
        );
    }

    private void addPositionTestingStep(
            final JsonObject scenario,
            final List<LayoutElement> elements,
            final PageConfiguration.Variant variant,
            final boolean isFiltered
    ) {
        final String withFilter = isFiltered ? " с фильтрацией" : "";
        final String layoutTestName = String.format(
                "%s. Позиция элементов%s. %s. %s",
                pageConfiguration.getTitle(),
                withFilter,
                variant.getRole().getRoleName(),
                elements.get(0).getDataVolume().name()
        );
        final List<String> step = new ArrayList<>();
        step.add(steps
                .getLayoutStepSimple()
                .replace("%path", dataSetPath)
                .replace("%scenario", layoutTestName));
        elements.forEach(le -> {
            if (le.isBlockElement()) {
                for (final int i : le.getBlocks()) {
                    final String blockNumber = i == -1 ? "last" : String.valueOf(i);
                    step.add(getTable(le.getName() + "->" + blockNumber, le.getMeasuringTypes().name()));
                }
            } else {
                step.add(getTable(le.getName(), le.getMeasuringTypes().name()));
            }
        });
        feature.addScenarioSection(scenario, Feature.ScenarioSection.STEPS, String.join("\n", step));
    }

    private void addSelfContainerTestingStep(
            final JsonObject scenario,
            final List<LayoutElement> elements,
            final PageConfiguration.Variant variant,
            final boolean isFiltered
    ) {
        // разбиваем весь поток элементов на элементы по имени, их будем тестировать в одном шаге
        final Map<String, List<LayoutElement>> map = new HashMap<>();
        elements.forEach(le -> {
            if (!map.containsKey(le.getName())) {
                map.put(le.getName(), new ArrayList<>());
            }
            map.get(le.getName()).add(le);
        });
        map.values().forEach(list -> {
            final String types = list
                    .stream()
                    .map(LayoutElement::getMeasuringTypes)
                    .map(MeasuringTypes::name)
                    .collect(Collectors.joining(" "));
            // в списке list все элементы различаются только параметром MeasuringTypes, поэтому
            // для сценария берём первый из них
            final LayoutElement firstElement = list.get(0);
            final String layoutTestName = String.format(
                    "%s. %s. %s",
                    firstElement.getName(),
                    variant.getRole().getRoleName(),
                    firstElement.getDataVolume().name()
            );
            final List<String> step = new ArrayList<>();
            step.add(steps
                    .getLayoutStepSelfContainer()
                    .replace("%path", dataSetPath)
                    .replace("%scenario", layoutTestName));
            if (firstElement.isBlockElement()) {
                for (final int i : firstElement.getBlocks()) {
                    final String blockNumber = i == -1 ? "last" : String.valueOf(i);
                    step.add(getTable(firstElement.getName() + "->" + blockNumber, types));
                }
            } else {
                step.add(getTable(firstElement.getName(), types));
            }
            if (isFiltered) {
                final String filterSteps;
                if (
                        hasPrevScenario() &&
                        Objects.nonNull(layoutScenarioParser.getFilterStep(variant)) &&
                        layoutScenarioParser.getFilterStep(variant).containsKey(firstElement.getName())
                ) {
                    filterSteps = "\n" + layoutScenarioParser.getFilterStep(variant).get(firstElement.getName()) + "\n";
                } else {
                    filterSteps = "\n\n";
                }
                feature.addScenarioSection(
                        scenario,
                        Feature.ScenarioSection.STEPS,
                        steps.getFilterStep()
                             .replaceAll("%s", firstElement.getName())
                             .replaceAll("\\n\\n", filterSteps)
                );
            }
            feature.addScenarioSection(scenario, Feature.ScenarioSection.STEPS, String.join("\n", step));
        });
    }

    private String upperFirstChar(final String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private String getTable(final String... elements) {
        return "| " + String.join(" | ", elements) + " |";
    }

    private boolean hasPrevScenario() {
        return Objects.nonNull(layoutScenarioParser);
    }
}
