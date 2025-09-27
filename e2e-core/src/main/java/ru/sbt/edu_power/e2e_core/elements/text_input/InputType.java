package ru.sbt.edu_power.e2e_core.elements.text_input;

import lombok.Getter;
import ru.sbt.edu_power.e2e_core.elements.ifaces.AutoIdentType;

@Getter
public enum InputType implements AutoIdentType {
    FILTER_INPUT(
            "descendant-or-self::input[@placeholder ='Поиск...'][following-sibling::*[name()='svg']]",
            "descendant-or-self::input",
            null,
            null
    ),
    COMBOBOX_INPUT(
            "descendant-or-self::input[ancestor::div[contains(@data-testid, 'UIKIT.ComboBox.') or @class='combobox__input']]",
            "descendant-or-self::input",
            null,
            null
    ),
    PHONE_INPUT(
            "descendant-or-self::input[@name = 'phone' or contains(@value, '+7')]",
            "descendant-or-self::input",
            "descendant-or-self::div[contains(@class, 'error-msg')] | descendant-or-self::*[contains(@class,'text-negative')]",
            "descendant-or-self::label"
    ),
    NUMBER_INPUT(
            "descendant-or-self::input[@type = 'number'] | descendant-or-self::div[contains(@class,'InputNumber') or contains(@class,'_InputContainer') or contains(@data-testid,'fieldCount') or contains(@data-testid,'ClassPlanChangeInput')]",
            "descendant-or-self::input",
            "descendant-or-self::div[contains(@class, 'error-msg') or contains(@class, 'HelperText')] | descendant-or-self::p[contains(@class, 'ErrorBlockStyled') or contains(@class, 'Mui-error')or contains(@class, 'ErrorText') or contains(@class,'_ErrorLabel')] | descendant-or-self::*[contains(@class,'text-negative')]",
            "descendant-or-self::label"
    ),
    S21_DATE_INPUT(
            "descendant-or-self::input[contains(@aria-label,'Choose date') and @type = 'text' and @aria-readonly='true']",
            "descendant-or-self::input[@type = 'text' or @type = 'email' or @type = 'tel' or @type = 'password' or @type = 'url']",
            NUMBER_INPUT.warningXpath,
            NUMBER_INPUT.labelXpath
    ),
    TEXT_INPUT(
            "descendant-or-self::input[(@type = 'text' or @type = 'email' or @type = 'tel' or @type = 'password' or @type = 'url') " +
            "and not(ancestor-or-self::div[contains(@class, '_AttemptsNumber')])]",
            "descendant-or-self::input[@type = 'text' or @type = 'email' or @type = 'tel' or @type = 'password' or @type = 'url']",
            NUMBER_INPUT.warningXpath +" | descendant-or-self::*[contains(@class,'text_leftStated')]",
            NUMBER_INPUT.labelXpath
    ),
    TEXTAREA(
            "descendant-or-self::textarea",
            "descendant-or-self::textarea[1]",
            NUMBER_INPUT.warningXpath +" | descendant-or-self::*[contains(@class,'metaTextLeft')]",
            TEXT_INPUT.labelXpath
    ),
    NUMBER_ATTEMPT_INPUT(
            "descendant-or-self::div[contains(@class, '_AttemptsNumber')]",
            "descendant-or-self::input[@type = 'text' or @type = 'tel' or @type = 'password']",
            "descendant-or-self::div[contains(@class, 'error-msg')]",
            "descendant-or-self::label"
    ),
    SIMPLE_INPUT(
            "descendant-or-self::input[not(@type)]",
            "descendant-or-self::input",
            "descendant-or-self::div[contains(@class,'_Hint')]",
            null
    ),
    TEXT_INPUT_SURVEY(
            ".//div[contains(@class,'_TextFieldBox')]",
            ".",
            null,
            null
    );

    private final String identificationXpath;
    private final String inputXpath;
    private final String warningXpath;
    private final String labelXpath;

    InputType(
            final String identificationXpath,
            final String inputXpath,
            final String warningXpath,
            final String labelXpath
    ) {
        this.identificationXpath = identificationXpath;
        this.inputXpath = inputXpath;
        this.warningXpath = warningXpath;
        this.labelXpath = labelXpath;
    }

    @Override
    public String getValueXpath() {
        return inputXpath;
    }
}
