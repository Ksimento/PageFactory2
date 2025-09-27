package ru.sbt.edu_power.e2e_core.elements.checkbox;

import lombok.Getter;
import ru.sbt.edu_power.e2e_core.elements.ifaces.AutoIdentType;

@Getter
public enum CheckBoxType implements AutoIdentType {

    MUI(
            "descendant-or-self::span[contains(@class, 'MuiCheckbox')]",
            "descendant-or-self::span[contains(@class,'MuiIconButton-root')]",
            "descendant-or-self::span[contains(@class, 'MuiCheckbox')]",
            ".//input",
            "ancestor-or-self::label"
    ),
    MUI_SWITCH(
            "descendant-or-self::span[contains(@class, 'MuiSwitch-switchBase')]",
            ".//input",
            "descendant-or-self::span[contains(@class, 'MuiSwitch-switchBase')]",
            ".//input",
            "ancestor-or-self::label"
    ),
    // чекбокс для SberEngage https://v2.crm.sbc.space
    SBER_ENGAGE(
            "descendant-or-self::custom-checkbox[contains(@class, 'ng-pristine ng-untouched')]",
            ".//input",
            ".//input",
            ".//input",
            "ancestor-or-self::label"
    ),
    // на странице /calendar кейс @EDU-T34359
    MFE_FILTER(
            "descendant-or-self::label[@data-testid ='calendar.filter.calendar.Label']",
            ".//input",
            "descendant-or-self::label[@data-testid ='calendar.filter.calendar.Label']",
            ".//input",
            ".//label"
    ),
    MFE(
            ".//label[contains(@data-testid,'Checkbox') and contains(@class,'group') and not(contains(@class, 'checkbox-module'))]//ancestor-or-self::div[contains(@class,'grid-row')] " +
            "| .//input[@data-testid='UIKit.Checkbox.Label.Input' or @data-analytics='UIKit.Checkbox.Label.Input' and not(contains(@class, 'checkbox-module'))]",
            ".//input",
            ".//input",
            ".//input",
            "descendant-or-self::label"
    ),
    STUDENT_MFE(
            "descendant-or-self::button[contains(@class,'checkbox font-semibold')]",
            "descendant-or-self::button[contains(@class,'checkbox font-semibold')]",
            ".//span",
            ".//span",
            "descendant-or-self::button[contains(@class,'checkbox font-semibold')]"
    ),
    // чек бокс на странице /schedule/timetable-draft
    MFE_MODULE_INPUT("descendant-or-self::label[contains(@class,'module__checkbox')]",
            ".",
            ".//input",
            ".//input",
            "descendant-or-self::label"
    ),
    // чек бокс на странице /administration/calendar
    MFE_CHECKBOX("descendant-or-self::input[contains(@name,'selectedDays')]",
            ".//input",
            ".",
            ".//input",
            "."
    ),
    // @EDU-T37592
    CHECKBOX_V5("descendant-or-self::*[contains(@class,'switch-input-container')]",
            "descendant-or-self::*[contains(@class,'switch-input-container')]",
            "descendant-or-self::*[contains(@class,'switch-input-container')]",
            "descendant-or-self::*[contains(@class,'switch-input-container')]",
            "descendant-or-self::*[contains(@class,'switch-input-container')]"
    ),
    DEFAULT(
            "descendant-or-self::div[@data-testid = 'UIKit.Checkbox.checkbox-container']",
            "descendant-or-self::div[@data-testid = 'UIKit.Checkbox.checkbox-container']",
            ".//*[name() = 'svg']",
            ".//input",
            ".//label"
    );

    private final String identificationXpath;
    private final String inputXpath;
    private final String valueXpath;
    private final String disabledAttributeXpath;
    private final String labelXpath;

    CheckBoxType(
            final String identificationXpath,
            final String inputXpath,
            final String valueXpath,
            final String disabledAttributeXpath,
            final String labelXpath
    ) {
        this.identificationXpath = identificationXpath;
        this.inputXpath = inputXpath;
        this.valueXpath = valueXpath;
        this.disabledAttributeXpath = disabledAttributeXpath;
        this.labelXpath = labelXpath;
    }
}
