package ru.sbt.edu_power.e2e_core.blocks.abstract_blocks;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import ru.sbt.edu_power.e2e_core.elements.checkbox.CheckBox;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.e2e_core.elements.FieldFillingException;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Fillable;
import ru.sbtqa.tag.pagefactory.annotations.ElementTitle;
import ru.sbtqa.tag.qautils.errors.AutotestError;
import ru.yandex.qatools.htmlelements.element.TextBlock;
import ru.sbt.edu_power.e2e_core.blocks.BlockInitialized;

/**
 * Элемент строки таблице-подобной структуры с названием элемента и чекбоксом.
 * Используется для описания списочных блоков, в которых требуется
 * отмечать строки чекбоксами (например выбор модулей из списка)
 * Сам блок описывается на странице как список их этих элементов List<YourClassOfBlockElement>
 * YourClassOfBlockElement - должен быть наследован от этого класса
 */
public abstract class MarkableListItem extends BlockInitialized {
    protected MarkableListItem(final WebElement webElement) {
        super(webElement);
    }

    @ElementTitle("Название")
    @FindBy(xpath = ".//*[string-length(text()) > 4]")
    public TextBlock taskNameTextBlock;

    @ElementTitle("Чекбокс")
    @FindBy(xpath = ".//*[input[@type='checkbox']]")
    public CheckBox choiceTaskCheckBox;

    public void setValue(final String state) {
        try {
            ((Fillable) this.choiceTaskCheckBox).fillField(state, true);
        } catch (final FieldFillingException e) {
            throw new AutotestError("Не удалось заполнить значение списка " + taskNameTextBlock.getText(), e);
        }
    }

    public void markIt() {
        setValue("true");
    }

    public boolean validate(final String state) {
        return isChecked() == DriverUtils.getBoolean(state);
    }

    public boolean isChecked() {
        return this.choiceTaskCheckBox.getFieldState();
    }
}
