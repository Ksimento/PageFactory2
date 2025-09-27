package ru.sbt.edu_power.e2e_core.actions;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptException;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.MoveTargetOutOfBoundsException;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbtqa.tag.pagefactory.environment.Environment;
import ru.sbtqa.tag.qautils.errors.AutotestError;
import ru.yandex.qatools.htmlelements.element.HtmlElement;
import ru.yandex.qatools.htmlelements.element.TypifiedElement;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

public class Mover {
    /**
     * Метод выполняет перемещение виртуального указателя мыши до указанного элемента
     * Так же можно выполнить клик в том месте, куда навёлся указатель
     * Метод не проверяет кликабельность элемента (например, если он был перекрыт другим элементом)
     *
     * @param element веб-элемент
     */
    private static final String XPATH_GRID = "//div[@data-analytics= 'UIKit.Grid']";

    public static void moveToElement(final WebElement element) {
        moveToElement(element, 0, 0, false);
    }

    public static void moveToElement(final WebElement element, final boolean withControlledScroll) {
        moveToElement(element, 0, 0, withControlledScroll);
    }

    public static void moveToElement(final WebElement element, final int xOffset, final int yOffset) {
        moveToElement(element, xOffset, yOffset, false);
    }

    public static void moveToElement(
            final WebElement element,
            final int xOffset,
            final int yOffset,
            final boolean withControlledScroll
    ) {
        actionToElement(element, xOffset, yOffset, false, withControlledScroll);
    }

    public static void moveAndClick(final WebElement element) {
        moveAndClick(element, 0, 0);
    }

    public static void moveAndClick(final WebElement element, final boolean withControlledScroll) {
        moveAndClick(element, 0, 0, withControlledScroll);
    }

    public static void moveAndClick(final WebElement element, final int xOffset, final int yOffset) {
        moveAndClick(element, xOffset, yOffset, false);
    }

    public static void moveAndClick(
            final WebElement element,
            final int xOffset,
            final int yOffset,
            final boolean withControlledScroll
    ) {
        actionToElement(element, xOffset, yOffset, true, withControlledScroll);
    }

    public static void scrollGridToElement(final WebElement element) {
        final List<WebElement> grid = Environment
                .getDriverService()
                .getDriver()
                .findElements(By.xpath("//div[@data-analytics= 'UIKit.Grid']"));
        if (!grid.isEmpty()) {
            final int gridY = grid.get(0).getLocation().getY();
            final int elementY = element.getLocation().getY();
            if (elementY < gridY) {
                Mover.scrollToElement(element, true);
            }
        }
    }

    public static void actionToElement(
            final WebElement element,
            final int xOffset,
            final int yOffset,
            final boolean click,
            final boolean withControlledScroll
    ) {
        final WebElement interactingElement = getRemoteElement(element);

        final AtomicReference<Throwable> throwable = new AtomicReference<>();

        final BooleanSupplier waitWhenElementBeInteracted = () -> {
            try {
                getActions().moveToElement(interactingElement, xOffset, yOffset).build().perform();
                return true;
            } catch (final MoveTargetOutOfBoundsException | JavascriptException e) {
                throwable.set(e);
                if (withControlledScroll) {
                    scrollToElement(element);
                }
                return false;
            }
        };

        if (!Timer.executeTimer(DriverConstants.TIMEOUT, waitWhenElementBeInteracted)) {
            throw new AutotestError(throwable.get());
        }


        if (click) {
            getActions().click().build().perform();
        }

        DriverUtils.freeze(DriverConstants.FREEZE_250_MS);
    }

    public static WebElement getRemoteElement(final WebElement element) {
        final WebElement remoteElement;
        if (element instanceof TypifiedElement) {
            remoteElement = ((TypifiedElement) element).getWrappedElement();
        } else if (element instanceof HtmlElement) {
            remoteElement = ((HtmlElement) element).getWrappedElement();
        } else {
            remoteElement = element;
        }
        return remoteElement;
    }

    /**
     * Метод перемещает мышь из левого верхнего угла текущего окна просмотра на указанное смещение
     * @param width - смещение на ось x
     * @param height - смещение на ось y
     */
    public static void moveOffset(final int width,final int height){
        PointerInput mouse = new PointerInput(PointerInput.Kind.MOUSE, "default mouse");

        Sequence actions = new Sequence(mouse, 0)
                .addAction(mouse.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), width, height));

        ((RemoteWebDriver) Environment.getDriverService().getDriver()).perform(Collections.singletonList(actions));
    }

    public static Actions getActions() {
        return new Actions((WebDriver) Environment.getDriverService().getDriver());
    }

    // Метод добавляет скрипт отслеживания указателя мыши по координатам
    public static void placeMouseEventScript() {
        if (Objects.isNull(System.getProperty("markMousePosition"))) {
            return;
        }
        Mover.removeHoverMarker();
        final String jsEvent = "function placeMouseEventScript() {" +
                               "let ss = document.createElement('script');" +
                               "ss.id = 'placeMouseEventScript';" +
                               "ss.innerText = 'var selMousePosition = {x: 0, y: 0};document.body.onmousemove = ' +" +
                               "'function(event) {selMousePosition.x = event.clientX;selMousePosition.y = event.clientY;};' +" +
                               "'function getSelMousePosition() {return selMousePosition}';" +
                               "document.body.appendChild(ss);}placeMouseEventScript();";
        DriverUtils.executeJS(jsEvent);
    }

    // Метод рисует на экране квадрат в центре которого находится виртуальный указатель мыши
    @SuppressWarnings("unchecked")
    public static void markMousePosition() {
        if (Objects.isNull(System.getProperty("markMousePosition"))) {
            return;
        }
        final String jsScriptNotInstalled = "return document.getElementById('placeMouseEventScript') == null";
        if ((boolean) DriverUtils.executeJS(jsScriptNotInstalled)) {
            return;
        }
        final String jsPosition = "return getSelMousePosition()";
        final Map<String, Long> position = (Map<String, Long>) DriverUtils.executeJS(jsPosition);
        final int xPos = position.get("x").intValue();
        final int yPos = position.get("y").intValue();
        final String divH = "<div class='hover-marker' style='position:absolute;top:topYpx;left:" +
                            (xPos - 7) +
                            "px;width:15px;height:3px;background-color:red'></div>";
        final String divV = "<div class='hover-marker' style='position:absolute;top:" +
                            (yPos - 7) +
                            "px;left:leftXpx;width:3px;height:15px;background-color:red'></div>";

        final String divH1 = divH.replace("topY", "" + (yPos - 7));
        final String divH2 = divH.replace("topY", "" + (yPos + 5));
        final String divV1 = divV.replace("leftX", "" + (xPos - 7));
        final String divV2 = divV.replace("leftX", "" + (xPos + 5));
        final String insert = "document.body.insertAdjacentHTML('beforeend',\"PLACEHOLDER\");";
        final String jsInsert = insert.replace("PLACEHOLDER", divH1) +
                                insert.replace("PLACEHOLDER", divH2) +
                                insert.replace("PLACEHOLDER", divV1) +
                                insert.replace("PLACEHOLDER", divV2);
        DriverUtils.executeJS(jsInsert);
    }

    // метод удаляет со страницы маркер расположения указателя мыши и скрипт, который определяет расположение
    public static void removeHoverMarker() {
        final String jsRemoveMarker = "[...document.getElementsByClassName('hover-marker')].forEach(c => c.parentNode.removeChild(c))";
        final String jsRemoveScript = "let c = document.getElementById('placeMouseEventScript'); if (c != null) c.parentNode.removeChild(c);";
        DriverUtils.executeJS(jsRemoveMarker);
        DriverUtils.executeJS(jsRemoveScript);
    }

    /**
     * Метод вызывает в браузере JS. Логика кода такая:
     * получаем веб-элемент, до которого нужно докрутить страницу.
     * В цикле перебираем от него родительские узлы сравнивая высоту с предыдущим узлом
     * В нормальном случае высота узлов должна увеличиваться до размера простыни и в одном из
     * следующих родительских элементов высота должна уменьшиться до высоты вьюпорта или меньше.
     * Этот блок и должен содержать скролл, и относительно него выполнится прокрутка до элемента
     */
    public static void scrollToElement(final WebElement element) {
        scrollToElement(element, false);
    }

    public static void scrollToElement(final WebElement element, final boolean force) {
        if (force || getElementVisibleSquarePercent(element) < 98) {
            final String js =
                    "const scroll=function(arguments){const target=arguments[0];let scrl=document.documentElement,prnt=target.parentNode;\n" +
                    "let cnt=prnt;while(true){if(prnt.parentNode===document){break;}if(prnt.scrollHeight>prnt.clientHeight){\n" +
                    "[...prnt.children].forEach(c=>{cnt=cnt.clientHeight>c.clientHeight?cnt:c});\n" +
                    "if (window.getComputedStyle(prnt)['overflow']==='auto'){scrl=prnt;break}}prnt=prnt.parentNode;}\n" +
                    "let distance=target.getBoundingClientRect().y-cnt.getBoundingClientRect().y+target.clientHeight/2-\n" +
                    "((scrl===document.documentElement?0:scrl.getBoundingClientRect().y)+scrl.offsetHeight/2);\n" +
                    "scrl.scroll(0,distance);};scroll(arguments);";
            DriverUtils.executeJS(js, element);
            DriverUtils.freeze(DriverConstants.FREEZE_500_MS);
        }
    }

    // метод считает процент видимости элемента во вьюпорте от полной площади элемента
    public static Integer getElementVisibleSquarePercent(final WebElement element) {
        final WebElement remoteElement = getRemoteElement(element);
        final Dimension elementSize = remoteElement.getSize();
        final Point elementLocation = remoteElement.getLocation();
        final int yElement = (int) Math.round(Double.parseDouble(DriverUtils.executeJS("return arguments[0].getBoundingClientRect().y",element).toString()));
        final Dimension viewport = DriverUtils.findElementsOnPageByXpath("//body").get(0).getSize();
        if (
                elementLocation.getX() > viewport.getWidth()
                || (elementLocation.getX() + elementSize.width) <= 0
                || (yElement + elementSize.height) <= 0
                || yElement > viewport.getHeight()
        ) {
            return 0;
        }
        if (
                (elementLocation.getX() + elementSize.getWidth()) < viewport.getWidth()
                && elementLocation.getX() >= 0 && yElement >= 0
                && (yElement + elementSize.getHeight()) < viewport.getHeight()
        ) {
            return 100;
        }
        final int elementSquare = elementSize.getHeight() * elementSize.getWidth();
        final int visibleLeftX = Math.max(0, elementLocation.getX());
        final int visibleRightX = Math.min(elementLocation.getX() + elementSize.width, viewport.width);
        final int visibleTopY = Math.max(0, yElement);
        final int visibleBottomY = Math.min(yElement + elementSize.height, viewport.height);
        final int visibleHeight = visibleTopY < visibleBottomY ? visibleBottomY - visibleTopY : 0;
        final int visibleWidth = visibleLeftX < visibleRightX ? visibleRightX - visibleLeftX : 0;
        final int visibleSquare = visibleHeight * visibleWidth;
        return visibleSquare * 100 / elementSquare;
    }

    /**
     * Единицы измерения для расчёта смещения
     */
    public enum Unit {
        PERCENT,
        PIXEL
    }

    public static Map<String, Integer> getOffset(
            final Dimension dimension,
            final String offsetString,
            final boolean isAbsolutePosition
    ) {
        final String[] offsetParts = offsetString.split(",");
        final Unit unitX = offsetParts[0].contains("%") ? Unit.PERCENT : Unit.PIXEL;
        final Unit unitY = offsetParts[1].contains("%") ? Unit.PERCENT : Unit.PIXEL;
        final Integer x = DriverUtils.getNumber(
                DataProcessing.decodeValue(
                        offsetParts[0].trim().replace("%", ""))
        );
        final Integer y = DriverUtils.getNumber(
                DataProcessing.decodeValue(
                        offsetParts[1].trim().replace("%", ""))
        );
        Assert.assertNotNull(x);
        Assert.assertNotNull(y);
        final int xOffset;
        final int xRelative = isAbsolutePosition ? (dimension.getWidth() / 2) + 1 : 0;
        final int yRelative = isAbsolutePosition ? (dimension.getHeight() / 2) + 1 : 0;
        if (unitX == Unit.PERCENT) {
            xOffset = (dimension.getWidth() * x / 100) - xRelative;
        } else {
            xOffset = x - xRelative;
        }
        final int yOffset;
        if (unitY == Unit.PERCENT) {
            yOffset = (dimension.getHeight() * y / 100) - yRelative;
        } else {
            yOffset = y - yRelative;
        }
        final Map<String, Integer> offset = new HashMap<>();
        offset.put("xOffset", xOffset);
        offset.put("yOffset", yOffset);
        return offset;
    }
}
