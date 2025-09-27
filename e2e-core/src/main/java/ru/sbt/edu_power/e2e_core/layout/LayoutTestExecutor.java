package ru.sbt.edu_power.e2e_core.layout;

import io.qameta.allure.Allure;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.actions.Mover;
import ru.sbt.edu_power.e2e_core.allure.AllureUtils;
import ru.sbt.edu_power.e2e_core.blocks.BlockExtractor;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.e2e_core.elements.text_input.TextInput;
import ru.sbt.edu_power.e2e_core.fields.FindUtils;
import ru.sbt.edu_power.e2e_core.layout.enums.DimensionEnum;
import ru.sbt.edu_power.e2e_core.layout.enums.MeasuringTypes;
import ru.sbt.edu_power.e2e_core.layout.enums.TestingMode;
import ru.sbt.edu_power.e2e_core.layout.repositories.DataSetElement;
import ru.sbt.edu_power.e2e_core.layout.repositories.DataSetFormsRepository;
import ru.sbt.edu_power.e2e_core.layout.repositories.DataSetImageRepository;
import ru.sbt.edu_power.e2e_core.layout.repositories.DataSetRepository;
import ru.sbt.edu_power.e2e_core.layout.repositories.DataSetTextRepository;
import ru.sbt.edu_power.e2e_core.smoke_layout.ScreenShotCollection;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbtqa.tag.pagefactory.PageManager;
import ru.sbtqa.tag.pagefactory.context.PageContext;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

public class LayoutTestExecutor {
    private final boolean selfRelative;
    private final String containerName;
    private final String screenPath;
    private final String scenarioName;
    private final Map<String, String> data;
    private final DataSetCollection actualDataSetCollection = new DataSetCollection();
    private final TestingMode testingMode;
    private String cleanScreenPath;
    private String cleanScenarioName;
    private WebElement container;
    private DimensionEnum currentDimension;
    private final Map<String, WebElement> elementNameToWebElement = new HashMap<>();

    /**
     * @param screenPath   Перечень экранов (в соответствии со структурой РТМ)
     *                     в виде Роль; Раздел; Экран 1; Экран 2; Экран 3
     *                     По этому пути будет сохранён сценарий в файловой системе
     * @param scenarioName Название сценария. Это название будет использовано для имени файла сценария
     * @param data         Дататэйбл
     */
    public LayoutTestExecutor(
            final TestingMode testingMode,
            final boolean selfRelative,
            final String containerName,
            final String screenPath,
            final String scenarioName,
            final Map<String, String> data
    ) {
        this.selfRelative = selfRelative;
        this.containerName = containerName;
        this.screenPath = screenPath;
        this.scenarioName = scenarioName;
        this.data = data;
        this.testingMode = testingMode;
    }

    public void execute() {
        prepare();
        final String coverProperty = System.getProperty("smoke.layout.get.screenshots");
        if (Objects.nonNull(coverProperty) && !coverProperty.isEmpty()) {
            generateScreenShots();
            return;
        }
        collectActualDataSets();
        sortElementsByTree();
        if (DataSetFileUtils.isDataSetExists(currentDimension, cleanScreenPath, cleanScenarioName)) {
            matchDataSets();
        } else {
            writeDataSetToDisk();
        }
        DataSetVisualisation.clearVisualization();
    }

    private void generateScreenShots() {
        final Map<String, List<String>> measureTypeToElementList = new HashMap<>();
        data.forEach((element, type) -> {
            if (!measureTypeToElementList.containsKey(type)) {
                measureTypeToElementList.put(type, new ArrayList<>());
            }
            measureTypeToElementList.get(type).add(element);
        });
        measureTypeToElementList.forEach((type, list) ->
                ScreenShotCollection
                        .getINSTANCE()
                        .create(PageContext.getCurrentPage().getClass(), type, list)
        );
    }

    private void matchDataSets() {
        final String expectedDataSet;
        try {
            expectedDataSet = DataSetFileUtils.dataSetRead(
                    currentDimension,
                    cleanScreenPath,
                    cleanScenarioName
            );
        } catch (final IOException e) {
            throw new AutotestError("Не удалось прочитать дата-сет с диска", e);
        }
        try (final InputStream stream = new ByteArrayInputStream(actualDataSetCollection
                .getJson()
                .getBytes(StandardCharsets.UTF_8))
        ) {
            Allure.addAttachment("actual data set", "json", stream, "json");
        } catch (final IOException e) {
            throw new AutotestError("Не удалось обработать актуальный дата-сет", e);
        }
        final DataSetCollection expectedDataSetCollection = new DataSetCollection(expectedDataSet);
        final ErrorReporter errorReporter = new ErrorReporter(
                testingMode,
                expectedDataSetCollection,
                actualDataSetCollection,
                new ArrayList<>(),
                true
        );
        errorReporter.generate();
    }

    private void writeDataSetToDisk() {
        final String executionEnvironment = System.getProperty("execution.environment");
        if ("jenkins".equals(executionEnvironment)) {
            throw new AutotestError(String.format("Для сценария не создан дата-сет для %s. Дата-сет можно создать только в локальном запуске", currentDimension)
            );
        }
//            if (!sizeOnly) {
//                markIgnoredElementsInDataSet(dataSetCollection);
//            }
        excludeDataFromDataSet();
        try {
            DataSetFileUtils.dataSetWrite(
                    currentDimension,
                    actualDataSetCollection.getJson(),
                    cleanScreenPath,
                    cleanScenarioName
            );
            AllureUtils.attachScreenShotToAllure("Экран дата-сета");
        } catch (final IOException e) {
            throw new AutotestError("Не удалось записать новый дата-сет на диск", e);
        }
    }

    private void excludeDataFromDataSet() {
        actualDataSetCollection.forEach(dse -> {
            final Map<Class<? extends DataSetRepository>, List<String>> exclusionDataList = getExclusionData(
                    dse.getElementName(),
                    elementNameToWebElement.get(dse.getElementName())
            );

            // заменяем текст звёздочкой
            if (!exclusionDataList.isEmpty()) {
                excludeTextElements(dse.getParams(), exclusionDataList.get(DataSetTextRepository.class));
                excludeFormsElements(dse.getParams(), exclusionDataList.get(DataSetFormsRepository.class));
                excludeImageElements(dse.getParams(), exclusionDataList.get(DataSetImageRepository.class));
            }
        });
    }

    private void sortElementsByTree() {
        final ElementsTree elementsTree = new ElementsTree();
        actualDataSetCollection.forEach(e -> elementsTree.put(e.getUuid(), e.getElement()));
        elementsTree.requestElementsTree();
        actualDataSetCollection.forEach(e -> {
            final List<String> children = elementsTree.getParentToChildrenMap().get(e.getUuid());
            if (Objects.nonNull(children)) {
                children.forEach(e::addChild);
            }
            final String parent = elementsTree.getParent(e.getUuid());
            e.setParent(Objects.isNull(parent) ? "" : parent);
        });
    }

    private void collectActualDataSets() {
        data.forEach(this::collectActualDataSet);
    }

    private void collectActualDataSet(final String fieldName, final String typeEnumeration) {
        final WebElement element = FindUtils.getElementByNameOrPath(fieldName);
        elementNameToWebElement.put(fieldName, element);

        if (selfRelative) {
            // минимально допустимый процент видимости элемента при котором продолжаем
            // делать тест, иначе выполняем скролл
            final int MIN_ELEMENT_VISIBLE_SQUARE = 60;
            if (Mover.getElementVisibleSquarePercent(element) < MIN_ELEMENT_VISIBLE_SQUARE) {
                Mover.scrollToElement(element);
            }
        }
        getMeasuringTypeList(typeEnumeration)
                .forEach(measuringType -> {
                    if (selfRelative && measuringType == MeasuringTypes.POSITION) {
                        throw new AutotestError("Нельзя использовать тип POSITION в этом шаге");
                    }
                    final List<DataSetElement> dataSetElements = MeasureElements.getDataSetRepositoryList(
                            element,
                            selfRelative ? element : container,
                            measuringType,
                            fieldName
                    );
                    actualDataSetCollection.addAll(dataSetElements);
                });
    }

    private void excludeTextElements(final DataSetRepository repository, final List<String> exclusionList) {
        if (repository instanceof DataSetTextRepository) {
            if (exclusionList.contains(((DataSetTextRepository) repository).getContent())) {
                ((DataSetTextRepository) repository).setContent("*");
            }
        }
    }

    private void excludeFormsElements(final DataSetRepository repository, final List<String> exclusionList) {
        if (repository instanceof DataSetFormsRepository) {
            if (exclusionList.contains(((DataSetFormsRepository) repository).getValue())) {
                ((DataSetFormsRepository) repository).setValue("*");
            }
            if (exclusionList.contains(((DataSetFormsRepository) repository).getPlaceholder())) {
                ((DataSetFormsRepository) repository).setPlaceholder("*");
            }
        }
    }

    private void excludeImageElements(final DataSetRepository repository, final List<String> exclusionList) {
        if (repository instanceof DataSetImageRepository) {
            if (exclusionList.contains(((DataSetImageRepository) repository).getSrc())) {
                ((DataSetImageRepository) repository).setSrc("*");
            }
        }
    }

    private Map<Class<? extends DataSetRepository>, List<String>> getExclusionData(
            final String elementName,
            final WebElement element
    ) {
        final String cleanElementName = elementName.split("->")[0].trim();
        final Field fieldByName = PageManager.getPageRepository()
                .get(PageContext.getCurrentPage().getClass())
                .entrySet()
                .stream()
                .filter(e -> e.getValue().equals(cleanElementName))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
        if (Objects.isNull(fieldByName)) {
            return new HashMap<>();
        }
        if (fieldByName.isAnnotationPresent(LayoutIgnore.class)) {
            final List<String> xpath = Arrays.asList(fieldByName.getAnnotation(LayoutIgnore.class).xpath());
            final List<WebElement> exclusionElements = xpath.stream()
                    .map(x -> element.findElements(By.xpath(x)))
                    .flatMap(List::stream)
                    .collect(Collectors.toList());

            final Map<Class<? extends DataSetRepository>, List<String>> typeToContent = new HashMap<>();
            typeToContent.put(DataSetTextRepository.class, new ArrayList<>());
            typeToContent.put(DataSetImageRepository.class, new ArrayList<>());
            typeToContent.put(DataSetFormsRepository.class, new ArrayList<>());
            exclusionElements.forEach(e -> {
                if ("img".equals(e.getTagName())) {
                    typeToContent.get(DataSetImageRepository.class).add(e.getAttribute("href"));
                } else if ("input".equals(e.getTagName()) || "textarea".equals(e.getTagName())) {
                    final TextInput input = new TextInput(e);
                    typeToContent.get(DataSetFormsRepository.class).add(input.getText());
                    typeToContent.get(DataSetFormsRepository.class).add(input.getAttribute("placeholder"));
                } else {
                    typeToContent.get(DataSetTextRepository.class).add(e.getText());
                }
            });
            return typeToContent;
        }
        return new HashMap<>();
    }

    private void prepare() {
        cleanScreenPath = LayoutUtils.badSymbolReplace(screenPath);
        cleanScenarioName = LayoutUtils.badSymbolReplace(scenarioName);
        container = Objects.isNull(containerName) ? null : FindUtils.getElementByNameOrPath(containerName);
        currentDimension = LayoutUtils.getCurrentDimension();
        LayoutUtils.setWindowSize(currentDimension);
        final String screenSizePreset = System.getProperty("webdriver.browser.size.preset", "DEFAULT");
        final double scaleFactor = DimensionEnum.valueOf(screenSizePreset).getScaleFactor();
        removeSystemMessages();
        DriverUtils.executeJS(JSResources.getPageScaleScript(scaleFactor));
        DriverUtils.executeJS(JSResources.getDecorationScript());
        DriverUtils.executeJS(JSResources.getLangChangeScript());
        DriverUtils.executeJS(JSResources.getTextMeasuringScript());
        DriverUtils.executeJS(JSResources.getBeforeAfterScript());
        DriverUtils.freeze(DriverConstants.FREEZE_250_MS);
    }

    /**
     * Преобразую перечень желаемых типов измерений (из дататейбла сценария) в список приведённых к стандартному enum
     *
     * @param typeEnumeration перечень через пробел, точку с запятой или запятую
     * @return
     */
    private List<MeasuringTypes> getMeasuringTypeList(final String typeEnumeration) {
        return Arrays
                .stream(typeEnumeration.split("[\\s;,]"))
                .map(String::trim)
                .map(MeasuringTypes::getMeasuringType)
                .collect(Collectors.toList());
    }

    /**
     * Метод помечает в датасете элементы которые нужно игнорировать при проверке
     *
     * @param dataSetElements датасет снятый с экрана
     */
    private void markIgnoredElementsInDataSet(final DataSetCollection dataSetElements) {
        DataSetVisualisation.snapshotVisualisation(dataSetElements);

        if ("Safari".equals(System.getProperty("webdriver.browser.name"))) {
            DriverUtils.freeze(DriverConstants.FREEZE_500_MS * 10);
            return;
        }

        final List<WebElement> element = new ArrayList<>();
        final BooleanSupplier waitWhenIgnoredListBeFinished = () -> {
            element.addAll(DriverUtils.findElementsOnPageByXpath("//div[@data-ready = 'true']"));
            return !element.isEmpty();
        };

        Timer.executeTimerThrowable(
                DriverConstants.TIMEOUT * 30,
                "Для завершения сценария нужно нажать кнопку \"Создать\" на экране тестируемой формы",
                waitWhenIgnoredListBeFinished
        );

        final String[] ignoredList = element.get(0).getAttribute("data-ignoredList").split(",");

        Arrays.stream(ignoredList)
                .filter(s -> s.startsWith("id_"))
                .map(s -> s.replace("id_", ""))
                .forEach(s -> dataSetElements.getByUuid(s).setIgnored(true));
    }

    //    Метод закрывает системные оповещения если таковые есть на экране
    private void removeSystemMessages() {
        final List<WebElement> notificationList = new BlockExtractor("Список оповещений").getBlockCollection();
        if (notificationList.isEmpty()) {
            return;
        }
        notificationList
                .forEach(element ->
                        DriverUtils.executeJS("arguments[0].parentNode.removeChild(arguments[0])", element
                        ));
    }
}
