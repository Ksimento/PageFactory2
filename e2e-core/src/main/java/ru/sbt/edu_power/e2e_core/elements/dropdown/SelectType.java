package ru.sbt.edu_power.e2e_core.elements.dropdown;

import lombok.Getter;
import ru.sbt.edu_power.e2e_core.elements.ifaces.AutoIdentType;

/**
 * Enum всех разновидностей дропдаунов с перечислением всех значимых xpath для работы с ним.
 * Чтобы добавить новый тип дропдауна в зоопарк, нужно добавить сюда название и все xpath,
 * после чего в универсальном классе дописать требуемое поведение
 */
@Getter
public enum SelectType implements AutoIdentType {
    TEACHER_FILTER(
            "descendant-or-self::*[@data-icon-name='ic-arrow-drop-down']",
            "//div[not(contains(@class,'Popup')) and not(contains(@class,'_Menu')) and (contains(@data-testid, '.Dropdown.') or contains(@data-testid, '.Menu'))]",
            ".//button",
            null,
            null,
            "//*[@data-icon-name='ic-arrow-drop-up']",
            "descendant-or-self::button[contains(@class, 'MuiButtonBase-root') and (contains(@data-testid, '.Dropdown.'))]",
            "descendant-or-self::button",
            null,
            "descendant-or-self::button"
    ),
    MULTI_SELECT_V2( // страница добавления ролей пользователю, выбор предмета учителя
            "descendant-or-self::div[contains(@class, '_SearchIcon')]",
            "//div[contains(@id, 'react-select-')]/parent::div",
            "./div",
            "descendant-or-self::input",
            "descendant-or-self::div[contains(@class, '_MultiValueRemove')]/parent::div",
            "descendant-or-self::div[contains(@class, '_SearchIcon')]",
            "descendant-or-self::div[contains(@class, 'MultiSelectstyle_')]",
            null,
            ".//div[contains(@class, '_MultiValueRemove')]",
            "descendant-or-self::input"
    ),
    FILTER_TAG_SELECT(
            "descendant-or-self::div[@data-testid = 'dropdown']",
            "//div[@data-testid = 'dropdown-control']/following-sibling::div[1]",
            "descendant-or-self::div[contains(@data-testid, 'UIKit.Dropdown.Option.option')]",
            ".//input",
            "//div[contains(@class, '_ActiveFilter')]",
            "//div[@data-testid = 'dropdown-control']",
            "descendant-or-self::div[text() = 'Введите тег...' and ancestor::div[contains(@class, '_FiltersContainer')]]",
            null,
            ".//*[name() = 'svg']",
            "descendant-or-self::input"
    ),
    //    Школа 21, создание турнира, выбор коалиции
    COLORED_TAG_SELECT(
            ".//button[@title = 'Open']",
            ".//ul",
            "./li",
            ".//input",
            "//p[contains(@class, '_ItemCoalitions')]",
            ".//button[@title = 'Close']",
            ".//input[@placeholder = 'Добавить трайб' or @placeholder = 'Добавить коалицию']",
            ".//input",
            ".//*[name() = 'svg']",
            ".//input"
    ),
    TAG_SELECT(
            FILTER_TAG_SELECT.getOpenButtonXpath(),
            FILTER_TAG_SELECT.getDropdownBoxXpath(),
            FILTER_TAG_SELECT.getDropdownItemXpath(),
            FILTER_TAG_SELECT.getInputXpath(),
            "following-sibling::div[contains(@class, '_Tags')]//div[contains(@class, '_Tag-')]",
            FILTER_TAG_SELECT.getCloseButtonXpath(),
            "descendant-or-self::div[text() = 'Введите тег...']",
            null,
            FILTER_TAG_SELECT.getRemoveTagXpath(),
            FILTER_TAG_SELECT.getDisabledElement()
    ),
    COMBOBOX_V1(
            ".//*[name() = 'svg']",
            "//div[contains(@data-testid, 'UIKit.Dropdown.Option.option')]/parent::div",
            "./div",
            ".//input",
            null,
            ".//*[name() = 'svg']",
            "descendant-or-self::div[@data-testid = 'dropdown-control']",
            "descendant-or-self::div[@data-testid = 'dropdown-control']/div[1]",
            null,
            "descendant-or-self::input"
    ),
    // селекты на странице Конструктор модуля просвещения - вкладка Разметка
    COMBOBOX_MULTIPLE(
            ".//button[@title = 'Open']",
            ".//li[contains(@id,'option-0')]//ancestor-or-self::div[contains(@class,'-popper')]",
            ".//li",
            ".//input",
            ".//div[contains(@data-testid, 'UIKIT.ComboBox.Multiple.Tag.') " +
            "or contains(@data-testid, 'EditRolePopup.FormElement.subjects.Tag.')]",
            ".//button[@title = 'Close']",
            "descendant-or-self::div[@data-testid = 'UIKIT.ComboBox.Multiple']",
            ".//input",
            ".//*[name()='svg']",
            "descendant-or-self::input"
    ),// Селект для страницы Итоговые отметки с детализацией
    REPORTS_SELECT(
            ".//button[@title = 'Open']",
            ".//li[contains(@id,'option-0')]//ancestor-or-self::div[contains(@class,'-popper')]",
            ".//li",
            ".//input",
            null,
            ".//button[@title = 'Close']",
            ".//input[@placeholder='Выбрать период']",
            ".//input",
            null,
            "descendant-or-self::input"
    ),// Селект для страницы Закрытые модули по ученикам (URL /monitoring/done-modules) элемента Диапазон параллелей
    SELECT_RANGE(
            ".//button[@title = 'Open']",
            "//ancestor-or-self::div[contains(@class,'-popper')]",
            ".//div[contains(@class,'_Item')]//div[contains(@class,'_Item')]",
            ".//input",
            null,
            ".//button[@title = 'Close']",
            ".//input[contains(@placeholder,'Диапазон')]",
            ".//input",
            null,
            "descendant-or-self::input"
    ),
    AWARD_COMBO_BOX(
            ".//button[@title = 'Open']",
            ".//ul[contains(@class,'groupUl')]",
            "./li",
            ".//input",
            null,
            ".//button[@title = 'Close']",
            "descendant-or-self::div[contains(@class,'_AwardComboBox')]",
            ".//input",
            null,
            "descendant-or-self::input"
    ),
    MULTI_COMBOBOX_V2(//аудит школы и тип события
            ".//button[@title = 'Open']",
            ".//div[contains(@class,'Autocomplete-popper')]",
            ".//li",
            ".//input",
            ".//div[contains(@data-testid, 'UIKIT.ComboBox.Multiple.Tag.') " +
            "or contains(@data-testid, 'EditRolePopup.FormElement.subjects.Tag.')]",
            ".//button[@title = 'Close']",
            "descendant-or-self::div[@data-testid = 'UIKIT.ComboBox.Multiple-autocomplete' " +
            "or contains(@data-testid,'Dropdown-autocomplete')" +
            "or @data-testid = 'UIKIT.ComboBox.Multiple.Input'" +
            "or contains(@data-testid,'Tenant-autocomplete')]",
            ".//input",
            ".//button",
            "descendant-or-self::input"
    ),
    COMBOBOX_V2(
            ".//button[@title = 'Open']",
            ".//div[contains(@class,'Autocomplete-popper')]",
            ".//li[@role='option']",
            ".//input",
            null,
            ".//button[@title = 'Close']",
            "descendant-or-self::div[@data-testid = 'UIKIT.ComboBox.Single-autocomplete' or @data-testid='stageGroup.Input' or @id = 'UIKIT.ComboBox.Single' or @data-testid = 'UIKIT.ComboBox.Single' or @role = 'combobox']",
            ".//input",
            null,
            "descendant-or-self::input"
    ),
    DISABLED_SELECT(
            null,
            null,
            null,
            null,
            null,
            null,
            "descendant-or-self::input[@disabled]",
            "descendant-or-self::input[@disabled]",
            null,
            "descendant-or-self::input"
    ),//Селект на странице добавления модулей в план под КС и Учителем
    SELECT_V3(
            "descendant-or-self::button",
            "//div[@data-testid = 'UIKit.Menu' or contains(@data-testid,'FilterMenu')]",
            ".//button",
            null,
            null,
            "//div[@id = 'simple-popover']",
            "descendant-or-self::button[contains(@data-testid,'Filter')]",
            "descendant-or-self::button",
            null,
            "descendant-or-self::button"
    ),
    SELECT_S21(
            ".//*[name()='svg' and contains(@class,'MuiSelect-icon') and not(contains(@class,'iconOpen'))]",
            ".//ul",
            "./li",
            ".//input",
            null,
            ".//*[name()='svg' and contains(@class,'iconOpen')]",
            ".//*[name()='svg' and @data-testid='ArrowDropDownIcon']",
            ".",
            null,
            "descendant-or-self::input"
    ),
    S21_TAGS_DROPDOWN(
            ".//button[@title = 'Open']",
            ".//ul",
            "./li",
            ".//input",
            "//div[contains(@data-testid, 'S21.TagsDropdown.Tag.')]",
            ".//button[@title = 'Close']",
            "ancestor-or-self::div[@id = 'S21.TagsDropdown']",
            ".//div[contains(@class, 'selectMenu')]",
            null,
            "descendant-or-self::input"
    ),
    SELECT_FILTER_MFE(
            ".//*[@aria-expanded='false']",
            "//ul[contains(@data-testid,'DropdownMenu.List')]",
            "//li[contains(@data-testid,'DropdownMenu.List')]",
            null,
            "ancestor-or-self::div[@data-testid='UIKit.Card']//span[contains(@class,'chip-module')]",
            ".//*[@aria-expanded='true']",
            "descendant-or-self::div[@data-testid = 'UIKit.DropdownMenu']",
            "ancestor-or-self::div[@data-testid='UIKit.Card']//span[contains(@class,'chip-module')]",
            "ancestor-or-self::div[@data-testid='UIKit.Card']//span[contains(@class,'chip-module')]//button[contains(@class,'chip-module__close')]",
            "."
    ),
    //@EDU-T24841,@EDU-T35707,@EDU-T34363,@EDU-T35692,@EDU-T28572
    SELECT_MFE(
            ".//*[contains(@data-icon-name, 'caret-down') or contains(@data-icon-name, '-chevron-down') or @data-icon-name = 'system-dropdown-item-down']",
            ".//ul[@data-analytics='UIKit.Dropdown.List'] |.//div[contains(@id,'tippy-')]",
            ".//li | .//button[contains(@class,'justify-between') or contains(@data-testid,'Dropdown.Item')]",
            null,
            null,
            ".//*[contains(@data-icon-name, 'caret-up')] | .//*[contains(@data-icon-name, 'caret-down') or contains(@data-icon-name, '-chevron-up') or @data-icon-name = 'system-dropdown-item-up']",
            "descendant-or-self::div[@data-testid = 'UIKit.Dropdown' or @data-analytics = 'UIKit.Dropdown'] | ancestor-or-self::div[@data-testid = 'CLSKit.Dropdown.OpenNode' or @data-analytics = 'UIKit.Dropable'] |.//p[@data-testid = 'UIKit.RedactedText']",
            "descendant-or-self::button",
            null,
            "descendant-or-self::button"
    ),
    SELECT_MFE_COMBOBOX(
            ".//*[@data-icon-name = 'caret-down-solid']",
            ".//div[@class = 'combobox__menu-wrapper']",
            ".//div[contains(@class,'combobox__option')]",
            ".//input[@type= 'text']",
            ".//div[contains(@class, 'multiValue combobox')]",
            ".//*[name()='svg' and @class = 'transform rotate-180']",
            "descendant-or-self::div[@data-analytics = 'UIKit.ComboBox' and not(contains(@data-testid,'LessonCurtain.LessonForm'))]",
            "descendant-or-self::div[contains(@class, 'combobox__value-container--is-multi')] | descendant-or-self::div[contains(@class, 'combobox__value-container cs')] | descendant-or-self::div[contains(@class, 'combobox__single-value cs')]",
            ".//div[contains(@class, 'multi-value__remove')]",
            ".//input[@type= 'text']"
    ),
    // @EDU-T27791 Селекты MFE на странице создания Нового урока в расписании под конфигуратором системы и на старице Сообщения
    SELECT_MFE_BA(
            ".//*[@data-icon-name = 'caret-down-solid']",
            ".//div[@class = 'combobox__menu-wrapper'] | .//div[@class='tippy-box']",
            ".//div[contains(@class,'combobox__option')] | .//li",
            ".//input",
            ".//div[contains(@class, 'multiValue combobox')]",
            ".//*[@data-icon-name = 'caret-up-solid'] | .//*[name()='svg' and @class='transform rotate-180' and @data-icon-name = 'caret-down-solid']",
            "descendant-or-self::div[@data-analytics = 'UIKit.ComboBox'] | descendant-or-self::button[@data-testid = 'class-filter']",
            "descendant-or-self::div[contains(@class, '-placeholder') or contains(@class,'-singleValue')] | descendant-or-self::span[contains(@class,'place-items-center')]//span",
            ".//div[contains(@class, 'multi-value__remove')]",
            "descendant-or-self::input | descendant-or-self::button"
    ),
    S21_COMBOBOX(
            "descendant-or-self::div[contains(@class,'MuiAutocomplete-inputRoot')]",
            ".//div[contains(@class,'Autocomplete-popper')]",
            ".//li[@role='option']",
            ".//input",
            null,
            "descendant-or-self::div[contains(@class,'MuiAutocomplete-inputRoot')]",
            "descendant-or-self::div[contains(@class,'MuiAutocomplete-inputRoot')]",
            ".//input",
            null,
            "descendant-or-self::input"
    ),
    //@EDU-T35692
    SELECT_V5(
            ".//*[contains(@data-icon-name, '-chevron-down')]",
            ".//div[contains(@id,'tippy-')]",
            ".//span[contains(@class,'label')]//span",
            null,
            null,
            ".//*[contains(@data-icon-name, '-chevron-up')]",
            "descendant-or-self::button[contains(@data-testid , 'Dropdown.OpenNode.Button')]",
            "descendant-or-self::button",
            null,
            "descendant-or-self::button"
    ),
    // @EDU-T39054, @EDU-T35417
    SELECT_V5_STATISTIC(
            ".//*[contains(@data-icon-name, '-chevron-down')]",
            ".//div[contains(@id,'-listbox')]",
            ".//div[contains(@id,'-option-')]",
            null,
            null,
            ".//*[contains(@data-icon-name, '-chevron-down')]",
            "descendant-or-self::div[contains(@id , 'react-select-')] | descendant-or-self::span[contains(@id , 'react-select-')]",
            ".",
            null,
            "descendant-or-self::input"
    ),
    // @EDU-T29210
    SELECT(
            "descendant-or-self::button",
            "//div[@data-testid = 'UIKit.Menu' or contains(@data-testid, 'Dropdown.Menu')]",
            ".//button",
            null,
            null,
            "//div[@id = 'simple-popover']",
            "descendant-or-self::button[contains(@class, 'MuiButtonBase-root') " +
            "or contains(@data-testid,'Dropdown')]",
            "descendant-or-self::button",
            null,
            "descendant-or-self::button"
    );

    private final String openButtonXpath;
    private final String dropdownBoxXpath;
    private final String dropdownItemXpath;
    private final String inputXpath;
    private final String tagXpath;
    private final String closeButtonXpath;
    private final String identificationXpath;
    private final String valueXpath;
    private final String removeTagXpath;
    private final String disabledElement;

    SelectType(
            final String openButtonXpath,
            final String dropdownBoxXpath,
            final String dropdownItemXpath,
            final String inputXpath,
            final String tagXpath,
            final String closeButtonXpath,
            final String identificationXpath,
            final String valueXpath,
            final String removeTagXpath,
            final String disabledElement
    ) {
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
    }
}
