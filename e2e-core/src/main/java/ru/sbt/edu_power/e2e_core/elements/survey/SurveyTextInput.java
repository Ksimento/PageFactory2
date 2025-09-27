package ru.sbt.edu_power.e2e_core.elements.survey;

import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.elements.text_input.TextInput;

public class SurveyTextInput extends TextInput implements SurveyJSField {
    public SurveyTextInput(final WebElement wrappedElement) {
        super(wrappedElement);
    }
}
