package ru.sbt.edu_power.e2e_core.actions;

import lombok.Getter;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.e2e_core.remote_access.BrowserUtils;
import ru.sbtqa.tag.pagefactory.environment.Environment;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.List;


public class ClipboardUtils {
    public static String getClipboard() throws IOException, UnsupportedFlavorException {
        if (!System.getProperty("webdriver.chrome.capability.options.args").contains("headless")) {
            return (String) Toolkit.getDefaultToolkit()
                                   .getSystemClipboard()
                                   .getData(DataFlavor.stringFlavor);
        } else {
            return fetchClipboardContents();
        }
    }

    public static String fetchClipboardContents() {
        final Keys controlButton = ClipboardUtils.getKeys();
        WebElement elementClipboard = createInputClipboard(Environment
                .getDriverService()
                .getDriver()
                .findElement(By.xpath("//body")), "");
        elementClipboard.sendKeys(Keys.chord(controlButton, "V"));
        final String text = elementClipboard.getAttribute("value");
        deleteInputClipboard();
        return text;
    }

    public static WebElement createInputClipboard(final WebElement element, final String value) {
        try {
            DriverUtils.executeJS("var input = document.createElement('input');\n" +
                                  "input.setAttribute('id', 'copyTextElement');" +
                                  "arguments[0].appendChild(input);" +
                                  "input.setAttribute('type', 'text');" +
                                  "input.value='" + value + "';", element);
            WebElement elementClipboard = Environment
                    .getDriverService()
                    .getDriver()
                    .findElement(By.id("copyTextElement"));
            Mover.scrollToElement(elementClipboard);
            elementClipboard.click();
            return elementClipboard;
        } catch (final ElementNotInteractableException e) {
            deleteInputClipboard();
            return null;
        }
    }

    public static WebElement getActiveElementClipboard(final String value) {
        WebElement elementClipboard;
        elementClipboard = createInputClipboard(DriverUtils.getWebDriver()
                                                           .findElement(By.xpath("//body")), value);
        if (elementClipboard == null) {
            for (HtmlElement xpathElement : HtmlElement.values()) {
                elementClipboard = getActiveElementInDOM(
                        DriverUtils.getWebDriver()
                                   .findElements(By.xpath(xpathElement.getHtmlElement())),
                        value
                );
                if (elementClipboard != null) {
                    break;
                }
            }
        } else {
            return elementClipboard;
        }
        Assert.assertNotNull("Не удалось создать элемент буфер обмена", elementClipboard);
        return elementClipboard;
    }

    private static WebElement getActiveElementInDOM(List<WebElement> elements, final String value) {
        if (elements.isEmpty()) {
            return null;
        }
        for (int i = 1; i <= elements.size(); i++) {
            WebElement elementClipboard = createInputClipboard(elements.get(elements.size() - i), value);
            if (elementClipboard != null) {
                return elementClipboard;
            }
        }
        return null;
    }

    public static void deleteInputClipboard() {
        DriverUtils.executeJS("var elem = document.getElementById('copyTextElement');" +
                              "elem.remove();");
    }

    public static void setValueClipboard(final String value) {

        final Keys controlButton = getKeys();
        WebElement elementClipboard = getActiveElementClipboard(value);
        elementClipboard.sendKeys(Keys.chord(controlButton, "A"));
        elementClipboard.sendKeys(Keys.chord(controlButton, "C"));
        deleteInputClipboard();

    }

    public static Keys getKeys() {
        final boolean IS_MAC = "Mac".equals(BrowserUtils.getBrowserOSType());
        return IS_MAC ? Keys.COMMAND : Keys.CONTROL;
    }

    // элементы относительно которых будет создаваться поле ввода для получения значения в буфер обмен
    @Getter
    enum HtmlElement {
        MODAL("//div[contains(@class, '_FullScreenPageLayout') " +
              "or @id = 'curtainContainer' " +
              "or @data-testid = 'UIKIT.Portal.ModalContent' " +
              "or @data-testid = 'UIKIT.Portal.ModalContent.Modal' " +
              "or contains(@class, '_ConfirmDialogContainer') " +
              "or contains(@class, 'MuiDialog-container') " +
              "or contains(@class, '_ModalWrapperStyled') " +
              "or @data-testid='UIKit.Curtain.CurtainContent' " +
              "or @data-testid='UIKit.Curtain.Content' " +
              "or @id='image-crop-portal' " +
              "or @data-testid='CurtainV3' " +
              "or @data-testid='UIKIT.CurtainV3' " +
              "or contains(@class, 'MuiDrawer-modal') " +
              "or contains(@class,'MuiDialog-paper')" +
              "or @id='modal-container']"),
        INPUT("//input/../.."),
        TEXTAREA("//textarea/../..");
        private final String HtmlElement;

        HtmlElement(final String HtmlElement) {
            this.HtmlElement = HtmlElement;
        }
    }
}
