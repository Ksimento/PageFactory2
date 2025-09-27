package ru.sbt.edu_power.e2e_core.elements;

import org.openqa.selenium.WebElement;
import ru.sbtqa.tag.qautils.errors.AutotestError;
import ru.yandex.qatools.htmlelements.element.TypifiedElement;

public class Table extends TypifiedElement {
    public Table(final WebElement wrappedElement) {
        super(wrappedElement);
    }

    @Override
    public String getText() {
        throw new AutotestError("Нельзя получить текст таблицы. Используйте шаги и методы для работы с таблицей TableActions");
    }
}
