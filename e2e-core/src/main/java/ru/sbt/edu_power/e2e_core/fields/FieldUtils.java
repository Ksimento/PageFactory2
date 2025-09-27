package ru.sbt.edu_power.e2e_core.fields;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.actions.ClickActions;
import ru.sbt.edu_power.e2e_core.actions.PageControls;
import ru.sbt.edu_power.e2e_core.blocks.BlockExtractor;
import ru.sbt.edu_power.e2e_core.blocks.BlockInitialized;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.e2e_core.elements.FieldFillingException;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Fillable;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Warning;
import ru.sbt.edu_power.e2e_core.elements.survey.SurveyJSField;
import ru.sbt.edu_power.e2e_core.elements.text_input.TextInput;
import ru.sbt.edu_power.e2e_core.error_processing.ErrorCollector;
import ru.sbt.edu_power.e2e_core.error_processing.NotCriticalErrorAccumulator;
import ru.sbt.edu_power.e2e_core.survey.SurveyStepActionsUtils;
import ru.sbt.edu_power.e2e_core.widgets.Widget;
import ru.sbt.edu_power.e2e_core.widgets.WidgetExtractor;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbt.edu_power.external_services.validator.Validator;
import ru.sbtqa.tag.pagefactory.Page;
import ru.sbtqa.tag.pagefactory.context.PageContext;
import ru.sbtqa.tag.qautils.errors.AutotestError;
import ru.yandex.qatools.htmlelements.element.Select;
import ru.yandex.qatools.htmlelements.element.TypifiedElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

public class FieldUtils {
    private static Function<WebElement, Class<? extends TypifiedElement>> detectFieldTypeFunction;

    public static void configure(
            final Function<WebElement, Class<? extends TypifiedElement>> detectFieldTypeFunction
    ) {
        FieldUtils.detectFieldTypeFunction = detectFieldTypeFunction;
    }

    public static Class<? extends TypifiedElement> detectFieldType(final WebElement element) {
        return detectFieldTypeFunction.apply(element);
    }

    /**
     * Метод выполняет заполнение поля значением.
     * Он попытается определить тип поля по классу элемента и запустить соответствующий метод заполнения
     * По мере добавления новых типов элементов потребуется добавлять их в этот метод.
     *
     * @param element веб-элемент, который необходимо заполнить данными
     * @param value   данные для заполнения
     */
    public static void fillField(final WebElement element, final String value, final Boolean validated)
            throws FieldFillingException {
        if (element instanceof Fillable) {
            ((Fillable) element).fillField(value, validated);
        } else if (element instanceof Select) {
            ((Select) element)
                    .selectByVisibleText(DataProcessing.decodeValue(value));
        } else {
            element.clear();
            element.sendKeys(DataProcessing.decodeValue(value));
        }
        PageControls.detectBackendErrors();
    }

    /**
     * Метод проверяет, отображается ли элемент в браузере по его названию из PageObject
     *
     * @param elementName название элемента
     * @return true если элемент отображается на странице, false если нет
     */
    public static boolean isElementDisplayed(final String elementName) {
        if ("".equals(elementName)) {
            return false;
        }
        final long timeout = 200;
        final BooleanSupplier waitWhileElementWillBeDisplayed = () -> {
            try {
                if (elementName.contains("->")) {
                    final WebElement blockElement = FindUtils.getElementByNameOrPath(elementName);
                    if (null != blockElement) {
                        return DriverUtils.isElementDisplayed(blockElement);
                    }
                }
                final List<WebElement> elementList = DriverUtils.findElementsOnPageByXpath(DriverUtils.getXpath(
                        elementName));
                if (!elementList.isEmpty()) {
                    return DriverUtils.isElementDisplayed(elementList.get(0));
                }
//                TODO: заменить исключения в BlockExtractor и здесь и в других местах их отлова на более подходящие
            } catch (final RuntimeException ignored) {
            }
            return false;
        };
        return Timer.executeTimerMillis(timeout, waitWhileElementWillBeDisplayed);
    }

    // Заполняем в цикле набор всех полей. Если в процессе заполнения поле будет перерисовано
    // фреймворк попытается заполнить это поле повторно
    public static void fillFields(final Map<String, String> data, final Boolean validated) {
        for (final Map.Entry<String, String> dataEntry : data.entrySet()) {
            final AtomicReference<StaleElementReferenceException> exception = new AtomicReference<>();
            final BooleanSupplier waitWhenFieldBeFilled = () -> {
                try {
                    final WebElement field = DriverUtils.getElementByTitle(dataEntry.getKey());
                    FieldUtils.fillField(field, dataEntry.getValue(), validated);
                } catch (final StaleElementReferenceException e) {
                    exception.set(e);
                    return false;
                } catch (final FieldFillingException e) {
                    throw new AutotestError("Не удалось заполнить поле " + dataEntry.getKey(), e);
                }
                return true;
            };
            if (!Timer.executeTimer(DriverConstants.ELEMENT_WAIT_5SEC * 2, waitWhenFieldBeFilled)) {
                throw exception.get();
            }
        }
    }

    public static void verifyFieldsDataBlock(final String blockPath, final Map<String, String> data) {
        final Page currentPage = PageContext.getCurrentPage();
        final ErrorCollector errorCollector = new ErrorCollector();
        final BlockExtractor blockExtractor;
        // Если список блоков в виджете, то сначала выделяем виджет, а потом из него получаем список блоков
        if (FindUtils.isWidget(blockPath)) {
            final String[] parts = blockPath.split("->", 2);
            final Widget widget = WidgetExtractor.getWidget(parts[0]);
            PageContext.setCurrentPage(widget);
            blockExtractor = new BlockExtractor(
                    parts[1],
                    widget
            );
        } else {
            blockExtractor = new BlockExtractor(blockPath);
        }
        final BlockInitialized blockElement = blockExtractor.getBlock();
        data.forEach((elementName, text) -> {
            final WebElement element = blockElement.getElementByName(elementName);
            final WebElement typifiedElement;
            if (element instanceof SurveyJSField) {
                typifiedElement = SurveyStepActionsUtils.createTypifiedSurveyInputElement(
                        elementName,
                        blockElement.getElementByName(elementName)
                );
            } else {
                typifiedElement = element;
            }
            Assert.assertNotNull(
                    String.format("Поле %s отсутствует в блоке %s", elementName, blockPath),
                    typifiedElement
            );
            final DriverUtils.ValidatedValue validatedValue = DriverUtils.validateField(
                    typifiedElement,
                    text
            );
            errorCollector.assertTrue(
                    String.format(
                            "Фактическое значение \"%s\" не соответствует ожидаемому \"%s\" в поле \"%s\"",
                            validatedValue.getActualValue(),
                            text,
                            elementName
                    ),
                    validatedValue.getValidateResult()
            );
            if (!validatedValue.getValidateResult() && typifiedElement instanceof TextInput) {
                ClickActions.safeClick("", typifiedElement);
            }
        });
        PageContext.setCurrentPage(currentPage);
        errorCollector.assertAll();
    }

    public static void verifyFieldsDataWidget(final String widgetNamePath, final Map<String, String> data) {
        final Widget widget = (Widget) FindUtils.getElementByNameOrPath(widgetNamePath);
        final ErrorCollector errorCollector = new ErrorCollector();
        widget.init();
        data.forEach((element, text) -> {
            {
              final String decodeText = DataProcessing.decodeValue(text);
                DriverUtils.ValidatedValue validatedValue = DriverUtils.validateField(
                        widget.getElementByName(element),
                        decodeText
                );

                errorCollector.assertTrue(
                        String.format(
                                "Фактическое значение \"%s\" не соответствует ожидаемому \"%s\" в поле \"%s\"",
                                validatedValue.getActualValue(),
                                decodeText,
                                element
                        ),
                        validatedValue.getValidateResult()
                );
            }
        });
        NotCriticalErrorAccumulator.setStepBroken();
        errorCollector.assertAll();
    }

    public static void availabilityInWidget(final String widgetNamePath, final Map<String, String> data) {
        final Widget widget = (Widget) FindUtils.getElementByNameOrPath(widgetNamePath);
        widget.init();
        data.forEach((name, value) -> {
            final WebElement field = widget.getElementByNameOrPath(name);
            final boolean status = field.isEnabled();
            if (!DriverUtils.getBoolean(value) == status) {
                final String message = String.format(
                        "Ожидаемый результат доступности на редактирование элемента \"%s\" \"%s\" не соответствует фактическому \"%s\"",
                        name,
                        value,
                        status
                );
                NotCriticalErrorAccumulator.setNotCriticalError(message);
            }
        });
        NotCriticalErrorAccumulator.setStepBroken();
    }

    public static void fillFieldsInWidget(
            final String widgetNamePath,
            final Map<String, String> data,
            final boolean validated
    ) {

        data.forEach((name, value) -> {
            {
                final Widget widget = (Widget) FindUtils.getElementByNameOrPath(widgetNamePath);
                widget.init();
                try {
                    FieldUtils.fillField(widget.getElementByName(name), value, validated);
                } catch (final FieldFillingException e) {
                    throw new AutotestError("Не удалось заполнить поле " + name, e);
                }
            }
        });
    }


    public static void fillFieldsInBlock(
            final String blockPath,
            final Map<String, String> data,
            final boolean validated
    ) {
        final AtomicReference<BlockInitialized> block = new AtomicReference<>();
        final BlockExtractor blockElement;
        final Page currentPage = PageContext.getCurrentPage();
        // Если список блоков в виджете, то сначала выделяем виджет, а потом из него получаем список блоков
        if (FindUtils.isWidget(blockPath)) {
            final String[] parts = blockPath.split("->", 2);
            final Widget widget = WidgetExtractor.getWidget(parts[0]);
            PageContext.setCurrentPage(widget);
            blockElement = new BlockExtractor(
                    parts[1],
                    widget
            );
        } else {
            blockElement = new BlockExtractor(blockPath);
        }
        block.set(blockElement.getBlockWithWait(DriverConstants.ELEMENT_WAIT_5SEC));
        final AtomicReference<WebElement> element = new AtomicReference<>();
        for (final Map.Entry<String, String> entry : data.entrySet()) {
            final BooleanSupplier waitWhenFieldBeFilled = () -> {
                try {
                    element.set(block.get().getElementByName(entry.getKey()));
                    if (element.get() == null) {
                        return false;
                    }
                    FieldUtils.fillField(element.get(), entry.getValue(), validated);
                } catch (final StaleElementReferenceException e) {
                    block.set(blockElement.getBlockWithWait(DriverConstants.ELEMENT_WAIT_5SEC));
                    return false;
                } catch (final FieldFillingException e) {
                    throw new AutotestError("Не удалось заполнить поле " + entry.getKey(), e);
                }
                return true;
            };
            Timer.executeTimerThrowable(
                    DriverConstants.TIMEOUT,
                    String.format("Не удалось заполнить поле \"%s\"", entry.getKey()),
                    waitWhenFieldBeFilled
            );
        }
        PageContext.setCurrentPage(currentPage);
    }

    public static void checkBlocksTotalCount(
            final BlockExtractor block,
            final String predicate,
            final String blocksQty
    ) {
        final AtomicInteger actualBlockCount = new AtomicInteger(0);
        final int expectedBlockCount = Integer.parseInt(DataProcessing.decodeValue(blocksQty));
        final AtomicReference<Throwable> throwable = new AtomicReference<>();
        final Validator.PredicateSymbol predicateSymbol = Validator
                .PredicateSymbol.valueOfSymbol("равно".equals(predicate) ? "=" : predicate);
        final BooleanSupplier waitWhenBlockListBeUpdated = () -> {
            try {
                actualBlockCount.set(block.reset().getBlockCollection().size());
                if (Validator.matchByPredicate(actualBlockCount.get(), expectedBlockCount, predicateSymbol)) {
                    return true;
                } else {
                    throwable.set(null);
                }
            } catch (final Throwable e) {
                throwable.set(e);
            }
            return false;
        };

        final boolean result = Timer.executeTimer(DriverConstants.ELEMENT_WAIT_5SEC, waitWhenBlockListBeUpdated);

        if (!result) {
            final String message;
            if (null == throwable.get()) {
                message = String.format(
                        "Количество найденных блоков '%d' не соответствует ожидаемому %s '%d'",
                        actualBlockCount.get(),
                        predicate,
                        expectedBlockCount
                );
            } else {
                message = throwable.get().getMessage();
            }
            throw new AutotestError(message);
        }
    }

    public static void checkTextHint(final Map<String, String> data) {
        final String buttonHint = ".//div[contains(@class, 'HintTooltipContainer') or @data-testid ='UIKit.Hint.Container']";
        final String hint = ".//div[contains(@class,'_HintWrapper') or contains(@class, 'tippy-popper')]";
        final ErrorCollector errorCollector = new ErrorCollector();
        final List<WebElement> listHint = new ArrayList<>();
        for (final Map.Entry<String, String> dataEntry : data.entrySet()) {
            final String elementName = dataEntry.getKey();
            final WebElement element = FindUtils.getElementByNameOrPath(DataProcessing.decodeValue(dataEntry.getKey()));
            final WebElement button = FieldUtils.getHintButton(element, buttonHint);
            ClickActions.safeClick(elementName, button);
            final BooleanSupplier waitWhenBlockListBeUpdated = () -> {
                listHint.clear();
                listHint.addAll(button.findElements(By.xpath(hint)));
                return !"".equals(listHint.get(0).getText());
            };
            final String errorMassage = String.format("Подсказка для элемента \"%s\" не найдена", elementName);
            Timer.executeTimerThrowable(
                    DriverConstants.ELEMENT_WAIT_5SEC,
                    errorMassage,
                    waitWhenBlockListBeUpdated
            );
            final String textHint = listHint.get(0).getText().replaceAll("\n", " ");
            ClickActions.safeClick(dataEntry.getKey(), button);
            errorCollector.assertEquals(
                    String.format(
                            "\"%s\"\n не соответствует ожидаемому \"%s\"\nв подсказке \"%s\"",
                            textHint,
                            dataEntry.getValue(),
                            elementName
                    ),
                    textHint,
                    dataEntry.getValue()
            );
        }
        errorCollector.assertAll();
    }

    public static void checkTextWarning(final Map<String, String> data, final Widget widget) {
        final ErrorCollector errorCollector = new ErrorCollector();
        for (final Map.Entry<String, String> dataEntry : data.entrySet()) {
            final WebElement field = widget !=
                                     null ? widget.getElementByName(dataEntry.getKey()) : FindUtils.getElementByNameOrPath(
                    dataEntry.getKey());
            final String massage = String.format(
                    "Для элемента \"%s\" не описан метод getTextWarning",
                    field.getClass()
            );
            Assert.assertTrue(massage, field instanceof Warning);
            if ("".equals(dataEntry.getValue())) {
                ((Warning) field).notTextWarning();
            } else {
                final String textWarning = ((Warning) field).getTextWarning()
                                                            .orElseThrow(() -> new AutotestError(String.format(
                                                                    "Предупреждение для поля \"%s\" не обнаружено на экране",
                                                                    dataEntry.getKey()
                                                            )));
                final String expectedWarning = dataEntry.getValue();
                errorCollector.assertEquals(
                        String.format(
                                "Предупреждение : \"%s\" не соответствует ожидаемому : \"%s\"",
                                textWarning,
                                expectedWarning
                        ),
                        textWarning,
                        expectedWarning
                );
            }

        }
        errorCollector.assertAll();
    }


    private static WebElement getHintButton(final WebElement element, final String xpathButtonHint) {
        final List<WebElement> elements = DriverUtils.searchParenElements(element, xpathButtonHint);
        if (elements.size() > 1) {
            throw new AutotestError(String.format(
                    "Найдено больше 1 кнопки подсказки для элемента \"%s\"",
                    element
            ));
        }
        return elements.get(0);
    }
    public static boolean checkCountSymbol(final int countSymbol, final String predicate, final String blocksQty){
        final Validator.PredicateSymbol predicateSymbol = Validator
                .PredicateSymbol.valueOfSymbol("равно".equals(predicate) ? "=" : predicate);
        return Validator.matchByPredicate(
                (countSymbol),
                (getCountSymbolInString(blocksQty)),
                predicateSymbol
        );
    }

    public static int getCountSymbolInString(final String text){
        try {
            return Integer.parseInt(DataProcessing.decodeValue(text));
        }catch (NumberFormatException e){
            throw new AutotestError(String.format("Невозможно распарсить значение \"%s\"", text));
        }
    }
}
