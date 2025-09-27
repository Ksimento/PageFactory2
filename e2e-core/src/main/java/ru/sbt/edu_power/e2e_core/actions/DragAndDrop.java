package ru.sbt.edu_power.e2e_core.actions;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.MoveTargetOutOfBoundsException;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.e2e_core.fields.FindUtils;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbtqa.tag.pagefactory.environment.Environment;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

public class DragAndDrop {
    private String typeDnd;
    private WebElement elementDrag;


    public void dragAndDropElement(final WebElement element, final String offsetString) {
        final WebElement remoteElement = Mover.getRemoteElement(element);
        final Map<String, Integer> offset = Mover.getOffset(remoteElement.getSize(), offsetString, false);
        dragAndDrop(remoteElement, offset.get("xOffset"), offset.get("yOffset"));
    }

    // Стандартное перетаскивание элемента
    private void dragAndDrop(final WebElement element, final int xOffset, final int yOffset) {
        Mover.getActions()
             .moveToElement(element)
             .clickAndHold()
             .pause(2000)
             .moveByOffset(xOffset, yOffset)
             .release(element)
             .pause(1000)
             .build()
             .perform();
    }

    // Метод предназначен для перетаскивания элементов которые не могут быть перемещены быстро, так же решается проблема
    // с автоскролом до 2-го элемента
    private void dragAndDropAdaptiveScroll(
            final String elementName,
            final String elementName2,
            final String controlElementName
    ) {
        Assert.assertNotEquals("Не задан контрольный элемент", "", controlElementName);
        final WebElement element = FindUtils.getElementByNameOrPath(elementName);
        final WebElement element2 = FindUtils.getElementByNameOrPath(elementName2);
        final WebElement elementControl = FindUtils.getElementByNameOrPath(controlElementName);
        final Dimension dropPosition = new Dimension((element2.getLocation().getX() +
                                                      element2.getSize().width) -
                                                     (elementControl.getLocation().getX() +
                                                      elementControl.getSize().width), (element2.getLocation().getY() +
                                                                                        element2.getSize().height) -
                                                                                       (elementControl
                                                                                                .getLocation()
                                                                                                .getY() +
                                                                                        elementControl.getSize().height));
        final int measurementPx = 8;
        final Dimension sizeWindow = Environment
                .getDriverService()
                .getDriver()
                .findElement(By.xpath("//body"))
                .getSize();
        Mover.getActions()
             .moveToElement(Mover.getRemoteElement(element))
             .pause(2000)
             .clickAndHold()
             .pause(1000)
             .perform();
        final BooleanSupplier dragAndDropElement = () -> {
            final WebElement elemDrag = FindUtils.getElementByNameOrPath(elementName);
            final int xExpected = ((elemDrag.getLocation().getX() +
                                    elemDrag.getSize().width) -
                                   ((elementControl.getLocation().getX() +
                                     elementControl.getSize().width) + dropPosition.width));
            final int yExpected = (elemDrag.getLocation().getY() +
                                   elemDrag.getSize().height) -
                                  ((elementControl.getLocation().getY() +
                                    elementControl.getSize().height) + dropPosition.height);
            final int xAbs = Math.abs(xExpected);
            final int yAbs = Math.abs(yExpected);
            final int speedX = getSpeedRelativeScreen(
                    -xExpected,
                    elemDrag.getLocation().getX() + elemDrag.getSize().width / 2,
                    sizeWindow.width
            );
            final int speedY = getSpeedRelativeScreen(
                    -yExpected,
                    elemDrag.getLocation().getY() + elemDrag.getSize().height / 2,
                    sizeWindow.height
            );
            try {
                if (xAbs >= measurementPx && yAbs >= measurementPx) {
                    Mover.getActions().moveByOffset(speedX, speedY).build().perform();
                    return false;
                } else if (xAbs >= measurementPx) {
                    Mover.getActions().moveByOffset(speedX, 0).build().perform();
                    return false;
                } else if (yAbs >= measurementPx) {
                    Mover.getActions().moveByOffset(0, speedY).build().perform();
                    Mover.markMousePosition();
                    return false;
                }
            } catch (final MoveTargetOutOfBoundsException e) {
                throw new AutotestError("Упс, мышь зашла за пределы экрана", e);
            }
            Mover.getActions().release().build().perform();
            return true;
        };

        Timer.executeTimerMillis(
                "Не удалось перетащить элемент по истечению таймаута",
                DriverConstants.TIMEOUT * 1000L,
                dragAndDropElement
        );
    }

    private int getSpeedRelativeScreen(final int stepPx, final int elementPosition, final int sizeWindowPx) {
        int newStepPx = stepPx;
        final int stepAbs = Math.abs(stepPx);
        // Ограничиваем длину шага так как ui не успевает за перемещением мыши
        if (stepAbs > 65) {
            for (int i = 1; i < 50; i++) {
                final int stepFix = stepAbs / i;
                if (stepFix <= 65 && stepFix >= 30) {
                    newStepPx = stepPx / i;
                }
            }
        }
        //Если будущая позиция находится за разрешением x/y - 50 то делим шаг на 2
        //автоскролл будут работать медленнее если шаг будет короче
        while (newStepPx + elementPosition > sizeWindowPx - 50 |
               newStepPx + elementPosition < 50) {
            newStepPx = newStepPx / 2;
        }
        return newStepPx;
    }


    //Метод перетаскивает элементы с помощью JS
    private static void dragAndDropJs(final WebElement element, final WebElement element2) {
        final String js =
                "const triggerDragAndDrop=function(arguments) { let dragStartEvent; const elemDrag=arguments[0];\n" +
                "const elemDrop=arguments[1];if(!elemDrag||!elemDrop){return false;}\n" +
                "function createNewDataTransfer(){let data ={};return{clearData:function(key){if (key === undefined) {\n" +
                "data={};}else{delete data[key];}},getData:function(key){return data[key];},setData:function(key, value) {\n" +
                "data[key]=value;},setDragImage: function () {},dropEffect: 'none',files:[],items:[],types:[]};}\n" +
                "function fireMouseEvent(type,elem,dataTransfer){const evt=document.createEvent('MouseEvents');evt.initMouseEvent(\n" +
                "type,true,true,window,1,1,1,0,0,false,false,false,false,0,elem);if(/^dr/i.test(type)){\n" +
                "evt.dataTransfer = dataTransfer || createNewDataTransfer();}elem.dispatchEvent(evt);return evt;}\n" +
                "fireMouseEvent('mousedown',elemDrag);dragStartEvent=fireMouseEvent('dragstart',elemDrag);\n" +
                "fireMouseEvent('drop', elemDrop, dragStartEvent.dataTransfer);\n" +
                "fireMouseEvent('dragend', elemDrag, dragStartEvent.dataTransfer);\n" +
                "fireMouseEvent('mouseup', elemDrop);return true;};triggerDragAndDrop(arguments);";
        ((JavascriptExecutor) Environment.getDriverService().getDriver()).executeScript(
                js,
                element,
                element2
        );
    }

    // Метод позволяет перенести элемент на элемент
    public void dragAndDropElementOnElement(
            final String elementName,
            final String elementName2,
            final String controlElementName
    ) {
        final WebElement element = FindUtils.getElementByNameOrPath(elementName);
        final WebElement element2 = FindUtils.getElementByNameOrPath(elementName2);
        final int xdExpected = element.getLocation().getX() - element2.getLocation().getX();
        final int ydExpected = element.getLocation().getY() - element2.getLocation().getY();
        setTypeDragAndDrop(element);
        switch (this.typeDnd) {
            case "DND_JS":
                dragAndDropJs(this.elementDrag, element2);
                break;
            case "DND_ADAPTIVE_SCROLL":
                dragAndDropAdaptiveScroll(elementName, elementName2, controlElementName);
                break;
            case "DND_DEFAULT":
                dragAndDrop(Mover.getRemoteElement(element), xdExpected, ydExpected);
                break;
            default:
                throw new AutotestError(String.format("dragAndDrop \"%s\" элемента не реализован", elementName));
        }

    }

    // Метод определяет тип элемента и сам элемент который может быть перенесен
    private void setTypeDragAndDrop(final WebElement element) {
        final String xpathElement = DriverUtils.getElementXPath(element);
        final String xpathElementDraggableTrue = xpathElement +
                                                 "//preceding::*[@draggable='true'] | " +
                                                 xpathElement +
                                                 "//descendant-or-self::*[@draggable='true']";
        final String xpathElementDraggableFalse = xpathElement +
                                                  "//preceding::*[@draggable='false' and contains(@data-testid,'LessonsConstructor.LessonContainer.LessonCard')] | " +
                                                  xpathElement +
                                                  "//descendant-or-self::*[@draggable='false' and contains(@data-testid,'LessonsConstructor.LessonContainer.LessonCard')]";
        final List<WebElement> jsDragAndDropElements = DriverUtils
                .findElementsOnPageByXpath(
                        xpathElementDraggableTrue);
        final List<WebElement> dragAdnDropAdaptiveScrollElements = DriverUtils
                .findElementsOnPageByXpath(
                        xpathElementDraggableFalse);
        if (!jsDragAndDropElements.isEmpty()) {
            this.typeDnd = "DND_JS";
            this.elementDrag = jsDragAndDropElements.get(0);
            return;
        }
        if (!dragAdnDropAdaptiveScrollElements.isEmpty()) {
            this.typeDnd = "DND_ADAPTIVE_SCROLL";
            this.elementDrag = dragAdnDropAdaptiveScrollElements.get(0);
            return;
        }
        this.typeDnd = "DND_DEFAULT";
        this.elementDrag = element;
    }
}
