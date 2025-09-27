package ru.sbt.edu_power.e2e_core.test_runner;

import cucumber.api.Scenario;
import cucumber.runtime.junit.FeatureRunner;
import gherkin.events.PickleEvent;
import gherkin.pickles.PickleTag;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

// Класс собирает список тегов упавших тестов и записывает их в виде сообщения в текстовый файл для передачи этих данных в джобу
public class FailedScenarioTagsCollector {
    private static final FailedScenarioTagsCollector INSTANCE = new FailedScenarioTagsCollector();
    private final List<String> failedTags = new ArrayList<>();
    private final List<String> allTags = new ArrayList<>();
    private final List<FeatureRunner> children = new ArrayList<>();

    public static FailedScenarioTagsCollector getInstance() {
        return INSTANCE;
    }

    public void addChildren(final List<FeatureRunner> children) {
        this.children.addAll(children);
    }

    // получение всех тегов тест-кейсов выбранных для запуска в этом прогоне
    @SuppressWarnings("unchecked")
    @SneakyThrows
    public void collectExecutedTags() {
        for (FeatureRunner fr : children) {
            final Class<?> clazz = fr.getClass();
            final Method m = clazz.getDeclaredMethod("getChildren");
            m.setAccessible(true);
            final List<Object> pickleRunnerList = (List<Object>) m.invoke(fr);
            for (final Object pickleRunner : pickleRunnerList) {
                final Class<?> pickleRunnerClass = pickleRunner.getClass();
                final Field pickleEventField = pickleRunnerClass.getDeclaredField("pickleEvent");
                pickleEventField.setAccessible(true);
                final PickleEvent pickleEvent = (PickleEvent) pickleEventField.get(pickleRunner);
                pickleEvent.pickle.getTags().stream()
                        .map(PickleTag::getName)
                        .filter(tag -> tag.replace("@", "").startsWith(ScenarioDataStorage.PROJECT + "-T"))
                        .findFirst()
                        .ifPresent(allTags::add);
            }
        }
    }

    public synchronized void removeTags(final Scenario scenario) {
        allTags.removeIf(t -> scenario.getSourceTagNames().contains(t));
    }

    public synchronized void collectFailedTag(final Scenario scenario) {
        if (scenario.isFailed()) {
            scenario.getSourceTagNames().stream()
                    .filter(t -> t.replace("@", "").startsWith(ScenarioDataStorage.PROJECT + "-T"))
                    .findFirst()
                    .ifPresent(failedTags::add);
        }
    }

    @SneakyThrows
    public void saveData() {
        final File report = new File("target/failedTagsList.txt");
        final List<String> info = new ArrayList<>();
        info.add(
                failedTags.isEmpty() ?
                        "<b>Все тесты прошли успешно</b>" :
                        "Строка запуска для упавших тестов: <b>" + String.join(" or ", failedTags) + "</b>"
        );
        info.add(
                allTags.isEmpty() ?
                        "Нет пропущенных тестов" :
                        "Пропущенные тесты: <b>" + String.join(" or ", allTags) + "</b>"
        );
        FileUtils.write(
                report,
                String.join("<br>", info),
                StandardCharsets.UTF_8
        );
    }
}
