package ru.sbt.edu_power.e2e_core.elements.date_select;

import lombok.Getter;
import ru.sbt.edu_power.e2e_core.elements.ifaces.AutoIdentType;

@Getter
public enum DateSelectType implements AutoIdentType {
    DATE_SELECT(
            "//*[@data-icon-name='ic-calendar']",
            ".//*[@data-icon-name='ic-calendar']",
            null,
            null,
            null,
            "//div[@class = 'DayPicker-Months']//button[2]",
            "//div[@class = 'DayPicker-Months']//button[1]",
            "//div[contains(@class, 'HelperText')]"
    ),
    // administration/calendar/
    MONTH_CALENDAR_MFE(
            "descendant-or-self::div[contains(@class,'month-calendar-block')]",
            ".//div[contains(@class,'month-selector')]",
            null,
            null,
            "//div[@data-analytics='UIKit.Calendar']//div[contains(@class,'justify-betwee')]//span",
            null,
            null,
            null
    ),
    DATE_SELECT_PLANS(
            "//div[contains(@class, 'ModuleDateWrapper')] | descendant-or-self::div[@data-testid='form-date-picker']",
            ".//button[contains(@data-testid,'.UIKIT.Switcher.ContentValue')] | descendant-or-self::button[contains(@data-testid,'calendarPicker.Navigation.Button')] | descendant-or-self::div[@data-analytics='UIKit.InputDatePicker'] | descendant-or-self::button[contains(@data-testid,'CalendarForChangeStartDate')]",
            null,
            null,
            "./div[1]//p | //*[contains(@data-testid,'calendarPicker.Title')] | //span[contains(@class,'capitalize')]",
            null,
            null,
            "//div[contains(@class, 'HelperText')] | //span[contains(@class, 'text-negative')]"
    ),
    //@EDU-T34441,@EDU-T34353
    STUDENT_DATE_MFE(
            "descendant-or-self::div[contains(@data-testid, 'dropDownMenu.Open')]",
            "descendant-or-self::button[contains(@data-testid, 'header.button.dropDownFilter')]",
            null,
            null,
            ".//p[@data-testid='calendar.filter.month']",
            null,
            null,
            "//div[contains(@class, 'HelperText')] | //span[contains(@class, 'text-negative')]"
    ),
    //кейс @EDU-T3393 url до элемента /my-school/periods
    DOUBLE_DATE_SELECT(
            "descendant-or-self::button[@data-testid ='UIKIT.Switcher.ContentValue']",
            "descendant-or-self::button[@data-testid ='UIKIT.Switcher.ContentValue']",
            null,
            null,
            "./div[1]//p",
            null,
            null,
            "//div[contains(@class, 'HelperText')]"
    ),
    //кейс @EDU-T29177 url до элемента /progress-electronic-journal?subjectId=15&studyPeriodId=1463
    DOUBLE_DATE_SELECT_JOURNAL(
            "descendant-or-self::div[@data-testid='Filter.By.Dates']",
            "descendant-or-self::div[@data-testid='Filter.By.Dates']",
            null,
            null,
            "//h3[@data-testid='UIKit.Calendar.Title']",
            null,
            null,
            "//div[contains(@class, 'HelperText')]"
    ),
    DOUBLE_DATE_SELECT_PERIOD(
            "ancestor-or-self::div[contains(@class,'StudyPeriodColumn')]",
            "ancestor-or-self::div[contains(@class,'StudyPeriodColumn')]",
            null,
            null,
            "./div[1]//p",
            null,
            null,
            "//div[contains(@class, 'HelperText')]"
    ),
    DATE_SELECT_MFE_PANS(
            "descendant-or-self::button[contains(@data-testid,'button-to-calendar')]",
            "descendant-or-self::button[contains(@data-testid,'button-to-calendar')]",
            null,
            null,
            ".//h3[contains(@data-testid,'Date-part:calendar') or contains(@data-testid,'end-date-picker-calendar')]",
            null,
            null,
            null
    ),
    // календарь на странице кейс @EDU-T32223 /dashboard
    DATE_SELECT_DASHBOARD_MFE(
            "descendant-or-self::*[name()='svg' and contains(@data-icon-name,'calendar') and contains(@data-testid,'primal')]",
            "descendant-or-self::*[name()='svg' and contains(@data-icon-name,'calendar') and contains(@data-testid,'primal')]",
            null,
            null,
            "//*[contains(@data-testid,'Picker.Calendar') and contains(@class,'title')] ",
            null,
            null,
            "//div[contains(@class, 'HelperText')] | //span[contains(@class, 'text-negative')]"
    ),
    // @EDU-T23547
    DATE_MFE_FILTER(
            "descendant-or-self::*[contains(@data-testid,'BirthDate.Filter')]",
            "descendant-or-self::*[contains(@data-testid,'BirthDate.Filter')]//*[name()='svg']",
            null,
            null,
            ".//h3[contains(@data-testid,'BirthDate.Tooltip')]",
            null,
            null,
            null
    ),
    // @EDU-T29990 , @EDU-T29212, @EDU-T34415
    DATE_SELECT_MFE(
            "descendant-or-self::button[contains(@data-testid,'calendarPicker.Navigation.Button')] | descendant-or-self::div[@data-analytics='UIKit.InputDatePicker']",
            "descendant-or-self::button[contains(@data-testid,'calendarPicker.Navigation.Button')] | descendant-or-self::div[@data-analytics='UIKit.InputDatePicker']",
            null,
            null,
            "//*[contains(@data-testid,'calendarPicker.Title')] | //span[contains(@class,'capitalize')]",
            null,
            null,
            "//div[contains(@class, 'HelperText')] | //span[contains(@class, 'text-negative')]"
    );

    private final String identificationXpath;
    private final String datePickerButton;
    private final String inputXpath;
    private final String valueXpath;
    private final String currentPeriod;
    private final String yearSelect;
    private final String monthSelect;

    private final String warningXpath;

    DateSelectType(
            final String identificationXpath,
            final String datePickerButton,
            final String inputXpath,
            final String valueXpath,
            final String currentPeriod,
            final String yearSelect,
            final String monthSelect,
            final String warningXpath

    ) {
        this.identificationXpath = identificationXpath;
        this.datePickerButton = datePickerButton;
        this.inputXpath = inputXpath;
        this.valueXpath = valueXpath;
        this.currentPeriod = currentPeriod;
        this.yearSelect = yearSelect;
        this.monthSelect = monthSelect;
        this.warningXpath = warningXpath;
    }

    public String getDatePickerButton() {
        return datePickerButton;
    }

}