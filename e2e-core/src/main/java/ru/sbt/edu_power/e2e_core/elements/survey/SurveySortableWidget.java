package ru.sbt.edu_power.e2e_core.elements.survey;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Fillable;
import ru.sbt.edu_power.external_services.validator.Validatable;
import ru.sbtqa.tag.pagefactory.environment.Environment;
import ru.sbtqa.tag.qautils.errors.AutotestError;
import ru.yandex.qatools.htmlelements.element.TypifiedElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SurveySortableWidget extends TypifiedElement implements SurveyJSField, Fillable, Validatable {
    public SurveySortableWidget(final WebElement wrappedElement) {
        super(wrappedElement);
    }

    private final List<WebElement> sequencedElements = new ArrayList<>();

    private List<String> getValuesSequence(final String values) {
        return Arrays.stream(values.split(";"))
                .map(DataProcessing::decodeValue)
                .collect(Collectors.toList());
    }

    private WebElement getElementByName(final String name) {
        final List<WebElement> elements = getWrappedElement()
                .findElements(By.xpath(".//div[@data-value='" + name + "']"));
        if (elements.isEmpty()) {
            throw new AutotestError(String.format("Элемент с названием \"%s\" не найден", name));
        }
        return elements.get(0);
    }

    private void collectItems(final String values) {
        final List<String> sequencedValues = getValuesSequence(values);
        for (final String name : sequencedValues) {
            sequencedElements.add(getElementByName(name));
        }
    }

    @Override
    public void fillField(final String value, final Boolean validated) {
        collectItems(value);
        final WebElement container = getWrappedElement().findElement(By.xpath(".//div[@class = 'sjs-sortablejs-result']"));
        final JavascriptExecutor executor = Environment.getDriverService().getDriver();
//        Переставление элементов в доме не даёт результата, survey отслеживает именно события мыши drag&drop
        sequencedElements.forEach(item -> {
                    executor.executeScript("arguments[0].append(arguments[1])", container, item);
                    DriverUtils.freeze(DriverConstants.FREEZE_250_MS);
                }
        );
    }

    @Override
    public String getFieldValue() {
        throw new AutotestError("Метод не реализован");
    }

    @Override
    public boolean validate(final String expected) {
        throw new AutotestError("Метод не реализован");
    }
}
