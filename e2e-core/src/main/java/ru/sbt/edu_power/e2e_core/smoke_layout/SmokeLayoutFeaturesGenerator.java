package ru.sbt.edu_power.e2e_core.smoke_layout;

import org.junit.Assert;
import ru.sbt.edu_power.e2e_core.features_utils.Feature;
import ru.sbt.edu_power.e2e_core.features_utils.FeaturesParser;
import ru.sbt.edu_power.e2e_core.smoke.EndPoints;
import ru.sbt.edu_power.e2e_core.smoke.Projects;
import ru.sbt.edu_power.e2e_core.smoke_layout.misc.Layout;
import ru.sbt.edu_power.e2e_core.smoke_layout.misc.LayoutDefault;
import ru.sbt.edu_power.e2e_core.smoke_layout.misc.PageUniqueId;
import ru.sbt.edu_power.external_services.jira.tc_verifier.NewJiraTCCollector;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCQueryBuilder;
import ru.sbtqa.tag.pagefactory.Page;
import ru.sbtqa.tag.pagefactory.PageManager;
import ru.sbtqa.tag.qautils.errors.AutotestError;
import ru.sbtqa.tag.qautils.properties.Props;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SmokeLayoutFeaturesGenerator {
    private final PageIdGenerator pageIdGenerator = new PageIdGenerator();
    // page_unique_id to page_class
    private final Map<Class<? extends Page>, Map<Field, String>> layoutPage = new HashMap<>();
    private final Map<Class<? extends Page>, String> pageToId = new HashMap<>();
    private final Map<Class<? extends Page>, PageConfiguration> pageConfigurationMap = new HashMap<>();
    private final Path featuresPathDev = Paths.get(Objects.requireNonNull(Props.get("layout.smoke.path.dev")));
    private final Path featuresPathProd = Paths.get(Objects.requireNonNull(Props.get("layout.smoke.path.prod")));
    private final SLTFeaturesSteps steps = new SLTFeaturesSteps();
    private final boolean forProd;
    private final String env;
    private final Projects project;
    private final Map<String, LayoutScenarioParser> pageUuidToParsedScenarios = new HashMap<>();
    private final NewJiraTCCollector collector = NewJiraTCCollector.getInstance();
    private String pageForSingleUse;

    public SmokeLayoutFeaturesGenerator(final boolean forProd, final String project) {
        Assert.assertFalse("Не задано свойство layout.smoke.path.dev в application.properties", featuresPathDev.toString().isEmpty());
        Assert.assertFalse("Не задано свойство layout.smoke.path.prod в application.properties", featuresPathProd.toString().isEmpty());
        this.forProd = forProd;
        env = forProd ? "PROD" : "DEV";
        this.project = Projects.valueOf(project);
    }

    public SmokeLayoutFeaturesGenerator(final boolean forProd, final String project, final String pageName) {
        this.forProd = forProd;
        env = forProd ? "PROD" : "DEV";
        this.project = Projects.valueOf(project);
        pageForSingleUse = pageName;
    }

    public void generate() {
        final TCQueryBuilder builder = new TCQueryBuilder();
        builder.addField(TCFields.PROJECT_ID, TCFields.ProjectId.valueOf(project.name()))
               .addField(TCFields.TEST_TYPE, TCFields.TestType.REGRESS)
               .addField(TCFields.TEST_VIEW, TCFields.TestView.LAYOUT)
               .addField(TCFields.ARCHIVED, false);
        collector.collect(builder);
        collectScenarioData();
        collectPageElements();
        layoutPage.keySet().forEach(this::createFeature);
    }

    private void collectScenarioData() {
        final Path path = forProd ? featuresPathProd : featuresPathDev;
        if (!Files.isDirectory(path)) {
            return;
        }
        final FeaturesParser parser = new FeaturesParser(path);
        parser.asFileList()
              .forEach(file -> {
                  final LayoutScenarioParser layoutScenarioParser = new LayoutScenarioParser(file.toPath());
                  if (!layoutScenarioParser.getUuid().isEmpty()) {
                      pageUuidToParsedScenarios.put(layoutScenarioParser.getUuid(), layoutScenarioParser);
                  }
              });
    }

    private void collectPageElements() {
        PageManager.getPageRepository().forEach((page, map) -> {
            if (Objects.nonNull(pageForSingleUse)) {
                if (!page.getSimpleName().equals(pageForSingleUse)) {
                    return;
                }
            }
            if (page.isAnnotationPresent(LayoutDefault.class) && pageHasLayoutElements(map)) {
                if (!page.isAnnotationPresent(EndPoints.class) || page.getAnnotation(EndPoints.class).ignored()) {
                    throw new AutotestError(String.format(
                            "Для страницы '%s' не указан валидный URL в аннотации EndPoints", page.getName()));
                }
                final String uuid;
                if (!page.isAnnotationPresent(PageUniqueId.class)) {
                    uuid = pageIdGenerator.generateId(page);
                } else {
                    uuid = page.getAnnotation(PageUniqueId.class).pageId();
                }
                layoutPage.put(page, map);
                pageToId.put(page, uuid);
                pageConfig(page);
            }
        });
    }

    private void createFeature(final Class<? extends Page> page) {
        final PageConfiguration pageConfig = pageConfigurationMap.get(page);
        // если page не относится к проекту project, то пропускаем генерацию тестов
        if (pageConfig.getRoles().stream().noneMatch(r -> r.getProject() == project)) {
            return;
        }
        final List<String> pagePath = pageConfig.getFeaturePath();
        pagePath.add(page.getSimpleName() + ".feature");
        final Path featurePath;
        if (forProd) {
            featurePath = Paths.get(featuresPathProd.toString(), pagePath.toArray(new String[]{})).toAbsolutePath();
        } else {
            featurePath = Paths.get(featuresPathDev.toString(), pagePath.toArray(new String[]{})).toAbsolutePath();
        }
        final String functional = steps.getFunctional().replace("%s", pageConfig.getFunctional() + ". " + env);
        final Feature feature = new Feature(featurePath);
        feature.addFeatureSection(Feature.Section.HEADER, steps.getLang());
        feature.addFeatureSection(Feature.Section.HEADER, "#uuid=" + pageToId.get(page));
        feature.addFeatureSection(Feature.Section.GLOBAL_TAGS, "@LT_regress");
        feature.addFeatureSection(Feature.Section.GLOBAL_TAGS, "@LTP_" + page.getSimpleName());
        feature.addFeatureSection(Feature.Section.FUNCTIONAL, functional);
        final ScenarioVariantGenerator scenarioVariantGenerator = new ScenarioVariantGenerator(
                feature,
                pageConfig,
                forProd,
                pageUuidToParsedScenarios.get(pageToId.get(page)),
                collector
        );
        scenarioVariantGenerator.generate();
        feature.saveToDisk();
    }

    private boolean pageHasLayoutElements(final Map<Field, String> pageElements) {
        return pageElements.keySet()
                           .stream()
                           .anyMatch(f -> f.isAnnotationPresent(Layout.class));
    }

    private void pageConfig(final Class<? extends Page> page) {
        final PageConfiguration pageConfiguration = new PageConfiguration(
                page,
                layoutPage.get(page),
                pageToId.get(page)
        );
        pageConfigurationMap.put(page, pageConfiguration);
    }
}
