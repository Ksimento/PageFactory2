package ru.sbt.edu_power.e2e_core.elements.date_select;


import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import ru.sbt.edu_power.e2e_core.actions.ClickActions;
import ru.sbt.edu_power.e2e_core.actions.Mover;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.e2e_core.elements.AutoIdentElement;
import ru.sbt.edu_power.e2e_core.elements.FieldFillingException;
import ru.sbt.edu_power.e2e_core.elements.dropdown.Select;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Fillable;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Warning;
import ru.sbt.edu_power.e2e_core.elements.text_input.TextInput;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbt.edu_power.external_services.validator.Validatable;
import ru.sbtqa.tag.pagefactory.environment.Environment;
import ru.sbtqa.tag.pagefactory.utils.Wait;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

public class DateSelect extends AutoIdentElement implements Fillable, Validatable, Warning {
    private DateSelectType dateSelectType;
    private WebElement datePicker;
    private boolean hasPeriodSelector;
    private final Map<String, String> monthMap = new HashMap<>();

    private WebElement getDatePickerButton() {
        return getWrappedElement().findElement(By.xpath(getDateSelectType().getDatePickerButton()));
    }

    @Override
    protected WebElement getInput() {
        final String xpath = "descendant-or-self::input";
        final List<WebElement> elements = getWrappedElement().findElements(By.xpath(xpath));
        return !elements.isEmpty() ? elements.get(0) : null;
    }

    public DateSelect(final WebElement wrappedElement) {
        super(wrappedElement);
    }

    @Override
    public void fillField(final String ddmmyyyy, final Boolean validated) throws FieldFillingException {
        dateSelectType = getDateSelectType();
        final boolean notIsNowDate = ddmmyyyy.contains(".");
        if (null != getInput()) {
            fillFieldInput(ddmmyyyy, notIsNowDate);
        } else {
            fillFieldCalendar(ddmmyyyy, notIsNowDate, validated);
        }
    }

    public void selectiveFilling(final String ddmmyyyy, final String method, final boolean validated)
            throws FieldFillingException {
        dateSelectType = getDateSelectType();
        final boolean notIsNowDate = ddmmyyyy.contains(".");
        switch (method) {
            case "Поля":
                fillFieldInput(ddmmyyyy, notIsNowDate);
                break;
            case "Календаря":
                initMonthMap();
                fillFieldCalendar(ddmmyyyy, notIsNowDate, validated);
                break;
            default:
                throw new AutotestError(String.format(
                        "Способ \"%s\" заполнения не реализован",
                        dateSelectType.name()
                ));
        }
    }

    //Данные записываются в зависимости от того какого они типа(двойная дата или одинарная)
    private Map<String, String> getDateCalendar(final String date) {
        final Map<String, String> mapDate = new HashMap<>();
        if (date.contains("--")) {
            final String[] period = date.split("--");
            final String[] date1 = DataProcessing.decodeValue(period[0]).split("\\.");
            final String[] date2 = DataProcessing.decodeValue(period[1]).split("\\.");
            mapDate.put("day1", date1[0].replaceAll("^0", ""));
            mapDate.put("month1", getMonthByNumber(date1[1]));
            mapDate.put("year1", date1[2].trim());
            mapDate.put("day2", date2[0].trim().replaceAll("^0", ""));
            mapDate.put("month2", getMonthByNumber(date2[1]));
            mapDate.put("year2", date2[2]);
        } else {
            final String[] dateComponent = DataProcessing.decodeValue(date).split("\\.");
            if (dateComponent.length >= 2 && dateComponent.length <= 3) {
                mapDate.put("day", dateComponent[0].replaceAll("^0", ""));
                mapDate.put("month", getMonthByNumber(dateComponent[1]));
                if (dateComponent.length == 3) {
                    mapDate.put("year", dateComponent[2]);
                }
            } else {
                throw new AutotestError("Дату необходимо указывать через точки в виде дд.мм.гггг или дд.мм");
            }

        }
        Assert.assertNotEquals(
                "Дату необходимо указывать через точки в виде дд.мм.гггг," +
                " для выбора периода стоит указать дату форматом  дд.мм.гггг - дд.мм.гггг",
                mapDate.size(),
                0
        );
        return mapDate;
    }

    private void fillFieldCalendar(final String ddmmyyyy, final boolean notIsNowDate, final boolean validated)
            throws FieldFillingException {
        final WebElement datePickerButton = getDatePickerButton();
        Mover.scrollToElement(datePickerButton);
        ClickActions.safeClick("Кнопка date picker", datePickerButton);
        initialDatePicker();
        if (notIsNowDate) {
            final Map<String, String> dateCalendar = getDateCalendar(ddmmyyyy.replace("<-", "").replace("->", ""));
            final String month = dateCalendar.get("month");
            final String year = dateCalendar.get("year");
            final String day = dateCalendar.get("day");
            switch (dateSelectType) {
                case DATE_SELECT:  // Выбираем месяц
                    fillDateSelect(month, year);
                    clickDayPoint(day);
                    break;
                case DATE_SELECT_PLANS:
                case DATE_SELECT_MFE_PANS:
                case DATE_MFE_FILTER:
                    moveToPeriod(month, year);
                    clickDayPoint(day);
                    break;
                case STUDENT_DATE_MFE:
                    moveToPeriod(month, year);
                    clickDayPoint(day);
                    ClickActions.safeClick("Кнопка date picker", datePickerButton);
                    break;
                case DOUBLE_DATE_SELECT_PERIOD:
                    final BooleanSupplier waitDateFill = () -> {
                        clearDoubleDate();
                        fillDoubleDateSelect(dateCalendar);
                        Mover.getActions().sendKeys(Keys.ESCAPE).build().perform();
                        if (validated) {
                            return validatedDate(datePickerButton.getText().trim().toLowerCase(), dateCalendar);
                        }
                        return true;
                    };
                    Timer.executeTimerThrowable(
                            DriverConstants.TIMEOUT,
                            String.format(
                                    "Дата не заполнена по истечению времени, дата в элементе \"%s\"",
                                    datePickerButton.getText()
                            ),
                            waitDateFill
                    );
                    break;
                case DOUBLE_DATE_SELECT_JOURNAL:
                case DOUBLE_DATE_SELECT:
                    fillDoubleDateSelect(dateCalendar);
                    Mover.getActions().sendKeys(Keys.ESCAPE).build().perform();
                    break;
                case DATE_SELECT_DASHBOARD_MFE:
                    fillDateSelectMfe(month, day, getButtonNavigation(ddmmyyyy));
                    break;
                case DATE_SELECT_MFE:
                    fillDateSelectMfe(month, day, getButtonNavigation(ddmmyyyy));
                    try {
                        if(datePicker.isDisplayed()) {
                            ClickActions.safeClick("Кнопка date picker", datePickerButton);
                        }
                    } catch (StaleElementReferenceException ignored) {
                    }
                    break;
                case MONTH_CALENDAR_MFE:
                    if (!getWrappedElement().getText().startsWith(month + " " + year)) {
                        moveToMoth(month, year);

                    }
                    ClickActions.safeClick("Кнопка date picker", datePickerButton);
                    ClickActions.safeClick(
                            "День",
                            DriverUtils
                                    .getWebDriver()
                                    .findElement(By.xpath("//button[contains(@class,'current-month')]//*[text()='" +
                                                          day +
                                                          "']"))
                    );
                    break;
                default:
                    throw new AutotestError(String.format(
                            "Для этого типа \"%s\" заполнение не реализовано",
                            dateSelectType.name()
                    ));
            }
        }
    }

    private boolean validatedDate(final String date, Map<String, String> dateCalendar) {
        final String abbreviatedMonth1 = dateCalendar.get("month1").equals("Май") ? "мая" : dateCalendar
                .get("month1")
                .substring(0, 3)
                .toLowerCase();
        final String abbreviatedMonth2 = dateCalendar.get("month2").equals("Май") ? "мая" : dateCalendar
                .get("month2")
                .substring(0, 3)
                .toLowerCase();
        if (!dateCalendar.get("year1").equals(dateCalendar.get("year2"))) {
            return date.equals(String.format(
                    "%s %s %s–%s %s %s",
                    dateCalendar.get("day1"),
                    abbreviatedMonth1,
                    dateCalendar.get("year1"),
                    dateCalendar.get("day2"),
                    abbreviatedMonth2,
                    dateCalendar.get("year2")
            ));
        } else if (abbreviatedMonth1.equals(abbreviatedMonth2)) {
            return date.equals(String.format(
                    "%s–%s %s %s",
                    dateCalendar.get("day1"),
                    dateCalendar.get("day2"),
                    abbreviatedMonth1,
                    dateCalendar.get("year1")
            ));
        } else {
            return date.equals(String.format(
                    "%s %s–%s %s %s",
                    dateCalendar.get("day1"),
                    abbreviatedMonth1,
                    dateCalendar.get("day2"),
                    abbreviatedMonth2,
                    dateCalendar.get("year2")
            ));
        }
    }

    private void fillFieldInput(final String ddmmyyyy, final boolean notIsNowDate) {
        final TextInput input = new TextInput(getInput());
        input.clear();
        if (notIsNowDate) {
            input.sendKeys(DataProcessing.decodeValue(ddmmyyyy));
        } else {
            final Date date = new Date();
            final DateFormat df = new SimpleDateFormat("dd.MM.yyyy");
            input.sendKeys(df.format(date));
        }
    }

    private void fillDateSelectMfe(final String moth, final String day, final WebElement buttonNavigation)
            throws FieldFillingException {
        moveToMonth(moth, buttonNavigation);
        clickDayPoint(day);
    }

    private WebElement getButtonNavigation(final String ddmmyyyy) {
        if (ddmmyyyy.contains("<-") || ddmmyyyy.contains("->")) {
            Assert.assertFalse(
                    "Укажите одну кнопку навигации <- или ->",
                    ddmmyyyy.contains("<-") && ddmmyyyy.contains("->")
            );
            return ddmmyyyy.contains("<-") ? getButtonBack() : getButtonForward();
        } else {
            return null;
        }
    }

    // Метод очищает дату в календаре нажатием на дату начала
    private void clearDoubleDate() {
        final String xpath = ".//div[@aria-selected='true']";
        final List<WebElement> date = datePicker.findElements(By.xpath(xpath));
        final Actions mover = Mover.getActions();
        final BooleanSupplier waitDateDelete = () -> {
            mover.click(date.get(0)).build().perform();
            DriverUtils.freeze(DriverConstants.FREEZE_250_MS);
            return datePicker.findElements(By.xpath(xpath)).isEmpty();
        };
        if (!date.isEmpty()) {
            Timer.executeTimer(
                    DriverConstants.ELEMENT_WAIT_5SEC,
                    waitDateDelete
            );
        }
    }

    private void clickDayPoint(final String day) throws FieldFillingException {
        final WebElement dayPoint = datePicker.findElements(By.xpath(".//div[@class = 'DayPicker-Week']/div[text() = '" +
                                                                    day +
                                                                    "' and @aria-disabled = 'false'] | .//p[contains(@data-testid,'day.current') and not(contains(@class,'text-base-4')) and text()='" +
                                                                    day +
                                                                    "']")).stream().reduce((first, second) -> second).orElseThrow(() -> new AutotestError("Не найден день по локатору, необходима унификация внутри ядра"));
        if (dayPoint.getAttribute("class").contains("Day--disabled")) {
            throw new FieldFillingException("День недоступен для выбора");
        }
        ClickActions.safeClick("День в календаре", dayPoint);
        DriverUtils.freeze(DriverConstants.FREEZE_250_MS);

    }

    private DateSelectType getDateSelectType() {
        return (DateSelectType) getElementType(DateSelectType.values());
    }

    private void moveToMoth(final String month, final String yearString) {
        final int year = Integer.parseInt(yearString);
        final int monthInt = getNumberMonth(month);
        BooleanSupplier waitMoveToYear = () -> {
            if (isStartWith(monthInt < 9, yearString)) {
                ClickActions.safeClick(
                        "",
                        year > Integer.parseInt(getCurrentPeriod().split("-")[0]) ? getButtonForward() : getButtonBack()
                );
                DriverUtils.freeze(DriverConstants.FREEZE_250_MS);
                return false;
            } else {
                return true;
            }
        };
        Timer.executeTimerThrowable(DriverConstants.TIMEOUT, "Не удалось выбрать год", waitMoveToYear);
        ClickActions.safeClick(
                "Месяц",
                getMathPicker().findElement(By.xpath(".//button[text()='" + month.substring(0, 3).toLowerCase() + "']"))
        );

    }

    private boolean isStartWith(final boolean flag, final String yearString) {
        if (flag) {
            return !getCurrentPeriod().endsWith(yearString);
        } else {
            return !getCurrentPeriod().startsWith(yearString);
        }
    }

    private Integer getNumberMonth(final String month) {
        return Integer.parseInt(monthMap.entrySet()
                                        .stream()
                                        .filter(entry -> month.equals(entry.getValue()))
                                        .map(Map.Entry::getKey)
                                        .findFirst().orElseThrow(() -> new AutotestError("Не найден номер месяца")));
    }

    private void moveToPeriod(final String month, final String yearString) {
        final int year = Integer.parseInt(yearString);
        if (year != getCurrentYear()) {
            while (year != getCurrentYear()) {
                ClickActions.safeClick("", year > getCurrentYear() ? getButtonForward() : getButtonBack());
                DriverUtils.freeze(DriverConstants.FREEZE_250_MS);
            }
        }
        if (!month.equals(getCurrentMonth())) {
            while (!month.equals(getCurrentMonth())) {
                ClickActions.safeClick(
                        "",
                        getNumberByMonthName(month) >
                        getNumberByMonthName(getCurrentMonth()) ? getButtonForward() : getButtonBack()
                );
                DriverUtils.freeze(DriverConstants.FREEZE_250_MS);
            }
        }
    }

    private void moveToMonth(final String month, final WebElement buttonNavigation) {
        for (int i = 0; i < 11; i++) {
            if (!getCurrentMonth().startsWith(month)) {
                Assert.assertNotNull(
                        "Месяц считается текущем, убедитесь что на форме открывается календарь с текущей датой",
                        buttonNavigation
                );
                ClickActions.safeClick("", buttonNavigation);
            } else {
                return;
            }
        }
    }

    private WebElement getButtonForward() {
        switch (dateSelectType) {
            case MONTH_CALENDAR_MFE:
                return getMathPicker().findElement(By.xpath(".//*[@data-icon-name ='caret-right']"));
            case DATE_SELECT_DASHBOARD_MFE:
            case STUDENT_DATE_MFE:
            case DATE_MFE_FILTER:
                return findElementInDomNotNull(datePicker, "//*[contains(@data-icon-name,'chevron-right') or contains(@data-icon-name,'arrow-right')]");
            case DATE_SELECT:
            case DATE_SELECT_PLANS:
            case DOUBLE_DATE_SELECT_PERIOD:
            case DOUBLE_DATE_SELECT_JOURNAL:
            case DOUBLE_DATE_SELECT:
            case DATE_SELECT_MFE_PANS:
            case DATE_SELECT_MFE:
                return findElementInDomNotNull(datePicker, "/..//*[contains(@data-icon-name,'arrow-right')]");
            default:
                throw new AutotestError(String.format(
                        "Для этого типа \"%s\" нет кнопки вперед",
                        dateSelectType.name()
                ));
        }

    }

    private WebElement getButtonBack() {
        switch (dateSelectType) {
            case MONTH_CALENDAR_MFE:
                return getMathPicker().findElement(By.xpath(".//*[@data-icon-name ='caret-left']"));
            case DATE_SELECT_DASHBOARD_MFE:
            case STUDENT_DATE_MFE:
            case DATE_MFE_FILTER:
                return findElementInDomNotNull(datePicker, "//*[contains(@data-icon-name,'chevron-left') or contains(@data-icon-name,'arrow-left')]");
            case DATE_SELECT:
            case DATE_SELECT_PLANS:
            case DOUBLE_DATE_SELECT_PERIOD:
            case DOUBLE_DATE_SELECT_JOURNAL:
            case DOUBLE_DATE_SELECT:
            case DATE_SELECT_MFE_PANS:
            case DATE_SELECT_MFE:
                return findElementInDomNotNull(datePicker, "/..//*[contains(@data-icon-name,'arrow-left')]");
            default:
                throw new AutotestError(String.format(
                        "Для этого типа \"%s\" нет кнопки назад",
                        dateSelectType.name()
                ));
        }
    }

    private void initMonthMap() {
        monthMap.put("01", "Январь");
        monthMap.put("02", "Февраль");
        monthMap.put("03", "Март");
        monthMap.put("04", "Апрель");
        monthMap.put("05", "Май");
        monthMap.put("06", "Июнь");
        monthMap.put("07", "Июль");
        monthMap.put("08", "Август");
        monthMap.put("09", "Сентябрь");
        monthMap.put("10", "Октябрь");
        monthMap.put("11", "Ноябрь");
        monthMap.put("12", "Декабрь");
    }

    private String getMonthByNumber(final String monthNumber) {
        Assert.assertTrue("Месяц должен быть цифрой от 01 до 12, в два знака", monthMap.containsKey(monthNumber));
        return monthMap.get(monthNumber);
    }

    private int getNumberByMonthName(final String month) {
        final String monthNumber = monthMap
                .keySet()
                .stream()
                .filter(k -> monthMap.get(k).equals(month))
                .collect(Collectors.toList()).get(0);
        return Integer.parseInt(monthNumber);
    }

    private Select getMonthSelect() {
        final String MONTH_SELECT_XPATH = dateSelectType.getMonthSelect();
        if (MONTH_SELECT_XPATH != null) {
            final List<WebElement> elementList = Environment
                    .getDriverService()
                    .getDriver()
                    .findElements(By.xpath(MONTH_SELECT_XPATH));
            if (!elementList.isEmpty()) {
                return new Select(elementList.get(0));
            } else {
                return null;
            }
        }
        return null;
    }

    private Select getYearSelect() {
        final String YEAR_SELECT_XPATH = dateSelectType.getYearSelect();
        final List<WebElement> elementList = Environment
                .getDriverService()
                .getDriver()
                .findElements(By.xpath(YEAR_SELECT_XPATH));
        if (!elementList.isEmpty()) {
            return new Select(elementList.get(0));
        } else {
            return null;
        }
    }

    private String getCurrentPeriod() {
        if (hasPeriodSelector) {
            return Objects.requireNonNull(getMonthSelect()).getText() +
                   " " +
                   Objects.requireNonNull(getYearSelect()).getText();
        } else {
            return datePicker.findElement(By.xpath(dateSelectType.getCurrentPeriod())).getText();
        }
    }

    private WebElement getMathPicker() {
        final String DATE_PICKER_XPATH = "//div[@data-analytics='UIKit.Calendar']";
        return Environment.getDriverService().getDriver().findElement(By.xpath(DATE_PICKER_XPATH));
    }

    private WebElement getDatePicker() {
        final String DATE_PICKER_XPATH = "//div[@class = 'DayPicker-wrapper'] | //div[@data-testid= 'calendar.header.dropDownMenu'] | //div[@class='tippy-content' and //text()='Пн']";
        return Environment.getDriverService().getDriver().findElement(By.xpath(DATE_PICKER_XPATH));
    }

    private void initialDatePicker() {
        initMonthMap();
        datePicker = getDateSelectType().name().equals("MONTH_CALENDAR_MFE") ? getMathPicker() : getDatePicker();
        Wait.visibility(datePicker, "Виджет календаря не появился");
        hasPeriodSelector = null != getMonthSelect();
    }

    private int getCurrentYear() {
        final String year;
        if (hasPeriodSelector) {
            year = Objects.requireNonNull(getYearSelect()).getText();
        } else {
            year = getCurrentPeriod().replace(",","").split(" ")[1];
        }
        return Integer.parseInt(year);
    }

    private String getCurrentMonth() {
        final String month;
        if (hasPeriodSelector) {
            month = Objects.requireNonNull(getMonthSelect()).getText();
        } else {
            month = getCurrentPeriod().replace(",","").split(" ")[0];
        }
        return month;
    }

    @Override
    public String getText() {
        switch (getDateSelectType()) {
            case DATE_SELECT:
            case DATE_SELECT_PLANS:
            case STUDENT_DATE_MFE:
            case DOUBLE_DATE_SELECT_JOURNAL:
            case DOUBLE_DATE_SELECT:
            case DOUBLE_DATE_SELECT_PERIOD:
            case DATE_SELECT_MFE_PANS:
                return getTextNotNull("");
            case DATE_SELECT_MFE:
                return getTextNotNull((String) DriverUtils.executeJS("return arguments[0].value", getInput()));
            default:
                throw new AutotestError(String.format(
                        "Для этого типа \"%s\" не реализовано получение текста",
                        getDateSelectType().name()
                ));
        }
    }

    public String getTextNotNull(final String value) {
        return getDatePickerButton().getText().isEmpty() ? Optional
                .ofNullable(this.getInput().getAttribute("value"))
                .orElse(value) : getDatePickerButton().getText();
    }

    @Override
    public String getFieldValue() {
        return getText();
    }

    @Override
    public boolean validate(final String expected) {
        final boolean validateResult;
        final String actualValue = getText();
        String decodedExpected = DataProcessing.decodeValue(expected);
        if (decodedExpected.equals(actualValue)) {
            validateResult = true;
        } else {
            if (!decodedExpected.contains(".")) {
                // если указано что-либо кроме даты, то берём сегодняшнее число
                final Date date = new Date();
                final DateFormat df = new SimpleDateFormat("dd.MM.yyyy");
                decodedExpected = df.format(date);
            }
            final String[] expectedDateParts = decodedExpected.split("\\.");
            final String[] actualDateParts = decodedExpected.split("\\.");
            Assert.assertEquals(String.format(
                            "Дата \"%s\" указана неверно. Укажите дату в формате дд.мм.гггг",
                            decodedExpected
                    ),
                    3, expectedDateParts.length
            );
            if (expectedDateParts[2].length() != actualDateParts[2].length()) {
                if (actualDateParts[2].length() == 2) {
                    expectedDateParts[2] = expectedDateParts[2].substring(2);
                } else {
                    int year = Integer.parseInt(expectedDateParts[2]);
                    if (year >= 40) {
                        year += 1900;
                    } else {
                        year += 2000;
                    }
                    expectedDateParts[2] = String.valueOf(year);
                }
            }
            validateResult = actualValue.equals(String.join(".", expectedDateParts));
        }
        return validateResult;
    }

    @Override
    public Optional<String> getTextWarning() {
        final AtomicReference<WebElement> element = new AtomicReference<>();
        final BooleanSupplier waitWhenWarningBeDisplayed = () -> {
            element.set(getWarningMessage());
            return null != element.get();
        };
        final boolean result = Timer.executeTimer(DriverConstants.TIMEOUT, waitWhenWarningBeDisplayed);
        return result ? Optional.of(element.get().getText()) : Optional.empty();
    }

    private WebElement getWarningMessage() {
        final List<WebElement> elements = findElementInDom(getParentIterator()
                .getCurrentParent(), getDateSelectType().getWarningXpath());
        if (elements.size() > 1) {
            throw new AutotestError("Найдено больше одного элемента Warning, проверьте xpath");
        }
        return elements.isEmpty() ? null : elements.get(0);
    }

    @Override
    public void notTextWarning() {
        final BooleanSupplier waitWhenWarningIsGone = () -> getWarningMessage() == null;
        Timer.executeTimerThrowable(
                DriverConstants.TIMEOUT,
                "Предупреждение не исчезло по истечению времени",
                waitWhenWarningIsGone
        );
    }

    private void fillDateSelect(final String month, final String year) throws FieldFillingException {
        if (!Objects.requireNonNull(getMonthSelect()).getText().equals(month)) {
            getMonthSelect().fillField(month, false);
            Assert.assertEquals(
                    String.format("Месяц не заполнился значением \"%s\"", month),
                    month,
                    getMonthSelect().getText()
            );
            DriverUtils.freeze(DriverConstants.FREEZE_250_MS);
        }
        // Выбираем год
        if (!Objects.requireNonNull(getYearSelect()).getText().equals(year)) {
            getYearSelect().fillField(year, false);
            Assert.assertEquals(
                    String.format("Год не заполнился значением \"%s\"", year),
                    year,
                    getYearSelect().getText()
            );
        }
        DriverUtils.freeze(DriverConstants.FREEZE_250_MS);
    }

    // Метод написан специально для взаимодействием с двойным календарем
    private void fillDoubleDateSelect(final Map<String, String> dateCalendar) {
        final String[] months = {dateCalendar.get("month1"), dateCalendar.get("month2")};
        final String[] years = {dateCalendar.get("year1"), dateCalendar.get("year2")};
        final String[] days = new String[]{dateCalendar.get("day1"), dateCalendar.get("day2")};
        // Цикл заполняет 2 даты, дата начала и дата конца (заполнение производится с помощью первого календаря путем выбора месяца)
        for (int i = 0; i < 2; i++) {
            moveToPeriod(months[i], years[i]);
            ClickActions.safeClick(
                    "День в календаре",
                    datePicker.findElements(By.xpath(".//div[@class = 'DayPicker-Month']")).get(0).findElement(By.xpath(
                            ".//div[@class = 'DayPicker-Week']/div[text() = '" +
                            days[i] +
                            "' and @aria-disabled = 'false']"))
            );
        }
    }
}
