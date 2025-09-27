package ru.sbt.sber_learning.elements.date_select;


import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.actions.ClickActions;
import ru.sbt.edu_power.e2e_core.actions.Mover;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.e2e_core.elements.AutoIdentElement;
import ru.sbt.edu_power.e2e_core.elements.FieldFillingException;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Available;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Fillable;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Warning;
import ru.sbt.edu_power.e2e_core.elements.text_input.TextInput;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbt.edu_power.external_services.validator.Validatable;
import ru.sbt.sber_learning.elements.WarningElement;
import ru.sbt.sber_learning.elements.dropdown.SelectAdaptive;
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

public class DateSelectAdaptive extends AutoIdentElement implements Fillable, Validatable, Warning, Available {
    private DateSelectTypeAdaptive dateSelectType;
    private WebElement datePicker;
    private boolean hasPeriodSelector;
    private final Map<String, String> monthMap = new HashMap<>();

    private WebElement getDatePickerButton() {
        return getWrappedElement().findElement(By.xpath(getDateSelectType().getDatePickerButton()));
    }

    /**
     *
     * @return возращает Input относительно элемента
     */
    @Override
    protected WebElement getInput() {
        final String xpath = "descendant-or-self::input";
        final List<WebElement> elements = getWrappedElement().findElements(By.xpath(xpath));
        return !elements.isEmpty() ? elements.get(0) : null;
    }

    public DateSelectAdaptive(final WebElement wrappedElement) {
        super(wrappedElement);
    }

    /**
     * Метод автоматически заполняет дату в поле если оно имеется, иначе заполнение происходит через календарь
     * @param ddmmyyyy  - дата формата dd.MM.yyyy
     * @param validated  - флаг валидации на заполнения даты
     * @throws FieldFillingException
     */
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

    /**
     * метод дает возможность заполнить дату нужным методом для этого нужно в степах реализовать шаг и вызвать этот метод
     *  заполняет элемент "([^"]*)" датой "([^"]*)" с помощью (Поля|Календаря)
     * @param ddmmyyyy - дата формата dd.MM.yyyy
     * @param method - метод заполнения даты Поля/Календаря
     * @param validated - флаг валидации на заполнения даты
     * @throws FieldFillingException
     */
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

    /**
     * Заполение даты с помощью календаря
     * @param ddmmyyyy - дата формата dd.MM.yyyy
     * @param notIsNowDate - флаг для генерации текущей даты
     * @param validated - флаг валидации даты на заполнения
     * @throws FieldFillingException
     */

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
                    moveToPeriod(month.toLowerCase(), year);
                    clickDayPoint(day);
                    break;
                default:
                    throw new AutotestError(String.format(
                            "Для этого типа \"%s\" заполнение не реализовано",
                            dateSelectType.name()
                    ));
            }
        }
    }

    /**
     * *
     * Заполнение поля даты с помощью Input
     * @param ddmmyyyy - дата формата dd.MM.yyyy
     * @param notIsNowDate - флаг для генерации текущей даты
     */

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


    /**
     * *
     * определяем какую кнопку нужно нажать для навигации по календарю
     * @param ddmmyyyy - передаваймая дата в формата <-ddmmyyyy
     * стрелки указывают на навигацию по календарю
     */
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


    private void clickDayPoint(final String day) throws FieldFillingException {
        final WebElement dayPoint = datePicker.findElement(By.xpath(dateSelectType.getPickersDay()+"//*[text() = '" +
                                                                    day+"']"));
         if (!dayPoint.isEnabled()) {
             throw new FieldFillingException("День недоступен для выбора");
         }
        ClickActions.safeClick("День в календаре", dayPoint);
        DriverUtils.freeze(DriverConstants.FREEZE_250_MS);

    }

    private DateSelectTypeAdaptive getDateSelectType() {
        return (DateSelectTypeAdaptive) getElementType(DateSelectTypeAdaptive.values());
    }

    /**
     * Переходим к нужному периоду
     * @param month - месяц
     * @param yearString - год
     */
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

    /**
     * Переводит календарь к нужному месяцу, работает в рамках года (такая реализация на UI)
     * @param month - месяц
     * @param buttonNavigation - кнопка навигации
     */
    private void moveToMonth(final String month, final WebElement buttonNavigation) {
        for (int i = 0; i < 11; i++) {
            if (!getCurrentMonth().startsWith(month)) {
                Assert.assertNotNull(
                        "Месяц считается текущем, убедитесь что на форме открывается календарь с текущей датой",
                        buttonNavigation);
                ClickActions.safeClick("", buttonNavigation);
            } else {
                return;
            }
        }
    }

    /**
     * @return кнопку вперед в календаре
     */
    private WebElement getButtonForward() {
        return datePicker.findElement(By.xpath(dateSelectType.getNextButton()));

    }

    /**
     * @return кнопку назад в календаре
     */
    private WebElement getButtonBack() {
        return datePicker.findElement(By.xpath(dateSelectType.getBackButton()));
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

    /**
     * Конвертация String месяца в Integer
     * @param month - месяц типа String
     * @return пример String Апрель, вернется 04
     */
    private int getNumberByMonthName(final String month) {
        final String monthNumber = monthMap
                .keySet()
                .stream()
                .filter(k -> monthMap.get(k).toLowerCase().equals(month.toLowerCase()))
                .collect(Collectors.toList()).get(0);
        return Integer.parseInt(monthNumber);
    }

    /**
     * @return возвращаем элемент селекта месяца для заполнения
     */
    private SelectAdaptive getMonthSelect() {
        final String MONTH_SELECT_XPATH = dateSelectType.getMonthSelect();
        if (MONTH_SELECT_XPATH != null) {
            final List<WebElement> elementList = Environment
                    .getDriverService()
                    .getDriver()
                    .findElements(By.xpath(MONTH_SELECT_XPATH));
            if (!elementList.isEmpty()) {
                return new SelectAdaptive(elementList.get(0));
            } else {
                return null;
            }
        }
        return null;
    }

    /**
     *
     * @return возвращаем элемент селекта года для заполнения
     */
    private SelectAdaptive getYearSelect() {
        final String YEAR_SELECT_XPATH = dateSelectType.getYearSelect();
        final List<WebElement> elementList = Environment
                .getDriverService()
                .getDriver()
                .findElements(By.xpath(YEAR_SELECT_XPATH));
        if (!elementList.isEmpty()) {
            return new SelectAdaptive(elementList.get(0));
        } else {
            return null;
        }
    }

    /**
     *
     * @return возвращаем текст в виде mm yyyy при наличии селектов, иначе берем период с календаря currentPeriod
     */
    private String getCurrentPeriod() {
        if (hasPeriodSelector) {
            return Objects.requireNonNull(getMonthSelect()).getText() +
                   " " +
                   Objects.requireNonNull(getYearSelect()).getText();
        } else {
            return datePicker.findElement(By.xpath(dateSelectType.getCurrentPeriod())).getText();
        }
    }

    /**
     * @return возвращаем элемент открытия всего календаря
     */
    private WebElement getDatePicker() {
        return Environment.getDriverService().getDriver().findElement(By.xpath(dateSelectType.getDatePicker()));
    }

    /**
     * Заполнение основных параметров
     * hasPeriodSelector - флаг наличия селекта в календаре
     * datePicker - окно Календаря
     * Валидация открытия календаря
     */
    private void initialDatePicker() {
        initMonthMap();
        datePicker = getDatePicker();
        Wait.visibility(datePicker, "Виджет календаря не появился");
        hasPeriodSelector = null != getMonthSelect();
    }

    private int getCurrentYear() {
        final String year;
        if (hasPeriodSelector) {
            year = Objects.requireNonNull(getYearSelect()).getText();
        } else {
            year = getCurrentValue(1);
        }
        return Integer.parseInt(year);
    }

    private String getCurrentMonth() {
        final String month;
        if (hasPeriodSelector) {
            month = Objects.requireNonNull(getMonthSelect()).getText();
        } else {
            month = getCurrentValue(0);
        }
        return month.toLowerCase();
    }

    /**
     * Разбиваем период на месяц и год, в зависимости от mothOrYears берем месяц или же год
     * @param mothOrYears 0 - берем месяц, 1 - берем год
     * @return возвращаем месяц или же год
     */
    private String getCurrentValue(final int mothOrYears){
        final String value;
        final String currentPeriod = getCurrentPeriod();
        if(currentPeriod.split(" ").length ==2){
            value = getCurrentPeriod().split(" ")[mothOrYears];
        }
        else if(currentPeriod.split("\n").length ==2){
            value = getCurrentPeriod().split("\n")[mothOrYears];
        }
        else {
            throw new AutotestError("Не удается получить месяц и год по отдельности, нужно проверить локатор");
        }
        return value;
    }

    @Override
    public String getText() {
        return getDatePickerButton().getText().isEmpty() ? Optional
                .ofNullable(this.getInput().getAttribute("value"))
                .orElse("") : getDatePickerButton().getText();
    }

    @Override
    public String getFieldValue() {
        return getText();
    }

    /**
     * Валидируем дату на лишние символы
     * @param expected - Дата которую нужно проверить
     * @return Возвращаем результат валидации
     */
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

    /**
     *
     * @return возвращаем текст предупреждения
     */
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

    /**
     * проверяем на отсутствие предупреждения
     */
    @Override
    public void notTextWarning() {
        final BooleanSupplier waitWhenWarningIsGone = () -> getWarningMessage() == null;
        Timer.executeTimerThrowable(
                DriverConstants.TIMEOUT,
                "Предупреждение не исчезло по истечению времени",
                waitWhenWarningIsGone
        );
    }

    private WebElement getWarningMessage() {
        final String warningXpath = getDateSelectType().getWarningXpath();
        if (null == warningXpath) {
            throw new AutotestError("Поле не поддерживает вывод предупреждений Warning");
        }
        final List<WebElement> elements = WarningElement.getWarningElement(getParentIterator()
                .getCurrentParent(),getDateSelectType().getWarningXpath());
        if (elements.size() > 1) {
            throw new AutotestError("Найдено больше одного элемента Warning, проверьте xpath");
        }
        return elements.isEmpty() ? null : elements.get(0);
    }

    /**
     * Метод где заполение года и месяца реализована с помощью селктов
     * @param month - месяц
     * @param year - год
     * @throws FieldFillingException
     */
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

    @Override
    public boolean isAvailable() {
        return getInput().isEnabled();
    }
}
