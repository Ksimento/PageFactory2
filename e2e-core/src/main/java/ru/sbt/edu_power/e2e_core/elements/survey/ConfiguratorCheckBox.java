package ru.sbt.edu_power.e2e_core.elements.survey;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Fillable;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbt.edu_power.e2e_core.elements.FieldFillingException;
import ru.sbt.edu_power.external_services.validator.Validatable;
import ru.yandex.qatools.htmlelements.element.TypifiedElement;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

public class ConfiguratorCheckBox extends TypifiedElement implements Fillable, Validatable {
    public ConfiguratorCheckBox(final WebElement wrappedElement) {
        super(wrappedElement);
    }

    private WebElement getInput() {
        return getWrappedElement().findElement(By.xpath(".//span[contains(@class, 'checkmark')]"));
    }

    private boolean isChecked() {
        return getInput().getAttribute("class").contains("main");
    }

    private boolean isChecked(final WebElement input) {
        return input.getAttribute("class").contains("main");
    }

    @Override
    public void fillField(final String value, final Boolean validated) throws FieldFillingException {
        final AtomicReference<WebElement> input = new AtomicReference<>();
        final boolean state = DriverUtils.getBoolean(value);

        final BooleanSupplier waitWhenCheckBoxBeSet = () -> {
            input.set(getInput());
            if (isChecked(input.get()) != state) {
                input.get().click();
                DriverUtils.freeze(DriverConstants.FREEZE_250_MS);
                return false;
            }
            return true;
        };

        final boolean result = Timer.executeTimer(DriverConstants.ELEMENT_WAIT_5SEC, waitWhenCheckBoxBeSet);
        if (!result) {
            throw new FieldFillingException("");
        }
    }

    @Override
    public String getFieldValue() {
        return isChecked() ? "Да" : "Нет";
    }

    @Override
    public boolean validate(final String expected) {
        return DriverUtils.getBoolean(expected) == isChecked();
    }
}
