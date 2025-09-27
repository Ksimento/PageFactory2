package ru.sbt.edu_power.e2e_core.layout;

import io.qameta.allure.Allure;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.e2e_core.layout.enums.ElementsColor;
import ru.sbt.edu_power.e2e_core.layout.enums.TestingMode;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ErrorReporter {
    private final DataSetCollection expectedDataSetCollection;
    private final DataSetCollection actualDataSetCollection;
    private final List<String> errorList;
    private final boolean isRootReporter;
    private final TestingMode testingMode;

    public ErrorReporter(
            final TestingMode testingMode,
            final DataSetCollection expectedDataSetCollection,
            final DataSetCollection actualDataSetCollection,
            final List<String> errorList,
            final boolean isRootReporter
    ) {
        this.testingMode = testingMode;
        this.expectedDataSetCollection = expectedDataSetCollection;
        this.actualDataSetCollection = actualDataSetCollection;
        this.errorList = errorList;
        this.isRootReporter = isRootReporter;
    }

    public void generate() {
        final Map<String, String> expectedToActualMap = testingRootElements();
        testingChildren(expectedToActualMap);
        if (isRootReporter) {
            throwingErrors();
        }
    }


    private Map<String, String> testingRootElements() {
        final DataSetCollection expectedRootCollection = expectedDataSetCollection.getRootCollection();
        final DataSetCollection actualRootCollection = actualDataSetCollection.getRootCollection();
        expectedRootCollection.match(actualRootCollection, testingMode);
        if (!checkIfErrors(expectedRootCollection, actualRootCollection)) {
            DataSetVisualisation.setGlobalContainer();
            displayErrorsWithDifferentParameters(expectedRootCollection.getElementPairs());
            displayErrorsWithDifferentPosition(expectedRootCollection.getElementPairs());
            displayMissingElements(expectedRootCollection);
            displayUnnecessaryElements(actualRootCollection);
            errorsToAllure(expectedRootCollection.getElementPairs());
        }
        return expectedRootCollection.getElementPairs().getExpectedToActualMap();
    }

    private void testingChildren(final Map<String, String> expectedToActualList) {
        expectedToActualList.forEach((e, a) -> {
            final DataSetCollection expectedChildrenTree = new DataSetCollection();
            getChildrenTree(e, expectedDataSetCollection, expectedChildrenTree);
            removeParentUuid(expectedChildrenTree);

            final DataSetCollection actualChildrenTree = new DataSetCollection();
            getChildrenTree(a, actualDataSetCollection, actualChildrenTree);
            removeParentUuid(actualChildrenTree);

            final ErrorReporter reporter = new ErrorReporter(testingMode, expectedChildrenTree, actualChildrenTree, errorList, false);
            reporter.generate();
        });
    }

    private void removeParentUuid(final DataSetCollection dataSetElements) {
        dataSetElements.forEach(e -> {
            if (!e.getParent().isEmpty() && !dataSetElements.isUuidExists(e.getParent())) {
                e.setParent("");
            }
        });
    }

    private void getChildrenTree(
            final String parent,
            final DataSetCollection original,
            final DataSetCollection childrenTree
    ) {
        final DataSetCollection children = original.getChildren(parent);
        childrenTree.addAll(children);
        children.forEach(ch -> getChildrenTree(ch.getUuid(), original, childrenTree));
    }

    // показывает места, где ожидались элементы, но их там нет (недостающие)
    private void displayMissingElements(final DataSetCollection dataSetElements) {
        if (!dataSetElements.isEmpty()) {
            dataSetElements.forEach(e -> {
                DataSetVisualisation.addElementToScreen(e, ElementsColor.ORANGE);
                errorList.add(String.format("Ожидался элемент, но он не найден:\n%s", e));
            });

        }
    }

    // показывает места, где есть элементы, которые не ожидались (лишние)
    private void displayUnnecessaryElements(final DataSetCollection dataSetElements) {
        if (!dataSetElements.isEmpty()) {
            dataSetElements.forEach(e -> {
                DataSetVisualisation.addElementToScreen(e, ElementsColor.VIOLET);
                errorList.add(String.format("Найден элемент, которого раньше не было:\n%s", e));
            });

        }
    }

    private void displayErrorsWithDifferentParameters(final List<ElementPair> elementPairs) {
        final List<ElementPair> pairsWithDifferentParameters = elementPairs
                .stream()
                .filter(ElementPair::isDifferentParameters)
                .collect(Collectors.toList());
        if (!pairsWithDifferentParameters.isEmpty()) {
            pairsWithDifferentParameters.stream()
                                        .map(ElementPair::getActual)
                                        .forEach(element -> DataSetVisualisation.addElementToScreen(
                                                element,
                                                ElementsColor.NAVY
                                        ));
        }
    }

    private void displayErrorsWithDifferentPosition(final List<ElementPair> elementPairs) {
        final List<ElementPair> pairsWithDifferentSizeOrPosition = elementPairs
                .stream()
                .filter(ElementPair::isDifferentSizeOrPosition)
                .collect(Collectors.toList());
        if (!pairsWithDifferentSizeOrPosition.isEmpty()) {
            pairsWithDifferentSizeOrPosition.forEach(ep -> {
                DataSetVisualisation.addElementToScreen(ep.getExpected(), ElementsColor.GREEN);
                DataSetVisualisation.addElementToScreen(ep.getActual(), ElementsColor.RED);
            });
        }
    }

    private void errorsToAllure(final List<ElementPair> pairs) {
        errorList.addAll(pairs
                .stream()
                .map(this::getErrors)
                .collect(Collectors.toList()));

    }

    private void throwingErrors() {
        if (!errorList.isEmpty()) {
            Allure.addAttachment(
                    "Данные о расхождении верстки (Ожидаемое значение : Фактическое значение)",
                    String.join("\n\n", errorList)
            );
            DriverUtils.freeze(5000);
            throw new AutotestError("Ошибка при тестировании верстки");
        }
    }

    private String getErrors(final ElementPair pair) {
        final List<String> errors = new ArrayList<>();
        if (pair.isDifferentSizeOrPosition()) {
            errors.addAll(pair.getExpected().matchByPosition(pair.getActual()));
        }
        if (pair.isDifferentParameters()) {
            errors.addAll(pair.getExpected().matchByParams(pair.getActual()));
        }
        if (!errors.isEmpty()) {
            errors.add(0, "Тип измерения: " + pair.getExpected().getMeasuringType().name());
            errors.add(1, "Название элемента: " + pair.getExpected().getElementName());
        }
        return String.join("\n", errors);
    }

    private boolean checkIfErrors(
            final DataSetCollection expectedRootCollection,
            final DataSetCollection actualRootCollection
    ) {
        return expectedRootCollection.isEmpty() &&
               actualRootCollection.isEmpty() &&
               expectedRootCollection.getElementPairs().isEmpty();
    }
}
