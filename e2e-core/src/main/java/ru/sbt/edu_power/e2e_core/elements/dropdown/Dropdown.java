package ru.sbt.edu_power.e2e_core.elements.dropdown;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbt.edu_power.external_services.validator.Validator;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

/**
 * Класс реализует объект выпадающего списка
 */
public class Dropdown {
    // Веб-элемент выпадающего списка
    private final WebElement box;
    // Сообщения об ошибки, возникшие при попытке создания объекта
    private final String errorMessage;
    // Xpath строки (элемента) списка
    private final String dropdownLineXpath;

    public Dropdown(final String dropdownBoxXpath, final String dropdownItemXpath) {
        final AtomicReference<List<WebElement>> elements = new AtomicReference<>();
        final BooleanSupplier waitWhenRenderingBeDone = () -> {
            elements.set(DriverUtils
                    .findElementsOnPageByXpath(dropdownBoxXpath)
                    .stream()
                    .filter(WebElement::isDisplayed)
                    .collect(Collectors.toList()));
            return !elements.get().isEmpty();
        };
        if (!Timer.executeTimerMillis(DriverConstants.ELEMENT_WAIT_5SEC, waitWhenRenderingBeDone)) {
            errorMessage = "Дропдаун не появился";
            box = null;
        } else if (elements.get().size() > 1) {
            errorMessage = "На странице отображается более двух дропдаунов";
            box = null;
        } else {
            errorMessage = "";
            box = elements.get().get(0);
        }
        if (errorMessage.isEmpty()) {
            renderingControl(DataProcessing.generator("word10"));
        }
        this.dropdownLineXpath = dropdownItemXpath;
    }

    public WebElement getItemByName(final String nameDraft, final boolean strictMode) {
        return getItemByName(nameDraft, strictMode, "");
    }

    /**
     * Метод реализует поиск строки в выпадающем списке по значению
     *
     * @param nameDraft  Искомое значение. Может использоваться конструкция stash# и маска из звёздочек
     * @param strictMode Если true, то будет выдано исключение в случае отсутствия значения, иначе вернёт null
     * @return Веб-элемент строки соответствущей запросу
     */
    public WebElement getItemByName(final String nameDraft, final boolean strictMode, final String exclude) {
        final String name = DataProcessing.decodeValue(nameDraft);
        final AtomicReference<WebElement> dropdownLine = new AtomicReference<>();
        final String decodeName = name.replaceAll("\\*", "");
        final String lineXpathWithName = dropdownLineXpath +
                "/descendant-or-self::*[contains(text(), '" +
                decodeName +
                "')]";
        final AtomicReference<String> message = new AtomicReference<>("");
        final BooleanSupplier waitWhenNameBePresent = () -> {
            final List<WebElement> elements = box
                    .findElements(By.xpath(lineXpathWithName))
                    .stream()
                    .filter(e -> {
                        final String rowValue = e.getText().replaceAll("­", "");
                        final boolean isExcluded = !exclude.isEmpty() && Validator.matchValues(rowValue, exclude);
                        return Validator.matchValues(rowValue, name) && !isExcluded;
                    })
                    .collect(Collectors.toList());
            if (!strictMode) {
                if (elements.isEmpty()) {
                    dropdownLine.set(null);
                } else {
                    dropdownLine.set(elements.get(0));
                }
                return true;
            }
            if (elements.isEmpty()) {
                // если мы не смогли найти элемент по xpath, то пробуем его найти по вхождению текста
                List<WebElement> dropdownItemElements = box.findElements(By.xpath(dropdownLineXpath));
                if (!dropdownItemElements.isEmpty()) {
                    dropdownLine.set(dropdownItemElements.stream().filter(e -> e.getText().replaceAll("­", "").contains(decodeName)).findFirst().orElseThrow(() -> new AutotestError(String.format(
                            "На странице нет элемента выпадающего списка с текстом \"%s\"",
                            name))));
                    return true;
                } else {
                    message.set("Выпадающий список пуст");
                    return false;
                }
            } else if (elements.size() > 1) {
                message.set(String.format(
                        "Найдено несколько значений в списке:\n\t%s",
                        elements.stream().map(WebElement::getText).collect(Collectors.joining("\n\t"))
                ));
                return false;
            } else {
                dropdownLine.set(elements.get(0));
                return true;
            }
        };
        final boolean result = Timer.executeTimer(DriverConstants.ELEMENT_WAIT_5SEC, waitWhenNameBePresent);
        Assert.assertTrue(message.get(), result);
        return dropdownLine.get();
    }

    public List<WebElement> getItemsListByNames(final List<String> values) {
        return values
                .stream()
                .map(v -> this.getItemByName(v, true))
                .collect(Collectors.toList());
    }

    // Возвращает список веб-элементов всех доступных строк
    public List<WebElement> getItemsList() {
        return box.findElements(By.xpath(dropdownLineXpath));
    }

    // Возвращает сообщение об ошибке
    public String getErrorMessage() {
        return errorMessage;
    }

    public WebElement getBox() {
        return box;
    }

    // содержимое дропдауна может некоторое время перестраиваться, поэтому ждём перестроения и проверяем
    // что в течении секунды содержимое не перестраивается
    private void renderingControl(final String timerKey) {
        Timer.isTimeoutThrowable(timerKey, DriverConstants.ELEMENT_WAIT_5SEC, "Дропдаун не готов в течении 5 сек");
        final String script = "return arguments[0].innerHTML";
        final int elementHash = DriverUtils.executeJS(script, box).hashCode();
        final BooleanSupplier waitWhenRenderingBeDone = () ->
                DriverUtils.executeJS(script, box).hashCode() != elementHash;
        if (Timer.executeTimerMillis(1000, waitWhenRenderingBeDone)) {
            renderingControl(timerKey);
        }
    }
}
