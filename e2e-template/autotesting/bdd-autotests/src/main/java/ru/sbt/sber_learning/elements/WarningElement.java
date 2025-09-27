package ru.sbt.sber_learning.elements;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

public class WarningElement {

    public static List<WebElement> getWarningElement(final WebElement element, final String xpathMassage) {
        String xpathParent = ".";
        for (int i = 0; i < 3; i++) {
            List<WebElement> warning = element.findElement(By.xpath(xpathParent)).findElements(By.xpath(xpathMassage));
            if (!warning.isEmpty()) {
                return warning;
            }
            xpathParent += "/..";
        }
        return element.findElements(By.xpath(xpathParent + xpathParent));
    }
}
