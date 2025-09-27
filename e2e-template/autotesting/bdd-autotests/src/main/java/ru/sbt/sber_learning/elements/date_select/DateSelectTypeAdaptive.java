package ru.sbt.sber_learning.elements.date_select;

import lombok.Getter;
import ru.sbt.edu_power.e2e_core.elements.ifaces.AutoIdentType;

@Getter
public enum DateSelectTypeAdaptive implements AutoIdentType {
    DATE_SELECT(
            ".//*[@data-testid='CalendarIcon']",
            "//div[contains(@class,'CalendarPicker-root')]",
            ".//*[@data-testid='CalendarIcon']",
            ".//input",
            ".//input",
            "//div[@role='presentation']",
            null,
            null,
            "//button[@aria-label='Next month']",
            "//button[@aria-label='Previous month']",
            ".//div[@role='cell']",
            ".//p[contains(@class, 'Text-root Mui-error')]"

    );

    // идентификатор по которому определяется тип элемента
    private final String identificationXpath;
    private final String datePicker;
    private final String datePickerButton;
    // Поле заполнения если имеется
    private final String inputXpath;
    // Поле со значением
    private final String valueXpath;
    private final String currentPeriod;
    private final String yearSelect;
    private final String monthSelect;
    private final String nextButton;
    private final String backButton;
    private final String pickersDay;
    private final String warningXpath;

    DateSelectTypeAdaptive(
            final String identificationXpath,
            final String datePicker,
            final String datePickerButton,
            final String inputXpath,
            final String valueXpath,
            final String currentPeriod,
            final String yearSelect,
            final String monthSelect,
            final String nextButton,
            final String backButton,
            final String pickersDay,
            final String warningXpath

    ) {
        this.identificationXpath = identificationXpath;
        this.datePicker = datePicker;
        this.datePickerButton = datePickerButton;
        this.inputXpath = inputXpath;
        this.valueXpath = valueXpath;
        this.currentPeriod = currentPeriod;
        this.yearSelect = yearSelect;
        this.monthSelect = monthSelect;
        this.backButton = backButton;
        this.nextButton = nextButton;
        this.pickersDay = pickersDay;
        this.warningXpath = warningXpath;
    }

    public String getDatePickerButton() {
        return datePickerButton;
    }

}
