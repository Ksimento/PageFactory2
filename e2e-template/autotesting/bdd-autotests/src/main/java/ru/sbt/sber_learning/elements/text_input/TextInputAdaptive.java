package ru.sbt.sber_learning.elements.text_input;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.e2e_core.elements.AutoIdentElement;
import ru.sbt.edu_power.e2e_core.elements.FieldFillingException;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Available;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Fillable;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Warning;
import ru.sbt.edu_power.e2e_core.elements.text_input.TextInput;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbt.edu_power.external_services.validator.Validatable;
import ru.sbt.edu_power.external_services.validator.Validator;
import ru.sbt.sber_learning.elements.WarningElement;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

public final class TextInputAdaptive extends AutoIdentElement implements Fillable, Available, Validatable, Warning {
    private String value;
    public InputTypeAdaptive getInputAdaptiveType(){
        return (InputTypeAdaptive)this.getElementType(InputTypeAdaptive.values());
    }
    public TextInputAdaptive(final WebElement wrappedElement) {
        super(wrappedElement);

    }

    @Override
    public String getText() {
        switch (getInputAdaptiveType()) {
            case TEXT_INPUT:
            case NUMBER_INPUT:
                return Optional.ofNullable(getInput().getAttribute("value")).orElse("");
            case TEXTAREA:
                return getInput().getText();
            default:
                throw new AutotestError(String.format(
                        "Для поля типа \"%s\" не реализовано получение текста",
                        this.getInputType()
                ));
        }
    }

    /**
     * проверяем доступность элемента
     */
    @Override
    public boolean isAvailable() {
        return this.getInput().isEnabled();
    }

    /**
     * Основной метод заполнения интпута, заполнение происходит в цикле пока поле не провалидируется и не заполнится
     * value - значение для заполнения
     * validated - валидация после заполнения
     */
    @Override
    public void fillField(final String value, final Boolean validated) throws FieldFillingException {
        final AtomicReference<String> errorMessage = new AtomicReference();
        final TextInput textInput = new TextInput(getWrappedElement());
        final BooleanSupplier waitWhenFieldBeFilled = () -> {
            switch (getInputAdaptiveType()) {
                case TEXTAREA:
                case TEXT_INPUT:
                case NUMBER_INPUT:
                    if (validated && this.validate(getValue(value), errorMessage)) {
                        return true;
                    }
                    if(validated){
                        textInput.clear();
                    }
                    textInput.sendKeys(getValue(value));
                    DriverUtils.freeze(250L);
                    return !validated;
                default:
                    throw new AutotestError(String.format(
                            "Для поля типа \"%s\" не реализовано заполнение",
                            this.getInputType()
                    ));
            }
        };
        final boolean result;
        try {
            result = Timer.executeTimer(DriverConstants.TIMEOUT, waitWhenFieldBeFilled);
        } catch (final AutotestError var7) {
            throw new FieldFillingException(var7);
        }

        if (!result) {
            throw new FieldFillingException(errorMessage.get());
        } else {
            if (this.value.startsWith("%")) {
                this.getInput().sendKeys(Keys.ENTER);
            }

            this.value = null;
        }
    }

    public void clear(){
        new TextInput(getWrappedElement()).clear();
    }

    /**
     * Валидируем строку на лишние символы
     * @param expected - Срока которую нужно проверить
     * @return Возвращаем результат валидации
     */
    @Override
    public boolean validate(final String expected) {
        final String decodedExpected = DataProcessing.decodeValue(expected).replaceAll("\u00ad", "").replaceAll("\n", " ");
        final String actualValue = getText();
        return Validator.matchValues(actualValue, decodedExpected) ||
               Validator.matchValues(
                       actualValue.replace(":", "").replaceAll("\\.", "").trim(),
                       decodedExpected.replaceAll("^%", "")
               );
    }

    private boolean validate(final String expected, final AtomicReference<String> errorMessage) {
        final boolean validateResult = this.validate(expected);
        if (!validateResult) {
            errorMessage.set(String.format("Не удалось заполнить поле. После попытки заполнения значением \"%s\", в поле устанавливается неверное значение \"%s\"", expected, this.getText()));
        }

        return validateResult;
    }


    private WebElement getSvg() {
        return this.getSpecifyElement(this.getInputType().getInputXpath(), "Не удалось получить элемент для установки значения поля");
    }

    /**
     * определяем тип Input
     */
    public InputTypeAdaptive getInputType() {
        return (InputTypeAdaptive) this.getElementType(InputTypeAdaptive.values());
    }

    @Override
    public Optional<String> getTextWarning() {
        final AtomicReference<WebElement> element = new AtomicReference<>();
        final BooleanSupplier waitWhenWarningBeDisplayed = () -> {
            element.set(getWarningMessage());
            return null != element.get();
        };
        final boolean result = Timer.executeTimer(DriverConstants.TIMEOUT, waitWhenWarningBeDisplayed);
        return result ? Optional.of(element.get().getText()) : Optional.empty();
    }

    @Override
    public void notTextWarning() {
        final BooleanSupplier waitWhenWarningIsGone = () -> getWarningMessage() == null;
        Timer.executeTimerThrowable(
                DriverConstants.TIMEOUT,
                "Предупреждение не исчезло по истечению времени",
                waitWhenWarningIsGone
        );
    }

    @Override
    public String getFieldValue() {
        return getText();
    }


    private String getValue(final String data) {
        if (null == this.value) {
            this.value = DataProcessing.decodeValue(data);
        }
        return this.value;
    }
    private WebElement getWarningMessage() {
        final String warningXpath = getInputType().getWarningXpath();
        if (null == warningXpath) {
            throw new AutotestError("Поле не поддерживает вывод предупреждений Warning");
        }
        final List<WebElement> elements = WarningElement.getWarningElement(getParentIterator()
                .getCurrentParent(),getInputAdaptiveType().getWarningXpath());
        if (elements.size() > 1) {
            throw new AutotestError("Найдено больше одного элемента Warning, проверьте xpath");
        }
        return elements.isEmpty() ? null : elements.get(0);
    }

    @Override
    protected String getLabel() {
        final String labelXpath = getInputType().getLabelXpath();
        if (null == labelXpath) {
            return "";
        }
        final List<WebElement> elements = getParentIterator()
                .getCurrentParent()
                .findElements(By.xpath(labelXpath));
        return elements.isEmpty() ? "" : elements.get(0).getText();


    }
}
