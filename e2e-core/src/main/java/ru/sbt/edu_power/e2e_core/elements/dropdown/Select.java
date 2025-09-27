package ru.sbt.edu_power.e2e_core.elements.dropdown;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbt.edu_power.e2e_core.elements.AutoIdentElement;
import ru.sbt.edu_power.e2e_core.elements.FieldFillingException;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Available;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Fillable;
import ru.sbt.edu_power.external_services.validator.Validatable;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Warning;
import ru.sbt.edu_power.e2e_core.elements.text_input.TextInput;
import ru.sbt.edu_power.e2e_core.actions.ClickActions;
import ru.sbt.edu_power.e2e_core.actions.PageControls;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

/**
 * Класс реализует универсальный доступ к любому типу элемента с выпадающим списком
 */
@Slf4j
public class Select extends AutoIdentElement implements Fillable, Validatable, Warning, Available {
    private final String WARNING_MESSAGE_XPATH = "descendant::div[contains(@class, 'error-msg')] | descendant::p[contains(@class,'_ErrorLabel') or contains(@class, 'Mui-error')] | descendant::div[contains(@class,'_Hint') and @data-error='true'] | descendant-or-self::*[contains(@class,'text-negative')]";
    private final List<String> values = new ArrayList<>();
    private final SelectBehavior selectBehavior;

    public Select(final WebElement wrappedElement) {
        super(wrappedElement);
        selectBehavior = new SelectBehavior(this);
    }

    @Override
    public void fillField(final String value, final Boolean validated) throws FieldFillingException {
        prepareValues(value);
        final AtomicReference<String> fillError = new AtomicReference<>("");
        final BooleanSupplier waitWhenElementBeAvailable = this::isAvailable;
        final BooleanSupplier waitWhenFieldBeFilled = () -> {
            if (validated && selectBehavior.validateFieldBehavior()) {
                return true;
            }
            Timer.executeTimerThrowable(
                    DriverConstants.ELEMENT_WAIT_5SEC,
                    "Элемент не доступен для заполнения",
                    waitWhenElementBeAvailable
            );
            try {
                selectBehavior.fillBehavior(validated);
                PageControls.waitWhileNetworkActive();
            } catch (final FieldFillingException e) {
                fillError.set(e.getMessage());
                return true;
            }
            return !validated;
        };
        final boolean result = Timer.executeTimer(DriverConstants.TIMEOUT * 2, waitWhenFieldBeFilled);
        if (!"".equals(fillError.get())) {
            throw new FieldFillingException(fillError.get());
        }
        if (!result) {
            throw new FieldFillingException("");
        }
    }

    @Override
    public String getText() {
        return selectBehavior.getTextBehavior();
    }

    @Override
    public String getFieldValue() {
        return getText();
    }

    @Override
    public boolean validate(final String expected) {
        prepareValues(expected);
        return selectBehavior.validateFieldBehavior();
    }

    public List<String> getAllOptions() {
        final Dropdown dropdown;
        try {
            dropdown = getDropdown("", true);
        } catch (final FieldFillingException e) {
            throw new AutotestError("Не удалось открыть дропдаун", e);
        }
        DriverUtils.freeze(DriverConstants.FREEZE_500_MS);
        AtomicReference<List<String>> items = new AtomicReference<>();
        final BooleanSupplier getItems = ()->{
            try {
                items.set(dropdown
                        .getItemsList()
                        .stream()
                        .map(WebElement::getText)
                        .collect(Collectors.toList()));
                return true;
            }catch (StaleElementReferenceException e){
                return false;
            }
        };
        Timer.executeTimer(DriverConstants.ELEMENT_WAIT_5SEC, getItems);
        ClickActions.safeClick("Кнопка закрытия дропдауна", getCloseButton());
        return items.get();
    }

    @Override
    public void clear() {
        try {
            clearSelect();
        } catch (final FieldFillingException e) {
            throw new AutotestError("Не удалось очистить поле", e);
        }
    }

    void clearSelect() throws FieldFillingException {
        selectBehavior.clearBehavior();
    }

    @Override
    public Optional<String> getTextWarning() {
        getSelectType();
        final List<WebElement> elements = new ArrayList<>();
        final BooleanSupplier waitWhenWarningBePresent = () -> {
            elements.addAll(getParentIterator()
                    .getCurrentParent()
                    .findElements(By.xpath(WARNING_MESSAGE_XPATH)));
            return !elements.isEmpty();
        };
        final boolean result = Timer.executeTimer(DriverConstants.TIMEOUT, waitWhenWarningBePresent);
        return result ? Optional.of(elements.get(0).getText()) : Optional.empty();
    }

    @Override
    public void notTextWarning() {
        getSelectType();
        final BooleanSupplier waitWhenWarningByHiding = () -> getParentIterator()
                .getCurrentParent()
                .findElements(By.xpath(WARNING_MESSAGE_XPATH))
                .isEmpty();
        Timer.executeTimerThrowable(
                DriverConstants.ELEMENT_WAIT_5SEC,
                "Поле содержит предупреждение, которого не должно быть",
                waitWhenWarningByHiding
        );
    }

    @Override
    public boolean isAvailable() {
        final WebElement currentValueElement = getDisabledElement();
        return currentValueElement.isEnabled();
    }

    /**
     * Метод инициирует появление и создаёт объект дропдауна
     *
     * @param value Для элементов с поиском, иначе передавать пустую строку
     * @return Возвращает объект дропдауна
     */
    Dropdown getDropdown(final String value, final Boolean validated) throws FieldFillingException {
        final AtomicReference<Dropdown> dropdown = new AtomicReference<>();
        final AtomicReference<String> message = new AtomicReference<>("");
        final AtomicBoolean textFieldError = new AtomicBoolean(false);
        Assert.assertNotNull(value);
        final BooleanSupplier waitWhenDropdownBeSet = () -> {
            try {
                if (value.isEmpty()) {
                    ClickActions.safeClick("Кнопка открытия селекта", getOpenButton());
                } else {
                    final TextInput input = getTextInput();
                    if ("true".equals(input.getAttribute("readonly"))) {
                        ClickActions.safeClick("Кнопка открытия селекта", getOpenButton());
                    } else {
                        final String preparedValue = DataProcessing
                                .decodeValue(value)
                                .replaceAll("\\*", "");
                        if (getSelectType() == SelectType.TAG_SELECT) {
                            ClickActions.safeClick("Кнопка открытия селекта", getOpenButton());
                            input.sendKeys(preparedValue);
                        } else {
                            try {
                                input.fillField(preparedValue, validated);
                            } catch (final FieldFillingException e) {
                                textFieldError.set(true);
                                return true;
                            }
                        }
                    }
                }
                PageControls.waitWhileNetworkActive();
                dropdown.set(new Dropdown(
                        getSelectType().getDropdownBoxXpath(),
                        getSelectType().getDropdownItemXpath()
                ));
                final boolean result = dropdown.get().getErrorMessage().isEmpty();
                message.set(dropdown.get().getErrorMessage());
                return result;
            } catch (final StaleElementReferenceException ignored) {
            }
            return false;
        };
        final boolean result = Timer.executeTimer(DriverConstants.TIMEOUT, waitWhenDropdownBeSet);
        if (textFieldError.get()) {
            throw new FieldFillingException("Не удалось заполнить поле поиска в селекте " + getSelectType());
        }
        if (!result) {
            throw new FieldFillingException(message.get() + " Тип селекта: " + getSelectType());
        }
        return dropdown.get();
    }

    List<WebElement> getTags() {
        return getParentIterator().getCurrentParent().findElements(By.xpath(getSelectType().getTagXpath()));
    }

    // Метод возвращает веб-элемент в котором содержится поле ввода для фильтрации списка дропдауна
    TextInput getTextInput() {
        Assert.assertNotNull("Селект не поддерживает фильтрацию списка", getSelectType().getInputXpath());
        return new TextInput(getInput());
    }

    public WebElement getValueElement() {
        return getSpecifyElement(
                getSelectType().getValueXpath(),
                "Не удалось получить элемент с текущим значением поля. Тип элемента " + getSelectType()
        );
    }

    public WebElement getDisabledElement() {
        return getSpecifyElement(
                getSelectType().getDisabledElement(),
                "Не удалось получить элемент. Тип элемента " + getSelectType()
        );
    }

    // Метод возвращает веб-элемент, по нажатии на который происходит открытие дропдауна
    private WebElement getOpenButton() {
        return getSpecifyElement(
                getSelectType().getOpenButtonXpath(),
                "Кнопка открытия дропдауна не определена. Тип элемента " + getSelectType()
        );
    }

    // Метод возвращает веб-элемент, по нажатии на который происходит закрытие дропдауна
    WebElement getCloseButton() {
        return getSpecifyElement(
                getSelectType().getCloseButtonXpath(),
                "Кнопка закрытия дропдауна не определена. Тип элемента " + getSelectType()
        );
    }


    // Метод автоматически определяет тип дропдауна по характерным признакам
    SelectType getSelectType() {
        return (SelectType) getElementType(SelectType.values());
    }

    List<String> getValues() {
        return values;
    }

    @Override
    public String getLabel() {
        final List<WebElement> elements = getParentIterator().getCurrentParent().findElements(By.xpath(".//label"));
        return elements.isEmpty() ? "" : elements.get(0).getText();
    }

    private void prepareValues(final String value) {
        values.clear();
        values.addAll(Arrays
                .stream(value.split(","))
                .map(DataProcessing::decodeValue)
                .collect(Collectors.toList())
        );
    }
}
