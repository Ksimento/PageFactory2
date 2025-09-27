package ru.sbt.edu_power.e2e_core.elements.image;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbt.edu_power.e2e_core.elements.ifaces.HasSpecificProperties;
import ru.sbt.edu_power.external_services.validator.Validatable;
import ru.sbt.edu_power.e2e_core.error_processing.ErrorCollector;
import ru.sbt.edu_power.external_services.validator.Validator;
import ru.sbtqa.tag.qautils.errors.AutotestError;
import ru.yandex.qatools.htmlelements.element.TypifiedElement;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Image extends TypifiedElement implements Validatable, HasSpecificProperties {
    public Image(final WebElement wrappedElement) {
        super(wrappedElement);
    }

    WebElement getElement() {
        final String xpath = "descendant-or-self::img";
        return this.getWrappedElement().findElement(By.xpath(xpath));
    }

    @Override
    public String getText() {
        final String[] srcPiece = getDecodedUrl().split("/");
        return srcPiece[srcPiece.length - 1];
    }

    String getDecodedUrl() {
        try {
            return URLDecoder.decode(getElement().getAttribute("src"), "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            throw new AutotestError("Не поддерживаемая кодировка", e);
        }
    }

    @Override
    public String getFieldValue() {
        return getText();
    }

    @Override
    public boolean validate(final String expected) {
        return Validator.matchValues(getText(), DataProcessing.decodeValue(expected));
    }

    // Доступные названия свойств в классе ImageProperties
    @Override
    public void validateSpecificProperties(final Map<String, String> propertiesMap) {
        final ImageProperties properties = new ImageProperties(getDecodedUrl());
        final ErrorCollector errorCollector = new ErrorCollector();
        propertiesMap.forEach((p, v) -> {
            final String value = DataProcessing.decodeValue(v);
            errorCollector.assertTrue(
                    String.format("Для свойства \"%s\" ожидаемое значение \"%s\" не соответствует фактическому \"%s\"",
                            p, value, properties.getPropertyValue(p)),
                    properties.validate(v, p)
            );
        });
        errorCollector.assertAll();
    }

    // Доступные названия свойств в классе ImageProperties
    @Override
    public Map<String, String> readSpecificProperties() {
        final ImageProperties properties = new ImageProperties(getDecodedUrl());
        return Stream.of(ImageProperties.ImagePropertiesType.values())
                     .collect(Collectors.toMap(
                             ImageProperties.ImagePropertiesType::getName,
                             properties::getPropertyValue
                     ));
    }

    private boolean validateProperty(final String property, final String value, final ImageProperties properties) {
        return properties.validate(value, property);
    }
}
