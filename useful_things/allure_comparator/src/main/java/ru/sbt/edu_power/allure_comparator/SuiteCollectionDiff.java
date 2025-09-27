package ru.sbt.edu_power.allure_comparator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.ExternalServicesException;
import ru.sbt.edu_power.external_services.jenkins.allure.SuiteCollector;
import ru.sbt.edu_power.external_services.jenkins.allure.enums.Children;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class SuiteCollectionDiff {
    private final String projectKey;
    private final SuiteCollector actual;
    private final SuiteCollector expected;
    private final Map<Children, Children> actualToExpectedChildren = new HashMap<>();
    // мапы Команда -> Список отчётов
    @Getter
    private final TeamCollection added = new TeamCollection();
    @Getter
    private final TeamCollection removed = new TeamCollection();
    @Getter
    private final TeamCollection fixed = new TeamCollection();
    @Getter
    private final TeamCollection broken = new TeamCollection();
    @Getter
    private final TeamCollection skipped = new TeamCollection();

    public SuiteCollectionDiff(
            final String projectKey,
            final SuiteCollector actual,
            final SuiteCollector expected
    ) {
        this.projectKey = projectKey;
        this.actual = actual;
        this.expected = expected;
    }

    public void compare() {
        separate();
        statusDiff();
    }

    // метод удаляет все сценарии из прошлого и текущего прогона с одинаковыми тегами,
    // те что остались - являются новыми и удалёнными сценариями
    private void separate() {
        final Map<String, Map<String, Children>> actualMap = new HashMap<>();
        separateByTags(actualMap, actual);

        final Map<String, Map<String, Children>> expectedMap = new HashMap<>();
        separateByTags(expectedMap, expected);

        actualMap.forEach((team, map) -> {
            updateTeam(team);
            if (!expectedMap.containsKey(team)) {
                added.get(team).addAll(pairsFromList(map.values()));
                return;
            }
            map.forEach((tag, ch) -> {
                if (expectedMap.get(team).containsKey(tag)) {
                    actualToExpectedChildren.put(ch, expectedMap.get(team).get(tag));
                } else {
                    added.get(team).add(new ChildrenPair(ch, ch));
                }
            });
        });

        expectedMap.forEach((team, map) -> {
            updateTeam(team);
            if (!actualMap.containsKey(team)) {
                removed.get(team).addAll(pairsFromList(map.values()));
                return;
            }
            map.forEach((tag, ch) -> {
                if (!actualMap.get(team).containsKey(tag)) {
                    removed.get(team).add(new ChildrenPair(ch, ch));
                }
            });
        });
    }

    private List<ChildrenPair> pairsFromList(final Collection<Children> list) {
        return list.stream()
                .map(e -> new ChildrenPair(e, e))
                .collect(Collectors.toList());
    }

    private void updateTeam(final String team) {
        if (!added.containsKey(team)) {
            added.put(team, new ArrayList<>());
        }
        if (!removed.containsKey(team)) {
            removed.put(team, new ArrayList<>());
        }
        if (!fixed.containsKey(team)) {
            fixed.put(team, new ArrayList<>());
        }
        if (!broken.containsKey(team)) {
            broken.put(team, new ArrayList<>());
        }
        if (!skipped.containsKey(team)) {
            skipped.put(team, new ArrayList<>());
        }

    }

    private void separateByTags(
            final Map<String, Map<String, Children>> consumer,
            final SuiteCollector source
    ) {
        source.getCollection().forEach((team, list) -> {
            final Map<String, Children> map = list
                    .stream()
                    .collect(Collectors.toMap(c -> c.searchTags(projectKey).get(0), c -> c, (a, b) -> b));
            consumer.put(team, map);
        });
    }

    private void statusDiff() {
        actual.getCollection()
                .keySet()
                .forEach(this::compareByTeam);
    }

    private void compareByTeam(final String team) {
        actual.getCollection().get(team).forEach(ac -> {
            final AllureDiffExtraStatus status = compare(ac, actualToExpectedChildren.get(ac));
            switch (status) {
                case UNDEFINED:
                case EQUALS:
                    return;
                case PASS:
                    fixed.get(team).add(new ChildrenPair(ac, actualToExpectedChildren.get(ac)));
                    break;
                case FAIL:
                    broken.get(team).add(new ChildrenPair(ac, actualToExpectedChildren.get(ac)));
                    break;
                case SKIP:
                    skipped.get(team).add(new ChildrenPair(ac, actualToExpectedChildren.get(ac)));
            }
        });
    }

    private AllureDiffExtraStatus compare(final Children actual, final Children expected) {
        try {
            final AllureDiffExtraStatus actualStatus = AllureDiffExtraStatus.getByAllureStatus(actual.getStatus());
            if (Objects.isNull(expected)) {
                return AllureDiffExtraStatus.UNDEFINED;
            }
            final AllureDiffExtraStatus expectedStatus = AllureDiffExtraStatus.getByAllureStatus(expected.getStatus());
            if (actualStatus == expectedStatus) {
                return AllureDiffExtraStatus.EQUALS;
            }
            return actualStatus;
        } catch (final ExternalServicesException e) {
            final Gson gson = new GsonBuilder().setPrettyPrinting().create();
            log.error("actual:\n{}", gson.toJson(actual));
            log.error("expected:\n{}", gson.toJson(expected));
            throw new ExternalServicesException(e);
        }
    }
}
