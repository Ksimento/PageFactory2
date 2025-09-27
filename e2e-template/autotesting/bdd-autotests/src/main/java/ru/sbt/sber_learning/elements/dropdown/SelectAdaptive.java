package ru.sbt.sber_learning.elements.dropdown;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.actions.ClickActions;
import ru.sbt.edu_power.e2e_core.actions.PageControls;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbt.edu_power.e2e_core.elements.AutoIdentElement;
import ru.sbt.edu_power.e2e_core.elements.FieldFillingException;
import ru.sbt.edu_power.e2e_core.elements.dropdown.Dropdown;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Available;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Fillable;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Warning;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbt.edu_power.external_services.validator.Validatable;
import ru.sbt.edu_power.external_services.validator.Validator;
import ru.sbt.sber_learning.elements.WarningElement;
import ru.sbt.sber_learning.elements.text_input.TextInputAdaptive;
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
public class SelectAdaptive extends AutoIdentElement implements Fillable, Validatable, Warning, Available {
    private final List<String> values = new ArrayList();

    public SelectAdaptive(WebElement wrappedElement) {
        super(wrappedElement);

    }


    /**
     * Основной метод заполнения селекта, заполнение происходит в цикле пока поле не провалидируется и не заполнится
     * value - значение для заполнения
     * validated - валидация после заполнения
     */
    @Override
    public void fillField(String value, Boolean validated) throws FieldFillingException {
        prepareValues(value);
        AtomicReference<String> fillError = new AtomicReference("");
        BooleanSupplier waitWhenElementBeAvailable = this::isAvailable;
        BooleanSupplier waitWhenFieldBeFilled = () -> {
            if (validated && validateFieldBehavior()) {
                return true;
            } else {
                Timer.executeTimerThrowable(5, "Элемент не доступен для заполнения", waitWhenElementBeAvailable);

                try {
                    /**
                     * метод fillBehavior обеспечивает логику заполнения в зависимости от типа
                     */fillBehavior(validated);
                    PageControls.waitWhileNetworkActive();
                } catch (FieldFillingException var5) {
                    fillError.set(var5.getMessage());
                    return true;
                }

                return !validated;
            }
        };

        /**
         * результат лямлы waitWhenFieldBeFilled помещается в переменную result, если значение = false, выкидываем исключение
         */
        boolean result = Timer.executeTimer(DriverConstants.TIMEOUT * 2, waitWhenFieldBeFilled);
        if (!"".equals(fillError.get())) {
            throw new FieldFillingException(fillError.get());
        } else if (!result) {
            throw new FieldFillingException("");
        }
    }

    @Override
    public String getText() {
        return getTextBehavior();
    }

    /**
     * получаем текст в зависимости от типа, текст может храниться в атрибутах элемента
     */
    String getTextBehavior() {
        final String currentValue;
        switch (getSelectType()) {
            case SBERCLEVER_COMBOBOX:
                currentValue = getValueElement().getAttribute("value");
                break;
            case SBERCLEVER_COMBOBOX_MULTI:
                currentValue = getTags().stream().map(WebElement::getText).collect(Collectors.joining(", "));
                break;
            case SBERCLEVER_SELECT:
                currentValue = getValueElement().getText();
                break;
            default:
                throw new AutotestError(String.format(
                        "Для дропдауна типа \"%s\" получение значения не реализовано",
                        getSelectType().name()
                ));
        }

        return currentValue;
    }

    /**
     * Очищаем поле в зависимости от типа селекта
     */
    void clearBehavior() throws FieldFillingException {
        switch (getSelectType()) {
            case SBERCLEVER_COMBOBOX_MULTI:
                final List<WebElement> elements = new ArrayList();
                BooleanSupplier waitWhenTagsBeRemoved = () -> {
                    elements.clear();
                    elements.addAll(getTags());
                    if (elements.isEmpty()) {
                        return true;
                    } else {
                        try {
                            ClickActions.safeClick((elements.get(0)).getText(), (elements.get(0)).findElement(
                                    By.xpath(getSelectType().getRemoveTagXpath())));
                        } catch (StaleElementReferenceException var3) {
                        }

                        return false;
                    }
                };
                boolean result = Timer.executeTimer(DriverConstants.TIMEOUT, waitWhenTagsBeRemoved);
                if (!result) {
                    throw new FieldFillingException("Не удалось удалить все теги");
                }
                break;
            case SBERCLEVER_COMBOBOX:
                getTextInput().clear();
                break;
            default:
                throw new FieldFillingException(String.format(
                        "Дропдаун типа \"%s\" не поддерживает очистку",
                        getSelectType().name()
                ));
        }
    }

    /**
     * заполняем селект в зависимости от типа самого селекта
     */
    void fillBehavior(final Boolean validated) throws FieldFillingException {
        final Dropdown dropdown;
        switch (getSelectType()) {
            case SBERCLEVER_SELECT:
            case SBERCLEVER_COMBOBOX:
                String value = getValues().get(0);
                clear();
                getCloseButton().click();
                if(!value.equals("")) {
                dropdown = getDropdown("", validated);
                    ClickActions.safeClick(
                            value,
                            dropdown.getItemByName(value, true)
                    );
                }
                break;
            case SBERCLEVER_COMBOBOX_MULTI:
                clearSelect();

                try {

                    tagSelect(false);
                } catch (final AutotestError var5) {
                    throw new FieldFillingException(var5);
                }
        }
    }

    /**
     * валидируем поле на заполнение, если поле не заполнено нужным нам значением возвращаем false
     */
    boolean validateFieldBehavior() {
        final List<String> actual = Arrays
                .stream(getText().split(","))
                .map(DataProcessing::decodeValue)
                .map(e -> e.replace("Введите или выберите из списка", ""))
                .collect(Collectors.toList());
        final boolean validateResult = getValues()
                .stream()
                .allMatch(e -> Validator.matchValueInList(actual, e));
        switch (getSelectType()) {
            case SBERCLEVER_COMBOBOX:
            case SBERCLEVER_SELECT:
            case SBERCLEVER_COMBOBOX_MULTI:

                return validateResult && getValues().size() == actual.size();
            default:
                throw new AutotestError("Поведение валидации поля не описано для типа " +
                                        getSelectType());
        }
    }

    /**
     * метод добавляет теги из дропдауна и валидирует их
     */
    private void tagSelect(final Boolean validated) {
        final AtomicReference<WebElement> itemElement = new AtomicReference();
        getValues().forEach((v) -> {
            Dropdown d;
            try {
                d = getDropdown(v, validated);
            } catch (final FieldFillingException var6) {
                throw new AutotestError(var6);
            }

            final BooleanSupplier waitWhenItemBeSelected = () -> {
                try {
                    itemElement.set(d.getItemByName(v, false, "Добавить новый тег: *"));
                    if (null == itemElement.get()) {
                        itemElement.set(d.getItemByName("Добавить новый тег: *", true));
                    }

                    ClickActions.safeClick(((WebElement) itemElement.get()).getText(), itemElement.get());
                    return true;
                } catch (final StaleElementReferenceException var4) {
                    return false;
                }
            };
            Timer.executeTimerThrowable(2000, "Не удалось выбрать элемент: " + v, waitWhenItemBeSelected);
        });
    }


    /**
     * проверяем доступность элемента
     */
    @Override
    public boolean isAvailable() {
        final WebElement currentValueElementSberclever =
                getDisabledElement();
        return currentValueElementSberclever.isEnabled();
    }

    /**
     * Метод инициирует появление и создаёт объект дропдауна
     *
     * @param value Для элементов с поиском, иначе передавать пустую строку
     * @return Возвращает объект дропдауна
     */
    Dropdown getDropdown(String value, Boolean validated) throws FieldFillingException {
        final AtomicReference<Dropdown> dropdownSberclever = new AtomicReference();
        final AtomicReference<String> message = new AtomicReference("");
        final AtomicBoolean textFieldError = new AtomicBoolean(false);
        Assert.assertNotNull(value);
        BooleanSupplier waitWhenDropdownBeSet = () -> {
            try {
                if (value.isEmpty()) {
                    ClickActions.safeClick(
                            "Кнопка открытия селекта",
                            getOpenButton()
                    );
                } else {
                    TextInputAdaptive input = getTextInput();
                    if ("true".equals(input.getAttribute("readonly"))) {
                        ClickActions.safeClick(
                                "Кнопка открытия селекта",
                                getOpenButton()
                        );
                    } else {
                        String preparedValue = DataProcessing.decodeValue(value).replaceAll("\\*", "");
                        if (
                                getSelectType() == SelectTypeAdaptive.SBERCLEVER_COMBOBOX) {
                            ClickActions.safeClick(
                                    "Кнопка открытия селекта",
                                    getOpenButton()
                            );
                            input.sendKeys(preparedValue);
                        } else {
                            try {
                                input.fillField(preparedValue, validated);
                            } catch (FieldFillingException var9) {
                                textFieldError.set(true);
                                return false;
                            }
                        }
                    }
                }

                PageControls.waitWhileNetworkActive();
                dropdownSberclever.set(new Dropdown(
                        getSelectType().getDropdownBoxXpath(),
                        getSelectType().getDropdownItemXpath()
                ));
                boolean result = (dropdownSberclever.get()).getErrorMessage().isEmpty();
                message.set((dropdownSberclever.get()).getErrorMessage());
                return result;
            } catch (StaleElementReferenceException var10) {
                return false;
            }
        };
        boolean result = Timer.executeTimer(DriverConstants.TIMEOUT, waitWhenDropdownBeSet);
        if (textFieldError.get()) {
            throw new FieldFillingException("Не удалось заполнить поле поиска в селекте " +
                                            getSelectType());
        } else if (!result) {
            throw new FieldFillingException(message.get() + " Тип селекта: " +
                                            getSelectType());
        } else {
            return dropdownSberclever.get();
        }
    }

    /**
     * Очистка поля
     */
    @Override
    public void clear() {
        try {
            clearSelect();
        } catch (FieldFillingException e) {
            throw new AutotestError("Не удалось очистить поле", e);
        }
    }

    void clearSelect() throws FieldFillingException {
        clearBehavior();
    }

    private void prepareValues(String value) {
        values.clear();
        values.addAll(Arrays.stream(value.split(",")).map(DataProcessing::decodeValue).collect(Collectors.toList()));
    }

    // УТИЛИТНЫЕ  методы получения различных атрибутов и состояний

    /**
     * получаем список тегов в поле
     */
    List<WebElement> getTags() {
        return
                getParentIterator().getCurrentParent().findElements(By.xpath(
                        getSelectType().getTagXpath()));
    }

    /**
     * получаем текст предупреждения над элементом (например: поле обязательное для заполнения)
     */
    @Override
    public Optional<String> getTextWarning() {
        final List<WebElement> elements = new ArrayList<>();
        final BooleanSupplier waitWhenWarningBePresent = () -> {
            elements.addAll(WarningElement.getWarningElement(getParentIterator()
                    .getCurrentParent(),getSelectType().getWarningMassage()));
            return true;
        };
        final boolean result = Timer.executeTimer(DriverConstants.TIMEOUT, waitWhenWarningBePresent);
        return result ? Optional.of(elements.get(0).getText()) : Optional.empty();
    }

    /**
     * проверяем отсутствие предупреждения
     */
    @Override
    public void notTextWarning() {
        final BooleanSupplier waitWhenWarningByHiding = () ->  WarningElement.getWarningElement(getParentIterator()
                .getCurrentParent(),getSelectType().getWarningMassage())
                .isEmpty();
        Timer.executeTimerThrowable(
                DriverConstants.ELEMENT_WAIT_5SEC,
                "Поле содержит предупреждение, которого не должно быть",
                waitWhenWarningByHiding
        );
    }


    /**
     * получаем текущий текст в полее
     */
    @Override
    public String getFieldValue() {
        return getTextBehavior();
    }

    /**
     * Получаем элемент открытия кнопки дропдауна
     */
    private WebElement getOpenButton() {
        return
                getSpecifyElement(
                        getSelectType().getOpenButtonXpath(),
                        "Кнопка открытия дропдауна не определена. Тип элемента " +
                        getSelectType()
                );
    }

    @Override
    public boolean validate(String expected) {
        prepareValues(expected);
        return validateFieldBehavior();
    }

    SelectTypeAdaptive getSelectType() {
        return (SelectTypeAdaptive)
                getElementType(SelectTypeAdaptive.values());
    }

    /**
     * получаем элемент с текущим значением поля
     */
    public WebElement getValueElement() {
        return
                getSpecifyElement(
                        getSelectType().getValueXpath(),
                        "Не удалось получить элемент с текущим значением поля. Тип элемента " +
                        getSelectType()
                );
    }

    TextInputAdaptive getTextInput() {
        Assert.assertNotNull(
                "Селект не поддерживает фильтрацию списка",
                getSelectType().getInputXpath()
        );
        return new TextInputAdaptive(
                getInput());
    }
    TextInputAdaptive getCloseButton() {
        return new TextInputAdaptive(
                getSpecifyElement(getSelectType().getCloseButtonXpath(),"Не удалось получить кнопку закрытия"));
    }


    List<String> getValues() {
        return values;
    }

    public WebElement getDisabledElement() {
        return
                getSpecifyElement(
                        getSelectType().getDisabledElement(), "Не удалось получить элемент. Тип элемента " +
                                                              getSelectType());
    }
}
