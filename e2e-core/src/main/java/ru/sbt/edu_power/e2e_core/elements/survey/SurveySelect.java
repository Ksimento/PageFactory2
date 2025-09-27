package ru.sbt.edu_power.e2e_core.elements.survey;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.external_services.validator.Validator;
import ru.sbt.edu_power.e2e_core.elements.FieldFillingException;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Available;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Fillable;
import ru.sbt.edu_power.external_services.validator.Validatable;
import ru.sbt.edu_power.e2e_core.actions.ClickActions;
import ru.yandex.qatools.htmlelements.element.Select;

import java.util.List;
import java.util.stream.Collectors;

public class SurveySelect extends Select implements
        SurveyJSField,
        Fillable,
        Validatable,
        ElementPartValidatable,
        Available {
    public SurveySelect(final WebElement wrappedElement) {
        super(wrappedElement);
    }

    private SurveySelect getSelect() {
        return new SurveySelect(getWrappedElement().findElement(By.xpath(".//button[contains(@data-testid, 'dropdown') or " +
                                                                         "contains(@data-testid, 'combobox.trigger')]")));
    }

    private List<WebElement> getValueSelect(){
   return getWrappedElement().findElements(By.xpath(".//button[contains(@data-testid, 'UIKit.Menu')]"));
    }

    private void setValue(final String valueDraft) throws FieldFillingException {
        final String value = DataProcessing.decodeValue(valueDraft);
        final SurveySelect select = getSelect();
        ClickActions.safeClick("Селест",select);
        final List<WebElement> options = select.getValueSelect();
        if (options.stream().anyMatch(e -> e.getAttribute("value").equals(value))) {
            select.selectByValue(value);
        } else if (options.stream().anyMatch(e -> e.getText().equals(value))) {
            final List<String> valueTextList = options.stream().map(WebElement::getText).collect(Collectors.toList());
            select.selectByIndex(valueTextList.indexOf(value));
        } else {
            final Integer optionNumber = DriverUtils.getNumber(value);
            if (optionNumber != null) {
                ClickActions.safeClick("Номер опции",
                        getWrappedElement().findElement(By.xpath("//button[@data-testid = " +
                                                                 "'UIKit.Menu.Item-"+optionNumber+"']")));
            } else {
                throw new FieldFillingException(String.format(
                        "Значения \"%s\" нет в списке выбора. Доступные значения:\n\"%s\"",
                        value,
                        select.getOptions().stream().map(WebElement::getText).collect(Collectors.joining("\n"))
                )
                );
            }
        }
    }

    @Override
    public void fillField(final String value, final Boolean validated) throws FieldFillingException {
        setValue(value);
    }

    @Override
    public String getFieldValue() {
        final SurveySelect select = getSelect();
        return isAvailable() ? select.getFirstSelectedOption().getText() : select.getWrappedElement().getText();
    }

    @Override
    public boolean validate(final String expected) {
        return Validator.matchValues(getFieldValue(), expected);
    }

    @Override
    public boolean partialValidate(final String elementPartId, final String expected) {
        Assert.assertTrue("Поле не доступно для редактирования, нельзя узнать варианты ответов", isAvailable());
        return Validator.matchValues(getPartContent(elementPartId), DataProcessing.decodeValue(expected));
    }

    @Override
    public String getPartContent(final String elementPartId) {
        final SurveySelect select = getSelect();
        final List<WebElement> options = select.getOptions();
        final int optionNumber = getElementNumber(elementPartId);
        Assert.assertTrue(
                String.format(
                        "Запрошен номер элемента \"%d\", всего элементов \"%d\"",
                        optionNumber, options.size()
                ),
                optionNumber <= options.size()
        );
        return options.get(optionNumber).getText();
    }

    @Override
    public boolean isAvailable() {
        return getWrappedElement().findElements(By.xpath("descendant-or-self::*[@disabled]")).isEmpty();
    }
}
