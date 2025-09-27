package ru.sbt.edu_power.e2e_core.layout;

import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.e2e_core.layout.enums.TestingMode;
import ru.sbt.edu_power.e2e_core.layout.repositories.DataSetElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

@Slf4j
public final class ElementPairCollection extends ArrayList<ElementPair> {
    private static final long serialVersionUID = 7774973660185675564L;
    private final Map<String, String> expectedToActualMap = new HashMap<>();
    private final Set<DataSetElement> expectedValidElements = new HashSet<>();
    private final Set<DataSetElement> actualValidElements = new HashSet<>();

    public ElementPairCollection() {
    }

    public void normalizeCollection(final TestingMode testingMode) {
        removeIfEqual();
        removeIfDifferentContext();
        removeByPositionToleranceWithEqualsContent(testingMode);
        collectByEqualsContentAndDifferentPosition();
        collectByPositionTolerance(testingMode);
    }

    public Set<DataSetElement> getExpectedValidElements() {
        return expectedValidElements;
    }

    public Set<DataSetElement> getActualValidElements() {
        return actualValidElements;
    }

    public Map<String, String> getExpectedToActualMap() {
        return expectedToActualMap;
    }

    private void removeIfEqual() {
        final ElementPairCollection equalsList = stream()
                .filter(ElementPair::isEquals)
                .peek(this::collectValidElements)
                .collect(Collectors.toCollection(ElementPairCollection::new));
        removeIf(ep -> equalsList
                .stream()
                .anyMatch(epEq -> epEq.getExpected().getUuid().equals(ep.getExpected().getUuid())));
        removeIf(ep -> equalsList
                .stream()
                .anyMatch(epEq -> epEq.getActual().getUuid().equals(ep.getActual().getUuid())));
    }

    private void removeIfDifferentContext() {
        removeIf(ep -> !ep.isRelated());
    }

    // удаляем пары, в которых контент идентичен, но есть допустимые отклонения по размеру / расположению
    private void removeByPositionToleranceWithEqualsContent(final TestingMode testingMode) {
        final int maxTolerance = testingMode == TestingMode.FULL_LAYOUT ? 6 : 100;
        LongStream.range(0, maxTolerance).forEach(this::removeByPositionToleranceWithEqualsContent);
    }

    private void removeByPositionToleranceWithEqualsContent(final long precision) {
        if (isEmpty()) {
            return;
        }
        final ElementPairCollection toleranceElements = stream()
                .filter(ElementPair::isEqualContent)
                .filter(ep -> ep.comparePosition(precision))
                .peek(this::collectValidElements)
                .collect(Collectors.toCollection(ElementPairCollection::new));
        removeIf(ep -> toleranceElements.stream().anyMatch(tep -> ep.getExpected().equals(tep.getExpected())));
        removeIf(ep -> toleranceElements.stream().anyMatch(tep -> ep.getActual().equals(tep.getActual())));
    }

    // к этому моменту остались только элементы:
    //   1. со схожей позицией но разным контентом
    //   2. с идентичным контентом, но разными позициями,
    //   3. c разными позициями и разным контентом
    // Здесь отфильтровываем элементы с идентичным контентом, но разными позициями
    // Сначала сопоставляем самые близкие позиции и постепенно увеличиваем допустимую погрешность
    private void collectByEqualsContentAndDifferentPosition() {
        final ElementPairCollection equalContentElements = stream()
                .filter(ElementPair::isEqualContent)
                .collect(Collectors.toCollection(ElementPairCollection::new));
        for (long i = 6; i <= 100; i = i + 3) {
            if (equalContentElements.stream().allMatch(ElementPair::isDifferentSizeOrPosition)) {
                break;
            }
            collectByEqualsContentAndDifferentPosition(i, equalContentElements);
        }
    }

    private void collectByEqualsContentAndDifferentPosition(
            final long precision,
            final ElementPairCollection elementPairs
    ) {
        final ElementPairCollection filtered = elementPairs
                .stream()
                .filter(ep -> !ep.isDifferentSizeOrPosition())
                .filter(ep -> ep.comparePosition(precision))
                .peek(this::collectValidElements)
                .peek(ElementPair::setDifferentSizeOrPosition)
                .collect(Collectors.toCollection(ElementPairCollection::new));
        // удаляю из исходной коллекции все ноды, в которых есть найденные выше элементы
        // кроме самих найденных выше нод
        elementPairs.removeIf(ep -> filtered.stream().noneMatch(fep -> fep.equals(ep)) &&
                                    filtered.stream()
                                            .anyMatch(fip -> fip
                                                    .getExpected()
                                                    .getUuid()
                                                    .equals(ep.getExpected().getUuid())));
        elementPairs.removeIf(ep -> filtered.stream().noneMatch(fep -> fep.equals(ep)) &&
                                    filtered.stream()
                                            .anyMatch(fip -> fip
                                                    .getActual()
                                                    .getUuid()
                                                    .equals(ep.getActual().getUuid())));
    }

    // Здесь отфильтровываем элементы с различными параметрами, но схожей позицией
    // Эти элементы будут выведены в виде ошибки как элементы с изменившимися параметрами
    public void collectByPositionTolerance(final TestingMode testingMode) {
        final ElementPairCollection toleranceElements = new ElementPairCollection();
        final int maxTolerance = testingMode == TestingMode.FULL_LAYOUT ? 6 : 100;
        LongStream.range(0, maxTolerance).forEach(p -> removeByPositionTolerance(p, toleranceElements));
        stream().filter(ep -> !ep.isEqualContent())
                .filter(ep -> !ep.isDifferentSizeOrPosition())
                .forEach(ElementPair::setFullDifferent);
        addAll(toleranceElements);
    }

    private void removeByPositionTolerance(final long precision, final ElementPairCollection toleranceElements) {
        final ElementPairCollection filtered = stream()
                .filter(ep -> !ep.isDifferentParameters())
                .filter(ep -> ep.comparePosition(precision))
                .peek(this::collectValidElements)
                .peek(ElementPair::setDifferentParameters)
                .collect(Collectors.toCollection(ElementPairCollection::new));
        removeIf(ep -> filtered.stream().anyMatch(tep -> ep.getExpected().equalsByParams(tep.getExpected())));
        removeIf(ep -> filtered.stream().anyMatch(tep -> ep.getActual().equals(tep.getActual())));
        toleranceElements.addAll(filtered);
    }

    private void collectValidElements(final ElementPair elementPair) {
        expectedValidElements.add(elementPair.getExpected());
        actualValidElements.add(elementPair.getActual());
        expectedToActualMap.put(elementPair.getExpected().getUuid(), elementPair.getActual().getUuid());
    }
}
