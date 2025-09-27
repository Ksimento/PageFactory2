package ru.sbt.edu_power.e2e_core.elements.survey;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.external_services.validator.Validator;
import ru.sbt.edu_power.e2e_core.elements.FieldFillingException;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Fillable;
import ru.sbt.edu_power.external_services.validator.Validatable;
import ru.sbt.edu_power.e2e_core.elements.multiple_type_content.MultipleTypeContent;
import ru.sbt.edu_power.e2e_core.survey.SurveyActions;
import ru.sbtqa.tag.qautils.errors.AutotestError;
import ru.yandex.qatools.htmlelements.element.TypifiedElement;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

/**
 * Класс реализует работу с группой чекбоксов
 * Параметры для заполнения передаются следующим образом:
 * Название чекбокса#устанавливаемое значение
 * Множественные значения передаются через амперсанд, например
 * первое имя#да & второе имя#нет & третье имя#да
 * В качестве значения можно передавать да, нет, true, false
 * В качестве имени можно передавать значение из стэша
 */

public class SurveyCheckBoxGroup extends TypifiedElement implements SurveyJSField, Fillable, Validatable, ElementPartValidatable {
    public SurveyCheckBoxGroup(final WebElement wrappedElement) {
        super(wrappedElement);
    }

    @Override
    public void fillField(final String values, final Boolean validated) throws FieldFillingException {
        final Map<String, Boolean> valuesMap = getValueMap(values);
        final Map<WebElement, Boolean> itemList = getItemValueMap(valuesMap);
        try {
            itemList.forEach((k, v) -> {
                try {
                    setValue(getTextOrSrc(k), k, v);
                } catch (final FieldFillingException e) {
                    throw new AutotestError(e);
                }
            });
        } catch (final AutotestError e) {
            throw new FieldFillingException(e);
        }

    }

    private void setValue(final String name, final WebElement checkBoxItem, final Boolean value)
    throws FieldFillingException {
        final BooleanSupplier waitWhenCheckboxBeSet = () -> value.equals(isChecked(checkBoxItem));
        SurveyActions.clickExecutor(name, checkBoxItem, waitWhenCheckboxBeSet);
    }

    private Map<String, Boolean> getValueMap(final String values) {
        final Map<String, Boolean> map = new HashMap<>();
        if ("".equals(values)) {
            return map;
        }
        Arrays.stream(values.split("&")).forEach(t -> {
            if (!t.contains("#")) {
                throw new AutotestError("Формат ответа должен быть в виде 'имя#boolean'");
            }
            final String name = DataProcessing.decodeValue(
                    t.substring(0, t.lastIndexOf("#"))
                            .trim()
            );
            final Boolean value = DriverUtils.getBoolean(
                    t.substring(t.lastIndexOf("#") + 1)
                            .trim()
            );
            map.put(name, value);
        });
        return map;
    }

    private boolean isChecked(final WebElement checkBoxItem) {
        return "true".equals(checkBoxItem.getAttribute("data-checked"));
    }

    WebElement getInput(final WebElement checkBoxItem) {
        return checkBoxItem.findElement(By.xpath(".//input[@type = 'checkbox']"));
    }

    private Map<WebElement, Boolean> getItemValueMap(final Map<String, Boolean> valueList) {
        final List<WebElement> items = getCheckBoxItems();
        final Map<WebElement, Boolean> itemValueMap = new LinkedHashMap<>();
        final List<String> itemNameList = getItemNameList(items);

        for (int i = 0; i < itemNameList.size(); i++) {
            final int position = i;
            final AtomicBoolean state = new AtomicBoolean(false);
            valueList.forEach((k, v) -> {
                if (matchTextOrSrc(itemNameList.get(position), k)) {
                    state.set(v);
                } else {
                    final Integer number = DriverUtils.getNumber(k);
                    if (null != number && number == (position + 1)) {
                        state.set(v);
                    }
                }
            });
            itemValueMap.put(items.get(i), state.get());
        }
        return itemValueMap;
    }

    private List<String> getItemNameList(final List<WebElement> checkBoxItems) {
        return checkBoxItems
                .stream()
                .map(this::getTextOrSrc)
                .collect(Collectors.toList());
    }

    boolean matchTextOrSrc(final String actual, final String expected) {
        return Validator.matchValues(actual, expected);
    }

    String getXPath() {
        return "descendant-or-self::fieldset/div[contains(@class, 'checkbox')]";
    }

    private List<WebElement> getCheckBoxItems() {
        return getWrappedElement().findElements(By.xpath(".//div[contains(@class, '_ChooseManyFromManyAnswer')]"));
    }

    String getTextOrSrc(final WebElement element) {
        return element.getText();
    }

    private MultipleTypeContent getMultipleTypeContentFromItemByIndex(final int index) {
        final List<WebElement> elements = getWrappedElement().findElements(By.xpath(getXPath()));
        if (index > elements.size()) {
            throw new AutotestError(String.format("Попытка получить %s-й элемент в списке из %s элементов", index, elements.size()));
        }
        return new MultipleTypeContent(elements.get(index - 1));
    }

    @Override
    public String getFieldValue() {
        return getCheckBoxItems().stream().map(e -> {
            final String name = getTextOrSrc(e);
            final String value = isChecked(e) ? "Да" : "Нет";
            return name + "#" + value;
        }).collect(Collectors.joining("; "));
    }

    @Override
    public boolean validate(final String expected) {
        final Map<String, Boolean> expectedValues = getValueMap(expected);
        final Map<WebElement, Boolean> itemValueMap = getItemValueMap(expectedValues);
        final AtomicBoolean result = new AtomicBoolean(true);
        itemValueMap.forEach((item, value) -> {
            if (isChecked(item) != value) {
                result.set(false);
            }
        });
        return result.get();
    }

    @Override
    public boolean partialValidate(final String elementPartId, final String expected) {
        return getMultipleTypeContentFromItemByIndex(
                getElementNumber(elementPartId)
        ).validate(expected);
    }

    @Override
    public String getPartContent(final String elementPartId) {
        return getMultipleTypeContentFromItemByIndex(
                getElementNumber(elementPartId)
        ).getFieldValue();
    }

    @Override
    public void clear() {
        try {
            fillField("", true);
        } catch (final FieldFillingException e) {
            throw new AutotestError("Не удалось очистить группу чекбоксов", e);
        }

    }
}
