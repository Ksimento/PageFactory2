package ru.sbt.edu_power.e2e_core.elements.survey;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

public class SurveyImageSelect extends SurveyRadioButton {
    public SurveyImageSelect(final WebElement wrappedElement) {
        super(wrappedElement);
    }

    @Override
    String getRadioElementXpath(final String value) {
        return ".//div[contains(@class, 'imagepicker') and descendant::img[@src = '" + value + "']]";
    }

    @Override
    String getRadioElementXpath() {
        return ".//div[contains(@class, 'imagepicker')]";
    }

    @Override
    public String getText() {
        final List<WebElement> elements = getWrappedElement()
                .findElements(By.xpath(".//div[contains(@class, 'checked')]//img"));
        return elements.isEmpty() ? "" : elements.get(0).getAttribute("src");
    }
}
