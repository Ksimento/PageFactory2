package ru.sbt.edu_power.e2e_core.elements;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Available;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.util.List;

public class Button extends ru.yandex.qatools.htmlelements.element.Button implements Available {
    public Button(final WebElement wrappedElement) {
        super(wrappedElement);
    }


    @Override
    public boolean isAvailable() {
        final List<WebElement> buttons = getWrappedElement().findElements(By.xpath("ancestor-or-self::button"));
        if (buttons.isEmpty()) {
            throw new AutotestError("Элемент Button не найден");
        }
        return buttons.get(0).isEnabled();
    }
}
