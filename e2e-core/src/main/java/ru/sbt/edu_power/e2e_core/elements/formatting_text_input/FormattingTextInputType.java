package ru.sbt.edu_power.e2e_core.elements.formatting_text_input;

import lombok.Getter;
import ru.sbt.edu_power.e2e_core.elements.ifaces.AutoIdentType;

@Getter
public enum FormattingTextInputType implements AutoIdentType {
    CKEDITOR_IFRAME(
            ".//iframe",
            ".",
            null,
            null
    ),
    CKEDITOR_INPUT(
            ".//div[contains(@class, 'ck-editor__editable')]",
            ".//p",
            null,
            null
    );

    private final String identificationXpath;
    private final String inputXpath;
    private final String warningXpath;
    private final String labelXpath;

    FormattingTextInputType(
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
