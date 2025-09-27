package ru.sbt.sber_learning.elements.text_input;

import lombok.Getter;
import ru.sbt.edu_power.e2e_core.elements.ifaces.AutoIdentType;
@Getter
public enum InputTypeAdaptive implements AutoIdentType {
    TEXT_INPUT("descendant-or-self::input[(@type = 'text') and not(ancestor-or-self::div[contains(@class, '_AttemptsNumber')])]", "descendant-or-self::input[@type = 'text']", ".//p[contains(@class, 'Text-root Mui-error')]", "descendant-or-self::label"),
    NUMBER_INPUT("descendant-or-self::input[@type = 'number']", "descendant-or-self::input[@type = 'number']", TEXT_INPUT.warningXpath, TEXT_INPUT.labelXpath),
    TEXTAREA("descendant-or-self::textarea", "descendant-or-self::textarea[1]", TEXT_INPUT.warningXpath, TEXT_INPUT.labelXpath);

    // идентификатор по которому определяется тип элемента
    private final String identificationXpath;
    // Поле заполнения
    private final String inputXpath;
    // предупреждение при заполнении поля
    private final String warningXpath;
    private final String labelXpath;

     InputTypeAdaptive(final String identificationXpath, final String inputXpath, final String warningXpath, final String labelXpath) {
        this.identificationXpath = identificationXpath;
        this.inputXpath = inputXpath;
        this.warningXpath = warningXpath;
        this.labelXpath = labelXpath;
    }


    @Override
    public String getValueXpath() {
        return this.inputXpath;
    }
}
