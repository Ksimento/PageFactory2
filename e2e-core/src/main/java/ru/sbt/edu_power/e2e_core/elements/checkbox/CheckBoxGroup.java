package ru.sbt.edu_power.e2e_core.elements.checkbox;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.elements.FieldFillingException;
import ru.sbt.edu_power.e2e_core.error_processing.ErrorCollector;
import ru.sbtqa.tag.qautils.errors.AutotestError;
import ru.yandex.qatools.htmlelements.element.TypifiedElement;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Класс реализует элемент группы чекбоксов. Группа определяется одним общим именем,
 * каждый чекбокс группы может иметь любое имя.
 */
public class CheckBoxGroup extends TypifiedElement {

    public CheckBoxGroup(final WebElement wrappedElement) {
        super(wrappedElement);
    }

    /**
     * Метод устанавливает выбранные значения для группы чекбоксов
     *
     * @param data dataTable из названий и состояний чекбоксов
     */
    public void fillGroup(final Map<String, String> data) {
        for (final String key : data.keySet()) {
            final WebElement element = getElementCheckBox(key);
            final CheckBox checkBox = new CheckBox(element);
            try {
                checkBox.fillField(data.get(key), true);
            } catch (final FieldFillingException e) {
                throw new AutotestError("Не удалось заполнить элемент " + key, e);
            }
        }
    }
    private WebElement getElementCheckBox(final String text){
        List<WebElement> elements;
       elements = getWrappedElement()
                .findElements(By.xpath(".//*[text() = '" + text + "']"));
        Assert.assertFalse(String.format("Не найден более 1 чекбокса с текстом", text),elements.size()>1);
        if (elements.isEmpty()) {
            elements = this.getWrappedElement().findElements(By.xpath(".//*[@value = '" + text + "']"));
            Assert.assertFalse(String.format("Не найден более 1 чекбокса с текстом", text), elements.size() > 1);
            Assert.assertFalse(String.format("Не найден текст \"%s\" внутри групп чекбоксов", text), elements.isEmpty());
            return elements.get(0);
        } else {
            return elements.get(0);
        }
    }

    /**
     * Метод сравнивает фактические состояния в группе с ожидаемыми
     *
     * @param data dataTable из ожидаемых значений по имени и состоянию чекбокса
     */
    public void validateGroup(final Map<String, String> data) {
        final Map<String, Boolean> actualData = collectActualData();
        final ErrorCollector errorCollector = new ErrorCollector();
        for (final String key : data.keySet()) {
            final Boolean isExists = actualData.containsKey(key);
            errorCollector.assertNotNull(String.format("Чекбокса \"%s\" нет в группе", key), isExists);
            if (isExists) {
                final Boolean entryValue = "да".equalsIgnoreCase(data.get(key)) ||
                        "true".equalsIgnoreCase(data.get(key));
                errorCollector.assertEquals(
                        String.format("Фактическое значение \"%b\" чекбокса \"%s\" не соответствует ожидаемому \"%s\"",
                                actualData.get(key), key, data.get(key)
                        ),
                        actualData.get(key), entryValue
                );
            }
        }
        errorCollector.assertAll();
    }

    Map<String, Boolean> collectActualData() {
        return getWrappedElement()
                .findElements(By.xpath(".//label"))
                .stream()
                .map(CheckBox::new)
                .collect(Collectors.toMap(CheckBox::getLabel, CheckBox::getFieldState, (a, b) -> b));
    }
}
