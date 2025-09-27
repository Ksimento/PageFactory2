package ru.sbt.edu_power.e2e_core.elements.survey;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.external_services.validator.Validator;

public class SurveyImageSelectMultiple extends SurveyCheckBoxGroup {
    public SurveyImageSelectMultiple(final WebElement wrappedElement) {
        super(wrappedElement);
    }

    @Override
    WebElement getInput(final WebElement checkBoxItem) {
        return checkBoxItem;
    }

    @Override
    String getTextOrSrc(final WebElement element) {
        return element.findElement(By.xpath(".//img")).getAttribute("src");
    }

    @Override
    String getXPath() {
        return ".//fieldset/div[contains(@class, 'imagepicker')]";
    }

    @Override
    boolean matchTextOrSrc(final String actual, final String expected) {
        if (expected.startsWith("http")) {
            return Validator.matchValues(actual, expected);
        } else {
            final String[] urlParts = actual.split("/");
            return Validator.matchValues(urlParts[urlParts.length - 1], expected);
        }
    }
}
