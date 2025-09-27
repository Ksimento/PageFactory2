package ru.sbt.edu_power.e2e_core.elements.survey;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.e2e_core.elements.FieldFillingException;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Fillable;
import ru.sbt.edu_power.e2e_core.survey.SurveyStepActionsUtils;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SurveyMatrixSelectMultiple extends SurveyMatrixSelectOne {
    public SurveyMatrixSelectMultiple(final WebElement wrappedElement) {
        super(wrappedElement);
    }

    private WebElement getCellByColumnName(final WebElement element, final String columnName) {
        final List<WebElement> cells = element.findElements(By.xpath(".//td[@headers = '" + columnName + "']"));
        if (cells.isEmpty()) {
            final Integer rowNumber = DriverUtils.getNumber(columnName);
            if (rowNumber != null) {
                cells.add(element.findElements(By.xpath(".//td")).get(rowNumber));
            } else {
                throw new AutotestError(String.format("Столбец со значением \"%s\" не найден", columnName));
            }
        }
        return SurveyStepActionsUtils.createTypifiedSurveyInputElement(columnName, cells.get(0));
    }

    private Map<String, Map<String, String>> getValuesMap(final String values) {
        final Map<String, Map<String, String>> valuesMap = new HashMap<>();
        for (final String row : values.split(";")) {
            final Map<String, String> argsList = new HashMap<>();
            final String[] args = row.split(":");
            final String rowName = DataProcessing.decodeValue(args[0]);
            valuesMap.put(rowName, argsList);
            final int columns = (args.length - 1) / 2;
            for (int i = 0; i < columns; i++) {
                valuesMap.get(rowName).put(DataProcessing.decodeValue(args[i * 2 + 1]), args[i * 2 + 2]);
            }
        }
        return valuesMap;
    }

    @Override
    public void fillField(final String value, final Boolean validated) throws FieldFillingException {
        final Map<String, Map<String, String>> valuesMap = getValuesMap(value);
        for (final String row : valuesMap.keySet()) {
            final WebElement rowElement = getRow(row);
            for (final String col : valuesMap.get(row).keySet()) {
                final WebElement colElement = getCellByColumnName(rowElement, col);
                ((Fillable) colElement).fillField(valuesMap.get(row).get(col), validated);
            }
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
