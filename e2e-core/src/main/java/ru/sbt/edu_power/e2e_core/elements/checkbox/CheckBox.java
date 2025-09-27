package ru.sbt.edu_power.e2e_core.elements.checkbox;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.actions.ClickActions;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.e2e_core.elements.AutoIdentElement;
import ru.sbt.edu_power.e2e_core.elements.FieldFillingException;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Available;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Fillable;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbt.edu_power.external_services.validator.Validatable;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.util.List;
import java.util.function.BooleanSupplier;

public class CheckBox extends AutoIdentElement implements Fillable, Validatable, Available {

    public CheckBox(final WebElement wrappedElement) {
        super(wrappedElement);
    }


    @Override
    public boolean isAvailable() {
        return getSpecifyElement(
                getCheckBoxType().getDisabledAttributeXpath(),
                "Не удалось получить элемент со статусом disabled"
        ).isEnabled();
    }

    @Override
    public void fillField(final String value, final Boolean validated) throws FieldFillingException {

        final BooleanSupplier waitAvailability = this::isAvailable;

        final BooleanSupplier waitWhenElementBeFilled = () -> {
            Timer.executeTimerThrowable(
                    DriverConstants.ELEMENT_WAIT_5SEC * 2,
                    String.format("Элемент \"%s\" не доступен для заполнения", getLabel()),
                    waitAvailability
            );
            if (validated && (validate(value) || !isAvailable())) {
                return true;
            }
            clickAction();
            return !validated;
        };
        final boolean result = Timer.executeTimer(
                DriverConstants.ELEMENT_WAIT_5SEC * 3,
                waitWhenElementBeFilled
        );
        if (!result) {
            throw new FieldFillingException("");
        }
    }

    @Override
    public String getFieldValue() {
        return String.valueOf(getFieldState());
    }

    @Override
    public boolean validate(final String expected) {
        return getFieldState() == DriverUtils.getBoolean(expected);
    }

    public boolean getFieldState() {
        final boolean result;
        switch (getCheckBoxType()) {
            case DEFAULT:
                result = getValueElement().getAttribute("data-testid").contains("checked");
                break;
            case CHECKBOX_V5:
                result = getValueElement().getAttribute("class").contains("justify-end");
                break;
            case MUI_SWITCH:
            case MUI:
            case MFE:
                final String value = getValueElement().getAttribute("value");
                final boolean isEmptyValue = value == null || (value.isEmpty());
                result = isEmptyValue ? getValueElement().getAttribute("class").contains("checked") : getValueElement()
                        .getAttribute("value")
                        .equals("true") || getLabelElement().getAttribute("class").contains("active");
                break;
            case SBER_ENGAGE:
                result = getValueElement().getAttribute("class").contains("not-empty");
                break;
            case MFE_FILTER:
                result = getValueElement().getAttribute("class").contains("active");
                break;
            case STUDENT_MFE:
                result = getLabelElement().getAttribute("class").contains("primary");
                break;
            case MFE_MODULE_INPUT:
                result = getLabelElement().getAttribute("class").contains("active");
                break;
            case MFE_CHECKBOX:
                result = getInput().getAttribute("value").equals("true");
                break;
            default:
                throw new AutotestError("Проверка не реализована для элемента типа " + getCheckBoxType());
        }
        return result;
    }

    private void clickAction() {
        switch (getCheckBoxType()) {
            case DEFAULT:
            case MUI_SWITCH:
            case MUI:
            case MFE:
            case SBER_ENGAGE:
                ClickActions.safeClick("Чекбокс", getInput());
                break;
            case MFE_CHECKBOX:
            case MFE_FILTER:
                DriverUtils.executeJS("arguments[0].click()", getWrappedElement());
                break;
            case MFE_MODULE_INPUT:
            case STUDENT_MFE:
            case CHECKBOX_V5:
                DriverUtils.executeJS("arguments[0].click()", getInput());
                DriverUtils.freeze(DriverConstants.FREEZE_250_MS);
                break;
            default:
                throw new AutotestError("Click не реализована для элемента типа " + getCheckBoxType());

        }
    }

    public WebElement getValueElement() {
        return getSpecifyElement(
                getCheckBoxType().getValueXpath(),
                "Не удалось получить элемент с текущим значением поля"
        );
    }

    public WebElement getLabelElement() {
        return getSpecifyElement(
                getCheckBoxType().getLabelXpath(),
                "Не удалось получить элемент с текущим значением поля"
        );
    }

    @Override
    protected String getLabel() {
        final List<WebElement> elements = getParentIterator()
                .getCurrentParent()
                .findElements(By.xpath(getCheckBoxType().getLabelXpath()));
        return elements.isEmpty() ? "" : elements.get(0).getText();
    }

    CheckBoxType getCheckBoxType() {
        return (CheckBoxType) getElementType(CheckBoxType.values());
    }
}
