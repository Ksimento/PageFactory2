package ru.sbt.edu_power.e2e_core.elements.survey;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.e2e_core.elements.FieldFillingException;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Fillable;
import ru.sbt.edu_power.external_services.validator.Validatable;
import ru.sbt.edu_power.e2e_core.survey.SurveyStepActionsUtils;
import ru.sbtqa.tag.qautils.errors.AutotestError;
import ru.yandex.qatools.htmlelements.element.TypifiedElement;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SurveyMatrixSelectOne extends TypifiedElement implements SurveyJSField, Fillable, Validatable, ElementPartValidatable {
    public SurveyMatrixSelectOne(final WebElement wrappedElement) {
        super(wrappedElement);
    }

    private Map<String, String> getValuesMap(final String values) {
        return Arrays
                .stream(values.split(";"))
                .map(s -> s.split(":"))
                .collect(Collectors
                        .toMap(parts -> DataProcessing.decodeValue(parts[0].trim()),
                                parts -> DataProcessing.decodeValue(parts[1].trim()),
                                (a, b) -> b)
                );
    }

    WebElement getRow(final String rowName) {
        final List<WebElement> elements = getWrappedElement().findElements(By.xpath(".//td[1][node() = '" + rowName + "']/parent::tr"));
        if (elements.isEmpty()) {
            final Integer rowNumber = DriverUtils.getNumber(rowName);
            if (rowNumber != null) {
                elements.add(getWrappedElement().findElements(By.xpath(".//tbody/tr")).get(rowNumber - 1));
            } else {
                throw new AutotestError(String.format("Строка со значением \"%s\" не найдена", rowName));
            }
        }
        return elements.get(0);
    }

    private Fillable getInput(final String rowName, final WebElement row) {
        return SurveyStepActionsUtils.createTypifiedSurveyInputElement(rowName, row);
    }

    @Override
    public void fillField(final String values, final Boolean validated) throws FieldFillingException {
        for (final Map.Entry<String, String> row : getValuesMap(values).entrySet()) {
            getInput(row.getKey(), getRow(row.getKey())).fillField(row.getValue(), validated);
        }
    }

    @Override
    public String getFieldValue() {
        throw new AutotestError("Метод не реализован");
    }

    @Override
    public boolean validate(final String expected) {
        throw new AutotestError("Метод не реализован");
    }

    @Override
    public boolean partialValidate(final String elementPartId, final String expected) {
        throw new AutotestError("Метод не реализован");
    }

    @Override
    public String getPartContent(final String elementPartId) {
        throw new AutotestError("Метод не реализован");
    }
}
