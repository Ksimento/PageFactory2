package ru.sbt.edu_power.e2e_core.survey;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import ru.sbt.edu_power.e2e_core.CoreConstants;
import ru.sbt.edu_power.e2e_core.survey.enums.ClickTypes;
import ru.sbt.edu_power.e2e_core.survey.enums.LoadControlMethod;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbt.edu_power.e2e_core.elements.FieldFillingException;
import ru.sbt.edu_power.e2e_core.actions.Mover;
import ru.sbt.edu_power.e2e_core.actions.PageControls;
import ru.sbt.edu_power.e2e_core.blocks.BlockExtractor;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbtqa.tag.pagefactory.environment.Environment;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

@Slf4j
public class SurveyActions {

    public static void clickWithChangeControl(final String elementName) {
        clickWithChangeControl(elementName, LoadControlMethod.BY_CONTAINER_NAME, null);
    }

    public static void clickWithChangeControl(
            final String elementName,
            final LoadControlMethod method,
            final String param
    ) {
        clickWithChangeControl(elementName, DriverUtils.getElementByTitle(elementName), method, param);
    }

    public static void clickWithChangeControl(
            final String elementName,
            final WebElement element,
            final LoadControlMethod method,
            final String param
    ) {
        if (method == LoadControlMethod.BY_BLOCK) {
            Assert.assertNotNull("Параметр для вычисления блоков не может быть пустым", param);
        }
        final int pageStateBefore = getPageState(method, param, DataProcessing.generator("word30"));
        clickWithChangeControl(
                elementName,
                element,
                method,
                param,
                pageStateBefore,
                DataProcessing.generator("word30"),
                ClickTypes.NATIVE
        );
    }

    /**
     * Метод пытается выполнить эффективный клик по веб-элементу. Если клик не привёл к изменению
     * страницы, метод будет перевызван рекурсивно и так в течении определённого времени. Если изменения страницы
     * получить не удалось по истечении времени таймаута, будет выкинуто исключение - это не позволит продолжать
     * неконтроллируемый сценарий
     *
     * @param elementName     имя элемента
     * @param element         Веб-элемент, по которому нужно выполнить клик
     * @param method          способ определить изменения
     * @param param           параметры для определения изменений
     * @param pageStateBefore состояние страницы до клика
     * @param timerName       имя таймера
     */
    private static void clickWithChangeControl(
            final String elementName,
            final WebElement element,
            final LoadControlMethod method,
            final String param,
            final int pageStateBefore,
            final String timerName,
            final ClickTypes clickType
    ) {
        clickGun(elementName, element, clickType);
        final boolean isPageLoaded = pageLoadControl(
                method,
                param,
                pageStateBefore,
                DataProcessing.generator("word30")
        );
        final boolean isTimerActive = !Timer.isTimeout(timerName, DriverConstants.ELEMENT_WAIT_5SEC);
        if (!isPageLoaded) {
            if (isTimerActive) {
                clickWithChangeControl(
                        elementName,
                        element,
                        method,
                        param,
                        pageStateBefore,
                        timerName,
                        getNextClickType(clickType)
                );
                return;
            }
            throw new AutotestError(String.format(
                    "Клик по элементу \"%s\" не привёл к изменениям страницы",
                    elementName
            ));
        }
    }

    private static ClickTypes clickGun(final String elementName, final WebElement element, final ClickTypes clickType) {
        final boolean clickResult = safeClick(element, 3, clickType);
        ClickTypes successfullyClickType = clickType;
        if (!clickResult) {
            if (clickType == ClickTypes.ACTION) {
                throw new AutotestError("Не удалось выполнить клик ни одним из возможных способов");
            }
            successfullyClickType = clickGun(elementName, element, getNextClickType(clickType));
        }
        return successfullyClickType;
    }

    private static ClickTypes getNextClickType(final ClickTypes currentType) {
        final ClickTypes nextClickType;
        if (currentType == ClickTypes.NATIVE) {
            nextClickType = ClickTypes.JS;
        } else if (currentType == ClickTypes.JS) {
            nextClickType = ClickTypes.ACTION;
        } else {
            nextClickType = ClickTypes.NATIVE;
        }
        return nextClickType;
    }

    /**
     * Метод возвращает число, характеризующее состояние страницы в зависимости от переданных параметров.
     * Если мы проверяем по списку блоков, то метод вернёт их количество.
     * Если проверяем по содержимому, то метод вернёт хэш контейнера
     * Метод будет пытаться получить состояние рекурсивно вызывая себя в течении некоторого времени, по истечении
     * которого выбросит исключение, если получить состояние не удалось.
     *
     * @param method    способ определить изменения
     * @param param     параметры для определения изменений
     * @param timerName имя таймера
     * @return число блоков или хэш содержимого контейнера
     */
    private static int getPageState(final LoadControlMethod method, final String param, final String timerName) {
        int result = 0;
        try {
            switch (method) {
                case BY_BLOCK:
                    result = new BlockExtractor(param).getBlockCollection().size();
                    break;
                case BY_MODAL:
                    result = DriverUtils
                            .findElementsOnPageByXpath(CoreConstants.SURVEY_MODAL)
                            .size();
                    break;
                case BY_CONTAINER_NAME:
                    result = DriverUtils.executeJS(
                            "return arguments[0].innerHTML",
                            getContainerByName(param)
                    ).hashCode();
                    break;
                case BY_XPATH:
                    result = DriverUtils.executeJS(
                            "return arguments[0].innerHTML",
                            getContainerByXPath(param)
                    ).hashCode();
                    break;
                default:
                    throw new AutotestError("Метод получения состояния страницы не реализован: " + method.name());

            }
        } catch (final Throwable e) {
            final String message = "Не удалось получить содержимое элемента: " +
                                   (param == null ? "Область контента" : param);
            if (!Timer.isTimeoutThrowable(timerName, DriverConstants.ELEMENT_WAIT_5SEC, message)) {
                DriverUtils.freeze(DriverConstants.FREEZE_500_MS);
                result = getPageState(method, param, timerName);
            }
        }
        return result;
    }

    private static WebElement getContainerByName(final String name) {
        return name == null ?
                getContainer(CoreConstants.CONTENT_WRAPPER) : DriverUtils.getElementByTitle(name);
    }

    private static WebElement getContainerByXPath(final String xPath) {
        return xPath == null ?
                getContainer(CoreConstants.CONTENT_WRAPPER) : getContainer(xPath);
    }

    private static WebElement getContainer(final String xpath) {
        return Environment
                .getDriverService()
                .getDriver()
                .findElement(By.xpath(xpath));
    }

    /**
     * Метод выполняет клик по элементу через экшен (не нативно)
     * В некоторых случаях нативный клик не приводит к изменениям в формах Survey, поэтому его выполнять бессмысленно
     *
     * @param element  Элемент по которому делаем клик
     * @param attempts Количество попыток для клика в случае выкидывания исключения (например, скролл не доехал)
     */
    private static boolean safeClick(
            final WebElement element,
            int attempts,
            final ClickTypes clickType
    ) {
        if (attempts == 0) {
            return false;
        }
        if (attempts < 2) {
            DriverUtils.executeJS("arguments[0].scrollIntoView", element);
            DriverUtils.freeze(DriverConstants.FREEZE_250_MS);
        }
        final boolean result;
        switch (clickType) {
            case NATIVE:
                result = nativeClick(element);
                break;
            case ACTION:
                Mover.scrollToElement(element);
                result = actionClick(element);
                break;
            default:
                Mover.scrollToElement(element);
                result = jsClick(element);
                break;
        }
        if (!result) {
            return safeClick(element, --attempts, clickType);

        }
        return true;
    }

    /**
     * Метод в течении 30 секунд ожидает изменения на странице, как только они произошли
     * возвращает true
     *
     * @param method          способ определить изменения
     * @param param           параметры для определения изменений
     * @param pageStateBefore состояние страницы до клика
     * @param timerName       имя таймера
     * @return true если страница изменилась, false по таймауту
     */
    private static boolean pageLoadControl(
            final LoadControlMethod method,
            final String param,
            final int pageStateBefore,
            final String timerName
    ) {
        PageControls.waitWhileNetworkActive();

        final AtomicReference<Integer> pageStateAfter = new AtomicReference<>(0);

        final BooleanSupplier waitWhenPageReturnState = () -> {
            pageStateAfter.set(getPageState(method, param, DataProcessing.generator("word30")));
            return pageStateBefore != pageStateAfter.get();
        };

        return Timer.executeTimer(
                timerName,
                DriverConstants.ELEMENT_WAIT_5SEC,
                waitWhenPageReturnState
        );
    }

    private static boolean nativeClick(final WebElement element) {
        try {
            element.click();
        } catch (final Throwable e) {
            Mover.scrollToElement(element);
            return false;
        }
        return true;
    }

    private static boolean actionClick(final WebElement element) {
        try {
            final WebElement locatable = Mover.getRemoteElement(element);
            new Actions((WebDriver) Environment.getDriverService().getDriver()).click(locatable).build().perform();
        } catch (final Throwable e) {
            return false;
        }
        return true;
    }

    private static boolean jsClick(final WebElement element) {
        try {
            DriverUtils.executeJS("arguments[0].click()", element);
        } catch (final Throwable e) {
            return false;
        }
        return true;
    }

    public static void clickExecutor(
            final String elementName,
            final WebElement element,
            final BooleanSupplier function
    ) throws FieldFillingException {
        final AtomicReference<ClickTypes> clickType = new AtomicReference<>(ClickTypes.NATIVE);

        final BooleanSupplier waitWhenClickBeEffectively = () -> {
            if (function.getAsBoolean()) {
                return true;
            }
            safeClick(element, 3, clickType.get());
            clickType.set(getNextClickType(clickType.get()));
            DriverUtils.freeze(DriverConstants.FREEZE_250_MS);
            return false;
        };

        final boolean result = Timer.executeTimer(DriverConstants.ELEMENT_WAIT_5SEC, waitWhenClickBeEffectively);
        if (!result) {
            final String message = "Не удалось выполнить клик по элементу " + elementName;
            throw new FieldFillingException(message);
        }
    }
}
