package ru.sbt.edu_power.e2e_core.elements;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Fillable;
import ru.sbt.edu_power.external_services.validator.Validatable;
import ru.sbt.edu_power.e2e_core.actions.ClickActions;
import ru.yandex.qatools.htmlelements.element.TypifiedElement;

import java.util.function.BooleanSupplier;

// Описание элемента переключателя языка на странице авторизации
public class LanguageSwitcher extends TypifiedElement implements Fillable, Validatable {
    public LanguageSwitcher(final WebElement wrappedElement) {
        super(wrappedElement);
    }

    @Override
    public void fillField(final String value, final Boolean validated) {
        final BooleanSupplier waitWhenLangBeSwitched = () -> {
            if (validate(value)) {
                return true;
            }
            ClickActions.safeClick("Переключатель языка", getLangElement());
            return false;
        };
        Timer.executeTimerThrowable(
                DriverConstants.ELEMENT_WAIT_5SEC,
                "Не удалось переключить язык локали на " + value,
                waitWhenLangBeSwitched
        );
    }

    @Override
    public String getFieldValue() {
        return getLangElement().getText();
    }

    @Override
    public boolean validate(final String expected) {
        return getFieldValue().equalsIgnoreCase(expected);
    }

    private WebElement getLangElement() {
        return getWrappedElement().findElement(By.xpath("descendant-or-self::span[@class = 'lang']"));
    }
}
