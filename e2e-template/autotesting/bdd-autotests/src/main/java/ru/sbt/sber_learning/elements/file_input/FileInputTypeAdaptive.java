package ru.sbt.sber_learning.elements.file_input;

import lombok.Getter;
import ru.sbt.edu_power.e2e_core.elements.ifaces.AutoIdentType;

@Getter
public enum FileInputTypeAdaptive implements AutoIdentType {
    FILE_INPUT(
            "descendant-or-self::input[@type = 'file']",
            "descendant-or-self::input[@type = 'file']",
            "ancestor-or-self::div[contains(@class, '_FileUploader')]//button",
            ".//p[contains(@class, 'Mui-error file-uploader-error')]"

    );

    // идентификатор по которому определяется тип элемента
    private final String identificationXpath;
    private final String inputXpath;
    private final String available;
    // Поле заполнения если имеется
    private final String warningXpath;
    // Поле со значением

    FileInputTypeAdaptive(
            final String identificationXpath,
            final String inputXpath,
            String available,
            final String warningXpath
    ) {
        this.identificationXpath = identificationXpath;
        this.inputXpath = inputXpath;
        this.available = available;
        this.warningXpath = warningXpath;
    }

    public String getAvailable() {
        return available;
    }

    @Override
    public String getValueXpath() {
        return inputXpath;
    }
}
