package ru.sbt.edu_power.e2e_core.step_defs;

import cucumber.api.java.ru.И;
import cucumber.api.java.ru.Когда;
import io.qameta.allure.Allure;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.MoveTargetOutOfBoundsException;
import ru.sbt.edu_power.e2e_core.actions.ClickActions;
import ru.sbt.edu_power.e2e_core.actions.ClipboardUtils;
import ru.sbt.edu_power.e2e_core.actions.DragAndDrop;
import ru.sbt.edu_power.e2e_core.actions.Mover;
import ru.sbt.edu_power.e2e_core.actions.PageControls;
import ru.sbt.edu_power.e2e_core.allure.AllureUtils;
import ru.sbt.edu_power.e2e_core.api.requests.APIAuth;
import ru.sbt.edu_power.e2e_core.api.requests.APIRequest;
import ru.sbt.edu_power.e2e_core.api.requests.APIResponse;
import ru.sbt.edu_power.e2e_core.api.requests.ParametrizedRequest;
import ru.sbt.edu_power.e2e_core.auth.AuthAction;
import ru.sbt.edu_power.e2e_core.blocks.BlockExtractor;
import ru.sbt.edu_power.e2e_core.blocks.BlockExtractorException;
import ru.sbt.edu_power.e2e_core.blocks.BlockInitialized;
import ru.sbt.edu_power.e2e_core.blocks.NoElementFoundInBlockContext;
import ru.sbt.edu_power.e2e_core.blocks.PathBuilder;
import ru.sbt.edu_power.e2e_core.blocks.abstract_blocks.MarkableListItem;
import ru.sbt.edu_power.e2e_core.blocks.system.SystemNotificationListItem;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbt.edu_power.e2e_core.data.ImageProcessing;
import ru.sbt.edu_power.e2e_core.data.UrlProcessing;
import ru.sbt.edu_power.e2e_core.devtools.CheckingRequests;
import ru.sbt.edu_power.e2e_core.devtools.DevTools;
import ru.sbt.edu_power.e2e_core.devtools.emulation.TimeZone;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.e2e_core.elements.DateSwitcher;
import ru.sbt.edu_power.e2e_core.elements.FieldFillingException;
import ru.sbt.edu_power.e2e_core.elements.QRCode;
import ru.sbt.edu_power.e2e_core.elements.SvgIcon;
import ru.sbt.edu_power.e2e_core.elements.checkbox.CheckBoxGroup;
import ru.sbt.edu_power.e2e_core.elements.date_select.DateSelect;
import ru.sbt.edu_power.e2e_core.elements.dropdown.Select;
import ru.sbt.edu_power.e2e_core.elements.formatting_text_input.FormattingTextInput;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Available;
import ru.sbt.edu_power.e2e_core.elements.ifaces.HasSpecificProperties;
import ru.sbt.edu_power.e2e_core.elements.pseudo_element.AbstractPseudoElement;
import ru.sbt.edu_power.e2e_core.elements.text_input.TextInput;
import ru.sbt.edu_power.e2e_core.email.EmailUtils;
import ru.sbt.edu_power.e2e_core.email.MailHandler;
import ru.sbt.edu_power.e2e_core.error_processing.ErrorCollector;
import ru.sbt.edu_power.e2e_core.error_processing.NotCriticalErrorAccumulator;
import ru.sbt.edu_power.e2e_core.fields.FieldUtils;
import ru.sbt.edu_power.e2e_core.fields.FindUtils;
import ru.sbt.edu_power.e2e_core.layout.LayoutTestExecutor;
import ru.sbt.edu_power.e2e_core.layout.LayoutUtils;
import ru.sbt.edu_power.e2e_core.layout.enums.DimensionEnum;
import ru.sbt.edu_power.e2e_core.layout.enums.TestingMode;
import ru.sbt.edu_power.e2e_core.remote_access.BrowserUtils;
import ru.sbt.edu_power.e2e_core.remote_access.Moon;
import ru.sbt.edu_power.e2e_core.smoke.RoutEvaluator;
import ru.sbt.edu_power.e2e_core.survey.SurveyExecutor;
import ru.sbt.edu_power.e2e_core.table_processing.TableActions;
import ru.sbt.edu_power.e2e_core.test_runner.WebDriverSwitcher;
import ru.sbt.edu_power.e2e_core.widgets.Widget;
import ru.sbt.edu_power.e2e_core.widgets.WidgetExtractor;
import ru.sbt.edu_power.external_services.ESUtils;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbt.edu_power.external_services.validator.Validator;
import ru.sbtqa.tag.datajack.Stash;
import ru.sbtqa.tag.pagefactory.Page;
import ru.sbtqa.tag.pagefactory.context.PageContext;
import ru.sbtqa.tag.pagefactory.environment.Environment;
import ru.sbtqa.tag.pagefactory.exception.ElementSearchError;
import ru.sbtqa.tag.pagefactory.exceptions.AllureNonCriticalError;
import ru.sbtqa.tag.pagefactory.web.junit.WebSteps;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class E2eCoreStepDefs {

    /**
     * Метод выполняет переход на адрес стенда, заполняет поля логина и пароля и жмёт кнопку входа.
     * Логин и пароль выбираются из users.json по названию профиля пользователя
     *
     * @param profileName название профиля пользователя из users.json
     */
    @Когда("^авторизуется на сайте под учетной записью \"([^\"]*)\"$")
    public void authWithProfileName(final String profileName) {
        AuthAction.authWithProfileName(profileName);
    }


    /**
     * Метод выполняет переход на адрес стенда, заполняет поля логина и пароля и жмёт кнопку входа.
     * Логин и пароль передаются напрямую в шаге, допускается использование данных из стэша
     *
     * @param userLogin    логин
     * @param userPassword пароль
     */
    @Когда("^авторизуется на сайте под логином \"([^\"]*)\" и паролем \"([^\"]*)\"$")
    public void authWithLoginAndPassword(final String userLogin, final String userPassword) {
        AuthAction.authWithLoginAndPassword(userLogin, userPassword);
    }

    /**
     * Метод кликает по кнопке Logout главного меню
     */
    @Когда("^выходит из системы$")
    public void logout() throws MalformedURLException {
        AuthAction.logout();
    }

    @Когда("^выходит из системы через меню$")
    public void logoutByMenu() {
        AuthAction.logoutByMenu();
    }


    /**
     * Вызывается метод ожидания завершения загрузки данных
     */
    @Когда("^ждет когда исчезнет прелоадер$")
    public void waitWhileLoading() {
        PageControls.waitWhileNetworkActive();
        PageControls.waitWhenPreloaderIsGone();
    }

    /**
     * Метод останавливает поток на указанное количество секунд
     *
     * @param waitInSeconds длительность остановки в секундах
     */
    @Когда("^ожидает \"([^\"]*)\" (?:секунд|секунду|секунды)$")
    public void wait(final String waitInSeconds) {
        try {
            DriverUtils.freeze(Long.parseLong(waitInSeconds) * DriverConstants.CONVERT_TO_MILLISECONDS);
        } catch (final NumberFormatException ignored) {
            throw new AutotestError(String
                    .format("\"%s\" не является корректной записью для времени задержки. " +
                            "Допустимо использовать только цифры", waitInSeconds));
        }
    }

    @Когда("^добавляет параметры текущему url \"([^\"]*)\"$")
    public void addParamAndGoToUrl(final String param) {
        Environment.getDriverService().getDriver().get(UrlProcessing.addUrlParam(param));
    }

    @Когда("^проверяет что количество вкладок равно \"([^\"]*)\"$")
    public void addParamAndGoToUrl(final Integer count) {
        final int actualCountTab = Environment.getDriverService().getDriver().getWindowHandles().size();
        Assert.assertEquals(String
                .format(
                        "Ожидаемое количество открытых вкладок \"%s\", не соответствует актуальному \"%s\"",
                        count,
                        actualCountTab
                ), (int) count, actualCountTab);

    }

    /**
     * Метод опрашивает поля формы на странице и сверяет их значения с ожидаемыми
     * Названия полей и ожидаемые значения передаются через дататэйбл
     * <p>
     * И пользователь проверяет что поля заполнены данными
     * | Название     | Среды жизни |
     * | Длительность | 10          |
     *
     * @param data таблица полей и ожидаемых значений
     */
    @Когда("^проверяет что поля заполнены данными$")
    public void verifyFieldsData(final Map<String, String> data) {
        final ErrorCollector errorCollector = new ErrorCollector();
        for (final Map.Entry<String, String> dataEntry : data.entrySet()) {
            final String fieldName = dataEntry.getKey();
            final String expected = dataEntry.getValue();
            WebElement element = DriverUtils.getElementByTitle(fieldName);
            DriverUtils.ValidatedValue validatedValue = DriverUtils.validateField(element, expected);

//            TODO: Обходим проблему с CKEditor https://gw.tools.pcbl.ru/jira/browse/EDU-8122
            if (element instanceof FormattingTextInput && !validatedValue.getValidateResult()) {
                PageControls.refreshPage();
                element = DriverUtils.getElementByTitle(fieldName);
                validatedValue = DriverUtils.validateField(element, expected);
            }
            errorCollector.assertTrue(
                    String.format(
                            "Фактическое значение \"%s\" не соответствует ожидаемому \"%s\" в поле \"%s\"",
                            validatedValue.getActualValue(),
                            expected,
                            fieldName
                    ),
                    validatedValue.getValidateResult()
            );
            if (!validatedValue.getValidateResult() && element instanceof TextInput) {
                ClickActions.safeClick("", element);
            }
        }
        NotCriticalErrorAccumulator.setStepBroken();
        errorCollector.assertAll();
    }

    /**
     * Метод заполняет в поля формы переданные данные
     * Поля и данные передаются через дататэйбл
     * <p>
     * И пользователь заполняет поля данными
     * | Название     | Среды жизни |
     * | Длительность | 10          |
     *
     * @param data таблица полей и данных для заполнения
     */
    @Когда("^заполняет поля данными$")
    public void fillFields(final Map<String, String> data) {
        FieldUtils.fillFields(data, true);
        DriverUtils.freeze(DriverConstants.FREEZE_500_MS);
    }

    @Когда("^заполняет поля данными без проверки$")
    public void fillFieldsNotValidated(final Map<String, String> data) {
        FieldUtils.fillFields(data, false);
    }

    @Когда("^заполняет фильтры$")
    public void fillFilters(final Map<String, String> data) {
        FieldUtils.fillFields(data, true);
        PageControls.waitWhileNetworkActive();
        PageControls.detectBackendErrors();
    }

    @Когда("^проверяет доступность элемента на редактирование$")
    public void checkAvailability(final Map<String, String> data) {
        checkAvailabilityInBlock(null, data);
    }

    @Когда("^проверяет доступность элемента на редактирование в блоке \"([^\"]*)\"$")
    public void checkAvailabilityInBlock(final String blockPath, final Map<String, String> data) {
        final BlockInitialized blockElement = blockPath == null ? null : new BlockExtractor(blockPath).getBlock();
        for (final Map.Entry<String, String> dataEntry : data.entrySet()) {
            final String path = dataEntry.getKey();
            WebElement field;
            if (FindUtils.isWidget(path)) {
                final Widget widget = (Widget) FindUtils.getElementByNameOrPath(path.split("->")[0]);
                widget.init();
                field = widget.getElementByName(path.split("->")[1]);
            } else {
                field = blockElement == null ?
                        DriverUtils.getElementByTitle(dataEntry.getKey()) : blockElement.getElementByName(
                        dataEntry.getKey());
            }
            if (field instanceof Available) {
                final boolean status = ((Available) field).isAvailable();
                if (!DriverUtils.getBoolean(dataEntry.getValue()) == status) {
                    final String message = String.format(
                            "Ожидаемый результат доступности на редактирование элемента \"%s\" \"%s\" не соответствует фактическому \"%s\"",
                            dataEntry.getKey(),
                            dataEntry.getValue(),
                            status
                    );
                    NotCriticalErrorAccumulator.setNotCriticalError(message);
                }
            } else {
                throw new AutotestError(String.format(
                        "Для элемента \"%s\" не описан метод isDisable",
                        field.getClass()
                ));
            }
        }
        NotCriticalErrorAccumulator.setStepBroken();
    }

    /**
     * @param request - вхождение запроса
     * @param data    - параметры в запросе
     */
    @Когда("^проверяет запрос с вхождением \"([^\"]*)\" на наличие параметров$")
    public void checkRequestParam(final String request, final Map<String, String> data) {
        new CheckingRequests().checkRequests(request, data);
    }

    @Когда("^проверяет наличие текста на странице$")
    public void elementIsDisplayed(final Map<String, String> data) {
        final ErrorCollector errorCollector = new ErrorCollector();
        final WebDriver driver = Environment.getDriverService().getDriver();
        for (final String text : data.keySet()) {
            final String decodeText = DataProcessing.decodeValue(text);
            final List<WebElement> textElements;
            final boolean result;
            if (text.startsWith("*") || text.endsWith("*")) {
                textElements = driver.findElements(
                        By.xpath("//*[contains(text(),'" + decodeText.replaceAll("\\*", "") + "')]"));
                result = Validator.matchValueInList(textElements
                        .stream()
                        .map(WebElement::getText)
                        .collect(Collectors.toList()), decodeText);
            } else {
                textElements = driver.findElements(
                        By.xpath("//*[text()='" + decodeText + "']"));
                result = !textElements.isEmpty();
            }

            if (DriverUtils.getBoolean(data.get(text))) {
                errorCollector.assertTrue(
                        String.format(
                                "Текст \"%s\" не найден на странице", decodeText),
                        result
                );
            } else {
                errorCollector.assertFalse(
                        String.format(
                                "Текст \"%s\" не должен отображаться на странице", decodeText),
                        result
                );
            }
        }
        errorCollector.assertAll();
    }

    /**
     * Метод заполняет поля в блоке. Отличие от предыдущего метода в том, что на форме может быть
     * динамически добавляемые одинаковые блоки с полями и в этом методе можно сослаться на конкретный блок
     * <p>
     * И пользователь заполняет поля данными в блоке "Элементы цели->4"
     * | Описание элемента цели для ученика | Тестовое описание элемента цели для ученика 4 |
     * | Описание элемента цели для учителя | Тестовое описание элемента цели для учителя 4 |
     * | Пример для ученика                 | Тестовый пример для ученика 4                 |
     *
     * @param blockPath путь до блока
     * @param data      таблица полей и данных для заполнения
     */
    @Когда("^заполняет поля данными в блоке \"([^\"]*)\"$")
    public static void fillFieldsInBlock(final String blockPath, final Map<String, String> data) {
        FieldUtils.fillFieldsInBlock(blockPath, data, true);
    }

    @Когда("^заполняет поля данными в блоке \"([^\"]*)\" без проверки$")
    public static void fillFieldsInBlockWithoutValidate(final String blockPath, final Map<String, String> data) {
        FieldUtils.fillFieldsInBlock(blockPath, data, false);
    }

    @Когда("^заполняет поля данными в виджете \"([^\"]*)\"$")
    public static void fillFieldsInWidget(final String widget, final Map<String, String> data) {
        FieldUtils.fillFieldsInWidget(widget, data, true);
    }

    @Когда("^заполняет поля данными в виджете \"([^\"]*)\" без проверки$")
    public static void fillFieldsInWidgetWithoutValidat(final String widget, final Map<String, String> data) {
        FieldUtils.fillFieldsInWidget(widget, data, false);
    }

    @Когда("^проверяет доступность элементов на редактирование в виджете \"([^\"]*)\"$")
    public void checkAvailabilityInWidget(final String widgetNamePath, final Map<String, String> data) {
        FieldUtils.availabilityInWidget(widgetNamePath, data);
    }

    /**
     * Метод опрашивает поля формы в блоке и сверяет их значения с ожидаемыми
     * Названия полей и ожидаемые значения передаются через дататэйбл
     * <p>
     * И пользователь проверяет что поля заполнены данными в блоке "Элементы цели->4"
     * | Описание элемента цели для ученика | Тестовое описание элемента цели для ученика 4 |
     * | Описание элемента цели для учителя | Тестовое описание элемента цели для учителя 4 |
     * | Пример для ученика                 | Тестовый пример для ученика 4                 |
     *
     * @param data таблица полей и ожидаемых значений
     */
    @Когда("^проверяет что поля заполнены данными в блоке \"([^\"]*)\"$")
    public void verifyFieldsDataInBlock(final String blockPath, final Map<String, String> data) {
        FieldUtils.verifyFieldsDataBlock(blockPath, data);
    }

    /**
     * Метод опрашивает поля формы в виджете и сверяет их значения с ожидаемыми
     * Названия полей и ожидаемые значения передаются через дататэйбл
     * <p>
     * И пользователь проверяет что поля заполнены данными в блоке "Виджет Аудио->4"
     * | Название аудио | Тестовое описание элемента цели для ученика 4 |
     *
     * @param data таблица полей и ожидаемых значений
     */
    @Когда("^проверяет что поля заполнены данными в виджете \"([^\"]*)\"$")
    public void verifyFieldsDataInWidget(final String widgetNamePath, final Map<String, String> data) {
        FieldUtils.verifyFieldsDataWidget(widgetNamePath, data);
    }

    @Когда("^проверяет что в виджете \"([^\"]*)\" количество блоков \"([^\"]*)\" (равно|=|>|<|>=|<=|!=) \"([^\"]*)\"$")
    public void checkBlocksTotalCountInWidget(
            final String widgetNamePath,
            final String blockName,
            final String predicate,
            final String blocksQty
    ) {
        final Widget widget = WidgetExtractor.getWidget(widgetNamePath);
        final Page page = PageContext.getCurrentPage();
        PageContext.setCurrentPage(widget);
        FieldUtils.checkBlocksTotalCount(new BlockExtractor(blockName, widget), predicate, blocksQty);
        PageContext.setCurrentPage(page);
    }

    /**
     * Метод выполняет клик по элементу, находящемуся в блоке
     *
     * @param blockName название или путь до блока
     */
    @Когда("^нажимает на (?:скрытый|скрытую) (?:кнопку|элемент|ссылку) \"([^\"]*)\" в блоке$")
    public void clickInHideBlockElement(final String blockName) {
        final AtomicReference<WebElement> element = new AtomicReference<>();
        final BlockExtractor block = new BlockExtractor(blockName);
        final String elementName = block.getBlockFieldName();
        final WebElement body = DriverUtils.findElementsOnPageByXpath("//body").get(0);
        final BooleanSupplier waitWhenHiddenElementBeDisplayed = () -> {
//        Для фикса проблемы, когда скрытый элемент не появляется на элементе, на котором указатель уже был до начала этого шага
            Mover.getActions().moveToElement(body, 10, 10).build().perform();
            DriverUtils.freeze(DriverConstants.FREEZE_250_MS);
            try {
                Mover.moveToElement(
                        block.getBlockWithWait(DriverConstants.ELEMENT_WAIT_5SEC),
                        true
                );
                Mover.markMousePosition();
                DriverUtils.freeze(DriverConstants.FREEZE_250_MS);
                element.set(block.reset().getElement());
                return DriverUtils.isElementDisplayed(element.get());
            } catch (final MoveTargetOutOfBoundsException ignored) {
                AllureUtils.attachScreenShotToAllure("После наведения скрытый элемент не появился");
                Mover.scrollToElement(block.getBlock());
            }
            return false;
        };

        final String message = String.format("Не удалось получить элемент \"%s\"", elementName);

        Timer.executeTimerThrowable(DriverConstants.ELEMENT_WAIT_5SEC, message, waitWhenHiddenElementBeDisplayed);

        Mover.moveToElement(element.get(), false);

        ClickActions.clickWithChangeControl(elementName, element.get());
    }

    @Когда("^вызывает подсказку на элементе \"([^\"]*)\"$")
    public void moveElement(final String element) {
        Mover.moveToElement(FindUtils.getElementByNameOrPath(DataProcessing.decodeValue(element)));
        DriverUtils.freeze(DriverConstants.FREEZE_500_MS);
    }

    /**
     * Метод считывает предупреждение и проверяет его на эквивалентность с ожидаемым значением
     * Для проверки отсутствия предупреждения нужно передать в value  = "" пустую строку
     *
     * @param data key = элемент, value = текст предупреждения
     */

    @Когда("^проверяет текст предупреждения на эквивалентность$")
    public void checkTextWarning(final Map<String, String> data) {
        FieldUtils.checkTextWarning(data, null);
    }

    @Когда("^проверяет текст предупреждения на эквивалентность в виджете \"([^\"]*)\"$")
    public void checkTextWarningInWidget(final String widgetName, final Map<String, String> data) {
        final Widget widget = (Widget) FindUtils.getElementByNameOrPath(widgetName);
        widget.init();
        FieldUtils.checkTextWarning(data, widget);
    }

    /**
     * Метод используется для случаев, когда элемент не описан в PageObject и по нему нужно кликнуть.
     * Например при динамическом добавлении элементов
     *
     * @param elementTitle Текст в элементе, по которому нужно кликнуть. Используется полное совпадение
     */
    @Когда("^находит и нажимает на (?:кнопку|элемент|ссылку|вкладку) \"([^\"]*)\"$")
    public void findAndClick(String elementTitle) {
        elementTitle = DataProcessing.decodeValue(elementTitle);
        try {
            final List<WebElement> elements = Environment
                    .getDriverService()
                    .getDriver()
                    .findElements(By.xpath("//*[text() = '" + elementTitle + "']"));
            final Map<String, WebElement> map = new HashMap<>();
            for (final WebElement element : elements) {
                map.put(element.getTagName(), element);
            }
            // На странице может оказаться несколько элементов с таким содержимым,
            // поэтому сначала нужно попытаться нажать на ссылку или кнопку и если таковых не нашлось,
            // тогда жмём на первый попавшийся элемент
            if (map.containsKey("a")) {
                ClickActions.clickWithChangeControl(elementTitle, map.get("a"));
                return;
            }
            if (map.containsKey("button")) {
                ClickActions.clickWithChangeControl(elementTitle, map.get("button"));
                return;
            }
            ClickActions.clickWithChangeControl(elementTitle, elements.get(0));
            waitWhileLoading();
        } catch (final NoSuchElementException ignored) {
            throw new AutotestError(String.format("Элемент \"%s\" не найден на странице \"%s\"",
                    elementTitle, PageContext.getCurrentPage().getTitle()
            ));
        }
    }


    @Когда("^проверяет текст подсказки$")
    public void checkHint(final Map<String, String> data) {
        FieldUtils.checkTextHint(data);
    }

    /**
     * Метод проверяет наличие элемента в блоке по пути до элемента
     * И пользователь проверяет наличие элемента в блоке "Название блока->Поле блока#содержимое->Кнопка создания" в блоке
     *
     * @param elementTitleOrPath путь до элемента в блоке
     */
    @Когда("^проверяет наличие (?:кнопки|элемента|ссылки|вкладки) \"([^\"]*)\"$")
    public void checkExistsElementInBlock(final String elementTitleOrPath) {
        Assert.assertNotNull(
                String.format("Элемент \"%s\" отсутствует на странице", elementTitleOrPath),
                FindUtils.getElementByNameOrPath(elementTitleOrPath)
        );
    }

    /**
     * Метод позволять проводить вычисления между двумя значениями и записывать их в стеш
     * И запоминает результат вычисления "stash#value1" прибавить "stash#value2" в переменную "ожидБаллы"
     *
     * @param value1    значение 1
     * @param operator  арифметическая операция
     * @param value2    значение 2
     * @param stashName переменная в которую будет записан результат вычисления
     */
    @Когда("^запоминает результат вычисления \"([^\"]*)\" (прибавить|отнять|разделить|умножить) \"([^\"]*)\" в переменную \"([^\"]*)\"$")
    public void valueCalculator(
            final String value1,
            final String operator,
            final String value2,
            final String stashName
    ) {
        try {
            final Integer decodeValue1 = Integer.parseInt(DataProcessing.decodeValue(value1));
            final Integer decodeValue2 = Integer.parseInt(DataProcessing.decodeValue(value2));

            switch (operator) {
                case ("прибавить"):
                    Stash.put(stashName, decodeValue1 + decodeValue2);
                    break;
                case ("отнять"):
                    Stash.put(stashName, decodeValue1 - decodeValue2);
                    break;
                case ("разделить"):
                    if (decodeValue2 == 0) {
                        throw new AutotestError("Деление на 0 запрещено");
                    }
                    Stash.put(stashName, decodeValue1 / decodeValue2);
                    break;
                case ("умножить"):
                    Stash.put(stashName, decodeValue1 * decodeValue2);
                    break;
                default:
                    throw new AutotestError("Не верно выбран оператор: " + operator);
            }
        } catch (final NumberFormatException e) {
            throw new AutotestError("Проверьте значения, одно из значений не цифрового формата", e);
        }
    }

    @Когда("^проверяет наличие (?:кнопки|элемента|ссылки|вкладки) \"([^\"]*)\" с ожиданием \"([^\"]*)\" секунд с перезагрузкой$")
    public void waitCheckExistsElementInBlock(final String elementTitleOrPath, final int wait) {
        FindUtils.waitCheckExistsElement(elementTitleOrPath, wait, null, true, true);
    }

    @Когда("^проверяет содержимое элемента \"([^\"]*)\" на эквивалентность \"([^\"]*)\" в течении \"([^\"]*)\" секунд с перезагрузкой$")
    public void checkElementDataWithPageReload(final String nameOrPath, final String data, final int seconds) {
        final AtomicReference<DriverUtils.ValidatedValue> validatedValue = new AtomicReference<>();
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final BooleanSupplier waitWhenDataBePresent = () -> {
            error.set(null);
            try {
                final WebElement element = FindUtils.getElementByNameOrPath(nameOrPath);
                validatedValue.set(DriverUtils.validateField(element, data));
                if (validatedValue.get().getValidateResult()) {
                    return true;
                }
            } catch (final ElementSearchError | IllegalArgumentException e) {
                throw new AutotestError(e);
            } catch (final Throwable e) {
                error.set(e);
            }
            return false;
        };
        final boolean result = Timer.executeTimer(seconds, waitWhenDataBePresent);
        if (!result) {
            if (Objects.nonNull(error.get())) {
                throw new AutotestError(error.get());
            }
            throw new AutotestError(String.format(
                    "Текущее значение '%s' в элементе '%s' не соответствует ожидаемому '%s'",
                    validatedValue.get().getActualValue(),
                    nameOrPath,
                    DataProcessing.decodeValue(data)
            ));
        }
    }

    @Когда("^переходит в меню \"([^\"]*)\"$")
    public void clickFromMenu(final String elementTitleOrPath) {
        PageControls.waitWhileNetworkActive();
        PageControls.waitWhenPreloaderIsGone();
        ClickActions.clickElementIfExist("Кнопка меню");
        final WebElement elementMenu = FindUtils.getElementByNameOrPath(elementTitleOrPath);
        ClickActions.clickWithChangeControl(elementTitleOrPath, elementMenu);
    }

    @Когда("^ожидает \"([^\"]*)\" секунд доступность элемента \"([^\"]*)\" на редактирование$")
    public void waitAvailableElement(final int wait, final String element) {
        final WebElement field = DriverUtils.getElementByTitle(element);
        if (field instanceof Available) {
            Timer.executeTimerThrowable(
                    wait,
                    String.format("Элемент \"%s\" не стал доступен для редактирование, время вышло", element),
                    ((Available) field)::isAvailable
            );
        } else {
            throw new AutotestError(String.format(
                    "Для элемента \"%s\" не описан метод isDisable",
                    field.getClass()
            ));
        }
    }

    @Когда("^переходит по URL \"([^\"]*)\"$")
    public void goToUrl(final String url) {
        UrlProcessing.goToUrl(url);
    }

    @Когда("^переходит по URL \"([^\"]*)\" на поддомен \"([^\"]*)\"$")
    public void goToUrlWithSubdomain(final String path, final String subdomain) {
        final String urlWithSubdomain = UrlProcessing.urlBuilderWithSubdomain(path, subdomain);
        UrlProcessing.goToUrl(urlWithSubdomain);
    }

    @Когда("^переходит по URL \"([^\"]*)\" без проверки$")
    public void goToUrlWithoutCheck(final String url) {
        UrlProcessing.goToUrlWithoutCheck(url);
    }

    @Когда("^переходит по URL \"([^\"]*)\" без контроля загрузки страницы$")
    public void goToUrlWithoutLoadControl(final String url) {
        UrlProcessing.goToUrlWithoutLoadControl(url);
    }

    @Когда("^сверяет значение URL на эквивалентность \"([^\"]*)\"$")
    public void checkUrl(final String expectedDraft) throws MalformedURLException {
        PageControls.waitWhenPreloaderIsGone();
        final String expected = DataProcessing.decodeValue(expectedDraft).replaceAll("/$", "");
        final String stand = new URL(System.getProperty("webdriver.starting.url")).getHost();
        final URL actualUrl = new URL(Environment.getDriverService().getDriver().getCurrentUrl());
        final String actual = actualUrl.getHost().equals(stand) || !expected.contains("://") ? actualUrl
                .getPath()
                .replaceAll("/$", "") : actualUrl
                .toString()
                .replaceAll("/$", "");
        Assert.assertTrue(
                String.format(
                        "Текущий URL \"%s\" не соответствует ожидаемому \"%s\"",
                        actual,
                        expected

                ),
                Validator.matchValues(actual, expected)
        );
    }

    @И("^сверяет значение URL на паттерн \"([^\"]*)\"$")
    public void checkSubstringUrl(final String expectedDraft) throws MalformedURLException {
        PageControls.waitWhenPreloaderIsGone();
        PageControls.waitWhileNetworkActive();
        final String expected = DataProcessing.decodeValue(expectedDraft);
        final URL actualUrl = new URL(Environment.getDriverService().getDriver().getCurrentUrl());
        final String actual = actualUrl.toString();
        Assert.assertTrue(
                String.format(
                        "Текущий URL \"%s\" не содержит \"%s\"",
                        actual,
                        expected

                ),
                actual.matches(expected)
        );
    }

    @Когда("^проверяет отсутствие элемента \"([^\"]*)\" с ожиданием \"([^\"]*)\" секунд$")
    public void checkNotExistsElementInTimeout(final String elementTitleOrPath, final int timeout) {
        FindUtils.checkNotExistsElementTimeout(elementTitleOrPath, timeout, false);
    }

    @Когда("^проверяет отсутствие элемента \"([^\"]*)\" с ожиданием \"([^\"]*)\" секунд с перезагрузкой$")
    public void checkNotExistsElementInTimeoutAndRefresh(final String elementTitleOrPath, final int timeout) {
        FindUtils.checkNotExistsElementTimeout(elementTitleOrPath, timeout, true);
    }

    @Когда("^нажимает кнопку назад в браузере$")
    public void backPage() {
        WebSteps.getInstance().backPage();
        PageControls.detectConsoleErrors();
        PageControls.detectBackendErrors();
        PageControls.detectUps404Errors();
        PageControls.waitWhenPreloaderIsGone();
    }

    @Когда("^проверяет количество символов в элементе \"([^\"]*)\" (равно|=|>|<|>=|<=|!=) \"([^\"]*)\"$")
    public void checkCountSymbolByElement(
            final String elementTitleOrPath,
            final String predicate,
            final String blocksQty
    ) {
        final int expectedCountSymbol = FindUtils.getElementByNameOrPath(elementTitleOrPath).getText().length();
        boolean result = FieldUtils.checkCountSymbol(expectedCountSymbol, predicate, blocksQty);
        Assert.assertTrue(String.format(
                "Условие не выполняется, фактическое количество символов = \"%s\"",
                expectedCountSymbol
        ), result);
    }

    @Когда("^проверяет количество символов в виджете \"([^\"]*)\" в элементе \"([^\"]*)\" (равно|=|>|<|>=|<=|!=) \"([^\"]*)\"$")
    public void checkCountSymbolByElementInWidget(
            final String widgetName,
            final String elementTitleOrPath,
            final String predicate,
            final String blocksQty
    ) {
        final Widget widget = (Widget) FindUtils.getElementByNameOrPath(widgetName);
        widget.init();
        final int expectedCountSymbol = widget.getElementByName(elementTitleOrPath).getText().length();
        boolean result = FieldUtils.checkCountSymbol(expectedCountSymbol, predicate, blocksQty);
        Assert.assertTrue(String.format(
                "Условие не выполняется, фактическое количество символов = \"%s\"",
                expectedCountSymbol
        ), result);
    }

    /**
     * Метод проверяет отсутствие элемента в блоке по пути до элемента
     * И пользователь проверяет отсутствие элемента в блоке "Название блока->Поле блока#содержимое->Кнопка создания" в блоке
     *
     * @param elementTitleOrPath путь до элемента в блоке
     */
    @Когда("^проверяет отсутствие (?:кнопки|элемента|ссылки|вкладки) \"([^\"]*)\"$")
    public static void checkNotExistsElementInBlock(final String elementTitleOrPath) {
        try {
            final WebElement element = FindUtils.getElementByNameOrPath(elementTitleOrPath, true);
            if (!DriverUtils.isElementDisplayed(element)) {
                return;
            }
            Mover.moveToElement(element, true);
        } catch (final ElementSearchError | IllegalArgumentException | NoElementFoundInBlockContext |
                       BlockExtractorException e) {
            throw new AutotestError(e);
        } catch (final RuntimeException | AssertionError ignored) {
            return;
        }
        throw new AutotestError(String.format(
                "Элемент \"%s\" не должен отображаться на странице",
                elementTitleOrPath
        ));
    }

    /**
     * Метод является альтернативой стандартному методу клика по элементу. Его использование предпочтительно.
     * В нём добавлено ожидание загрузки данных после клика, так как это типичная ситуация для многих кнопок и ссылок
     * Дополнительно выполняется проверка наличия изменений на странице после клика. Если изменения не произошли,
     * будет выполнен повторный клик и так в цикле три раза, после чего будет выкинуто исключение о том, что
     * клик по элементу не срабатывает
     *
     * @param elementTitle название элемента, по которому нужно выполнить клик
     */
    @Когда("^нажимает на (?:кнопку|элемент|ссылку|вкладку) \"([^\"]*)\"$")
    public void clickWithLoadControl(final String elementTitle) {
        final WebElement element = FindUtils.getElementByNameOrPath(elementTitle);
        ClickActions.clickWithChangeControl(elementTitle, element);
    }

    /**
     * Шаг выполняет нажатие на элемент, если элемент присутствует на странице
     *
     * @param elementTitle xpath элемента
     */
    @Когда("^нажимает на (?:кнопку|элемент|ссылку|вкладку) \"([^\"]*)\" если существует$")
    public void clickIfExist(final String elementTitle) {
        try {
            final WebElement element = FindUtils.getElementByNameOrPath(elementTitle, true);
            if (DriverUtils.isElementDisplayed(element)) {
                ClickActions.clickWithChangeControl(elementTitle, element);
            }
        } catch (final AllureNonCriticalError e) {
            throw new AllureNonCriticalError(e);
        } catch (final RuntimeException | AssertionError ignored) {
        }
    }

    @Когда("^нажимает на (?:кнопку|элемент|ссылку|вкладку) \"([^\"]*)\" с ожиданием \"([^\"]*)\" секунд$")
    public void waitClickWithLoadControl(final String elementTitle, final int wait) {
        FindUtils.waitCheckExistsElement(elementTitle, wait, null, true, true);
        final WebElement element = FindUtils.getElementByNameOrPath(elementTitle);
        ClickActions.clickWithChangeControl(elementTitle, element);
    }

    @Когда("^родитель выбирает ребенка по ФИО \"([^\"]*)\"$")
    public void choosingChild(final String elementTitle) {
        final String childPath = "Список детей пользователя->ФИО ребенка#" + elementTitle;
        clickFromMenu("Список переключателей профиля->1");
        final String choosingChildPath = childPath + "->Признак выбранного ребенка";
        final WebElement choosingChildElement = FindUtils.getElementByNameOrPath(choosingChildPath);
        if (null == choosingChildElement) {
            final WebElement element = FindUtils.getElementByNameOrPath(childPath);
            ClickActions.clickWithChangeControl(childPath, element);
        }
    }


    @Когда("^нажимает на (?:кнопку|элемент|ссылку|вкладку) \"([^\"]*)\" без проверки$")
    public void click(final String elementTitle) {
        ClickActions.safeClick("", FindUtils.getElementByNameOrPath(elementTitle));
        DriverUtils.freeze(DriverConstants.FREEZE_250_MS);
        PageControls.detectBackendErrors();
        PageControls.detectUps404Errors();
    }

    @Когда("^нажимает на элемент \"([^\"]*)\" со смещением x,y \"([^\"]*)\"$")
    public void clickWithOffset(final String elementTitle, final String offset) {
        ClickActions.clickElementWithOffset(FindUtils.getElementByNameOrPath(elementTitle), offset);
    }


    @Когда("^заполняет элемент \"([^\"]*)\" датой \"([^\"]*)\" с помощью (Поля|Календаря)$")
    public void selectiveFillingDate(final String element, final String date, final String method)
            throws FieldFillingException {
        DateSelect dateSelect;
        if (FindUtils.isWidget(element)) {
            Widget widget = (Widget) FindUtils.getElementByNameOrPath(element.split("->")[0]);
            widget.init();
            dateSelect = (DateSelect) widget.getElementByName(element.split("->")[1]);
        } else {
            dateSelect = DriverUtils.getElementByTitle(element);
        }
        dateSelect.selectiveFilling(date, method, true);
    }

    @Когда("^проверяет доступность элементов в виджете \"([^\"]*)\"$")
    public void checkElementAvailabilityInWidget(final String widgetName, final Map<String, String> data) {
        final Widget widget = (Widget) FindUtils.getElementByNameOrPath(widgetName);
        widget.init();
        data.forEach((name, state) -> {
            try {
                widget.getElementByNameOrPath(name);
            } catch (final AutotestError e) {
                if (!(e.getCause() instanceof NoSuchElementException)) {
                    throw new AutotestError(e);
                }
                if (DriverUtils.getBoolean(state)) {
                    NotCriticalErrorAccumulator.setNotCriticalError(String.format(
                            "Элемент '%s' не отображается в виджете '%s'",
                            name,
                            widgetName
                    ));
                    return;
                }
            } catch (final AssertionError e) {
                if (DriverUtils.getBoolean(state)) {
                    NotCriticalErrorAccumulator.setNotCriticalError(String.format(
                            "Элемент '%s' не отображается в виджете '%s'",
                            name,
                            widgetName
                    ));
                }
                return;
            }
            if (!DriverUtils.getBoolean(state)) {
                NotCriticalErrorAccumulator.setNotCriticalError(String.format(
                        "Элемент '%s' не должен отображаться в виджете '%s'",
                        name,
                        widgetName
                ));
            }
        });
        NotCriticalErrorAccumulator.setStepBroken();
    }

    /**
     * Метод используется в тестировании ролевой модели.
     * Все проверяемые объекты должны быть описаны в PageObject
     * Перечисление элементов производится в таблице с указанием того, должен ли элемент отображаться на странице
     * или не должен (да или true -  если должен)
     * <p>
     * И пользователь проверяет доступность элементов
     * | элемент 1 | да  |
     * | элемент 2 | нет |
     *
     * @param data таблица элементов
     */
    @Когда("^проверяет доступность элементов$")
    public void checkElementAvailability(final Map<String, String> data) {
        checkElementAvailability(null, data);
    }

    @Когда("^проверяет доступность элементов в блоке \"([^\"]*)\"$")
    public void checkElementAvailability(final String path, final Map<String, String> data) {
        final BlockInitialized block = path == null ? null : new BlockExtractor(path).getBlock();
        for (final String elementTitle : data.keySet()) {
            final String xpath = DriverUtils.getXpath(elementTitle, block == null ? null : block.getClass());
            final boolean isElementDisplayed = DriverUtils.isElementDisplayed(xpath, block);
            if (DriverUtils.getBoolean(data.get(elementTitle))) {
                if (!isElementDisplayed) {
//                    Если элемент не видно, то он возможно есть и на него нужно навести указатель
                    final List<WebElement> elements = new ArrayList<>();
                    if (block == null) {
                        elements.addAll(Environment.getDriverService().getDriver().findElements(By.xpath(xpath)));
                    } else {
                        elements.addAll(block.findElements(By.xpath(xpath)));
                    }
                    if (!elements.isEmpty()) {
                        Mover.moveToElement(elements.get(0).findElement(By.xpath("./parent::*")), true);
                        if (DriverUtils.isElementDisplayed(elements.get(0))) {
                            continue;
                        }
                    }
                    NotCriticalErrorAccumulator.setNotCriticalError(String.format(
                            "Элемент \"%s\" не отобразился на странице \"%s\"",
                            elementTitle,
                            PageContext.getCurrentPage().getTitle()
                    ));
                }
            } else {
                if (isElementDisplayed) {
                    NotCriticalErrorAccumulator.setNotCriticalError(String.format(
                            "Элемент \"%s\" не должен отображаться на странице \"%s\"",
                            elementTitle,
                            PageContext.getCurrentPage().getTitle()
                    ));
                }
            }
        }
        NotCriticalErrorAccumulator.setStepBroken();
    }

    /**
     * Метод запоминает количество нужных блоков на странице
     *
     * @param blockName название блока из PageObject
     * @param stashKey  ключ для стэша
     */
    @Когда("^запоминает количество блоков \"([^\"]*)\" под кодом \"([^\"]*)\"$")
    public void getBlocksTotalCount(final String blockName, final String stashKey) {
        final BlockExtractor blockExtractor = new BlockExtractor(blockName);
        Stash.put(stashKey, blockExtractor.getBlockCollection().size());
    }

    /**
     * Метод сравнивает два значения из стэша по предикату
     *
     * @param stashKey1 первый ключ
     * @param predicate предикат для сравнения
     * @param stashKey2 второй ключ
     */
    @Когда("^проверяет что значение кода \"([^\"]*)\" (равно|=|>|<|>=|<=|!=|входит) значению кода \"([^\"]*)\"$")
    public void compareTwoKeys(final String stashKey1, final String predicate, final String stashKey2) {

        if (Stash.asMap().containsKey(stashKey1) && Stash.asMap().containsKey(stashKey2)) {
            final Object obj1 = Stash.getValue(stashKey1);
            final Object obj2 = Stash.getValue(stashKey2);
            if (obj1 instanceof List || obj2 instanceof List) {
                Validator.compareListsByPredicate(stashKey1, stashKey2, obj1, obj2, predicate);
                return;
            }
        }

        final String value1 = DataProcessing.decodeValue("stash#" + stashKey1);
        final String value2 = DataProcessing.decodeValue("stash#" + stashKey2);
        final Validator.PredicateSymbol predicateSymbol = Validator
                .PredicateSymbol.valueOfSymbol("равно".equals(predicate) ? "=" : predicate);
        final boolean isStashValue1Number = NumberUtils.isNumber(value1);
        final boolean isStashValue2Number = NumberUtils.isNumber(value2);
        final boolean result;
        if (isStashValue1Number && isStashValue2Number) {
            result = Validator.matchByPredicate(
                    (Integer.parseInt(value1)),
                    (Integer.parseInt(value2)),
                    predicateSymbol
            );
        } else if (!isStashValue1Number && !isStashValue2Number) {
            result = predicateSymbol == Validator.PredicateSymbol.EQUALS ==
                    Validator.matchValues(value1, value2);
        } else {
            throw new AutotestError(String.format(
                    "Коды '%s' и '%s' имеют значения несравнимых типов '%s' и '%s''",
                    stashKey1,
                    stashKey2,
                    Stash.getValue(stashKey1),
                    Stash.getValue(stashKey2)
            ));
        }

        Assert.assertTrue(String.format(
                "Значение кода '%s' - '%s' не соответствует значению кода '%s' - '%s'",
                stashKey1,
                value1,
                stashKey2,
                value2
        ), result);
    }

    /**
     * Метод используется при необходимости проверить количество блоков на странице, когда они добавляются динамически
     *
     * @param blockName название блока из PageObject
     * @param blocksQty количество блоков, которое мы ожидаем увидеть на текущей странице
     */
    @Когда("^проверяет что количество блоков \"([^\"]*)\" (равно|=|>|<|>=|<=|!=) \"([^\"]*)\"$")
    public void checkBlocksTotalCount(final String blockName, final String predicate, final String blocksQty) {
        FieldUtils.checkBlocksTotalCount(new BlockExtractor(blockName), predicate, blocksQty);
    }

    /**
     * Метод считывает текст из указанного элемента и проверяет его на эквивалентность с ожидаемым значением
     * Есть возможность проверить на вхождение значения, для этого нужно добавить звёздочку в начало или в конец
     * или в начало и в конец.
     * Если звёздочка в начале, то проверяем, что текст элемента заканчивается на наше значение
     * Если звёздочка в конце, то проверяем, что текст элемента начинается на наше значение
     * Если звёздочка с обеих сторон, то проверяем на вхождение нашего значения в текст элемента
     *
     * @param elementTitle название элемента
     * @param draftValue   ожидаемое значение текста в элементе
     */
    @Когда("^сверяет текст в элементе \"([^\"]*)\" на эквивалентность \"([^\"]*)\"$")
    public void validateTextOnElementEqual(final String elementTitle, final String draftValue) {
        final String value = DataProcessing.decodeValue(draftValue);
        final AtomicReference<DriverUtils.ValidatedValue> validatedValue = new AtomicReference<>();
        final BooleanSupplier waitWhenTextBeEquivalent = () -> {
            try {
                validatedValue.set(DriverUtils.validateField(FindUtils
                        .getElementByNameOrPath(elementTitle), value));
            } catch (final AutotestError | StaleElementReferenceException error) {
                return false;
            }
            return validatedValue.get().getValidateResult() || !"".equals(validatedValue.get().getActualValue());
        };
        Timer.executeTimer(DriverConstants.ELEMENT_WAIT_5SEC, waitWhenTextBeEquivalent);
        Assert.assertNotNull(String.format("Элемент \"%s\" не найден", elementTitle), validatedValue.get());
        Assert.assertTrue(
                String.format(
                        "Фактическое значение текста \"%s\" не соответствует ожидаемому \"%s\" в элементе \"%s\"",
                        validatedValue.get().getActualValue(),
                        value,
                        elementTitle
                ),
                validatedValue.get().getValidateResult()
        );
    }

    @Когда("^наводит указатель на элемент \"([^\"]*)\"$")
    public void moveToWebElement(final String elementName) {
        Mover.moveToElement(FindUtils.getElementByNameOrPath(elementName), true);
        Mover.markMousePosition();
        ESUtils.freeze(1000);
    }

    //    Переключение на вкладку браузера по номеру, начиная с 1
    @Когда("^переходит на вкладку \"([^\"]*)\"$")
    public void goToTab(final int number) {
        final WebDriver driver = Environment.getDriverService().getDriver();
        final ArrayList<String> winHandle = new ArrayList<>(driver.getWindowHandles());
        driver.switchTo().window(winHandle.get(number - 1));
        setScreenSize(System.getProperty("webdriver.browser.size.preset"));
    }


    /**
     * Метод принимает на вход название группы чекбоксов и таблицу из титлов каждого чекбокса
     * и значения, которое нужно установить.
     * В качестве значений принимаются да, нет, true, false в любом регистре.
     * <p>
     * И пользователь заполняет группу чекбоксов "Эмоциональный интеллект" значениями
     * | Проектирование | да  |
     * | Исследование   | нет |
     *
     * @param groupName название группы чекбоксов из PageObject
     * @param data      таблица значений
     */
    @Когда("^заполняет группу чекбоксов \"([^\"]*)\" значениями$")
    public void fillCheckBoxGroup(final String groupName, final Map<String, String> data) {
        final CheckBoxGroup checkBoxGroup = DriverUtils.getElementByTitle(groupName);
        checkBoxGroup.fillGroup(data);
    }

    @Когда("^заполняет в виджете \"([^\"]*)\" группу чекбоксов \"([^\"]*)\" значениями$")
    public void fillCheckBoxGroupInWidget(
            final String widgetName,
            final String groupName,
            final Map<String, String> data
    ) {
        final Widget widget = (Widget) FindUtils.getElementByNameOrPath(widgetName);
        widget.init();
        final CheckBoxGroup checkBoxGroup = (CheckBoxGroup) widget.getElementByName(groupName);
        checkBoxGroup.fillGroup(data);
    }

    /**
     * Аналогично предыдущему методу, только вместо заполнения - проверка
     *
     * @param groupName название группы чекбоксов
     * @param data      таблица значений для проверки
     */
    @Когда("^проверяет что группа чекбоксов \"([^\"]*)\" заполнена значениями$")
    public void validateCheckBoxGroup(final String groupName, final Map<String, String> data) {
        final CheckBoxGroup checkBoxGroup = DriverUtils.getElementByTitle(groupName);
        checkBoxGroup.validateGroup(data);
    }

    @Когда("^проверяет что в виджете \"([^\"]*)\" группу чекбоксов \"([^\"]*)\" заполнена значениями$")
    public void validateCheckInWidget(final String widgetName, final String groupName, final Map<String, String> data) {
        final Widget widget = (Widget) FindUtils.getElementByNameOrPath(widgetName);
        widget.init();
        final CheckBoxGroup checkBoxGroup = (CheckBoxGroup) widget.getElementByName(groupName);
        checkBoxGroup.validateGroup(data);
    }

    /**
     * Метод отмечает чекбоксы в списке элементов.
     * Блок на странице должен быть описан как списочный элемент List<BlockClassName>
     * BlockClassName должен быть наследован от MarkableListItem
     * <p>
     * И пользователь отмечает в списке строки с названиями в блоке "Список заданий"
     * | Название задания 1 | Да  |
     * | Название задания 2 | Нет |
     *
     * @param blockName имя блока из ElementTitle
     * @param data      dataTable в один столбец из названий строк, которые нужно отметить
     */
    @Когда("^отмечает в списке строки с названиями в блоке \"([^\"]*)\"$")
    public void markItemsInList(final String blockName, final Map<String, String> data) {
        markItemsInList(blockName, data, false);
    }

    @Когда("^отмечает в списке строки с названиями в блоке \"([^\"]*)\" с поиском$")
    public void markItemsInListWithSearch(final String blockName, final Map<String, String> data) {
        markItemsInList(blockName, data, true);
    }

    private void markItemsInList(
            final String blockName,
            final Map<String, String> dataDraft,
            final boolean withSearch
    ) {
        WebElement search = null;
        if (withSearch) {
            try {
                final String xpath = DriverUtils.getXpath("Поиск по названию в модальном окне");
                final WebElement element = DriverUtils.findElementsOnPageByXpath(xpath).get(0);
                search = new TextInput(element);
            } catch (final NoSuchElementException e) {
                throw new AutotestError(
                        "Форма не содержит строки поиска, либо она не обнаружена по заданному xpath",
                        e
                );
            }
        }
        final Map<String, String> data = new LinkedHashMap<>();
        for (final String item : dataDraft.keySet()) {
            data.put(DataProcessing.decodeValue(item), dataDraft.get(item));
        }
        final AtomicReference<MarkableListItem> block = new AtomicReference<>();
        for (final String name : data.keySet()) {
            final String path = new PathBuilder()
                    .setBlock(blockName)
                    .setBlockArgument("Название", name)
                    .build();
            if (withSearch) {
                search.clear();
                search.sendKeys(name);
            }
            final AtomicReference<Throwable> throwable = new AtomicReference<>();
            final BlockExtractor blockElement = new BlockExtractor(path);
            final BooleanSupplier waitWhenBlockListUpdated = () -> {
                try {
                    block.set(blockElement.reset().getBlock());
                    block.get().setValue(data.get(name));
                    return true;
                } catch (final Throwable e) {
                    throwable.set(e);
                    return false;
                }
            };
            final boolean result = Timer.executeTimer(DriverConstants.ELEMENT_WAIT_5SEC, waitWhenBlockListUpdated);
            if (!result) {
                if (null != throwable.get()) {
                    throw new AutotestError(throwable.get());
                } else {
                    final String errorMessage = String.format(
                            "В списке нет значений \"%s\" или появилось больше 1",
                            name
                    );
                    throw new AutotestError(errorMessage);
                }
            }
        }
    }

    @Когда("^проверяет в списке отмеченные строки с названиями в блоке \"([^\"]*)\"$")
    public void validateCheckedItemsInList(final String blockName, final Map<String, String> data) {
        final ErrorCollector errorCollector = new ErrorCollector();
        for (final Map.Entry<String, String> entry : data.entrySet()) {
            final String path = new PathBuilder()
                    .setBlock(blockName)
                    .setBlockArgument("Название", entry.getKey())
                    .build();
            final MarkableListItem item = new BlockExtractor(path).getBlock();
            errorCollector.assertTrue(
                    String.format(
                            "Фактическое значение \"%s\" в строке \"%s\" не соответствует ожидаемому \"%s\"",
                            item.isChecked(),
                            entry.getKey(),
                            entry.getValue()
                    ),
                    item.validate(entry.getValue())
            );
        }
        errorCollector.assertAll();
    }

    @Когда("^отмечает в списке все строки в блоке \"([^\"]*)\"$")
    public void markAllItemsInList(final String listName) {
        final List<MarkableListItem> unmarkedList = new ArrayList<>();
        final BooleanSupplier waitWhenListBeMarked = () -> {
            unmarkedList.clear();
            unmarkedList.addAll(new BlockExtractor(listName).getInitializedBlockCollection());
            unmarkedList.removeIf(MarkableListItem::isChecked);
            if (unmarkedList.isEmpty()) {
                return true;
            }
            unmarkedList.forEach(MarkableListItem::markIt);
            return false;
        };
        final boolean result = Timer.executeTimer(DriverConstants.TIMEOUT, waitWhenListBeMarked);
        if (!result) {
            throw new AutotestError("Не удалось отметить строки:\n" + unmarkedList
                    .stream()
                    .map(item -> item.taskNameTextBlock.getText())
                    .collect(Collectors.joining("\n")));

        }
    }

    @Когда("^отмечает в списке строку номер \"([^\"]*)\" в блоке \"([^\"]*)\"$")
    public void markItemInList(final String rowNumber, final String blockName) {
        final int row = Integer.parseInt(rowNumber);
        final String path = new PathBuilder()
                .setBlock(blockName)
                .setBlockNumber(row)
                .build();
        final MarkableListItem block = new BlockExtractor(path).getBlock();
        block.setValue("true");
    }

    @Когда("^запоминает содержимое элемента \"([^\"]*)\" под кодом \"([^\"]*)\"$")
    public void putInStashFromElement(final String elementTitle, final String stashKey) {
        final WebElement element = FindUtils.getElementByNameOrPath(elementTitle);
        String text = element.getText().replaceAll("\\n", " ").replaceAll("\u00AD", "");
        if (text.isEmpty()) {
            text = element.getAttribute("value").replaceAll("\\n", " ").replaceAll("\u00AD", "");
        }
        Stash.put(stashKey, text);
        log.info("Запомнил текст: {}", text);
    }

    @И("^делает снимок экрана \"([^\"]*)\"$")
    public void takeScreenShot(final String imageName) {
        AllureUtils.attachScreenShotToAllure(imageName);
    }

    @И("^возвращается по хлебным крошкам$")
    public void backByBreadcrumbs() {
        final String BreadCrumbXpath = DriverUtils.getXpath("Хлебные крошки");
        final List<WebElement> breadCrumbs = new ArrayList<>();
        final List<WebElement> breadCrumbButtonList = new ArrayList<>();
        final AtomicReference<String> message = new AtomicReference<>();
        final BooleanSupplier waitWhenBackByBreadcrumbs = () -> {
            try {
                breadCrumbs.clear();
                breadCrumbs.addAll(DriverUtils.findElementsOnPageByXpath(BreadCrumbXpath));
                if (breadCrumbs.isEmpty()) {
                    message.set("Хлебных крошек нет на странице");
                    return false;
                }
                breadCrumbButtonList.clear();
                breadCrumbButtonList.addAll(breadCrumbs.get(0).findElements(By.xpath("./span/button")));
                if (breadCrumbButtonList.isEmpty()) {
                    message.set("Хлебные крошки не содержат пути для перехода");
                    return false;
                }
                ClickActions.clickWithChangeControl(
                        String.format("Хлебная крошка \"%s\"", breadCrumbButtonList.get(0).getText()),
                        breadCrumbButtonList.get(breadCrumbButtonList.size() - 1)
                );
                final boolean isAlertExist = ClickActions.clickElementIfExist("Потерять изменения");
                if (isAlertExist) {
                    log.info("Предупреждение на экране о потере изменений");
                }
                return true;

            } catch (final StaleElementReferenceException e) {
                message.set("Недостаточно времени таймаута для рендеринга хлебных крошек на этой странице");
                return false;
            }
        };

        Timer.executeTimerThrowable(DriverConstants.ELEMENT_WAIT_5SEC, message.get(), waitWhenBackByBreadcrumbs);
    }

    @И("^двигает период (вперёд|назад) на \"([^\"]*)\" (?:позиции|позицию|позиций)$")
    public void movePeriod(final String direction, final String moveTimes) {
        try {
            final DateSwitcher dateSwitcher = DriverUtils.getElementByTitle("Переключатель периода");
            final int counter = Integer.parseInt(moveTimes);
            if ("вперёд".equals(direction)) {
                dateSwitcher.changeDateForward(counter);
            } else if ("назад".equals(direction)) {
                dateSwitcher.changeDateBack(counter);
            } else {
                throw new AutotestError(String.format(
                        "Для выбора направления смещения периода следует использовать ключевые слова 'вперёд' или 'назад' вместо '%s'",
                        direction
                ));
            }
        } catch (final NoSuchElementException e) {
            throw new AutotestError("На текущей странице нет переключателя периодов", e);
        } catch (final NumberFormatException e) {
            throw new AutotestError("Укажите количество переключений периода цифрой", e);
        }
    }

    @И("^обновляет страницу с контролем загрузки$")
    public void refreshPage() {
        PageControls.refreshPage();
    }

    @И("^подтверждает алерт$")
    public void acceptAlert() {
        DriverUtils.freeze(DriverConstants.FREEZE_250_MS);
        Environment.getDriverService().getDriver().switchTo().alert().accept();
    }

    @И("^отклоняет алерт$")
    public void rejectsAlert() {
        DriverUtils.freeze(DriverConstants.FREEZE_250_MS);
        Environment.getDriverService().getDriver().switchTo().alert().dismiss();
    }

    @И("^заполняет таблицу \"([^\"]*)\"$")
    public void fillTableAction(final String tableName, final List<List<String>> data) {
        new TableActions(tableName)
                .fillTable(data, true);
    }

    @И("^заполняет таблицу \"([^\"]*)\" без проверки")
    public void fillTableNotValidatedAction(final String tableName, final List<List<String>> data) {
        new TableActions(tableName)
                .fillTable(data, false);
    }

    @И("^заполняет пустую таблицу \"([^\"]*)\"$")
    public void fillEmptyTableAction(final String tableName, final List<List<String>> data) {
        new TableActions(tableName)
                .fillEmptyTable(data);
    }

    /**
     * Метод ожидает появления элемента без перезагрузки страницы
     *
     * @param wait         секунды ожидания
     * @param elementTitle элемент
     */
    @И("^проверяет наличие (?:кнопки|элемента|ссылки|вкладки) \"([^\"]*)\" с ожиданием \"([^\"]*)\" секунд$")
    public void waitElementOnDisplay(final String elementTitle, final int wait) {
        FindUtils.waitCheckExistsElement(elementTitle, wait, null, true, false);
    }

    @И("^запоминает количество строк таблице \"([^\"]*)\" в переменную \"([^\"]*)\"$")
    public void rememberAllTableRowsNumber(final String tableName, final String key) {
        final int rows = new TableActions(tableName).getAllRowsNumber();
        Allure.addAttachment("Количество строк в таблице", String.valueOf(rows));
        Stash.put(key, rows);
    }

    @И("^проверяет наличие строк в таблице \"([^\"]*)\"$")
    public void verifyExistingRowsTableAction(final String tableName, final List<List<String>> data) {
        TableActions.verifyTableRows(tableName, data, true);
    }

    @И("^проверяет наличие строк в таблице \"([^\"]*)\" с ожиданием \"([^\"]*)\"$")
    public void waitVerifyExistingRowsTableAction(
            final String tableName,
            final int wait,
            final List<List<String>> data
    ) {
        FindUtils.waitCheckExistsElement(tableName, wait, data, false, true);
    }

    @И("^проверяет наличие строк в таблице \"([^\"]*)\" с ожиданием \"([^\"]*)\" без перезагрузки$")
    public void waitVerifyExistingRowsTableActionNoRefresh(
            final String tableName,
            final int wait,
            final List<List<String>> data
    ) {
        FindUtils.waitCheckExistsElement(tableName, wait, data, false, false);
    }

    @И("^проверяет отсутствие строк в таблице \"([^\"]*)\"$")
    public void verifyMissingRowsTableAction(final String tableName, final List<List<String>> data) {
        TableActions.verifyTableRows(tableName, data, false);
    }

    @И("^находит строку в таблице \"([^\"]*)\" и сохраняет элемент в столбце \"([^\"]*)\" под кодом \"([^\"]*)\"$")
    public void saveValueTableAction(
            final String tableNameOrPath,
            final String columnName,
            final String key,
            final List<List<String>> data
    ) {
        final WebElement cellElement = new TableActions(tableNameOrPath).findTableElement(data, columnName);
        Stash.put(key, cellElement.getText());
    }


    @И("^находит строку в таблице \"([^\"]*)\" и нажимает на элемент в столбце \"([^\"]*)\"$")
    public void findAndClickInTable(
            final String tableNameOrPath,
            final String columnName,
            final List<List<String>> data
    ) {
        new TableActions(tableNameOrPath)
                .clickOnTableElement(data, columnName, true);
    }

    @И("^находит строку в таблице \"([^\"]*)\" и нажимает на элемент в столбце \"([^\"]*)\" без проверки$")
    public void findAndClickWithChangeControlInTable(
            final String tableNameOrPath,
            final String columnName,
            final List<List<String>> data
    ) {
        new TableActions(tableNameOrPath)
                .clickOnTableElement(data, columnName, false);
    }

    @И("^находит строку в таблице \"([^\"]*)\" и наводит указатель на элемент в столбце \"([^\"]*)\"$")
    public void findAndMoveToElementInTable(
            final String tableNameOrPath,
            final String columnName,
            final List<List<String>> data
    ) {
        new TableActions(tableNameOrPath)
                .moveOnTableElement(data, columnName);
    }

    @И("^нажимает на столбец \"([^\"]*)\" в таблице \"([^\"]*)\"$")
    public void clickOnColumnHeader(final String columnName, final String tableNameOrPath) {
        DriverUtils.staleElementCatcher(
                () -> new TableActions(tableNameOrPath).clickOnColumnHeader(columnName)
        );
        PageControls.waitWhileNetworkActive();
        PageControls.waitWhenPreloaderIsGone();
    }

    /**
     * Шаг проверяет сортировку данных в столбце таблицы
     * Для типа поля "дата" так же требуется указать ожидаемый формат даты:
     * например для даты 02 апреля 2020 необходимо указать тип поля дата#dd MMMM yyyy
     *
     * @param sortDirection   по возрастанию или по убыванию
     * @param columnName      название колонки, в которой проверяется сортировка
     * @param fieldFormat     тип поля (строка, число, дата)
     * @param tableNameOrPath название таблицы или путь до таблицы если она в списках
     */
    @И("^проверяет сортировку по (возрастанию|убыванию) в столбце \"([^\"]*)\" с типом данных \"([^\"]*)\" таблицы \"([^\"]*)\"$")
    public void checkTableSort(
            final String sortDirection,
            final String columnName,
            final String fieldFormat,
            final String tableNameOrPath
    ) {
        final BooleanSupplier waitWhenNotificationBeClosed = () -> {
            try {
                new TableActions(tableNameOrPath).checkTableSort(sortDirection, columnName, fieldFormat);
                return true;
            } catch (final StaleElementReferenceException e) {
                return false;
            }
        };
        Timer.executeTimer(DriverConstants.ELEMENT_WAIT_5SEC, waitWhenNotificationBeClosed);
    }

    /**
     * Шаг проверяет сортировку блоков
     * Для типа поля "дата" так же требуется указать ожидаемый формат даты:
     * например для даты 02 апреля 2020 необходимо указать тип поля дата#dd MMMM yyyy
     *
     * @param sortDirection по возрастанию или по убыванию
     * @param fieldName     название поля в классе блока
     * @param fieldFormat   тип поля (строка, число, дата)
     * @param path          название списка блоков или путь до вложенного списка блоков
     */
    @И("^проверяет сортировку по (возрастанию|убыванию) в поле \"([^\"]*)\" типа \"([^\"]*)\" списка блоков \"([^\"]*)\"$")
    public void checkBlockSort(
            final String sortDirection,
            final String fieldName,
            final String fieldFormat,
            final String path
    ) {
        new BlockExtractor(path).checkBlockSort(sortDirection, fieldName, fieldFormat);
    }

    @И("^запоминает номер блока \"([^\"]*)\" под кодом \"([^\"]*)\"$")
    public void rememberBlockNumber(final String path, final String stashKey) {
        Stash.put(stashKey, String.valueOf(new BlockExtractor(path).getBlockNumber()));
    }

    /**
     * Шаг выполнит нажатие на кнопку загрузки файла, определит какие файлы были скачаны и перенесёт их в target
     * для дальнейшей работы
     *
     * @param fileType   предполагаемое расширение скачиваемого файла
     * @param buttonName ElementTitle кнопки по нажатии на которую будет выполнено скачивание файла
     * @param stashKey   ключ по которому сохранится путь до скачанного файла в target
     */
    @И("^загружает файл типа \"([^\"]*)\" кнопкой \"([^\"]*)\" и запоминает путь в переменную \"([^\"]*)\"$")
    public void downloadFile(final String fileType, final String buttonName, final String stashKey) {
        final List<String> filesList = BrowserUtils.downloadFiles(buttonName, fileType);
        if (filesList.size() > 1) {
            Allure.addAttachment("Пути до файлов", String.join("\n", filesList));
            Stash.put(stashKey, filesList);
        } else {
            Allure.addAttachment("Путь до файла", filesList.get(0));
            Stash.put(stashKey, filesList.get(0));
        }
    }

    /**
     * Аналогично шагу выше - выполняет контроллируемую загрузку файла из таблицы
     * <p>
     * И загружает файл типа "pdf" из таблицы "Таблица" в колонке "Ссылка на инструкцию" и запоминает путь в переменную "file"
     * | Тип инструкции  |
     * | Памятка учителя |
     *
     * @param fileType  Ожидаемый тип файла
     * @param tableName Название таблицы из pageObject
     * @param colName   Название столбца, из которого производится скачивание файла
     * @param stashKey  Имя переменной, в которую запишется полный путь до скачанного файла
     * @param data      Данные для поиска подходящей строки в которой находится ссылка на скачиваемый файл
     */
    @И("^загружает файл типа \"([^\"]*)\" из таблицы \"([^\"]*)\" в колонке \"([^\"]*)\" и запоминает путь в переменную \"([^\"]*)\"$")
    public void downloadFileFromTable(
            final String fileType,
            final String tableName,
            final String colName,
            final String stashKey,
            final List<List<String>> data
    ) {
        final List<String> filesList = BrowserUtils.downloadFiles(tableName, colName, data, fileType);
        if (filesList.size() > 1) {
            Allure.addAttachment("Пути до файлов", String.join("\n", filesList));
            Stash.put(stashKey, filesList);
        } else {
            Allure.addAttachment("Путь до файла", filesList.get(0));
            Stash.put(stashKey, filesList.get(0));
        }
    }

    /**
     * Универсальный шаг. Используется как для обычного элемента, так и для списочного.
     * Шаг позволяет выполнить проверку фактического векторного описания SVG иконки с ожидаемым
     * Векторные описания иконок пишем в отдельный файл src/test/resources/data/svg-icons.resources
     * в виде имени иконки и содержимого аттрибута "d" тега path внутри svg
     */
    @И("^проверяет иконку \"([^\"]*)\" на соответствие \"([^\"]*)\"$")
    public void validateIcon(final String title, final String svgName) {
        ((SvgIcon) FindUtils.getElementByNameOrPath(title)).validate(svgName);
    }

    /**
     * Универсальный шаг. Используется как для обычного элемента, так и для списочного.
     * Цвет указывается в таком же виде, как указан в свойстве BackgroundColor или fill элемента на странице
     */
    @И("^проверяет цвет элемента \"([^\"]*)\" на соответствие \"([^\"]*)\"$")
    public void validateElementColor(final String elementTitle, final String color) {
        final WebElement element = FindUtils.getElementByNameOrPath(elementTitle);
        final String expectedColor = DataProcessing.decodeValue(color);
        final String actualBackgroundColor;
        final String actualFillColor;
        final String actualColor;
        final String actualAttributeColor;
        if (element instanceof AbstractPseudoElement) {
            actualBackgroundColor = ((AbstractPseudoElement) element).getCssPropertyValue("backgroundColor");
            actualFillColor = ((AbstractPseudoElement) element).getCssPropertyValue("fill");
            actualColor = ((AbstractPseudoElement) element).getCssPropertyValue("color");
            actualAttributeColor = "";
        } else {
            actualBackgroundColor = (String) DriverUtils.executeJS(
                    "return window.getComputedStyle(arguments[0])['backgroundColor']",
                    element
            );
            actualFillColor = (String) DriverUtils.executeJS(
                    "return window.getComputedStyle(arguments[0])['fill']",
                    element
            );
            actualColor = (String) DriverUtils.executeJS(
                    "return window.getComputedStyle(arguments[0])['color']",
                    element
            );
            actualAttributeColor = element.getAttribute("color");
        }
        if (!(expectedColor.equals(actualBackgroundColor) ||
                expectedColor.equals(actualColor) ||
                expectedColor.equals(actualFillColor) ||
                expectedColor.equals(actualAttributeColor))) {
            final String message = String.format(
                    "Актуальное значение цвета backgroundColor \"%s\" или fill \"%s\" или AttributeColor \"%s\" или color \"%s\" не соответствует ожидаемому \"%s\"",
                    actualBackgroundColor,
                    actualFillColor,
                    actualAttributeColor,
                    actualColor,
                    expectedColor
            );
            NotCriticalErrorAccumulator.setNotCriticalError(message);
            NotCriticalErrorAccumulator.setStepBroken();
        }
    }

    @И("^проверяет доступность элементов выпадающего списка \"([^\"]*)\"$")
    public void validateDropdownList(final String selectNameOrPath, final Map<String, String> data) {
        final Select select = (Select) FindUtils.getElementByNameOrPath(selectNameOrPath);
        final List<String> actualOptions = select.getAllOptions();

        for (final String optionDraft : data.keySet()) {
            final String option = DataProcessing.decodeValue(optionDraft);
            if (DriverUtils.getBoolean(data.get(option)) && !Validator.matchValueInList(actualOptions, option)) {
                NotCriticalErrorAccumulator.setNotCriticalError(
                        String.format("Значение \"%s\" отсутствует в выпадающем списке", option)
                );
            } else if (!DriverUtils.getBoolean(data.get(option)) &&
                    Validator.matchValueInList(actualOptions, option)) {
                NotCriticalErrorAccumulator.setNotCriticalError(
                        String.format("Значения \"%s\" не должно быть в выпадающем списке", option)
                );
            }
        }
        NotCriticalErrorAccumulator.setStepBroken();
    }

    @И("^проверяет наличие тенантов у всех элементов выпадающего списка \"([^\"]*)\"$")
    public void checkDropdownListForTenants(final String selectListNameOrPath) {
        final Select selectList = (Select) FindUtils.getElementByNameOrPath(selectListNameOrPath);
        final List<String> allElements = selectList.getAllOptions();
        for (final String element : allElements) {
            if (!element.endsWith(")")) {
                NotCriticalErrorAccumulator.setNotCriticalError(String.format(
                        "У элемента \"%s\" отсутствует тенант",
                        element
                ));
            }
        }
        NotCriticalErrorAccumulator.setStepBroken();
    }

    /**
     * Шаг запоминает значение в буфер обмен
     * В безголовом режиме создается элемент для копирования значения в буфер
     */
    @И("^запоминает значение \"([^\"]*)\" в буфер обмен$")
    public void setValueInClipboard(final String value) {
        final boolean IS_MOON = !"".equals(System.getProperty("webdriver.url"));
        final String decodeValue = DataProcessing.decodeValue(value);
        if (IS_MOON) {
            Moon.setClipBoard(decodeValue);
        } else {
            ClipboardUtils.setValueClipboard(decodeValue);
        }
    }

    /**
     * Шаг проверяет стили элемента на соответствия
     * Значения с px будут округлены в меньшую сторону
     */
    @И("^проверяет стили элемента \"([^\"]*)\" на соответствие$")
    public void validateElementStyles(final String elementTitleOrPath, final Map<String, String> data) {
        final WebElement element = FindUtils.getElementByNameOrPath(elementTitleOrPath);
        for (final String styleName : data.keySet()) {
            if (styleName.contains("-")) {
                throw new AutotestError(
                        String.format(
                                "Наименование стиля \"%s\" нужно написать в стиле camelCase, " +
                                        "то-есть убрать дефисы и каждое следующее слово с большой буквы",
                                styleName
                        )
                );
            }
            final String actual;
            if (element instanceof AbstractPseudoElement) {
                actual = ((AbstractPseudoElement) element).getCssPropertyValue(styleName);
            } else {
                final String script = "return window.getComputedStyle(arguments[0])['" + styleName + "']";
                actual = (String) DriverUtils.executeJS(script, element);
            }
            final String actualRounded;
            if (actual.endsWith("px") && actual.contains(".")) {
                actualRounded = ((int) Math.floor(Double.parseDouble(actual.replace("px", "")))) + "px";
            } else {
                actualRounded = actual;
            }
            final boolean result = Validator.matchValues(actualRounded, data.get(styleName));
            if (!result) {
                NotCriticalErrorAccumulator.setNotCriticalError(
                        String.format(
                                "Фактическое значение \"%s\" не соответствует ожидаемому \"%s\" в стиле \"%s\"",
                                actualRounded,
                                data.get(styleName),
                                styleName
                        )
                );
            }
        }
        NotCriticalErrorAccumulator.setStepBroken();
    }


    @И("^получает значение атрибута \"([^\"]*)\" в элементе \"([^\"]*)\" и сохраняет его по ключу \"([^\"]*)\"$")
    public void getAttributeValue(final String attributeName, final String titleOrPath, final String key) {
        final WebElement element = FindUtils.getElementByNameOrPath(titleOrPath);
        Stash.put(key, element.getAttribute(attributeName));
    }

    /**
     * Шаг получает значение атрибута из элемента, например class и сверяет его с ожидаемым значением
     *
     * @param attrName   полное название атрибута, регистрозависимое
     * @param nameOrPath имя или путь до элемента
     * @param expected   ожидаемое значение. Поддерживается значение из стэш и маска
     */
    @И("^проверяет значение атрибута \"([^\"]*)\" в элементе \"([^\"]*)\" на эквивалентность \"([^\"]*)\"$")
    public void checkAttribute(final String attrName, final String nameOrPath, final String expected) {
        final WebElement element = FindUtils.getElementByNameOrPath(nameOrPath);
        final String actual = element.getAttribute(attrName);
        final String expectedValue = DataProcessing.decodeValue(expected);
        Assert.assertTrue(
                String.format(
                        "Актуальное значение \"%s\" аттрибута \"%s\" не соответствует ожидаемому \"%s\"",
                        actual,
                        attrName,
                        expectedValue
                ),
                Validator.matchValues(actual, expectedValue)
        );
    }

    /**
     * Шаг используется только для текстовых полей ввода. Проверяет, находится ли курсор в поле ввода
     *
     * @param nameOrPath имя или путь до элемента
     */
    @И("^проверяет что поле ввода \"([^\"]*)\" активно$")
    public void checkElementInFocus(final String nameOrPath) {
        final WebElement element = ((TextInput) FindUtils.getElementByNameOrPath(nameOrPath)).getInput();
        final boolean elementInFocus = (boolean) DriverUtils.executeJS(
                "return arguments[0] === document.activeElement",
                element
        );
        Assert.assertTrue(String.format("Элемент \"%s\" сейчас не активен", nameOrPath), elementInFocus);
    }

    /**
     * Шаг проверяет набор элементов на видимость во вьюпорте.
     * Допустима не полная видимость - процент от общей площади элемента
     *
     * @param acceptVisiblePercent допустимый процент видимости элемента
     * @param data                 таблица элементов и значение видимости (да или нет)
     */
    @И("^проверяет наличие элемента во вьюпорте с доступностью (\\d*)%$")
    public void checkIfElementInViewport(final int acceptVisiblePercent, final Map<String, String> data) {
        final ErrorCollector errorCollector = new ErrorCollector();
        for (final String nameOrPath : data.keySet()) {
            final WebElement element = FindUtils.getElementByNameOrPath(nameOrPath);
            final int visiblePercent = Mover.getElementVisibleSquarePercent(element);
            final boolean expectedVisibility = DriverUtils.getBoolean(data.get(nameOrPath));
            if (expectedVisibility) {
                errorCollector.assertTrue(
                        String.format(
                                "Видимость %d%% элемента \"%s\" ниже ожидаемой %d%%",
                                visiblePercent,
                                nameOrPath,
                                acceptVisiblePercent
                        ), visiblePercent >= acceptVisiblePercent
                );
            }
        }
        errorCollector.assertAll();
    }

    @И("^добавляет информацию с параметром \"([^\"]*)\" в отчёт Allure из стэша \"([^\"]*)\"$")
    public void addInfoToAllure(final String param, final String key) {
        AllureUtils.attachMessageToAllureStep(param, Stash.getValue(key));
    }

    @И("^добавляет survey вопрос с типом \"([^\"]*)\" и параметрами$")
    public void surveyQuestion(final String questionType, final Map<String, String> data) {
        SurveyExecutor
                .getSurveyStep()
                .addAndConfigureWidget(questionType, data);
    }

    /**
     * Это шаг конструктора survey. Он выполняет конфигурирование активного в данный момент виджета
     * Выбрать нужный виджет можно шагом И пользователь выбирает вопрос "" в конструкторе survey
     *
     * @param data параметры
     */
    @И("^заполняет параметры активного вопроса$")
    public void surveyQuestion(final Map<String, String> data) {
        SurveyExecutor
                .getSurveyStep()
                .configureActiveWidget(data);
    }

    @И("^заполняет ответы на вопросы survey$")
    public void fillAnswerSurvey(final Map<String, String> data) {
        SurveyExecutor
                .getSurveyStep()
                .fillAnswerListByStudent(data);
    }

    @И("^проверяет правильность ответов survey$")
    public void checkSurveyAnswers(final Map<String, String> data) {
        SurveyExecutor
                .getSurveyStep()
                .checkAnswersByBorderColor(data);
    }

    @И("^проверяет корректность отображения элементов survey в вопросе \"([^\"]*)\"$")
    public void partialElementsCheck(final String question, final Map<String, String> data) {
        SurveyExecutor
                .getSurveyStep()
                .partialElementsCheck(question, data);
    }

    /**
     * Шаг находит в списке вопросов в конструкторе нужный вопрос по названию или по номеру в списке
     * и выделяет его с помощью клика
     *
     * @param questionOrNumber вопрос или номер вопроса на текущей странице
     */
    @И("^выбирает вопрос \"([^\"]*)\" в конструкторе survey$")
    public void selectQuestionNumber(final String questionOrNumber) {
        SurveyExecutor.getSurveyStep().selectWidget(questionOrNumber);
    }

    @И("^проверяет survey вопрос \"([^\"]*)\" на соответствие параметрам$")
    public void surveyVerifyQuestion(final String questionOrNumber, final Map<String, String> data) {
        selectQuestionNumber(questionOrNumber);
        SurveyExecutor
                .getSurveyStep()
                .verifyActiveWidget(data);
    }

    @И("^проверяет количество символов в полях$")
    public void checkSymbolQtyInFields(final Map<String, String> data) {
        final ErrorCollector errorCollector = new ErrorCollector();
        data.forEach((name, value) -> {
            final Integer expectedValueSize = DriverUtils.getNumber(value);
            errorCollector.assertNotNull(
                    String.format(
                            "Значение \"%s\" для поля \"%s\" должно быть цифрой", name, value
                    ),
                    expectedValueSize
            );
            final Integer actualValueSize = FindUtils
                    .getElementByNameOrPath(name)
                    .getText()
                    .length();
            final String message = String.format(
                    "Актуальное значение \"%s\" не соответствует ожидаемому \"%s\" в поле \"%s\"",
                    actualValueSize,
                    expectedValueSize,
                    name
            );
            errorCollector.assertEquals(message, actualValueSize, expectedValueSize);
        });
        errorCollector.assertAll();
    }

    /**
     * Шаг реализует тестирование вёрстки в перечисленных элементах относительно окна браузера
     * <p>
     * И проверяет параметры верстки. Путь "Учитель; Помощь". Сценарий "Экран помощи под ролью учителя"
     * | Область контента  | TEXT SVG FORMS |
     * | Блок с телефонами | DECOR          |
     */
    @И("^проверяет параметры (верстки|оформления). Путь \"([^\"]*)\". Сценарий \"([^\"]*)\"$")
    public void layoutMeasuring(
            final String testingMode,
            final String screenPath,
            final String scenarioName,
            final Map<String, String> data
    ) throws IOException {
        new LayoutTestExecutor(
                TestingMode.determine(testingMode),
                false,
                null,
                screenPath,
                scenarioName,
                data
        ).execute();
    }

    /**
     * Шаг реализует тестирование вёрстки в перечисленных элементах относительно указанного контейнера
     * Это полезно для списков, где могут появиться другие элементы или измениться сортировка списка
     * <p>
     * И проверяет параметры верстки относительно контейнера "Список учеников->Имя ученика#Копылова Анфиса". Путь "Учитель; Мои классы; Класс". Сценарий "Бусины у Копыловой"
     * | Список учеников->Имя ученика#Копылова Анфиса | DECOR |
     */
    @И("^проверяет параметры (верстки|оформления) относительно контейнера \"([^\"]*)\". Путь \"([^\"]*)\". Сценарий \"([^\"]*)\"$")
    public void relativeLayoutMeasuring(
            final String testingMode,
            final String containerName,
            final String screenPath,
            final String scenarioName,
            final Map<String, String> data
    ) throws IOException {
        new LayoutTestExecutor(TestingMode.determine(testingMode), false, containerName, screenPath, scenarioName, data)
                .execute();
    }

    /**
     * Шаг реализует тестирование вёрстки в перечисленных элементах относительно самих элементов.
     * В этом шаге нельзя использовать тип измерения POSITION, так как в этом случае позиция всегда будет 0,0
     */
    @И("^проверяет параметры (верстки|оформления) относительно контейнера. Путь \"([^\"]*)\". Сценарий \"([^\"]*)\"$")
    public void relativeLayoutMeasuring(
            final String testingMode,
            final String screenPath,
            final String scenarioName,
            final Map<String, String> data
    ) throws IOException {
        new LayoutTestExecutor(
                TestingMode.determine(testingMode),
                true,
                null,
                screenPath,
                scenarioName,
                data
        ).execute();
    }

    @И("^закрывает текущее окно$")
    public void closeBrowserTab() {
        final WebDriver driver = Environment.getDriverService().getDriver();
        driver.close();
        for (final String window : driver.getWindowHandles()) {
            driver.switchTo().window(window);
        }
    }


    /*
     Метод подключается к почтовому ящику Yandex по логину и паролю и удаляет все имеющиеся письма
     допустимые параметры для дататэйбла: login, password
    */
    @И("^очищает почтовый ящик по логину \"([^\"]*)\" и паролю \"([^\"]*)\"$")
    public void clearMailBox(final String login, final String password) {
        new MailHandler(login, password).deleteMessages();
    }

    @И("^очищает почтовый ящик (mail|gmail|yandex) по логину \"([^\"]*)\" и паролю \"([^\"]*)\"$")
    public void clearMailBoxInService(final String service, final String login, final String password) {
        new MailHandler(login, password, EmailUtils.getImapEmailService(service)).deleteMessages();
    }

    @И("^проверяет (наличие|отсутствие) письма в сервисе (mail|gmail|yandex) с текстом \"([^\"]*)\" по логину \"([^\"]*)\" и паролю \"([^\"]*)\"$")
    public void checkMessageMail(
            final String attribute,
            final String service,
            final String message,
            final String login,
            final String password
    ) {
        new MailHandler(login, password, EmailUtils.getImapEmailService(service)).checkMassageText(message, "наличие".equals(attribute));
    }

    @И("^проверяет (наличие|отсутствие) письма с текстом \"([^\"]*)\" по логину \"([^\"]*)\" и паролю \"([^\"]*)\"$")
    public void checkMessageMail(
            final String attribute,
            final String message,
            final String login,
            final String password
    ) {
        new MailHandler(login, password).checkMassageText(message, "наличие".equals(attribute));
    }

    /*
      Метод подключается к почтовому ящику Yandex по логину и паролю и получает ссылку активации
      из первого письма. Ссылку запоминает в стэш
      Допустимые параметры для дататэйбла: key, login, password
    */
    @И("^запоминает (ссылку|тему|сообщение|sms код) из почтового ящика по логину \"([^\"]*)\" и паролю \"([^\"]*)\" по ключу \"([^\"]*)\"$")
    public void activateEmail(final String attribute, final String login, final String password, final String key) {
        final MailHandler mailHandler = new MailHandler(login, password);
        EmailUtils.saveStashData(mailHandler, attribute, key);
    }
    @И("^запоминает (ссылку|тему|сообщение|sms код) из почтового ящика (mail|gmail|yandex) по логину \"([^\"]*)\" и паролю \"([^\"]*)\" по ключу \"([^\"]*)\"$")
    public void activateEmailInService(final String attribute,final String service, final String login, final String password, final String key) {
        final MailHandler mailHandler = new MailHandler(login, password,EmailUtils.getImapEmailService(service));
        EmailUtils.saveStashData(mailHandler, attribute, key);
    }

    @И("^запоминает содержимое буфера обмена по ключу \"([^\"]*)\"$")
    public void saveClipboard(final String key) throws IOException, UnsupportedFlavorException {
        final boolean IS_MOON = !"".equals(System.getProperty("webdriver.url"));
        final String clipboardContent;
        if (IS_MOON) {
            clipboardContent = Moon.getClipboard();
        } else {
            clipboardContent = ClipboardUtils.getClipboard();
        }
        Stash.put(key, clipboardContent);
    }

    @И("^проверяет (наличие|отсутствие) данных в (xls|xlsx|csv|json|pdf|doc|txt|yml|yaml) \"([^\"]*)\"$")
    public void matchDataInFile(
            final String presentState,
            final String fileType,
            final String fileName,
            final List<List<String>> data
    ) {
        DataProcessing.matchDataInFile(fileType, fileName, 1, data, "наличие".equals(presentState));
    }

    @И("^проверяет (наличие|отсутствие) данных в (xls|xlsx) \"([^\"]*)\" на странице (\\d)$")
    public void matchDataInFile(
            final String presentState,
            final String fileType,
            final String fileName,
            final int pageNumber,
            final List<List<String>> data
    ) {
        DataProcessing.matchDataInFile(fileType, fileName, pageNumber, data, "наличие".equals(presentState));
    }

    @И("^добавляет данные в (xls|xlsx) \"([^\"]*)\" на странице (\\d)$")
    public void writeInFile(
            final String fileType,
            final String fileName,
            final int pageNumber,
            final List<List<String>> data
    ) {
        DataProcessing.writeInFile(fileType, fileName, pageNumber, data);
    }

    @И("^сверяет количество строк в (xls|xlsx|csv|json|pdf|doc|yml|yaml) \"([^\"]*)\" со значением (\\d)$")
    public void matchRowNumberInFile(final String fileType, final String fileName, final int rowNumber) {
        DataProcessing.matchRowNumberInFile(fileType, fileName, rowNumber);
    }

    @И("^берёт и перемещает элемент \"([^\"]*)\" на расстояние x,y \"([^\"]*)\"$")
    public void dragAndDropElement(final String elementName, final String offset) {
        new DragAndDrop().dragAndDropElement(FindUtils.getElementByNameOrPath(elementName), offset);
        PageControls.detectConsoleErrors();
    }

    /**
     * Шаг реализует перемещение элемента на элемент
     * Важно что бы у потомков или предка был атрибут draggable
     * <p>
     * elementName - элемент который хотим переместить
     * elementName2 - элемент куда будет перемещаться elementName
     */
    // Подлежит удалению после выхода 48 версии EDU-69659
    @Deprecated
    @И("^берёт и перемещает элемент \"([^\"]*)\" на элемент \"([^\"]*)\"$")
    public void dragAndDropElementOnElement(final String elementName, final String elementName2) {
        new DragAndDrop().dragAndDropElementOnElement(elementName, elementName2, "");
        PageControls.detectConsoleErrors();
    }

    /**
     * Шаг реализует перемещение элемента на элемент
     * Важно что бы у потомков или предка был атрибут draggable
     * <p>
     * elementName - элемент который хотим переместить
     * elementName2 - элемент куда будет перемещаться elementName
     * controlElementName - точка контроля которая выступает координатой 0,0 указывается в случае динамически изменяемых
     * элементов во время перемещения на другой элемент
     */

    @И("^берёт и перемещает элемент \"([^\"]*)\" на элемент \"([^\"]*)\" контрольный элемент \"([^\"]*)\"$")
    public void dragAndDropElementOnElement(
            final String elementName,
            final String elementName2,
            final String controlElementName
    ) {
        new DragAndDrop().dragAndDropElementOnElement(elementName, elementName2, controlElementName);
        PageControls.detectConsoleErrors();
    }

    @И("^сравнивает значение CRC32 \"([^\"]*)\" с файлом из переменной \"([^\"]*)\"$")
    public void compareTwoFilesCrc32(final String CRC32, final String stashKey) throws IOException {
        final String newValue = DataProcessing.computeCrc32(Stash.getValue(stashKey)).toString();
        Assert.assertEquals(CRC32, newValue);
    }

    @И("^загружает файл из элемента \"([^\"]*)\" и запоминает путь в переменную \"([^\"]*)\"$")
    public void contextDownloadFile(final String buttonName, final String stashKey)
            throws IOException, InterruptedException {
        final String filePath = BrowserUtils.directDownloadFile(buttonName);
        Stash.put(stashKey, filePath);
    }

    @И("^запоминает текущий URL страницы в переменную \"([^\"]*)\"$")
    public void getCurrentUrl(final String stashKey) throws MalformedURLException {
        final URL currentUrl = new URL(Environment
                .getDriverService()
                .getDriver()
                .getCurrentUrl());
        Stash.put(stashKey, currentUrl.getPath());
    }

    @И("^сверяет значение из переменной \"([^\"]*)\" с \"([^\"]*)\"$")
    public void compareStashkeys(final String stashKey, final String compareWithText) {
        Assert.assertEquals(Stash.getValue(stashKey), DataProcessing.decodeValue(compareWithText));
    }

    /**
     * Шаг позволяет выполнить произвольную комбинацию клавиш относительно текущего элемент или окна
     * Последовательные нажатия нужно разделить замятыми, совместные плюсами
     * Названия функциональных клавиш можно посмотреть в классе Keys
     * Пример:
     * TAB, ENTER выполнит по очереди нажатие сначала TAB потом ENTER
     * SHIFT + 6 выполнит комбинацию этих клавиш
     * Если предварительно кликнуть в поле ввода и передать нажатия клавиш A,S,D,F поле будет заполнено фразой ASDF
     *
     * @param buttonNames функциональная кнопка клавиатуры
     */
    @И("^нажимает функциональную кнопку клавиатуры \"([^\"]*)\"$")
    public void sendFunctionalKeyboardButton(final String buttonNames) {
        final CharSequence[] keys = Stream.of(buttonNames.split("[,+]"))
                .map(String::trim)
                .map(key -> {
                    try {
                        return (CharSequence) Keys.valueOf(key);
                    } catch (final IllegalArgumentException e) {
                        return key;
                    }
                })
                .toArray(CharSequence[]::new);
        if (buttonNames.contains("+")) {
            Environment.getDriverService().getDriver().switchTo().activeElement().sendKeys(Keys.chord(keys));
        } else {
            Mover.getActions().sendKeys(keys).build().perform();
        }
        PageControls.waitWhileNetworkActive();
        PageControls.detectConsoleErrors();
        PageControls.detectBackendErrors();
        PageControls.detectUps404Errors();
        PageControls.waitWhenPreloaderIsGone();
    }

    @И("^нажимает и удерживает кнопку мыши$")
    public void clickAndHold() {
        Mover.getActions().clickAndHold().build().perform();
    }

    @И("^отпускает кнопку мыши$")
    public void releaseHold() {
        Mover.getActions().release().build().perform();
    }

    @И("^нажимает функциональную кнопку клавиатуры \"([^\"]*)\" ([^\"]*) (?:раз|раза)$")
    public void sendFunctionalKeyboardButtonMultiple(final String buttonNames, final String arg) {
        final int qty;
        if (arg.contains("stash")) {
            qty = Integer.parseInt(DataProcessing.decodeValue(arg));
        } else {
            qty = Integer.parseInt(arg);
        }
        for (int i = 0; i < qty; i++) {
            sendFunctionalKeyboardButton(buttonNames);
        }
    }

    @И("^устанавливает разрешение экрана (FULL_HD|DESKTOP|TABLET_LANDSCAPE|TABLET_PORTRAIT|MOBILE_PORTRAIT)$")
    public void setScreenSize(final String screenSize) {
        LayoutUtils.setWindowSize(DimensionEnum.valueOf(screenSize));
    }

    @И("^устанавливает специфическое разрешение экрана \"([^\"]*)\"$")
    public void setCustomScreenSize(final String screenSize) {
        final String[] widthAndHeight = screenSize.split("x");
        Assert.assertEquals(
                "Ошибка формата, требуется передать формат типа WidthxHeight, например 1024x768",
                2,
                widthAndHeight.length
        );
        LayoutUtils.setWindowSize(Integer.parseInt(widthAndHeight[0]), Integer.parseInt(widthAndHeight[1]));
    }

    @И("^проверяет специфические параметры элемента \"([^\"]*)\"$")
    public void validateImageProperties(final String elementNameOrPath, final Map<String, String> data) {
        final WebElement element = FindUtils.getElementByNameOrPath(elementNameOrPath);
        if (element instanceof HasSpecificProperties) {
            ((HasSpecificProperties) element).validateSpecificProperties(data);
        } else {
            throw new AutotestError(String.format(
                    "Элемент \"%s\" не поддерживает получение специфических свойств",
                    elementNameOrPath
            ));
        }
    }


    @И("^выполняет (DELETE|GET) запрос \"([^\"]*)\" в домене \"([^\"]*)\" без параметров$")
    public void apiRequestInMethodNotParam(final String method, final String request, final String domain) {
        APIRequest.doRequestByMethodUrl(request, domain, method);
    }

    @И("^выполняет (DELETE|GET) запрос \"([^\"]*)\" в домене \"([^\"]*)\" с параметрами$")
    public void apiRequestInMethodInParam(
            final String method,
            final String requestId,
            final String domain,
            final Map<String, String> data
    ) {
        final String request = new ParametrizedRequest(requestId, data).toString();
        Allure.addAttachment(requestId, request);
        APIRequest.doRequestByMethodUrl(request, domain, method);
    }

    @И("^выполняет (POST|PUT) запрос \"([^\"]*)\" в домене \"([^\"]*)\" по url \"([^\"]*)\" с параметрами$")
    public void apiRequestInMethodInParamPost(
            final String method,
            final String requestId,
            final String domain,
            final String url,
            final Map<String, String> data
    ) {
        final String request = new ParametrizedRequest(requestId, data).toString();
        Allure.addAttachment(requestId, request);
        APIRequest.doRequestByMethod(url, domain, method, request);
    }

    @И("^выполняет (DELETE|GET) запрос \"([^\"]*)\" в домене \"([^\"]*)\" с сохранением ответа по ключу \"([^\"]*)\" и с параметрами$")
    public void apiRequestInMethodInParam(
            final String method,
            final String requestId,
            final String domain,
            final String stashKey,
            final Map<String, String> data
    ) {
        final String request = new ParametrizedRequest(requestId, data).toString();
        Allure.addAttachment(requestId, request);
        Stash.put(stashKey, APIRequest.doRequestByMethod(request, domain, method, null));
    }

    @И("^выполняет (POST|PUT) запрос \"([^\"]*)\" в домене \"([^\"]*)\" по url \"([^\"]*)\" с сохранением ответа по ключу \"([^\"]*)\" и с параметрами$")
    public void apiRequestInMethodInParamPostAndSave(
            final String method,
            final String requestId,
            final String domain,
            final String url,
            final String stashKey,
            final Map<String, String> data
    ) {
        final String request = new ParametrizedRequest(requestId, data).toString();
        Allure.addAttachment(requestId, request);
        Stash.put(stashKey, APIRequest.doRequestByMethod(url, domain, method, request));
    }

    @И("^выполняет (DELETE|GET) запрос \"([^\"]*)\" в домене \"([^\"]*)\" с сохранением ответа по ключу \"([^\"]*)\" без параметров$")
    public void apiRequestInMethodInParam(
            final String method,
            final String request,
            final String domain,
            final String stashKey
    ) {
        Stash.put(stashKey, APIRequest.doRequestByMethodUrl(request, domain, method));
    }

    @И("^выполняет API на стенде \"([^\"]*)\" под профилем \"([^\"]*)\"$")
    public void getToken(final String stand, final String profile) {
        APIAuth.authByProfileName(stand, profile);
    }

    @И("^выполняет API под профилем \"([^\"]*)\"$")
    public void getToken(final String profile) {
        APIAuth.authByProfileName(profile);
    }

    @Когда("^выполняет API под логином \"([^\"]*)\" и паролем \"([^\"]*)\"$")
    public void getTokenWithLoginAndPassword(final String userLogin, final String userPassword) {
        APIAuth.authWithLoginAndPassword(DataProcessing.decodeValue(userLogin), userPassword);
    }

    @И("^сохраняет результат API запроса \"([^\"]*)\" на стенд \"([^\"]*)\" по ключу \"([^\"]*)\"$")
    public void apiRequestOnStand(final String requestID, final String stand, final String key) {
        Stash.put(key, APIRequest.doRequestByRequestID(stand, requestID, true, false));
    }

    @И("^сохраняет результат (синхронного) API запроса \"([^\"]*)\" по ключу \"([^\"]*)\"$")
    @Когда("^сохраняет результат() API запроса \"([^\"]*)\" по ключу \"([^\"]*)\"$")
    public void apiRequest(final String type, final String requestID, final String key) {
        Stash.put(key, APIRequest.doRequestByRequestID(requestID, "синхронного".equals(type)));
    }

    @И("^выполняет (синхронный) API запрос \"([^\"]*)\" с параметрами$")
    @Когда("^выполняет() API запрос \"([^\"]*)\" с параметрами$")
    public void parametrizedApiRequest(final String type, final String requestID, final Map<String, String> data) {
        final String request = new ParametrizedRequest(requestID, data).toString();
        Allure.addAttachment(requestID, request);
        APIRequest.doRequest(request, true, "синхронный".equals(type));
    }

    @И("^выполняет (синхронный) API запрос \"([^\"]*)\" с сохранением ответа по ключу \"([^\"]*)\" с параметрами$")
    @Когда("^выполняет() API запрос \"([^\"]*)\" с сохранением ответа по ключу \"([^\"]*)\" с параметрами$")
    public void parametrizedApiRequestWithResponse(
            final String type,
            final String requestID,
            final String key,
            final Map<String, String> data
    ) {
        final String request = new ParametrizedRequest(requestID, data).toString();
        Allure.addAttachment(requestID, request);
        final String response = APIRequest.doRequest(request, true, "синхронный".equals(type));
        Stash.put(key, response);
    }

    @И("^выполняет (синхронный) API запрос \"([^\"]*)\" с параметрами по url \"([^\"]*)\" без проверки ответа$")
    @Когда("^выполняет() API запрос \"([^\"]*)\" с параметрами по url \"([^\"]*)\" без проверки ответа$")
    public void parametrizedApiRequestUrlWithoutResponse(
            final String type,
            final String requestID,
            final String url,
            final Map<String, String> data
    ) {
        final String request = new ParametrizedRequest(requestID, data).toString();
        Allure.addAttachment(requestID, request);
        final String decodedUrl = UrlProcessing.getUrl(url, false);
        APIRequest.doRequestUrlWithoutResponse(decodedUrl, request, "синхронный".equals(type));
    }

    /**
     * API запрос будет выполняться если строковое условие соответствует  ожидаемому
     * Например:
     * И выполняет API запрос "admin.save.configuration" с параметрами если условие "stash#value" равно "false"
     *
     * @param requestID         - идентификатор API запроса
     * @param condition         - строковое условие
     * @param expectedCondition - ожидаемое строковое условие
     * @param data              - параметры API запроса
     */
    @И("^выполняет (синхронный) API запрос \"([^\"]*)\" с параметрами если условие \"([^\"]*)\" равно \"([^\"]*)\"$")
    @Когда("^выполняет() API запрос \"([^\"]*)\" с параметрами если условие \"([^\"]*)\" равно \"([^\"]*)\"$")
    public void parametrizedApiRequestCondition(
            final String type,
            final String requestID,
            final String condition,
            final String expectedCondition,
            final Map<String, String> data
    ) {
        final String request = new ParametrizedRequest(requestID, data).toString();
        Allure.addAttachment(requestID, request);
        if (DataProcessing.decodeValue(condition).equals(expectedCondition)) {
            APIRequest.doRequest(request, true, "синхронный".equals(type));
        }
    }

    /**
     * Иногда API запрос возвращает ответ, данные из которого нужны для следующего API запроса
     * Ответ может иметь вид {"data":{"student":{"addEventToTimetable":[{"id":"12"}]}}}
     * где нужно получить значение только параметра "id"
     * В этом случае шаг будет иметь вид
     * "И сохраняет поле "student:addEventToTimetable:id" из ответа запроса "stash#response" под кодом "slot_id""
     *
     * @param fields   - уникальные поля в ответе, в примере выше это "student:addEventToTimetable:id"
     * @param response - строка ответа
     * @param key      - stash ключ, под которым будет сохранено значение параметра
     */
    @И("^сохраняет поле \"([^\"]*)\" из ответа запроса \"([^\"]*)\" под кодом \"([^\"]*)\"$")
    public void getParamFromRequest(
            final String fields,
            final String response,
            final String key
    ) {
        final String data = APIResponse.getDataByKey(response, fields);
        Stash.put(key, data);
    }

    /**
     * Сохраянет все значения заданного поля из ответа на API запрос в список под заданным кодом, исключая null значения
     * Пример: Ответ может иметь вид {"data":{"student":{"getCodeReviewRoundsByStudentGoalId":[{"eventId":"0"},{"eventId":"141"}, {"eventId":"null"}]}}}
     * требуется сохранить поля eventId
     * сохранятся значения {0, 141}
     *
     * @param field    - уникальное поле в ответе, в примере выше это "student:getCodeReviewRoundsByStudentGoalId:eventId"
     * @param response - строка ответа
     * @param key      - stash ключ, под которым будет сохранено значение параметра
     */
    @И("^сохраняет все поля \"([^\"]*)\" из ответа на запрос \"([^\"]*)\" под кодом \"([^\"]*)\"$")
    public void saveParamListFromRequest(
            final String field,
            final String response,
            final String key
    ) {
        final List<String> data = APIResponse.getDataListByKey(response, field);
        Stash.put(key, data);
    }


    /**
     * Сохраняет элементы из строки, разделенные '|', в список под заданным кодом
     * Пример строка "123|name|year" будет сохранена списком {'123', 'name', 'year'}
     *
     * @param list строка с элементами
     * @param key  stash ключ, под которым будет сохранено значение
     */
    @И("^сохраняет все элементы из списка \"([^\"]*)\" под кодом \"([^\"]*)\"$")
    public void putListInStash(final String list, final String key) {
        final List<String> stringList = Arrays.stream(list.split("\\|")).map(String::trim).collect(Collectors.toList());
        Stash.put(key, stringList);
    }

    @Когда("^переходит по history back$")
    public void goToHistoryBack() {
        final WebDriver driver = Environment.getDriverService().getDriver();
        final JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.history.go(-1)");
    }

    /**
     * Запоминает строковую переменную в stash. Может комбинировать несколько строк с помощью "+"
     * <p>
     * Примеры использования:
     * И запоминает значение "value" в переменную "example"
     * В результате в stash будет сохранена строка "value"
     * <p>
     * И запоминает значение "value+0" в переменную "example"
     * В результате в stash будет сохранена строка "value0"
     * <p>
     * И запоминает значение "value+ +0" в переменную "example"
     * В результате в stash будет сохранена строка "value 0"
     * <p>
     * И запоминает значение "stash#value+ +stash#value" в переменную "example"
     * где stash#value = temp
     * В результате в stash будет сохранено "temp temp"
     *
     * @param valueText строковое значение, которое нужно сохранить,
     *                  может представлять собой комбинацию строк, разделенных знаком "+".
     * @param stashKey  Имя stash переменной, в которую запишется сохраняемое значение
     */

    @Когда("^запоминает значение \"([^\"]*)\" в переменную \"([^\"]*)\"$")
    public void saveStash(final String valueText, final String stashKey) {
        final String value = DriverUtils.generateValue(valueText);
        Stash.put(stashKey, value);
    }

    @Когда("^сверяет наименование файла из стеша \"([^\"]*)\" на эквивалентность \"([^\"]*)\"$")
    public void checkNameFile(final String stashFileName, final String expectedNameFile) {
        DriverUtils.checkNameFile(stashFileName, expectedNameFile);
    }

    @Когда("^запоминает значение \"([^\"]*)\" в переменную \"([^\"]*)\" с разделителем \"([^\"]*)\"$")
    public void saveStash(final String valueText, final String stashKey, final String delimiter) {
        final String value = Arrays.stream(valueText.split(delimiter))
                .map(i -> {
                            String temp = i;
                            if (!"".equals(i.trim())) {
                                temp = DataProcessing.decodeValue(i);
                            }
                            return temp;
                        }
                ).collect(Collectors.joining(""));
        Stash.put(stashKey, value);
    }

    @Когда("^преобразует значение url \"([^\"]*)\" в gitlab tag url \"([^\"]*)\"$")
    public void saveUrlTagGitlabStash(final String urlText, final String stashKey) {
        final String decodedUrlText = DataProcessing.decodeValue(urlText);
        final String target = ".git";
        final String replacement = "/-/tags/new";
        final String url = decodedUrlText.replace(target, replacement);
        Stash.put(stashKey, url);
    }

    @И("^запоминает список опций из дропдауна \"([^\"]*)\" в переменную \"([^\"]*)\"$")
    public void saveOptionsListToStash(final String elementNameOrPath, final String key) {
        final Select select = (Select) FindUtils.getElementByNameOrPath(elementNameOrPath);
        Stash.put(key, select.getAllOptions());
    }

    /**
     * Получает набор значений из полей списка и запоминает их в стэш в виде списка значений
     * <p>
     * Пример использования:
     * И запоминает набор значений поля "Название" из списка "Список навыков->Группа#HARD_SKILL" в переменную "skills"
     *
     * @param fieldName Имя поля в блоке
     * @param path      Список с параметрами или без
     * @param key       Ключ для стэша
     */
    @И("^запоминает набор значений поля \"([^\"]*)\" из списка \"([^\"]*)\" в переменную \"([^\"]*)\"$")
    public void saveValuesListToStash(final String fieldName, final String path, final String key) {
        final List<String> values = new BlockExtractor(path)
                .getInitializedBlockCollection()
                .stream()
                .map(block -> block
                        .getElementByName(fieldName)
                        .getText()

                )
                .collect(Collectors.toList());
        Stash.put(key, values);
    }

    //    шаг закрывает все оранжевые оповещения в верхней части страницы
    @И("^закрывает все системные оповещения$")
    public void closeSystemNotifications() {
        final BlockExtractor blockExtractor = new BlockExtractor("Список оповещений");
        final BooleanSupplier waitWhenNotificationBeClosed = () -> {
            final List<SystemNotificationListItem> notifications = blockExtractor.getInitializedBlockCollection();
            if (!notifications.isEmpty()) {
                ClickActions.safeClick("Оповещение", notifications.get(0).notificationCloseTextBlock);
                blockExtractor.reset();
                return false;
            }
            return true;
        };
        Timer.executeTimerThrowable(
                DriverConstants.TIMEOUT,
                "Не удалось закрыть оповещения",
                waitWhenNotificationBeClosed
        );
    }

    @И("^запоминает значение CSS атрибута \"([^\"]*)\" элемента \"([^\"]*)\" в переменную \"([^\"]*)\"$")
    public void stashCssAttribute(final String attributeName, final String nameOrPath, final String key) {
        final WebElement element = FindUtils.getElementByNameOrPath(nameOrPath);
        final String jsScript = "return window.getComputedStyle(arguments[0])['" + attributeName + "']";
        final String attributeValue = (String) DriverUtils.executeJS(jsScript, element);
        Stash.put(key, attributeValue);
    }

    @И("^заходит в iframe \"([^\"]*)\"$")
    public void goToIframe(final String iframeName) {
        final WebElement element = FindUtils.getElementByNameOrPath(iframeName);
        final List<WebElement> iframe = element.findElements(By.xpath("descendant-or-self::iframe"));
        if (iframe.isEmpty()) {
            throw new AutotestError(String.format("В элементе \"%s\" отсутствует iframe", iframeName));
        }
        Environment.getDriverService().getDriver().switchTo().frame(iframe.get(0));
    }

    @И("^выходит из iframe$")
    public void goOutFromIframe() {
        Environment.getDriverService().getDriver().switchTo().parentFrame();
    }

    /**
     * Шаг выполняет переключение на другой изолированный браузер.
     * Если он ещё не был открыт, то он откроется автоматически.
     * Учёт браузеров ведётся по номеру. Можно передать любой номер от 1 до 999.
     * Можно открыть браузер с номером 345 и потом переходить в него по этому же номеру.
     * Браузер по умолчанию всегда под номером 1. То-есть, после переключения на другой браузер, вернуться
     * обратно можно шагом И открывает браузер 1
     *
     * @param browserNumber номер браузера, от 1 до 999
     */
    @И("^открывает браузер (\\d*)$")
    public void openAnotherBrowser(final int browserNumber) {
        WebDriverSwitcher.changeBrowser(browserNumber);
        setScreenSize(System.getProperty("webdriver.browser.size.preset"));
    }

    @И("^открывает браузер (\\d*) с юзерагентом \"([^\"]*)\"$")
    public void openAnotherBrowserWithCustomUserAgent(final int browserNumber, final String userAgentKey) {
        WebDriverSwitcher.changeBrowser(browserNumber, userAgentKey);
        setScreenSize(System.getProperty("webdriver.browser.size.preset"));
    }

    @И("^меняет язык браузера на \"([^\"]*)\"$")
    public void setLanguageBrowser(final String languageKey) {
        WebDriverSwitcher.setLanguage(languageKey);
    }

    /**
     * Эмуляция часового пояса в браузере (не влияет на JVM)
     * Для смены часового пояса нужно указать числовое смещение от GMT, например:
     * +3 для Москвы, +10 для Владивостока, 0 для UTC
     * Вероятно, потребуется обновление страницы после смены часового пояса
     *
     * @param timeZoneOffset смещение часового пояса от -12 до +12
     */
    @И("^устанавливает часовой пояс в браузере ([-+]?\\d*)$")
    public void setTimeZone(final int timeZoneOffset) {
        TimeZone.set(timeZoneOffset);
    }

    @Когда("^открывает новую вкладку по URL \"([^\"]*)\"$")
    public void openNewTabUrl(final String url) {
        final WebDriver driver = Environment.getDriverService().getDriver();
        final JavascriptExecutor js = (JavascriptExecutor) driver;
        final String urlString = UrlProcessing.getUrl(url);
        js.executeScript("window.open('" + urlString + "', '_blank')");
        setScreenSize(System.getProperty("webdriver.browser.size.preset"));
    }

    @Когда("^прокрутить web страницу вниз$")
    public void scrollDown() {
        final WebDriver driver = Environment.getDriverService().getDriver();
        final JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.scrollBy(0,document.body.scrollHeight || document.documentElement.scrollHeight)");
    }

    @И("^проверяет что элемент \"([^\"]*)\" в фокусе$")
    public void checkIfElementActive(final String elementTitleOrPath) {
        final WebElement element = FindUtils.getElementByNameOrPath(elementTitleOrPath);
        final WebElement currentActiveElement = Environment
                .getDriverService()
                .getDriver()
                .switchTo()
                .activeElement();
        if (!Mover.getRemoteElement(element).equals(currentActiveElement)) {
            final String currentElementContent = (String) DriverUtils.executeJS(
                    "return arguments[0].innerHTML",
                    currentActiveElement
            );
            AllureUtils.attachMessageToAllureStep("Контент элемента в фокусе", currentElementContent);
            final String elementContent = (String) DriverUtils.executeJS("return arguments[0].innerHTML", element);
            AllureUtils.attachMessageToAllureStep("Контент ожидаемого элемента", elementContent);

            final String currentElementClass = currentActiveElement.getAttribute("class");
            AllureUtils.attachMessageToAllureStep("Класс элемента в фокусе", currentElementClass);
            final String elementClass = element.getAttribute("class");
            AllureUtils.attachMessageToAllureStep("Класс ожидаемого элемента", elementClass);

            final byte[] currentElementImage = ImageProcessing.takeScreenShotFragment(currentActiveElement);
            AllureUtils.attachScreenShotToAllure("Элемент в фокусе", currentElementImage);
            final byte[] elementImage = ImageProcessing.takeScreenShotFragment(element);
            AllureUtils.attachScreenShotToAllure("Ожидаемый элемент", elementImage);
            throw new AutotestError(String.format("Элемент \"%s\" сейчас не в фокусе", elementTitleOrPath));
        }
    }

    @И("^выделяет текст в элементе \"([^\"]*)\"$")
    public void selectText(final String elementTitleOrPath) {
        final String js = "function setSelection(a) {let rng, sel, target = a;if (document.createRange) {\n" +
                "rng = document.createRange();rng.selectNode(target);sel = window.getSelection();\n" +
                "sel.removeAllRanges();sel.addRange(rng);} else {let rng = document.body.createTextRange();\n" +
                "rng.moveToElementText(target);rng.select();}}setSelection(arguments[0]);";
        final WebElement element = FindUtils.getElementByNameOrPath(elementTitleOrPath);
        DriverUtils.executeJS(js, element);
    }

    @И("^проверяет роут \"([^\"]*)\"$")
    public void checkEndPoint(final String page) {
        RoutEvaluator.evaluate(page, new ArrayList<>());
    }

    @И("^проверяет роут \"([^\"]*)\" с параметрами$")
    public void checkEndPointWithParams(final String page, final List<String> params) {
        RoutEvaluator.evaluate(page, params);
    }

    // Шаг используется для регистрации секретного ключа из QR кода
    @И("^запоминает секретный ключ по QR коду \"([^\"]*)\" в переменую \"([^\"]*)\"$")
    public void rememberSecretKey2FA(final String qrCodeElementName, final String key) {
        final QRCode qrCode = (QRCode) FindUtils.getElementByNameOrPath(qrCodeElementName);
        Stash.put(key, qrCode.getSecret());
    }

    // Шаг используется для получения одноразового пароля из google authenticator
    @И("^запоминает одноразовый пароль по секретному ключу \"([^\"]*)\" в переменую \"([^\"]*)\"$")
    public void rememberOneTimePassword(final String secretKey, final String key) {
        Stash.put(key, QRCode.getOneTimePasswordFromGoogleAuth(DataProcessing.decodeValue(secretKey)));
    }

    @И("^запоминает название стенда в переменную \"([^\"]*)\"$")
    public void getStand(final String stashKey) throws MalformedURLException {
        final URL currentUrl = new URL(Environment
                .getDriverService()
                .getDriver()
                .getCurrentUrl());
        final String url = currentUrl.toString();
        final Pattern pattern = Pattern.compile("(dev\\d-\\d{1,3})");
        final Matcher matcher = pattern.matcher(url);
        Stash.put(stashKey, matcher.find() ? matcher.group(1) : "");
    }

    /**
     * В приложении id многих компонентов можно узнать по их url
     * например:
     * https://devX-XX.pcbltools.ru/trajectory/12871
     * https://devX-XX.pcbltools.ru/task/19340
     * https://devX-XX.pcbltools.ru/materials/modules/12925
     * В таком случае id элемента можно получить как:
     * "И запоминает id в url "stash#url" в переменную "task_id""
     *
     * @param url      полный url компонента
     * @param stashKey Ключ для stash
     */
    @И("^запоминает id в url \"([^\"]*)\" в переменную \"([^\"]*)\"$")
    public void getUrlPart(final String url, final String stashKey) {
        final String url_temp = DataProcessing.decodeValue(url);
        final String id = url_temp.replaceAll("[^0-9+]", "");
        Stash.put(stashKey, id);
    }

    @И("^сверяет значение переменной \"([^\"]*)\" на паттерн \"([^\"]*)\"$")
    public void checkSubstringStashKey(final String stashKey, final String expectedDraft) {
        final String value = DataProcessing.decodeValue(stashKey);
        final String expected = DataProcessing.decodeValue(expectedDraft);
        Assert.assertTrue(
                String.format(
                        "переменная \"%s\" не содержит \"%s\"",
                        value,
                        expected
                ),
                value.matches(expected)
        );
    }

    @И("^(ставит|снимает) задержку сети$")
    public void setNetworkThrottling(final String option) {
        if ("ставит".equals(option)) {
            DevTools.getNetwork().emulateNetworkConditions(false, 40d, 1500d, 750d);
        } else {
            DevTools.getNetwork().emulateNetworkConditions(false, 2d, 50000d, 50000d);
        }
    }

    // оптимальные значения задержки сети 2000 - 3000.
    @И("^ставит задержку сети (\\d*)$")
    public void setNetworkThrottling(final double download) {
        DevTools.getNetwork().emulateNetworkConditions(false, 40d, download, download / 2);
    }
}
