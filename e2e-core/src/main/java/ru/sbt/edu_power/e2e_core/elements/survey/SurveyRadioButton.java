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

import java.util.List;
import java.util.function.BooleanSupplier;

public class SurveyRadioButton extends TypifiedElement
        implements SurveyJSField, Fillable, Validatable, ElementPartValidatable
{
    public SurveyRadioButton(final WebElement wrappedElement) {
        super(wrappedElement);
    }

    @Override
    public void fillField(final String draftValue, final Boolean validated) throws FieldFillingException {
        final String value = DataProcessing.decodeValue(draftValue);

        final WebElement element = getRadioElement(value);

        final BooleanSupplier waitWhenValueBeSet = () -> isChecked(element);

        SurveyActions.clickExecutor(value, element, waitWhenValueBeSet);
    }

    private WebElement getRadioElement(final String value) {
        final List<WebElement> elements = getWrappedElement()
                .findElements(By.xpath(getRadioElementXpath(value)));
        if (!elements.isEmpty() && !isKatexRadioElement(elements.get(0))) {
            return elements.get(0);
        }
        elements.clear();
        final Integer elementNumber = DriverUtils.getNumber(value);
        if (elementNumber != null) {
            elements.add(getWrappedElement().findElements(By.xpath(getRadioElementXpath())).get(elementNumber - 1));
        } else {
            throw new AutotestError(String.format("Значение \"%s\" не найдено", value));
        }
        return elements.get(0);
    }

    private boolean isKatexRadioElement(final WebElement element) {
        return !element.findElements(By.xpath(".//span[contains(@class, 'katex')]")).isEmpty();
    }

    String getRadioElementXpath(final String value) {
        return ".//span[text()='"+value+"']//ancestor-or-self::div[contains(@class, '_ChooseOneFromManyAnswer')]";
    }

    String getRadioElementXpath() {
        return ".//div[contains(@class, '_ChooseOneFromManyAnswer')]";
    }

    private boolean isChecked(final WebElement element) {
        return "true".equals(element.getAttribute("data-checked"));
    }

    private MultipleTypeContent getMultipleTypeContentFromItemByIndex(final int index) {
        final List<WebElement> elements = getWrappedElement().findElements(By.xpath(getRadioElementXpath()));
        if (index > elements.size()) {
            throw new AutotestError(String.format(
                    "Попытка получить %s-й элемент в списке из %s элементов",
                    index,
                    elements.size()
            ));
        }
        return new MultipleTypeContent(elements.get(index - 1));
    }

    @Override
    public String getFieldValue() {
        return getText();
    }

    @Override
    public boolean validate(final String expected) {
        return Validator.matchValues(getText(), expected);
    }

    @Override
    public String getText() {
        final List<WebElement> elements = getWrappedElement()
                .findElements(By.xpath(
                        "descendant::div[contains(@class, 'checked')] | descendant::label[contains(@class, 'checked')]"));
        return elements.isEmpty() ? "" : getRadioText(elements.get(0));
    }

    private String getRadioText(final WebElement element) {
        return "div".equals(element.getTagName()) ? element.getText() : element.getAttribute("value");
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
}
