package ru.sbt.edu_power.e2e_core.elements;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.external_services.validator.Validatable;
import ru.sbt.edu_power.e2e_core.resource_repository.ResourceRepository;
import ru.sbt.edu_power.external_services.validator.Validator;
import ru.sbtqa.tag.qautils.errors.AutotestError;
import ru.sbtqa.tag.qautils.properties.Props;
import ru.yandex.qatools.htmlelements.element.TypifiedElement;

import java.util.Map;

/**
 * Элемент реализует иконку SVG и позволяет выполнить проверку фактического векторного описания иконки с ожидаемым
 * Векторные описания иконок пишем в отдельный файл src/test/resources/data/svg-icons.resources
 * в виде имени иконки и содержимого аттрибута "d" тега path внутри svg
 */
public class SvgIcon extends TypifiedElement implements Validatable {
    public SvgIcon(final WebElement wrappedElement) {
        super(wrappedElement);
    }

    @Override
    public boolean validate(final String name) {
        final String actualSvgPath = getFieldValue();
        return Validator.matchValues(actualSvgPath, getIcon(name));
    }

    @Override
    public String getFieldValue() {
        return getWrappedElement().findElement(By.xpath(".//*[name() = 'path']")).getAttribute("d");
    }

    private static String getIcon(final String name) {
        final Map<String, String> svgCollection = ResourceRepository.getResource(ResourceRepository.AvailableResource.SVG_ICONS);
        final String icon = svgCollection.get(name);
        if (null == icon) {
            throw new AutotestError(String.format("Иконка %s не описана в файле %s", name, Props.get("svg.icons.resources")));
        }
        return icon;
    }
}
