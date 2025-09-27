package ru.sbt.edu_power.e2e_core.layout;

import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.e2e_core.layout.enums.ElementsColor;
import ru.sbt.edu_power.e2e_core.layout.repositories.DataSetElement;

import java.util.Objects;

public class DataSetVisualisation {

    // метод выполняет визуализацию добавляемых в снапшот данных
    static void snapshotVisualisation(final DataSetCollection dataSetElements) {
        DriverUtils.executeJS(JSResources.getSwitcherScript());
        DriverUtils.executeJS(JSResources.getFinishingScript());
        setGlobalContainer();
        dataSetElements.forEach(DataSetVisualisation::addElementToScreen);
        if (!"Safari".equals(System.getProperty("webdriver.browser.name"))) {
            DriverUtils.executeJS(JSResources.getCreateButton());
        }
    }

    public static void setGlobalContainer() {
        if (DriverUtils.findElementsOnPageByXpath("//div[@id = 'visualisationContainer']").isEmpty()) {
            DriverUtils.executeJS(JSResources.getGlobalContainerScript());
//            DriverUtils.executeJS(JSResources.getChangeScrollStyle());
            DriverUtils.freeze(DriverConstants.FREEZE_250_MS);
        }
    }

    public static void clearVisualization() {
        DriverUtils.executeJS(JSResources.getClearScript());
    }

    public static void addElementsToScreen(final DataSetCollection dataSetElements, final ElementsColor color) {
        dataSetElements.forEach(k -> addElementToScreen(k, color));
    }
    static void addCompleteButtonToScreen() {
        DriverUtils.executeJS(JSResources.getCompleteButton());
    }

    public static void addElementToScreen(final DataSetElement dataSetElement) {
        addElementToScreen(dataSetElement, ElementsColor.DEFAULT);
    }

    static void addElementToScreen(
            final DataSetElement dataSetElement,
            final ElementsColor color
    ) {
        if (Objects.nonNull(dataSetElement.getContainer())) {
            DriverUtils.executeJS(JSResources.getBlock(dataSetElement.getUuid(), dataSetElement, color));
        }
    }
}
