package ru.sbt.sber_learning.elements.dropdown;

import lombok.Getter;
import ru.sbt.edu_power.e2e_core.elements.ifaces.AutoIdentType;

@Getter
public enum SelectTypeAdaptive implements AutoIdentType {
    SBERCLEVER_COMBOBOX(".//button[@title = 'Открыть']", ".//ul[@role='listbox']", ".//li[@role='option']", ".//input", null, ".//button[@title = 'Закрыть']", "descendant-or-self::div[@data-testid = 'combobox']", ".//input", "descendant-or-self::button[@title = 'Очистить']","descendant-or-self::input","descendant::p[contains(@class, 'Text-root Mui-error')]"),
    SBERCLEVER_COMBOBOX_MULTI( ".//button[@title = 'Открыть']", ".//ul[@role='listbox']",".//li[@role='option']",".//input",".//div[contains(@class, 'MuiAutocomplete-tag')]", ".//button[@title = 'Закрыть']","descendant-or-self::div[@data-testid = 'combobox-multiply']",".//input", "descendant-or-self::*[@data-testid = 'CancelIcon']","descendant-or-self::input",".//p[contains(@class, 'Text-root Mui-error')]"),
    SBERCLEVER_SELECT(".//*[@data-testid='ArrowDropDownIcon']",".//ul[@role='listbox']",".//li[@role='option']",".//input", null,".//*[@data-testid='ArrowDropDownIcon']","descendant-or-self::div[@data-testid = 'select']",".//div[@data-testid='select']",null,"descendant-or-self::input", "descendant::p[contains(@class, 'Text-root Mui-error')]");
    // Кнопка открытия дропдауна
    private final String openButtonXpath;
    // Окно элементов выпадающего списка
    private final String dropdownBoxXpath;
    // Элементы выпадающего списка
    private final String dropdownItemXpath;
    // Поле селекта
    private final String inputXpath;
    // список тегов
    private final String tagXpath;
    // кнопка закрытия дропдауна
    private final String closeButtonXpath;
    // идентификатор по которому определяется тип элемента
    private final String identificationXpath;
    // значения
    private final String valueXpath;
    // иконка удаления тега
    private final String removeTagXpath;
    // путь до атрибута доступности элемента
    private final String disabledElement;
    // предупреждение при заполнении поля
    private final String warningMassage;

     SelectTypeAdaptive(final String openButtonXpath, final String dropdownBoxXpath, final String dropdownItemXpath, final String inputXpath, final String tagXpath, final String closeButtonXpath, final String identificationXpath, final String valueXpath, final String removeTagXpath, final String disabledElement, final String warningMassage) {
        this.openButtonXpath = openButtonXpath;
        this.dropdownBoxXpath = dropdownBoxXpath;
        this.dropdownItemXpath = dropdownItemXpath;
        this.inputXpath = inputXpath;
        this.tagXpath = tagXpath;
        this.closeButtonXpath = closeButtonXpath;
        this.identificationXpath = identificationXpath;
        this.valueXpath = valueXpath;
        this.removeTagXpath = removeTagXpath;
        this.disabledElement = disabledElement;
        this.warningMassage = warningMassage;
    }


    public String getOpenButtonXpath() {
        return this.openButtonXpath;
    }
    public String getWarningMassage() {
        return this.warningMassage;
    }

    public String getDropdownBoxXpath() {
        return this.dropdownBoxXpath;
    }

    public String getDropdownItemXpath() {
        return this.dropdownItemXpath;
    }

    @Override
    public String getInputXpath() {
        return this.inputXpath;
    }

    public String getTagXpath() {
        return this.tagXpath;
    }

    public String getCloseButtonXpath() {
        return this.closeButtonXpath;
    }

    @Override
    public String getIdentificationXpath() {
        return this.identificationXpath;
    }

    @Override
    public String getValueXpath() {
        return this.valueXpath;
    }

    public String getRemoveTagXpath() {
        return this.removeTagXpath;
    }

    public String getDisabledElement() {
        return this.disabledElement;
    }
}
