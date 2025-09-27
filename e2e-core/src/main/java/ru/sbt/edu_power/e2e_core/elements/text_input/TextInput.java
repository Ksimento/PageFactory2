package ru.sbt.edu_power.e2e_core.elements.text_input;

import io.qameta.allure.Allure;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptException;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.actions.ClickActions;
import ru.sbt.edu_power.e2e_core.actions.ClipboardUtils;
import ru.sbt.edu_power.e2e_core.actions.Mover;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.e2e_core.elements.AutoIdentElement;
import ru.sbt.edu_power.e2e_core.elements.FieldFillingException;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Available;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Fillable;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Warning;
import ru.sbt.edu_power.e2e_core.remote_access.BrowserUtils;
import ru.sbt.edu_power.e2e_core.remote_access.Moon;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbt.edu_power.external_services.validator.Validatable;
import ru.sbt.edu_power.external_services.validator.Validator;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.reflections.Reflections.log;

public class TextInput extends AutoIdentElement implements Fillable, Available, Validatable, Warning {
    private String value;

    public TextInput(final WebElement wrappedElement) {
        super(wrappedElement);
    }

    @Override
    public boolean isAvailable() {
        return getInput().isEnabled();
    }

    @Override
    public void clear() {
        clearOrSelectDataInField().sendKeys(Keys.BACK_SPACE);
    }

    public WebElement clearOrSelectDataInField() {
        final WebElement input = getInput();
        Mover.getActions().click(input).build().perform();
        if ("".equals(getText())) {
            return input;
        }
        if ("Mac".equals(BrowserUtils.getBrowserOSType())) {
            Mover.getActions().keyDown(Keys.COMMAND).sendKeys("a").keyUp(Keys.COMMAND).build().perform();
        } else {
            Mover.getActions().keyDown(Keys.CONTROL).sendKeys("a").keyUp(Keys.CONTROL).build().perform();
        }
        Mover.getActions().sendKeys(Keys.BACK_SPACE);

//        workaround для полей, которые не позволяют удалить все данные
//        перед заполнением содержимое поле будет выделено и заполнение заменит его новым содержимым
        if ("Mac".equals(BrowserUtils.getBrowserOSType())) {
            Mover.getActions().keyDown(Keys.COMMAND).sendKeys("a").keyUp(Keys.COMMAND).build().perform();
        } else {
            Mover.getActions().keyDown(Keys.CONTROL).sendKeys("a").keyUp(Keys.CONTROL).build().perform();
        }
        return input;
    }

    @Override
    public void fillField(final String value, final Boolean validated) throws FieldFillingException {
        final AtomicReference<String> errorMessage = new AtomicReference<>();
        final String decodeValue = getValue(value);
        final BooleanSupplier waitWhenFieldBeFilled = () -> {
            try {
                switch (getInputType()) {
                    case FILTER_INPUT:
                        if (validated && validate(decodeValue, errorMessage)) {
                            ClickActions.safeClick("Поиск", getSvg());
                            return true;
                        }
                        clearOrSelectDataInField();
                        sendKeys(decodeValue);
                        if (!validated) {
                            ClickActions.safeClick("Поиск", getSvg());
                        }
                        return !validated;
                    case TEXTAREA:
                    case TEXT_INPUT:
                        return buildSendKeys(CLEAR.BACK_SPACE, false, validated, decodeValue, errorMessage);
                    case NUMBER_INPUT:
                        return buildSendKeys(CLEAR.ALL, false, validated, decodeValue, errorMessage);
                    case SIMPLE_INPUT:
                    case COMBOBOX_INPUT:
                        return buildSendKeys(CLEAR.SELECT_DATA, true, validated, decodeValue, errorMessage);
                    case PHONE_INPUT:
                    case NUMBER_ATTEMPT_INPUT:
                        clearOrSelectDataInField();
                        sendKeys(decodeValue);
                        return true;
                    case S21_DATE_INPUT:
                        if ("jenkins".equals(System.getProperty("execution.environment"))) {
                            return sendKeysS21Date(decodeValue);
                        } else {
                            return buildSendKeys(CLEAR.BACK_SPACE, false, validated, decodeValue, errorMessage);
                        }
                    case TEXT_INPUT_SURVEY:
                        clearOrSelectDataInField();
                        Mover.getActions().sendKeys(decodeValue).build().perform();
                        return true;
                    default:
                        throw new AutotestError(String.format(
                                "Для поля типа \"%s\" не реализовано заполнение",
                                getInputType()
                        ));
                }
            } catch (StaleElementReferenceException e) {
                return false;
            }
        };
        final boolean result;
        try {
            result = Timer.executeTimer(
                    DriverConstants.TIMEOUT * (decodeValue.length() > 100 ? 4 : 1),
                    waitWhenFieldBeFilled
            );
        } catch (final AutotestError e) {
            throw new FieldFillingException(e);
        }
        try {
            Allure.addAttachment("Тип поля: " + getInputType().name(), "Ожидаемое значение в поле: " + value);
        } catch (StaleElementReferenceException ignored) {
            log.info("Потеряна ссылка на объект");
        } catch (NoSuchElementException ignored) {
            log.info("Элемент не найден");
        }
        if (!result) {
            throw new FieldFillingException(errorMessage.get());
        }
        if (this.value.startsWith("%")) {
            getInput().sendKeys(Keys.ENTER);
        }
        this.value = null;
    }

    private boolean sendKeysS21Date(final String value) {
        try {
            final String dataContainer = "//div[@role='dialog']";
            final String inputDate = dataContainer + "//input[@type='tel']";
            final String openTextInput = dataContainer + "//button[contains(@aria-label,'text input')]";
            final WebDriver driver = DriverUtils.getWebDriver();
            final WebElement input = getInput();
            ClickActions.safeClick("Поле Календаря", input);
            DriverUtils.freeze(DriverConstants.FREEZE_250_MS);
            ClickActions.safeClick("Редактировать дату", driver.findElement(By.xpath(openTextInput)));
            DriverUtils.freeze(DriverConstants.FREEZE_250_MS);
            final WebElement inputElement = driver.findElement(By.xpath(inputDate));
            inputElement.sendKeys(Keys.chord(ClipboardUtils.getKeys(), "A"));
            inputElement.sendKeys(Keys.BACK_SPACE);
            inputElement.sendKeys(value);
            ClickActions.safeClick("ОК", driver.findElement(By.xpath(dataContainer + "//button[node()='OK']")));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean buildSendKeys(
            final CLEAR clearMethod,
            final boolean moverSendKeys,
            final boolean validated,
            final String value,
            final AtomicReference<String> errorMessage
    ) {
        if (validated && validate(value, errorMessage)) {
            return true;
        }
        switch (clearMethod) {
            case ALL:
                getInput().sendKeys(Keys.chord(ClipboardUtils.getKeys(), "A"));
                break;
            case SELECT_DATA:
                clearOrSelectDataInField();
                break;
            case BACK_SPACE:
                clear();
                break;
            default:
                throw new AutotestError(String.format(
                        "Для поля типа \"%s\" не реализована очистка",
                        getInputType()
                ));
        }

        if (moverSendKeys) {
            Mover.getActions().sendKeys(value).build().perform();
        } else {
            sendKeys(value);
        }
        DriverUtils.freeze(DriverConstants.FREEZE_250_MS);
        return !validated;
    }

    @Override
    public void sendKeys(final CharSequence... keysToSend) {
        final String value = String.join("", keysToSend);
        final WebElement input = getInput();
        if ("".equals(value)) {
            input.sendKeys(Keys.BACK_SPACE);
        } else {
            final String cleanedValue = value.replaceAll("^%", "");
            if (!fillFieldByClipboard(input, cleanedValue)) {
                input.sendKeys(cleanedValue);
            }
            try {
                log.info("текст после заполнения : {}", input.getAttribute("value"));
            } catch (StaleElementReferenceException ignored) {
                log.info("Потеряна ссылка на объект");
            }
        }
    }

    private static synchronized boolean fillFieldByClipboard(final WebElement input, final String value) {
        final int VALUE_LENGTH_BY_USE_CLIPBOARD = 100;
        final boolean IS_BIG_LENGTH = value.length() > VALUE_LENGTH_BY_USE_CLIPBOARD;
        if (IS_BIG_LENGTH) {
            final Keys controlButton = ClipboardUtils.getKeys();
            //В headless режиме не работает заполнение с помощью буфер обмена, по этой причине используем JS
            //Перед заполнением требуется повторная очистка поля
            if (DriverUtils.isHeadless()) {
                log.info("Заполняю длинное поле с помощью Actions значением : {}", value);
                return pasteLongTextInHeadless(input, value, controlButton);
            } else {
                final BrowserUtils.ClipboardType clipboardType = BrowserUtils.getClipboardType();
                // Для длинного текста будем использовать копи-паст, если это возможно
                // В Mac OS не работает нажатие COMMAND+V через селениум
                switch (clipboardType) {
                    case MOON:
                        Moon.setClipBoard(value);
                        input.sendKeys(Keys.chord(controlButton, "v"));
                        return true;
                    case NATIVE:
                        Toolkit.getDefaultToolkit()
                               .getSystemClipboard()
                               .setContents(new StringSelection(value), null);
                        input.sendKeys(Keys.chord(controlButton, "v"));
                        return true;
                    default:
                        break;
                }
            }
        }
        return false;
    }


    /**
     * Метод предназначен для заполнения полей ввода большими значениями в режиме headless
     * Для копирования создается http элемент с нужным значением для копирования горячими клавишами
     * в дальнейшем элемент удаляется
     *
     * @param input - Элемент ввода
     * @param value - Текстовое значение
     * @return успешность заполнения поля
     */
    private static boolean pasteLongTextInHeadless(
            final WebElement input,
            final String value,
            final Keys controlButton
    ) {
        BooleanSupplier send = () -> {
            DriverUtils.freeze(DriverConstants.FREEZE_250_MS);
            try {
                WebElement elementClipboard;
                elementClipboard = ClipboardUtils.createInputClipboard(input.findElement(By.xpath(
                        "./parent::*")), value);
                if(elementClipboard == null){
                    elementClipboard = ClipboardUtils.getActiveElementClipboard(value);
                }
                elementClipboard.sendKeys(Keys.chord(controlButton, "A"));
                elementClipboard.sendKeys(Keys.chord(controlButton, "C"));
                ClipboardUtils.deleteInputClipboard();
                DriverUtils.freeze(DriverConstants.FREEZE_250_MS);
                Mover.scrollToElement(input);
                DriverUtils.executeJS("arguments[0].click()", input);
                input.sendKeys(Keys.chord(controlButton, "A"));
                input.sendKeys(Keys.chord(controlButton, "V"));
                return true;
            } catch (JavascriptException e) {
                Allure.addAttachment("При заполнении возникла проблема, пытаюсь повторно заполнить поле", e.toString());
                return false;
            }
        };
        return Timer.executeTimer(DriverConstants.TIMEOUT, send);
    }

    @Override
    public String getText() {
        return Optional.ofNullable(getInput().getAttribute("value")).orElse(getInput().getText()).trim();
    }

    @Override
    public String getFieldValue() {
        return getText();
    }

    @Override
    public boolean validate(final String expected) {
        final String decodedExpected = DataProcessing
                .decodeValue(expected)
                .replaceAll("\u00AD", "")
                .replaceAll("\n", " ");
        if (!Validator.matchValues(getText(), decodedExpected)) {
            return Validator.matchValues(getText()
                    .replace(":", "")
                    .replaceAll("\\.", "")
                    .trim(), decodedExpected
                    .replaceAll("^%", ""));
        }
        return true;
    }

    private boolean validate(final String expected, final AtomicReference<String> errorMessage) {
        final boolean validateResult = validate(expected);
        if (!validateResult) {
            errorMessage.set(String.format(
                    "Не удалось заполнить поле. После попытки заполнения значением \"%s\", в поле устанавливается неверное значение \"%s\"",
                    expected,
                    getText()
            ));
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

    @Override
    public void notTextWarning() {
        final BooleanSupplier waitWhenWarningIsGone = () -> getWarningMessage() == null;
        Timer.executeTimerThrowable(
                DriverConstants.TIMEOUT,
                "Предупреждение не исчезло по истечению времени , текст предупреждения:",
                waitWhenWarningIsGone
        );
    }

    public String getInputTypeAttribute() {
        return getInput().getAttribute("type");
    }


    @Override
    protected String getLabel() {
        final String labelXpath = getInputType().getLabelXpath();
        if (null == labelXpath) {
            return "";
        }
        final List<WebElement> elements = getParentIterator()
                .getCurrentParent()
                .findElements(By.xpath(labelXpath));
        return elements.isEmpty() ? "" : elements.get(0).getText();
    }

    @Override
    public WebElement getInput() {
        return getSpecifyElement(
                getInputType().getInputXpath(),
                "Не удалось получить элемент для установки значения поля"
        );
    }

    protected WebElement getSvg() {
        return getSpecifyElement(
                "descendant-or-self::*[name()='svg']",
                "Не удалось получить элемент для клика"
        );
    }

    private WebElement getWarningMessage() {
        final String warningXpath = getInputType().getWarningXpath();
        if (null == warningXpath) {
            throw new AutotestError("Поле не поддерживает вывод предупреждений Warning");
        }
        final List<WebElement> elements = getParentIterator()
                .getCurrentParent()
                .findElements(By.xpath(warningXpath));
        if (elements.size() > 1) {
            throw new AutotestError("Найдено больше одного элемента Warning, проверьте xpath");
        }
        return elements.isEmpty() ? null : elements.get(0);
    }

    public InputType getInputType() {
        return (InputType) getElementType(InputType.values());
    }

    private String getValue(final String data) {
        if (null == value) {
            value = DataProcessing.decodeValue(data);
        }
        return value;
    }

    public enum CLEAR {
        ALL,
        BACK_SPACE,
        SELECT_DATA
    }
}
