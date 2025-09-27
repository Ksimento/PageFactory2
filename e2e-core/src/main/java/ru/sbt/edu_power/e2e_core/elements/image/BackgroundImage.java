package ru.sbt.edu_power.e2e_core.elements.image;

import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.external_services.validator.Validatable;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class BackgroundImage extends Image implements Validatable {
    public BackgroundImage(final WebElement wrappedElement) {
        super(wrappedElement);
    }

    @Override
    WebElement getElement() {
        return this.getWrappedElement();
    }

    @Override
    String getDecodedUrl() {
        try {
            final String imgUrl = (String) DriverUtils.executeJS("return window.getComputedStyle(arguments[0])['backgroundImage']", getWrappedElement());
            return URLDecoder.decode(imgUrl, "UTF-8").replaceAll("^url\\(\"|\"\\)", "");
        } catch (final UnsupportedEncodingException e) {
            throw new AutotestError("Не поддерживаемая кодировка", e);
        }
    }
}
