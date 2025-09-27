package ru.sbt.edu_power.e2e_core.elements;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbt.edu_power.external_services.validator.Validator;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Available;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Fillable;
import ru.sbt.edu_power.external_services.validator.Validatable;
import ru.sbtqa.tag.pagefactory.environment.Environment;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.util.List;
import java.util.function.BooleanSupplier;

public class RadioButton extends AutoIdentElement implements Available, Fillable, Validatable {

    @Override
    protected WebElement getInput() {
            return getSpecifyElement(
                    "descendant-or-self::div[descendant-or-self::input[@type = 'radio']][1]",
                    "Не удалось найти элементы ввода"
            );
    }

    public RadioButton(final WebElement wrappedElement) {
        super(wrappedElement);
    }

    private WebElement getButtonByName(final String name) {
        try {
            final String xpath = ".//label[node() = '" + name + "']";
            return getInput().findElement(By.xpath(xpath));
        } catch (final NoSuchElementException e) {
            throw new AutotestError(String.format("Пункта \"%s\" нет в радио-переключателе \"%s\"", name, getLabel()), e);
        }
    }

    @Override
    public void fillField(final String name, final Boolean validated) throws FieldFillingException {
        final String decodedName = DataProcessing.decodeValue(name);
        final BooleanSupplier waitWhenFieldBeFilled = () -> {
            if (validated && isValid(decodedName)) {
                return true;
            }
            setValue(decodedName);
            DriverUtils.freeze(DriverConstants.FREEZE_250_MS);
            return !validated;
        };

        final boolean result = Timer.executeTimer(DriverConstants.TIMEOUT, waitWhenFieldBeFilled);
        if (!result) {
            throw new FieldFillingException("");
        }
    }

    private void setValue(final String name) {
        new Actions((WebDriver) Environment.getDriverService().getDriver())
                .moveToElement(getButtonByName(name))
                .click()
                .build()
                .perform();
    }

    private boolean isValid(final String expectedValue) {
        return expectedValue.equals(getCheckedName(expectedValue));
    }

    private String getCheckedName() {
        final List<WebElement> valueList = getInput().findElements(By.xpath(".//span[contains(@class,'checked')]/following-sibling::span | ancestor-or-self::div[contains(@class,'active') and contains(@class,'radio')]"));
        return !valueList.isEmpty() ? valueList.get(0).getText() : "";
    }
    private String getCheckedName(final String name) {
        final List<WebElement> valueList = getButtonByName(name).findElements(By.xpath(".//span[contains(@class,'checked')]/following-sibling::span | ancestor-or-self::div[contains(@class,'active') and contains(@class,'radio')]"));
        return !valueList.isEmpty() ? valueList.get(0).getText() : "";
    }

    @Override
    public String getLabel() {
            return getInput().findElement(By.xpath("descendant-or-self::label")).getText();
    }

    @Override
    public boolean isAvailable() {
        final String path = "descendant-or-self::input";
        return getWrappedElement().findElement(By.xpath(path)).isEnabled();
    }

    @Override
    public String getFieldValue() {
        return getCheckedName();
    }

    @Override
    public boolean validate(final String expected) {
        return Validator.matchValues(getCheckedName(), DataProcessing.decodeValue(expected));
    }
}
