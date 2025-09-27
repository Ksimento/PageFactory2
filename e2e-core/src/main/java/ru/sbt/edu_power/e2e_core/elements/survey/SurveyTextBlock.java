package ru.sbt.edu_power.e2e_core.elements.survey;

import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbt.edu_power.external_services.validator.Validator;
import ru.sbt.edu_power.external_services.validator.Validatable;
import ru.yandex.qatools.htmlelements.element.TypifiedElement;

public class SurveyTextBlock extends TypifiedElement implements SurveyJSField, Validatable {
    public SurveyTextBlock(final WebElement wrappedElement) {
        super(wrappedElement);
    }

    @Override
    public String getFieldValue() {
        return getText();
    }

    @Override
    public boolean validate(final String expected) {
        return Validator.matchValues(getText(), DataProcessing.decodeValue(expected));
    }
}
