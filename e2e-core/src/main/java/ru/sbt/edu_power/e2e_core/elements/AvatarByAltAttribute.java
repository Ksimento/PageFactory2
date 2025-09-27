package ru.sbt.edu_power.e2e_core.elements;

import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbt.edu_power.external_services.validator.Validatable;
import ru.sbt.edu_power.external_services.validator.Validator;
import ru.yandex.qatools.htmlelements.element.TypifiedElement;

public class AvatarByAltAttribute extends TypifiedElement implements Validatable {
    public AvatarByAltAttribute(final WebElement wrappedElement) {
        super(wrappedElement);
    }

    @Override
    public String getText() {
        return getWrappedElement().getAttribute("alt");
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
