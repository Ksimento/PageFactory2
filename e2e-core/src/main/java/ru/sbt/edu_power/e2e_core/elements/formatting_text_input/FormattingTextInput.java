package ru.sbt.edu_power.e2e_core.elements.formatting_text_input;

import lombok.Getter;
import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptException;
import org.openqa.selenium.Keys;
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
import ru.sbt.edu_power.e2e_core.remote_access.BrowserUtils;
import ru.sbt.edu_power.e2e_core.resource_repository.ResourceRepository;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbt.edu_power.external_services.validator.Validatable;
import ru.sbt.edu_power.external_services.validator.Validator;
import ru.sbtqa.tag.pagefactory.environment.Environment;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/**
 * Элемент реализует многострочное поле с редактором HTML контента ck-editor
 */
public class FormattingTextInput extends AutoIdentElement implements Fillable, Validatable, Available {
    private String ckeInstanceId;
    private String errorMessage;

    private WebElement getFrame() {
        final List<WebElement> elements = new ArrayList<>();
        final String xpath = "ancestor::div[contains(@class, 'FormElement')]//iframe | descendant-or-self::iframe";

        final BooleanSupplier waitWhenFrameBeDisplayed = () -> {
            elements.clear();
            elements.addAll(this.findElements(By.xpath(xpath)));
            return !elements.isEmpty();
        };

        Timer.executeTimerThrowable(
                DriverConstants.TIMEOUT,
                "Поле CKEDITOR не инициализировано",
                waitWhenFrameBeDisplayed
        );

        return elements.get(0);
    }

    @SneakyThrows
    @Override
    public void clear() {
        clearField();
    }

    public FormattingTextInput(final WebElement wrappedElement) {
        super(wrappedElement);
    }

    @Override
    public String getText() {
        switch (getType()) {
            case CKEDITOR_IFRAME:
                return (String) DriverUtils.executeJS("return CKEDITOR.instances."
                        .concat(getInstanceId())
                        .concat(".getData('')"));
            case CKEDITOR_INPUT:
                if (isFormattingText()) {
                    return ((String) DriverUtils.executeJS(
                            "return arguments[0].innerHTML;",
                            getWrappedElement().findElement(By.xpath(".//div[contains(@class,'_Editor5')]"))
                    )).replaceAll("⁠", "").replaceAll("\"", "");
                } else {
                    return getCkeditorInput().getText();
                }
            default:
                throw new AutotestError(String.format(
                        "Для поля типа \"%s\" не реализовано получение текста",
                        getType()
                ));
        }
    }

    private WebElement getCkeditorInput() {
        return getSpecifyElement(
                getType().getInputXpath(),
                "Не удалось получить элемент для установки значения поля"
        );
    }

    private WebElement getCkeditorIdentification() {
        return getSpecifyElement(
                getType().getIdentificationXpath(),
                "Не удалось получить элемент для установки значения поля"
        );
    }

    private void clearField() throws FieldFillingException {
        switch (getType()) {
            case CKEDITOR_IFRAME:
                final BooleanSupplier waitWhenEditorBeenCleaned = () -> {
                    try {
                        DriverUtils.executeJS("CKEDITOR.instances.".concat(getInstanceId()).concat(".setData('')"));
                        DriverUtils.freeze(DriverConstants.FREEZE_500_MS);
                        return validate("");
                    } catch (final JavascriptException e) {
                        ckeInstanceId = null;
                        errorMessage = e.getMessage();
                        return false;
                    }
                };
                final boolean result = Timer.executeTimer(DriverConstants.ELEMENT_WAIT_5SEC, waitWhenEditorBeenCleaned);
                if (!result) {
                    throw new FieldFillingException("Не удалось очистить поле\n" + errorMessage);
                }
                break;
            case CKEDITOR_INPUT:
                final WebElement input = getInput();
                ClickActions.safeClick("поле ввода", input);
                if (!"".equals(getText())) {
                    final AtomicReference<String> fieldText = new AtomicReference<>("");
                    final BooleanSupplier waitWhenFieldBeCleaned = () -> {
                        if (fieldText.get().equals(getText())) {
                            return true;
                        }
                        fieldText.set(getText());
                        if ("Mac".equals(BrowserUtils.getBrowserOSType())) {
                            Mover.getActions().keyDown(Keys.COMMAND).sendKeys("a").keyUp(Keys.COMMAND).build().perform();
                        } else {
                            Mover.getActions().keyDown(Keys.CONTROL).sendKeys("a").keyUp(Keys.CONTROL).build().perform();
                        }
                        Mover.getActions().sendKeys(Keys.BACK_SPACE);
                        return false;
                    };
                    Timer.executeTimerThrowable(
                            DriverConstants.ELEMENT_WAIT_5SEC,
                            String.format("Не удалось очистить поле \"%s\"", getLabel()),
                            waitWhenFieldBeCleaned
                    );
                }
                break;
            default:
                throw new AutotestError(String.format(
                        "Для поля типа \"%s\" не реализована очистка",
                        getType()
                ));
        }

    }

    private void fill(final String value, final boolean validated) throws FieldFillingException {
        switch (getType()) {
            case CKEDITOR_IFRAME:
                final BooleanSupplier waitWhenFieldBeFilled = () -> {
                    if (validated && validate(value)) {
                        return true;
                    }
                    try {
                        DriverUtils.executeJS("CKEDITOR.instances."
                                .concat(getInstanceId())
                                .concat(".insertHtml(\"")
                                .concat(value.replaceAll("\"", "'").replaceAll("\\\\", "\\\\\\\\"))
                                .concat("\")"));
                        DriverUtils.freeze(DriverConstants.FREEZE_500_MS);
                        Mover.getActions().sendKeys(Keys.TAB).build().perform();
                    } catch (final JavascriptException e) {
                        ckeInstanceId = null;
                        errorMessage = e.getMessage();
                    }
                    return !validated;
                };
                final boolean result = Timer.executeTimer(DriverConstants.TIMEOUT, waitWhenFieldBeFilled);
                if (!result) {
                    throw new FieldFillingException("Не удалось заполнить поле\n" + errorMessage);
                }
                break;
            case CKEDITOR_INPUT:
                if (value.contains("/")) {
                    fillFormattingText(value);
                } else {
                    Mover.getActions().sendKeys(value).build().perform();
                }
                break;
            default:
                throw new AutotestError(String.format(
                        "Для поля типа \"%s\" не реализовано заполнение",
                        getType()
                ));
        }
    }

    private String getInstanceId() {
        if (null == ckeInstanceId) {
            ckeInstanceId = getFrame()
                    .findElement(By.xpath("./ancestor::div[contains(@class, 'cke_editor_editor') or " +
                                          "contains(@class, 'cke_editor_modalEditor')]"))
                    .getAttribute("id").substring(4);
        }
        return ckeInstanceId;
    }

    private FormattingTextInputType getType() {
        return (FormattingTextInputType) getElementType(FormattingTextInputType.values());
    }

    /**
     * Метод заполняет CK5 посимвольно, при этом с помощью масок выбирает формат (Жирный, подчеркивание и т.д.)
     * При передачи текста без масок заполнение будет происходить так же посимвольно
     */
    private void fillFormattingText(String text) {
        final String[] value = text.split("");
        FormattingType formattingType;
        for (int i = 0; i < value.length; i++) {
            while (value[i].equals("/")) {
                final int nextChar = i + 1;
                formattingType = Arrays
                        .stream(FormattingType.values())
                        .filter(e -> e.mask.equalsIgnoreCase(value[nextChar]))
                        .findFirst()
                        .orElseThrow(() -> new AutotestError(String.format(
                                "Неизвестный формат элемента \"%s\"",
                                value[nextChar]
                        )));
                ClickActions.safeClick(
                        formattingType.name(),
                        Environment
                                .getDriverService()
                                .getDriver()
                                .findElements(By.xpath(formattingType.buttonXpath))
                                .get(0)
                );
                if (i + 2 >= value.length) {
                    return;
                } else {
                    i += 2;
                }
            }
            Mover.getActions().sendKeys(value[i]).build().perform();
        }
    }


    @Override
    public void fillField(final String value, final Boolean validated) throws FieldFillingException {
        clearField();
        fill(DataProcessing.decodeValue(value), validated);
    }

    @Override
    public String getFieldValue() {
        return getText();
    }

    @Override
    public boolean validate(final String expectedDraft) {
        if (isFormattingText()) {
            final Map<String, String> resource = ResourceRepository
                    .getResource(ResourceRepository.AvailableResource.HTML_SOURCE);
            final String text = resource.get(expectedDraft);
            return Validator.matchValues(getText(), text);
        } else {
            final String expected = DataProcessing.decodeValue(expectedDraft);
            return Validator.matchValues(
                    Jsoup.clean(getText(), Whitelist.simpleText()).replaceAll("&nbsp;", " "),
                    Jsoup.clean(expected, Whitelist.simpleText())
            );
        }
    }

    private boolean isFormattingText() {
        return getWrappedElement()
                       .findElements(By.xpath(
                               ".//u | " +
                               ".//strong | " +
                               ".//i | " +
                               ".//u | " +
                               ".//p[contains(@style,'center')] | " +
                               ".//p[contains(@style,'justify')] | " +
                               ".//ol | " +
                               ".//ul | " +
                               ".//li"))
                       .size() != 0;
    }

    @Override
    public boolean isAvailable() {
        switch (getType()) {
            case CKEDITOR_IFRAME:
                return getFrame().findElements(By.xpath("ancestor::div[@disabled]")).isEmpty();
            case CKEDITOR_INPUT:
                return "true".equals(getCkeditorIdentification().getAttribute("contenteditable"));
            default:
                throw new AutotestError(String.format(
                        "Для поля типа \"%s\" не реализован метод isAvailable",
                        getType()
                ));
        }
    }

    /**
     * Перечисление форматов, маска определяет какую из кнопок форматирования нужно нажать
     * Для добавления нужно просто на просто добавить новую маску (Важно 1 символ которого нет в перечислении)
     * а так же xpath до кнопки
     */
    @Getter
    enum FormattingType {
        BOLD("b", "//button[node()='Жирный']"),
        ITALICS("i", "//button[node()='Курсив']"),
        UNDERLINED("u", "//button[node()='Подчеркнутый']"),
        LEFT_ALIGNMENT("l", "//button[node()='Выравнивание по левому краю']"),
        CENTER_ALIGNMENT("c", "//button[node()='Выравнивание по центру']"),
        RIGHT_ALIGNMENT("r", "//button[node()='Выравнивание по правому краю']"),
        WIDTH_ALIGNMENT("j", "//button[node()='Выравнивание по ширине']");

        private final String buttonXpath;
        private final String mask;

        FormattingType(final String mask, final String buttonXpath) {
            this.mask = mask;
            this.buttonXpath = buttonXpath;

        }
    }
}
