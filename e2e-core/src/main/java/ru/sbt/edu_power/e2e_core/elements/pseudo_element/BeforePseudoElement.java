package ru.sbt.edu_power.e2e_core.elements.pseudo_element;

import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.external_services.validator.Validatable;
import ru.sbt.edu_power.external_services.validator.Validator;

/**
 * Класс реализует работу с псевдо элементом :before
 * Позволяет получать значения CSS параметров
 * Для валидации по дефолту используется значение параметра "content"
 * В pageObject необходимо использовать xpath до элемента, содержащего нужный псевдо элемент
 */
public class BeforePseudoElement extends AbstractPseudoElement implements Validatable {
    public BeforePseudoElement(final WebElement wrappedElement) {
        super(wrappedElement);
    }

    @Override
    public String getCssPropertyValue(final String propertyName) {
        return getCssPropertyValue(Pseudo.BEFORE, propertyName);
    }

    @Override
    public String getFieldValue() {
        return getCssPropertyValue(Pseudo.BEFORE, "content");
    }

    @Override
    public boolean validate(final String expected) {
        return Validator.matchValues(getFieldValue(), expected);
    }
}
