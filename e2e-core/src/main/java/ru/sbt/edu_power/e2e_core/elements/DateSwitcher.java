package ru.sbt.edu_power.e2e_core.elements;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.actions.ClickActions;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbt.edu_power.external_services.validator.Validator;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Fillable;
import ru.sbt.edu_power.external_services.validator.Validatable;
import ru.yandex.qatools.htmlelements.element.TypifiedElement;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/**
 * Класс реализует элемент выбора даты в таблицах (с кнопками на период вперёд и назад)
 */
@Slf4j
public class DateSwitcher extends TypifiedElement implements Fillable, Validatable {

    public DateSwitcher(final WebElement wrappedElement) {
        super(wrappedElement);
    }

    public void changeDateBack() {
        changeDateBack(1);
    }

    public void changeDateForward() {
        changeDateForward(1);
    }

    public void changeDateBack(final int counter) {
        for (int i = 0; i < counter; i++) {
            ClickActions.safeClick("Кнопка назад переключателя даты", getBackButton());
        }
    }

    public void changeDateForward(final int counter) {
        for (int i = 0; i < counter; i++) {
            ClickActions.safeClick("Кнопка вперёд переключателя даты", getForwardButton());
        }
    }

    @Override
    public void fillField(final String value, final Boolean validated) throws FieldFillingException {
        final AtomicBoolean rightDirection = new AtomicBoolean(getForwardButton().isAvailable());
        final Button forwardButton = getForwardButton();
        final Button backButton = getBackButton();
        final int expectedPeriod = getPeriodFirstYear(value);
        final BooleanSupplier waitWhenPeriodBeSet = () -> {
            if (validate(value)) {
                return true;
            }
            final int diff = expectedPeriod - getPeriodFirstYear(getFieldValue());
            if (diff > 0 == rightDirection.get()) {
                forwardButton.click();
            } else {
                backButton.click();
            }
            if (Math.abs(expectedPeriod - getPeriodFirstYear(getFieldValue())) > Math.abs(diff)) {
                rightDirection.set(false);
            }
            return false;
        };
        final boolean result = Timer.executeTimer(DriverConstants.TIMEOUT, waitWhenPeriodBeSet);
        if (!result) {
            throw new FieldFillingException("Не удалось выбрать нужный период");
        }
    }

    private int getPeriodFirstYear(final String period) {
        return Integer.parseInt(period.replaceAll("\\*", "").split("\\D")[0]);
    }

    private Button getForwardButton() {
        return new Button(getWrappedElement().findElement(By.xpath(getVersionForm().get("xpathForwardButton"))));
    }

    private Button getBackButton() {

        return new Button(getWrappedElement().findElement(By.xpath(getVersionForm().get("xpathBackButton"))));

    }

    @Override
    public String getFieldValue() {
        return getWrappedElement().findElement(By.xpath(getVersionForm().get("xpathFieldValue"))).getText();
    }

    @Override
    public boolean validate(final String expected) {
        return Validator.matchValues(getFieldValue(), expected);
    }

    private Map<String,String> getVersionForm(){
        Map<String,String> versionElements = new HashMap<>();
        if(!getWrappedElement().findElements(By.xpath(".//*[contains(@data-testid,'.CurrentYear')]")).isEmpty()){
            versionElements.put("xpathForwardButton",".//*[contains(@data-testid,'.Next')]");
            versionElements.put("xpathFieldValue",".//*[contains(@data-testid,'.CurrentYear')]");
            versionElements.put("xpathBackButton",".//*[contains(@data-testid,'.Previous')]");
        }
        else {
            versionElements.put("xpathForwardButton",".//button[3]");
            versionElements.put("xpathFieldValue",".//button[2]");
            versionElements.put("xpathBackButton",".//button[1]");
        }
        return versionElements;
        }
    }
