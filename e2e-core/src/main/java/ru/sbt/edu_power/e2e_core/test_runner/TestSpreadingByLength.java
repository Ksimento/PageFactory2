package ru.sbt.edu_power.e2e_core.test_runner;

import cucumber.runtime.junit.FeatureRunner;
import gherkin.events.PickleEvent;
import gherkin.pickles.PickleTag;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Sorter;
import ru.sbt.edu_power.external_services.PropReader;
import ru.sbt.edu_power.external_services.jenkins.JenkinsHttpConnection;
import ru.sbt.edu_power.external_services.jenkins.allure.NexusCache;
import ru.sbt.edu_power.external_services.jenkins.allure.SuiteCollector;
import ru.sbt.edu_power.external_services.jenkins.allure.SuiteGrabber;
import ru.sbt.edu_power.external_services.jenkins.allure.enums.Children;
import ru.sbt.edu_power.external_services.jenkins.allure.enums.AllureReport;
import ru.sbt.edu_power.external_services.jenkins.downstream_job_finder.BuildActions;
import ru.sbt.edu_power.external_services.jenkins.downstream_job_finder.BuildElement;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class TestSpreadingByLength {
    private static final String FIRST_PRIORITY_TAG = "@first_priority";
    private static final String LAST_PRIORITY_TAG = "@last_priority";
    // искуственное значение продолжительности теста, которое будет применяться для тестов с тегом @last_priority
    // что бы расположить эти тесты не в самом конце прогона, а перед 20 последними тестами
    private long lastPriorityDuration;
    private final List<BuildElement> masterRegressBuilds = new ArrayList<>();
    private final List<SuiteCollector> suiteCollectors = new ArrayList<>();
    private final Map<String, Long> suiteDuration = new HashMap<>();
    private final List<FeatureRunner> children;
    private final Map<Description, String> descriptionUriMap = new HashMap<>();
    private final Map<Description, List<String>> descriptionToTagList = new HashMap<>();
    private final Sorter sorter = new Sorter((d1, d2) -> {
        final boolean isFirstPriority_1 = getTags(d1).contains(FIRST_PRIORITY_TAG);
        final boolean isFirstPriority_2 = getTags(d2).contains(FIRST_PRIORITY_TAG);
        final boolean isLastPriority_1 = getTags(d1).contains(LAST_PRIORITY_TAG);
        final boolean isLastPriority_2 = getTags(d2).contains(LAST_PRIORITY_TAG);
        if (isFirstPriority_1 && isFirstPriority_2 || isLastPriority_1 && isLastPriority_2) {
            return 0;
        }
        if (isFirstPriority_1) {
            return -9999999;
        }
        if (isFirstPriority_2) {
            return 9999999;
        }
        if (isLastPriority_1) {
            suiteDuration.put(getUri(d1), lastPriorityDuration);
        }
        if (isLastPriority_2) {
            suiteDuration.put(getUri(d2), lastPriorityDuration);
        }
        final Long dur1 = suiteDuration.get(getUri(d1));
        final Long dur2 = suiteDuration.get(getUri(d2));
        return (int) ((Objects.isNull(dur2) ? 0 : dur2) - (Objects.isNull(dur1) ? 0 : dur1));
    });

    public TestSpreadingByLength(final List<FeatureRunner> children) {
        new Thread(NexusCache::getInstance).start();
        this.children = children;
    }

    public void collectData() {
        collectDataFromChildren();
        collectBuilds();
        collectAllureData();
        collectSuiteDurations();
    }

    public Sorter getSorter() {
        return sorter;
    }

    private String getUri(final Description description) {
        if (!descriptionUriMap.containsKey(description)) {
            collectDataFromChildren(description);
        }
        return descriptionUriMap.get(description);
    }

    private List<String> getTags(final Description description) {
        if (!descriptionToTagList.containsKey(description)) {
            collectDataFromChildren(description);
        }
        return descriptionToTagList.get(description);
    }

    private void collectDataFromChildren() {
        children.stream().map(FeatureRunner::getDescription).forEach(this::getUri);
        children.stream()
                .map(FeatureRunner::getDescription)
                .map(Description::getChildren)
                .flatMap(List::stream)
                .map(Description::getChildren)
                .flatMap(List::stream)
                .forEach(this::getUri);
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private void collectDataFromChildren(final Description description) {
        final Optional<FeatureRunner> fr = children.stream()
                                                   .filter(ch -> ch.getDescription().equals(description) ||
                                                                 ch
                                                                         .getDescription()
                                                                         .getChildren()
                                                                         .stream()
                                                                         .anyMatch(d -> d.equals(description)))
                                                   .findFirst();
        if (!fr.isPresent()) {
            descriptionUriMap.put(description, "");
            descriptionToTagList.put(description, new ArrayList<>());
            return;
        }
        final FeatureRunner o = fr.get();
        final Class<?> clazz = o.getClass();
        final Method m = clazz.getDeclaredMethod("getChildren");
        m.setAccessible(true);
        final List<Object> pickleRunnerList = (List<Object>) m.invoke(o);
        if (pickleRunnerList.isEmpty()) {
            descriptionUriMap.put(description, "");
            descriptionToTagList.put(description, new ArrayList<>());
            return;
        }
        final Object pickleRunner = pickleRunnerList.get(0);
        final Class<?> pickleRunnerClass = pickleRunner.getClass();
        final Field pickleEventField = pickleRunnerClass.getDeclaredField("pickleEvent");
        pickleEventField.setAccessible(true);
        final PickleEvent pickleEvent = (PickleEvent) pickleEventField.get(pickleRunner);
        descriptionUriMap.put(description, pickleEvent.uri.split("features")[1]);
        descriptionToTagList.put(
                description,
                pickleEvent.pickle.getTags().stream().map(PickleTag::getName).collect(Collectors.toList())
        );
    }

    private void collectSuiteDurations() {
        suiteCollectors.forEach(sc -> {
            final Map<String, List<Children>> suiteMap = new HashMap<>();
            sc.getCollection().values().stream().flatMap(List::stream).forEach(ch -> {
                final Optional<String> featurePath = ch.getReport().getLabels().stream()
                                                       .filter(l -> l.getName().equals("gherkin_uri"))
                                                       .map(AllureReport.Label::getValue)
                                                       .findFirst();
                if (!featurePath.isPresent()) {
                    return;
                }
                final String uri = featurePath.get().split("features")[1];
                if (descriptionUriMap.values().contains(uri)) {
                    if (!suiteMap.containsKey(uri)) {
                        suiteMap.put(uri, new ArrayList<>());
                    }
                    suiteMap.get(uri).add(ch);
                }
            });
            suiteMap.forEach((suite, list) -> {
                final Long suiteLength = list.stream()
                                             .map(Children::getReport)
                                             .map(AllureReport::getTime)
                                             .mapToLong(AllureReport.Time::getDuration)
                                             .sum();
                if (!suiteDuration.containsKey(suite)) {
                    suiteDuration.put(suite, suiteLength);
                    return;
                }
                final Long currentLength = suiteDuration.get(suite);
                if (suiteLength > currentLength) {
                    suiteDuration.put(suite, suiteLength);
                }
            });
        });
        final List<Long> list = suiteDuration.values()
                                             .stream()
                                             .sorted(Comparator.naturalOrder())
                                             .collect(Collectors.toList());
        if (list.isEmpty()) {
            lastPriorityDuration = 0L;
        } else {
            lastPriorityDuration = list.size() > 19 ? list.get(20) : list.get(list.size() - 1);
        }
    }

    private void collectAllureData() {
        masterRegressBuilds.forEach(be -> {
            final SuiteCollector collector = new SuiteCollector();
            new SuiteGrabber().grab(collector, be.getUrl());
            suiteCollectors.add(collector);
        });
    }

    private void collectBuilds() {
        final List<BuildElement> lastBuilds = JenkinsHttpConnection.getCluster3().getPageBuilds(
                PropReader.get("jenkins.autotestParallel.job.url"),
                0,
                20
        );
        // выбираем два последних успешных прогона мастера в ночной сборке по соответствующему проекту
        masterRegressBuilds.addAll(lastBuilds
                .stream()
                .filter(b -> "SUCCESS".equals(b.getResult()))
                .filter(b -> ScenarioDataStorage.PROJECT.equals(getParamByName(b.getActions(), "JIRA_PROJECT_KEY")))
                .filter(b -> "master".equals(getParamByName(b.getActions(), "FRONTEND_BRANCH")))
                .filter(b -> "Regress".equals(getParamByName(b.getActions(), "TEST_PACK")))
                .filter(b -> {
                    final String stand = getParamByName(b.getActions(), "STAND_NAME");
                    return "dev1-95".equals(stand) || "dev1-94".equals(stand) || "dev1-5".equals(stand);
                })
                .limit(2)
                .collect(Collectors.toList())
        );
        log.info(
                "Для источника данных используются сборки:\n{}",
                masterRegressBuilds.stream().map(BuildElement::getUrl).collect(Collectors.joining("\n"))
        );
    }

    public String getParamByName(final Set<BuildActions> actions, final String paramName) {
        return actions.stream()
                      .filter(a -> !a.getParameters().isEmpty())
                      .flatMap(a -> a.getParameters().stream())
                      .filter(m -> paramName.equals(m.get("name")))
                      .map(m -> m.get("value").toString())
                      .findFirst()
                      .orElse("");
    }


}
