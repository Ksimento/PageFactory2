package ru.sbt.edu_power.e2e_core.driver_utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbt.edu_power.external_services.validator.Validatable;
import ru.sbt.edu_power.external_services.validator.Validator;
import ru.sbtqa.tag.datajack.Stash;
import ru.sbtqa.tag.pagefactory.Page;
import ru.sbtqa.tag.pagefactory.PageManager;
import ru.sbtqa.tag.pagefactory.annotations.ElementTitle;
import ru.sbtqa.tag.pagefactory.context.PageContext;
import ru.sbtqa.tag.pagefactory.environment.Environment;
import ru.sbtqa.tag.pagefactory.exception.ElementSearchError;
import ru.sbtqa.tag.pagefactory.exceptions.PageException;
import ru.sbtqa.tag.pagefactory.exceptions.PageInitializationException;
import ru.sbtqa.tag.qautils.errors.AutotestError;
import ru.yandex.qatools.htmlelements.element.Link;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DriverUtils {
    public static Actions getActions() {
        return new Actions((WebDriver) Environment.getDriverService().getDriver());
    }

    /**
     * Метод вызывает остановку потока на указанное количество миллисекунд
     *
     * @param millis длительность остановки в миллисекундах
     */
    public static void freeze(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (final InterruptedException e) {
            throw new AutotestError("Слип сломался", e);
        }
    }

    public static boolean isHeadless(){
        return System.getProperty("webdriver.chrome.capability.options.args").contains("headless");
    }

    public static WebDriver getWebDriver(){
        return Environment.getDriverService().getDriver();
    }

    /**
     * Конвертор строки в булево значение. Вернёт true если в него передать "да" или "true"
     * в любом регистре, иначе false
     *
     * @param state логическое состояние в виде строки
     * @return булево значение
     */
    public static boolean getBoolean(final String state) {
        final boolean isStateValid = "да".equalsIgnoreCase(state)
                                     || "true".equalsIgnoreCase(state)
                                     || "нет".equalsIgnoreCase(state)
                                     || "false".equalsIgnoreCase(state);
        Assert.assertTrue("Значение должно быть Да, TRUE, Нет, FALSE в любом регистре", isStateValid);
        return "да".equalsIgnoreCase(state) || "true".equalsIgnoreCase(state);
    }

    public static boolean empty(final String value) {
        return value == null || value.isEmpty();
    }

    public static Integer getNumber(final String param) {
        boolean isInteger = true;
        for (final char s : param.toCharArray()) {
            if (((int) s < 48 || (int) s > 57) && s != '-') {
                isInteger = false;
                break;
            }
        }
        return isInteger ? Integer.valueOf(param) : null;
    }

    /**
     * Метод вынимает целочисленные значения из текстовой строки по его порядковому номеру в этой строке
     * Например в строке: Ещё 95 очков до 6-го уровня, всего 245 очков опыта
     * по порядковому номеру "3" будет получено значение "245"
     *
     * @param stringWithNumber текстовая строка содержащая числовые значения в произвольном месте
     * @param numberPosition порядковый номер числового значения
     * @return Integer значение числа
     */
    public static int extractNumberFromString(final String stringWithNumber, final int numberPosition) {
        final String[] parts = stringWithNumber.split("[a-zA-Zа-яёА-ЯЁ\\s{}\\[\\]!@#$%^&*().,<>/|\\\\'\"?]+");
        final String value = Stream.of(parts)
                                   .map(v -> v.replaceAll("\\D*$", ""))
                                   .filter(v -> v.matches("[-+]*\\d+"))
                                   .collect(Collectors.toList())
                                   .get(numberPosition - 1);
        return Integer.parseInt(value);
    }

    /**
     * Метод позволяет выполнить произвольный JS код над элементом
     *
     * @param script  Javascript, например "arguments[0].innerHTML = 'Abracadabra'"
     * @param element WebElement
     * @return результат выполнения скрипта
     */
    public static Object executeJS(final String script, final WebElement element) {
        return ((JavascriptExecutor) Environment.getDriverService().getDriver()).executeScript(script, element);
    }

    public static Object executeJS(final String script) {
        return ((JavascriptExecutor) Environment.getDriverService().getDriver()).executeScript(script);
    }

    /**
     * Метод выполняет инициализацию объекта страницы PageObject. Эта страница становится активной (текущей)
     * и досупна через PageContext.getCurrentPage()
     *
     * @param pageClass класс страницы
     * @return экземпляр этой страницы
     */
    public static Page getPage(final Class<? extends Page> pageClass) {
        try {
            return PageManager.getPage(pageClass);
        } catch (final PageInitializationException e) {
            throw new AutotestError(String.format("Ошибка инициализации страницы \"%s\"", pageClass.getName()), e);
        }
    }

    /**
     * Метод возвращает экземпляр элемента страницы из PageObject по его названию (ElementTitle)
     *
     * @param elementTitle Название элемента
     * @param <T>          Класс элемента должен имплементировать WebElement
     * @return экземпляр элемента соответствующего класса
     */
    public static <T extends WebElement> T getElementByTitle(final String elementTitle) {
        try {
            return Environment.getFindUtils().getElementByTitle(PageContext.getCurrentPage(), elementTitle);
        } catch (final PageException e) {
            throw new IllegalArgumentException(String.format(
                    "Элемент \"%s\" не описан в классе страницы \"%s\"",
                    elementTitle,
                    PageContext.getCurrentPage().getClass()
            ), e);
        }
    }

    public static List<WebElement> findElementsOnPageByXpath(final String xpath) {
        return Environment
                .getDriverService()
                .getDriver()
                .findElements(By.xpath(xpath));
    }

    public static String getElementXPath(final WebElement element) {
        return (String) executeJS(
                "getXPath = function (node) {if (node.id !== '') {\n" +
                "return '//' + node.tagName.toLowerCase() + '[@id=\"' + node.id + '\"]'}\n" +
                "if (node === document.body) {return '//' + node.tagName.toLowerCase()}\n" +
                "let nodeCount = 0;let childNodes = node.parentNode.childNodes;\n" +
                "for (let i = 0; i < childNodes.length; i++) {let currentNode = childNodes[i];\n" +
                "if (currentNode === node) {\n" +
                "return getXPath(node.parentNode) + '/' + node.tagName.toLowerCase() + '[' + (nodeCount + 1) + ']'}\n" +
                "if (currentNode.nodeType === 1 && currentNode.tagName.toLowerCase() === node.tagName.toLowerCase()) {\n" +
                "nodeCount++}}};return getXPath(arguments[0]);",
                element
        );

    }

    /**
     * Метод проверяет, отображается ли веб-элемент в браузере
     *
     * @param webElement объект элемента
     * @return true если отображается, false если нет
     */
    public static boolean isElementDisplayed(final WebElement webElement) {
        if (null == webElement) {
            return false;
        }
        try {
            return webElement.isDisplayed();
        } catch (final NoSuchElementException | StaleElementReferenceException e) {
            return false;
        }
    }

    /**
     * Метод проверяет, отображается ли веб-элемент в браузере
     *
     * @param xpath   xpath объекта
     * @param context веб-элемент в контексте которого нужно найти элемент.
     *                Если нужно искать на странице, то нужно передать null
     * @return true если отображается, false если нет
     */
    public static boolean isElementDisplayed(final String xpath, final WebElement context) {
        final List<WebElement> elementList = new ArrayList<>();
        if (null == context) {
            elementList.addAll(findElementsOnPageByXpath(xpath));
        } else {
            elementList.addAll(context.findElements(By.xpath(xpath)));
        }
        return !elementList.isEmpty() && elementList.get(0).isDisplayed();
    }

    /**
     * Метод получает параметр xpath из аннотации FindBy элемента текущей страницы
     *
     * @param elementTitle название элемента ElementTitle
     * @return строка xpath
     */
    public static String getXpath(final String elementTitle) {
        return getXpath(elementTitle, PageContext.getCurrentPage().getClass());
    }

    /**
     * Метод получает параметр xpath из аннотации FindBy
     *
     * @param elementTitle название элемента ElementTitle
     * @param blockClass   класс, в котором расположен элемент
     * @return строка xpath
     */
    public static String getXpath(final String elementTitle, final Class blockClass) {
        if (null == blockClass) {
            return getXpath(elementTitle);
        }
        for (final Field fieldClass : blockClass.getFields()) {
            if (fieldClass.isAnnotationPresent(ElementTitle.class)
                    && fieldClass.getAnnotation(ElementTitle.class).value().equals(elementTitle)) {
                return fieldClass.getAnnotation(FindBy.class).xpath();
            }
        }
        throw new ElementSearchError(String.format(
                "Элемент \"%s\" не описан на странице \"%s\"",
                elementTitle,
                blockClass.getName()
        ));
    }

    public static void checkNameFile(final String stashFileName,final String expectedNameFile){
        final String decodeNameFile = generateValue(expectedNameFile);
        Assert.assertTrue(String.format("Имя файла не соответствует ожидаемому \"%s\"", decodeNameFile), Stash.getValue(stashFileName).toString().endsWith(decodeNameFile));
    }

    public static String generateValue(final String valueText){
      return Arrays.stream(valueText.split("\\+")).map((i) -> {
            String temp = i;
            if (!"".equals(i.trim())) {
                temp = DataProcessing.decodeValue(i);
            }

            return temp;
        }).collect(Collectors.joining(""));
    }

    public static void staleElementCatcher(final Runnable function) {
        final BooleanSupplier f = () -> {
            try {
                function.run();
                return true;
            } catch (final StaleElementReferenceException e) {
                return false;
            }
        };
        Timer.executeTimerThrowable(DriverConstants.ELEMENT_WAIT_5SEC, "Что-то пошло не так", f);
    }

    /**
     * Метод осуществляет поиск элемента по локатору в родительских узлах
     *
     * @param element элемент относительно которого будет произведен поиск
     * @param xpath   локатор по которому будет осуществляться поиск
     */
    public static List<WebElement> searchParenElements(WebElement element, final String xpath) {
        List<WebElement> elements;
        for (int i = 0; i < 3; i++) {
            elements = element.findElements((By.xpath(xpath)));
            if (!elements.isEmpty()) {
                return elements;
            } else {
                element = element.findElement((By.xpath("parent::*")));
            }
        }
        throw new AutotestError(String.format("В родительских узлах не найден локатор \"%s\"", xpath));
    }

    public static ValidatedValue validateField(final WebElement element, final String expected) {
        final boolean validateResult;
        final String actualValue;
        if (element instanceof Validatable) {
            actualValue = ((Validatable) element).getFieldValue();
            validateResult = ((Validatable) element).validate(expected);
        } else if (element instanceof Link) {
            actualValue = ((Link) element).getReference();
            validateResult = Validator.matchValues(actualValue, DataProcessing.decodeValue(expected));
        } else {
            actualValue = element
                    .getText()
                    .trim()
                    .replaceAll("\n", " ")
                    .replaceAll("\u00AD", "")
                    .replaceAll("\u00A0", " ");
            validateResult = Validator.matchValues(actualValue, DataProcessing.decodeValue(expected));
        }
        return new ValidatedValue(actualValue, validateResult);
    }

    @AllArgsConstructor
    @Getter
    public static class ValidatedValue {
        private final String actualValue;
        private final Boolean validateResult;
    }
}
