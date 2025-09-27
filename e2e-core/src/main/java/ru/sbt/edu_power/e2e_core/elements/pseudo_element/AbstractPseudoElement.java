package ru.sbt.edu_power.e2e_core.elements.pseudo_element;

import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbtqa.tag.qautils.errors.AutotestError;
import ru.yandex.qatools.htmlelements.element.TypifiedElement;

public abstract class AbstractPseudoElement extends TypifiedElement {
    private static final String JS_COMMAND = "return window.getComputedStyle(arguments[0], ':{PSEUDO}')['{PROP}']";
    protected AbstractPseudoElement(final WebElement wrappedElement) {
        super(wrappedElement);
    }

    public String getCssPropertyValue(final String propertyName) {
        throw new AutotestError("Необходимо переопределить этот метод");
    }

    String getCssPropertyValue(final Pseudo pseudo, final String propertyName) {
        return ((String) DriverUtils.executeJS(
                JS_COMMAND
                        .replace("{PSEUDO}", pseudo.pseudoAttribute)
                        .replace("{PROP}", propertyName),
                getWrappedElement()
        )).replaceFirst("^\"", "")
          .replaceFirst("\"$", "");
    }

    enum Pseudo {
        AFTER("after"),
        BEFORE("before");

        private final String pseudoAttribute;

        Pseudo(final String pseudoAttribute) {
            this.pseudoAttribute = pseudoAttribute;
        }

        public String getPseudoAttribute() {
            return pseudoAttribute;
        }
    }
}
