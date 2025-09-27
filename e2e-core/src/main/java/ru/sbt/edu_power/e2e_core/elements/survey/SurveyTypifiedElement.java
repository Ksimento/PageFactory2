package ru.sbt.edu_power.e2e_core.elements.survey;

import org.openqa.selenium.WebElement;
import ru.yandex.qatools.htmlelements.element.TypifiedElement;

/**
 * Элемент используется для обозначения любого заполняемого поля Survey и впоследствии будет динамически изменён на
 * тип, определённый автоматически
 */
public class SurveyTypifiedElement extends TypifiedElement implements SurveyJSField {
    public SurveyTypifiedElement(final WebElement wrappedElement) {
        super(wrappedElement);
    }
}
