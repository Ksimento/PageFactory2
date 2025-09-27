package ru.sbt.edu_power.e2e_core.elements;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbt.edu_power.external_services.validator.Validatable;
import ru.sbt.edu_power.external_services.validator.Validator;
import ru.sbtqa.tag.qautils.errors.AutotestError;
import ru.yandex.qatools.htmlelements.element.TypifiedElement;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class FileLink extends TypifiedElement implements Validatable {
    public FileLink(final WebElement wrappedElement) {
        super(wrappedElement);
    }

    private WebElement getLink() {
        final String xpath = "descendant-or-self::a";
        return getWrappedElement().findElement(By.xpath(xpath));
    }

    public String getHref() {
        return getLink().getAttribute("href");
    }

    private String getFileNameFromUrl() {
        final String url;
        try {
            url = URLDecoder.decode(getHref(), "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            throw new AutotestError("Не поддерживаемая кодировка", e);
        }
        final String[] urlPieces = url.split("/");
        return urlPieces[urlPieces.length - 1];
    }

    @Override
    public String getText() {
        return getFileNameFromUrl();
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
