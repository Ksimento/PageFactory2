package ru.sbt.edu_power.e2e_core.actions;

import io.qameta.allure.Allure;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.MoveTargetOutOfBoundsException;
import ru.sbt.edu_power.e2e_core.allure.AllureUtils;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.e2e_core.fields.FindUtils;
import ru.sbt.edu_power.external_services.ESUtils;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbtqa.tag.pagefactory.environment.Environment;
import ru.sbtqa.tag.qautils.properties.Props;

import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

@Slf4j
public class ClickActions {
    private static BooleanSupplier isAuthPage;

    public static void configure(final BooleanSupplier isAuthPage) {
        ClickActions.isAuthPage = isAuthPage;
    }

    private static final String DISABLE_CLICK_CHANGE_CONTROL = Props.get("disable.click.change.control", "false");

    public static void clickWithChangeControl(final String elementTitle) {
        clickWithChangeControl(elementTitle, FindUtils.getElementByNameOrPath(elementTitle));
    }

    public static void clickWithChangeControl(final String elementTitle, final WebElement element) {
        final String message = String.format("Кнопка \"%s\" не сработала", elementTitle);
        final String timerName = DataProcessing.generator("word30");
        final BooleanSupplier waitWhenElementBeClicked = () -> clickWithChangeControl(elementTitle, element, timerName);
        Timer.executeTimerThrowable(DriverConstants.TIMEOUT, message, waitWhenElementBeClicked);
    }

    public static void clickElementWithOffset(final WebElement element, final String offsetString) {
        final WebElement remoteElement = Mover.getRemoteElement(element);
        final Map<String, Integer> offset = Mover.getOffset(remoteElement.getSize(), offsetString, true);
        Mover.getActions()
             .moveToElement(remoteElement, offset.get("xOffset"), offset.get("yOffset"))
             .click()
             .build()
             .perform();
    }

    public static boolean clickWithChangeControl(
            final String elementTitle,
            final WebElement element,
            final String timerName
    ) {
        Assert.assertNotNull("Для клика передан NULL объект", element);
        final boolean isAuthPage = ClickActions.isAuthPage.getAsBoolean();
        if (!isAuthPage) {
            PageControls.removeToasts();
        }
        final int beforeHash = PageControls.getHtmlSectionHashCode(DataProcessing.generator("word30"));
        final int tabsCount = Environment.getDriverService().getDriver().getWindowHandles().size();

        safeClick(elementTitle, element);

        PageControls.waitWhileNetworkActive();
        PageControls.detectBackendErrors();
        PageControls.detectConsoleErrors();

        if ("true".equals(DISABLE_CLICK_CHANGE_CONTROL)) {
            return true;
        }

        if (tabsCount != Environment.getDriverService().getDriver().getWindowHandles().size()) {
            return true;
        }

        //проверяю изменение страницы. Если изменений не произошло, считаю что кнопка не сработала и вызываю этот метод повторно
        final int afterHash = PageControls.getHtmlSectionHashCode(DataProcessing.generator("word30"));
        if (beforeHash != afterHash) {
            Timer.removeTimer(timerName);
            return true;
        }
        log.info("Кнопка {} не отработала", elementTitle);
        return false;
    }

    public static void safeClick(final String elementTitle, final WebElement element) {
        final boolean displayClickPoint = "true".equals(Props.get("webdriver.browser.display_click_point", "false"));
        Assert.assertNotNull(element);
        String addPointScript = null;
        if (displayClickPoint) {
            addPointScript = getAddPointScript(element);
        }
        try {
//            Костыль для сафари, в котором стандартный клик по элементу не работает
            if ("Safari".equalsIgnoreCase(Props.get("webdriver.browser.name"))) {
                Mover.scrollToElement(element);
                DriverUtils.executeJS("arguments[0].click()", element);
            } else {
                element.click();
            }
        } catch (final ElementNotInteractableException | MoveTargetOutOfBoundsException e) {

//            Проверяю не появилась ли подсказка над каким-нибудь элементом которая может заблокировать нажатие
            PageControls.detectHintAndRemove();

            Allure.addAttachment("При клике возникла проблема", e.toString());
            if (e instanceof ElementNotInteractableException) {
                try {
                    final WebElement parents = element.findElement(By.xpath("parent::*/parent::*/parent::*"));
                    Allure.addAttachment(
                            "Часть DOM секции содержащей целевой элемент",
                            (String) DriverUtils.executeJS("return arguments[0].innerHTML", parents)
                    );
                } catch (final Throwable t) {
                    log.error("", e);
                }
            }
            if (e instanceof MoveTargetOutOfBoundsException) {
                AllureUtils.attachScreenShotToAllure("MoveTargetOutOfBoundsException");
            }

            log.info("Не удалось выполнить клик по элементу: {}", elementTitle);

            final BooleanSupplier waitWhenElementBeClicked = () -> {
                Mover.scrollToElement(element, true);
                try {
                    element.click();
                } catch (final ElementNotInteractableException | MoveTargetOutOfBoundsException ignored) {
                    return false;
                }
                return true;
            };

            final boolean isClicked = Timer.executeTimer(DriverConstants.ELEMENT_WAIT_5SEC, waitWhenElementBeClicked);

            if (!isClicked) {
                DriverUtils.executeJS("arguments[0].scrollIntoView()", element);
                DriverUtils.freeze(DriverConstants.FREEZE_500_MS);
                log.info("Не удалось выполнить клик нативно, выполняем через экшн");
                Mover.moveAndClick(element);
            }
        }
        if (displayClickPoint) {
            DriverUtils.executeJS(addPointScript);
        }
        ESUtils.freeze(DriverConstants.FREEZE_250_MS);
        PageControls.detectConsoleErrors();
        Mover.markMousePosition();
    }

    public static void safeClick(final String elementTitle) {
        safeClick(elementTitle, FindUtils.getElementByNameOrPath(elementTitle));
    }

    public static boolean clickElementIfExist(final String elementTitle) {
        return clickElementIfExist(elementTitle, true);
    }

    public static boolean clickElementIfExist(final String elementTitle, final boolean withChangeControl) {
        final BooleanSupplier waitWhenElementBePresent = () -> {
            try {
                final WebElement element = DriverUtils
                        .findElementsOnPageByXpath(DriverUtils.getXpath(elementTitle))
                        .get(0);
                if (!element.isDisplayed()) {
                    return false;
                }
                if (withChangeControl) {
                    clickWithChangeControl(elementTitle, element);
                } else {
                    safeClick(elementTitle, element);
                }
                return true;
            } catch (final Throwable e) {
                return false;
            }
        };
        return Timer.executeTimer(1, waitWhenElementBePresent);
    }

    private static String getAddPointScript(final WebElement element) {
        final WebElement remoteElement = Mover.getRemoteElement(element);
        final int xPos = remoteElement.getLocation().x;
        final int yPos = remoteElement.getLocation().y;
        final int width = remoteElement.getRect().width;
        final int height = remoteElement.getRect().height;
        final String cssText = "position: absolute;" +
                               "top:" + (yPos + (height / 2) - 2) + "px;" +
                               "left:" + (xPos + (width / 2) - 2) + "px;" +
                               "width:4px;height:4px;" +
                               "background: red;" +
                               "border-radius:4px;" +
                               "z-index: 100000;";
        return "var div = document.createElement('div'); div.style.cssText = '"
               + cssText
               + "'; document.body.appendChild(div)";

    }

    private static boolean isPanelVisible(final String xpath) {
        final List<WebElement> menuElementList = DriverUtils.findElementsOnPageByXpath(xpath);
        if (menuElementList.isEmpty()) {
            return false;
        }
        return DriverUtils.isElementDisplayed(menuElementList.get(0));
    }
}
