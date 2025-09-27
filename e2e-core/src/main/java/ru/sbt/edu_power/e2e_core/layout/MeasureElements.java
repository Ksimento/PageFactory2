package ru.sbt.edu_power.e2e_core.layout;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.external_services.data.Archiver;
import ru.sbt.edu_power.e2e_core.data.ImageProcessing;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.e2e_core.actions.Mover;
import ru.sbt.edu_power.e2e_core.layout.enums.MeasuringTypes;
import ru.sbt.edu_power.e2e_core.layout.repositories.DataSetBeforeAfterRepository;
import ru.sbt.edu_power.e2e_core.layout.repositories.DataSetChamomileItemsRepository;
import ru.sbt.edu_power.e2e_core.layout.repositories.DataSetCompositionRepository;
import ru.sbt.edu_power.e2e_core.layout.repositories.DataSetContainerRepository;
import ru.sbt.edu_power.e2e_core.layout.repositories.DataSetDecorRepository;
import ru.sbt.edu_power.e2e_core.layout.repositories.DataSetElement;
import ru.sbt.edu_power.e2e_core.layout.repositories.DataSetFormsRepository;
import ru.sbt.edu_power.e2e_core.layout.repositories.DataSetImageRepository;
import ru.sbt.edu_power.e2e_core.layout.repositories.DataSetLibraryRepository;
import ru.sbt.edu_power.e2e_core.layout.repositories.DataSetPositionRepository;
import ru.sbt.edu_power.e2e_core.layout.repositories.DataSetSvgRepository;
import ru.sbt.edu_power.e2e_core.layout.repositories.DataSetTextRepository;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MeasureElements {

    /**
     * Метод выполняет измерения типа POSITION для переданного веб-элемента
     *
     * @param element   веб-элемент для выполнения измерений
     * @param container веб-элемент используемый в качестве контейнера, относительно которого ведётся рассчёт позиции
     * @return объект с данными измерений
     */

    private static DataSetPositionRepository getDataSetPositionFromElement(
            final WebElement element,
            final WebElement container
    ) {
        final WebElement remoteElement = Mover.getRemoteElement(element);
        try {
            final Dimension dimension = remoteElement.getSize();
            if (dimension.getWidth() == 0 && dimension.getHeight() == 0) {
                return null;
            }
            final Point elementLocation = remoteElement.getLocation();
            final int containerLocationY;
            final int containerLocationX;
            if (null != container) {
                final WebElement remoteContainer = Mover.getRemoteElement(container);
                final Point containerLocation = remoteContainer.getLocation();
                containerLocationY = containerLocation.getY();
                containerLocationX = containerLocation.getX();
            } else {
                containerLocationY = 0;
                containerLocationX = 0;
            }
            return new DataSetPositionRepository(
                    dimension.getWidth(),
                    dimension.getHeight(),
                    elementLocation.getY() - containerLocationY,
                    elementLocation.getX() - containerLocationX
            );
        } catch (final ElementNotInteractableException e) {
            return null;
        }
    }

    // возвращает размер и позицию псевдоэлемента
    private static DataSetPositionRepository getPseudoElementPosition(
            final WebElement element,
            final WebElement container
    ) {
        final DataSetPositionRepository elementPosition = getDataSetPositionFromElement(element, container);
        if (elementPosition == null) {
            return null;
        }
        final String query = getBeforeAfterQuery(element);
        final String width = (String) DriverUtils.executeJS(query.replace("{p}", "width"), element);
        final String height = (String) DriverUtils.executeJS(query.replace("{p}", "height"), element);
        final String top = (String) DriverUtils.executeJS(query.replace("{p}", "top"), element);
        final String left = (String) DriverUtils.executeJS(query.replace("{p}", "left"), element);
        if (
                "0px".equals(width)
                || "0px".equals(height)
                || "auto".equals(top)
                || "auto".equals(left)
                || "auto".equals(width)
                || "auto".equals(height)
        ) {
            return null;
        }
        return new DataSetPositionRepository(
                DriverUtils.extractNumberFromString(width, 1),
                DriverUtils.extractNumberFromString(height, 1),
                DriverUtils.extractNumberFromString(top, 1) + elementPosition.getTop(),
                DriverUtils.extractNumberFromString(left, 1) + elementPosition.getLeft()
        );
    }

    // строит JS скрипт для получения данных о псевдоэлементе
    private static String getBeforeAfterQuery(final WebElement element) {
        final String classAttr = element.getAttribute("class");
        final String pseudoElement;
        if (classAttr.contains("layoutAfterElement")) {
            pseudoElement = ":after";
        } else if (classAttr.contains("layoutBeforeElement")) {
            pseudoElement = ":before";
        } else {
            throw new AutotestError("Элемент не содержит псевдоэлемент");
        }
        return "return window.getComputedStyle(arguments[0], '{pE}').getPropertyValue('{p}')"
                .replace("{pE}", pseudoElement);
    }

    /**
     * Метод выполняет измерения типа TEXT текстовых блоков внутри веб-элемента
     *
     * @param element веб-элемент для выполнения измерений
     * @return список объектов с данными измерений
     */
    private static DataSetTextRepository getDataSetTextFromElement(final WebElement element) {
        final AtomicReference<String> elementText = new AtomicReference<>(element
                .getText()
                .replaceAll("\u00AD", ""));
        element.findElements(By.xpath(".//span[@class = 'katex']")).forEach(s ->
                elementText.set(elementText.get().replace(s.getText(), " "))
        );
        return new DataSetTextRepository(
                (String) DriverUtils.executeJS("return window.getComputedStyle(arguments[0])['color']", element),
                (String) DriverUtils.executeJS("return window.getComputedStyle(arguments[0])['fontSize']", element),
                (String) DriverUtils.executeJS("return window.getComputedStyle(arguments[0])['fontFamily']", element),
                (String) DriverUtils.executeJS("return window.getComputedStyle(arguments[0])['fontWeight']", element),
                elementText.get(),
                (String) DriverUtils.executeJS("return arguments[0].tagName", element)
        );
    }

    private static DataSetLibraryRepository getDataSetLibraryFromElement(final WebElement element) {
        final AtomicReference<String> elementText = new AtomicReference<>(element
                .getText()
                .replaceAll("\u00AD", ""));
        element.findElements(By.xpath(".//span[@class = 'katex']")).forEach(s ->
                elementText.set(elementText.get().replace(s.getText(), " "))
        );
        return elementText.get().length() < 4 ? null :
                new DataSetLibraryRepository(elementText.get());
    }

    private static DataSetSvgRepository getDataSetSvgFromElement(final WebElement element) {
        final WebElement path_1 = svgPathElement(element, 1);
        final WebElement path_2 = svgPathElement(element, 2);
        final WebElement path_3 = svgPathElement(element, 3);
        return path_1 == null ? null : new DataSetSvgRepository(
                path_1.getAttribute("d"),
                path_2 == null ? "" : path_2.getAttribute("d"),
                path_3 == null ? "" : path_3.getAttribute("d"),
                (String) DriverUtils.executeJS("return window.getComputedStyle(arguments[0])['fill']", element)
        );
    }

    private static WebElement svgPathElement(final WebElement svg, final int number) {
        final List<WebElement> path = svg.findElements(By.xpath(".//*[name() = 'path'][" + number + "]"));
        if (path.isEmpty()) {
            final List<WebElement> use = svg.findElements(By.xpath(".//*[name() = 'use']"));
            if (use.isEmpty()) {
                return null;
            }
            final String id = use.get(0).getAttribute("xlink:href").replace("#", "");
            final List<WebElement> useElements = DriverUtils.findElementsOnPageByXpath(
                    "//*[name() = 'symbol' and @id = '" + id + "']/*[name() = 'path'][" + number + "]"
            );
            return useElements.isEmpty() ? null : useElements.get(0);
        } else {
            return path.get(0);
        }
    }

    //Метод возвращает параметры Ромашки по предметам
    private static DataSetChamomileItemsRepository getDataSetChamomileItemsRepository(final WebElement element) {
        //Список целых лепестков ромашки
        final List<WebElement> chamomilePetalsList = element.findElements(By.xpath(
                ".//*[@data-testid='Widget.StudentProgress.Subject.Petal']/*[1]"));
        //Список лепестков ромашки относительно текущего прогресса
        final List<WebElement> chamomilePetalsCurrentProgressList = element.findElements(By.xpath(
                ".//*[@data-testid='Widget.StudentProgress.Subject.Petal']/*[3]"));
        final String petals = IntStream
                .range(0, chamomilePetalsList.size())
                .mapToObj(i -> chamomilePetalsList.get(i).getAttribute("d") +
                               ";" +
                               chamomilePetalsList.get(i).getAttribute("fill") +
                               ";" +
                               chamomilePetalsCurrentProgressList.get(i).getAttribute("d") +
                               ";" +
                               chamomilePetalsCurrentProgressList.get(i).getAttribute("fill") +
                               ";")
                .collect(Collectors.joining());

        return new DataSetChamomileItemsRepository(
                petals
        );
    }

    private static DataSetImageRepository getDataSetImageFromElement(final WebElement element) {
        final String src = element.getAttribute("src");
        final String bgSrc = (String) DriverUtils.executeJS(
                "return window.getComputedStyle(arguments[0])['backgroundImage']",
                element
        );
        return src == null && "none".equals(bgSrc) ? null : new DataSetImageRepository(
                urlHostCut(src),
                urlHostCut(bgSrc)
        );
    }

    private static DataSetCompositionRepository getDataSetCompositionFromElement(final WebElement element) {
        final int vectorCompositionHash = DriverUtils.executeJS("return arguments[0].innerHTML", element).hashCode();
        final byte[] image = ImageProcessing.takeScreenShotFragment(element);
        return new DataSetCompositionRepository(
                vectorCompositionHash,
                new Archiver().setSourceBytes(image).gzipSource().archiveAsBytes()
        );
    }

    private static DataSetFormsRepository getDataSetFormsFromElement(final WebElement element) {
        return new DataSetFormsRepository(
                element.getAttribute("type"),
                element.getAttribute("placeholder"),
                element.getAttribute("value")
        );
    }

    private static DataSetDecorRepository getDataSetDecorFromElement(final WebElement element) {
        return new DataSetDecorRepository(
                (String) DriverUtils.executeJS("return window.getComputedStyle(arguments[0])['border']", element),
                (String) DriverUtils.executeJS("return window.getComputedStyle(arguments[0])['borderRadius']", element),
                (String) DriverUtils.executeJS("return window.getComputedStyle(arguments[0])['boxShadow']", element),
                (String) DriverUtils.executeJS("return window.getComputedStyle(arguments[0])['background']", element)
        );
    }

    private static DataSetBeforeAfterRepository getDataSetBeforeAfterFromElement(final WebElement element) {
        final String query = getBeforeAfterQuery(element);
        return new DataSetBeforeAfterRepository(
                (String) DriverUtils.executeJS(query.replace("{p}", "content"), element),
                (String) DriverUtils.executeJS(query.replace("{p}", "color"), element),
                (String) DriverUtils.executeJS(query.replace("{p}", "background"), element)
        );
    }

    private static DataSetContainerRepository getDataSetContainerFromElement(final WebElement element) {
        final int xPos;
        final int yPos;
        if (null == element) {
            xPos = 0;
            yPos = 0;
        } else {
            final WebElement remoteElement = Mover.getRemoteElement(element);
            final Point point = remoteElement.getLocation();
            xPos = point.getX();
            yPos = point.getY();
        }
        return new DataSetContainerRepository(yPos, xPos);
    }

    private static String urlHostCut(final String url) {
        if (null == url || "".equals(url)) {
            return "";
        }
        if (url.contains("https://") || url.contains("http://")) {
            return url.replace("\")", "").split("/", 4)[3];
        }
        return url;
    }

    public static List<DataSetElement> getDataSetRepositoryList(
            final WebElement element,
            final WebElement container,
            final MeasuringTypes measuringType,
            final String elementName
    ) {
        final List<WebElement> elements = element.findElements(By.xpath(measuringType.getXpath()));
        final DataSetContainerRepository containerRepository = getDataSetContainerFromElement(container);
        switch (measuringType) {
            case KATEX:
            case TEXT:
                return elements.stream().map(e -> {
                    final DataSetPositionRepository position = getDataSetPositionFromElement(e, container);
                    return position == null ? null :
                            new DataSetElement(
                                    measuringType,
                                    elementName,
                                    getDataSetTextFromElement(e),
                                    position,
                                    containerRepository,
                                    e
                            );
                }).filter(Objects::nonNull).collect(Collectors.toList());
            case LIBRARY:
                return elements.stream().map(e -> {
                    final DataSetLibraryRepository library = getDataSetLibraryFromElement(e);
                    return null == library ? null :
                            new DataSetElement(
                                    measuringType,
                                    elementName,
                                    library,
                                    getDataSetPositionFromElement(e, container),
                                    null,
                                    e
                            );
                }).filter(Objects::nonNull).collect(Collectors.toList());
            case POSITION:
                return elements.stream().map(e -> {
                    final DataSetPositionRepository position = getDataSetPositionFromElement(e, container);
                    return position == null ? null :
                            new DataSetElement(
                                    measuringType,
                                    elementName,
                                    null,
                                    position,
                                    containerRepository,
                                    e
                            );
                }).filter(Objects::nonNull).collect(Collectors.toList());
            case SVG:
                return elements.stream().map(e -> {
                    final DataSetPositionRepository position = getDataSetPositionFromElement(e, container);
                    final DataSetSvgRepository repository = getDataSetSvgFromElement(e);
                    return position == null || repository == null ? null :
                            new DataSetElement(
                                    measuringType,
                                    elementName,
                                    repository,
                                    position,
                                    containerRepository,
                                    e
                            );
                }).filter(Objects::nonNull).collect(Collectors.toList());
            case CHAMOMILEITEMS:
                return elements.stream().map(e -> {
                    final DataSetPositionRepository position = getDataSetPositionFromElement(e, container);
                    return position == null ? null :
                            new DataSetElement(
                                    measuringType,
                                    elementName,
                                    getDataSetChamomileItemsRepository(e),
                                    position,
                                    containerRepository,
                                    e
                            );
                }).filter(Objects::nonNull).collect(Collectors.toList());
            case IMAGE:
                return elements.stream().map(e -> {
                    final DataSetPositionRepository position = getDataSetPositionFromElement(e, container);
                    final DataSetImageRepository image = position == null ? null : getDataSetImageFromElement(e);
                    return image == null ? null :
                            new DataSetElement(
                                    measuringType,
                                    elementName,
                                    image,
                                    position,
                                    containerRepository,
                                    e
                            );
                }).filter(Objects::nonNull).collect(Collectors.toList());
            case FORMS:
                return elements.stream().map(e -> {
                    final DataSetPositionRepository position = getDataSetPositionFromElement(e, container);
                    return position == null ? null :
                            new DataSetElement(
                                    measuringType,
                                    elementName,
                                    getDataSetFormsFromElement(e),
                                    position,
                                    containerRepository,
                                    e
                            );
                }).filter(Objects::nonNull).collect(Collectors.toList());
            case DECOR:
                return elements.stream().map(e -> {
                    final DataSetPositionRepository position = getDataSetPositionFromElement(e, container);
                    return position == null ? null :
                            new DataSetElement(
                                    measuringType,
                                    elementName,
                                    getDataSetDecorFromElement(e),
                                    position,
                                    containerRepository,
                                    e
                            );
                }).filter(Objects::nonNull).collect(Collectors.toList());
            case BEFORE_AFTER:
                return elements.stream().map(e -> {
                    final DataSetPositionRepository position = getPseudoElementPosition(e, container);
                    return position == null ? null :
                            new DataSetElement(
                                    measuringType,
                                    elementName,
                                    getDataSetBeforeAfterFromElement(e),
                                    position,
                                    containerRepository,
                                    e
                            );
                }).filter(Objects::nonNull).collect(Collectors.toList());
            case COMPOSITION:
                return elements.stream().map(e -> {
                    final DataSetPositionRepository position = getDataSetPositionFromElement(e, container);
                    return position == null ? null :
                            new DataSetElement(
                                    measuringType,
                                    elementName,
                                    getDataSetCompositionFromElement(e),
                                    position,
                                    containerRepository,
                                    e
                            );
                }).filter(Objects::nonNull).collect(Collectors.toList());
            case MULTITYPE:
                final List<DataSetElement> list = new ArrayList<>();
                for (final MeasuringTypes type : MeasuringTypes.values()) {
                    if (!type.isMultitype()) {
                        continue;
                    }
                    list.addAll(getDataSetRepositoryList(element, container, type, elementName));
                }
                return list;
            default:
                throw new AutotestError(String.format(
                        "Для метода \"%s\" не реализован сбор списка данных",
                        measuringType.name()
                ));
        }
    }
}
