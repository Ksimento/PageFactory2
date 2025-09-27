package ru.sbt.edu_power.e2e_core.actions;

import com.github.kklisura.cdt.services.exceptions.ChromeDevToolsInvocationException;
import io.qameta.allure.Allure;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Evaluator;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.allure.AllureUtils;
import ru.sbt.edu_power.e2e_core.blocks.BlockExtractor;
import ru.sbt.edu_power.e2e_core.blocks.abstract_blocks.AbstractToastListItem;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbt.edu_power.e2e_core.devtools.DevTools;
import ru.sbt.edu_power.e2e_core.devtools.RuntimeControl;
import ru.sbt.edu_power.e2e_core.devtools.network.NetworkControl;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.e2e_core.error_processing.NotCriticalErrorAccumulator;
import ru.sbt.edu_power.e2e_core.fields.FindUtils;
import ru.sbt.edu_power.e2e_core.widgets.IsUps404Widget;
import ru.sbt.edu_power.e2e_core.widgets.Widget;
import ru.sbt.edu_power.external_services.ESUtils;
import ru.sbt.edu_power.external_services.shared.fail_categories.JavaUiFailCategoriesEdu;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbtqa.tag.datajack.Stash;
import ru.sbtqa.tag.pagefactory.context.PageContext;
import ru.sbtqa.tag.pagefactory.environment.Environment;
import ru.sbtqa.tag.qautils.errors.AutotestError;
import ru.sbtqa.tag.qautils.properties.Props;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Slf4j
public class PageControls {
    private static final String APP_AND_LOGIN_XPATH =
            "//div[@id = 'app-root' or @id='root' or @id = 'login-screen'] | //body[@class='login-totp-config' or @class = 'sms-validation']";
    static final String TOAST_LIST_NAME = Props.get("TOAST_LIST_NAME");
    private static final boolean ALERT_FOR_PRELOADER = "true".equals(Props.get("alert.for.preloader", "true"));
    private static final String PRELOADER = Props.get("PRELOADER");
    private static final String WIDGET_UPS_404_NAME = Props.get("WIDGET_UPS_404_NAME");
    private static final AtomicLong CRASHED_TIME = new AtomicLong(0);
    private static final int CRASH_TIMEOUT_5_MINUTES = 5 * 60;
    private static Supplier<List<Evaluator>> EXCLUSION_PAGE_FRAGMENTS_LIST;

    public static void configure(final Supplier<List<Evaluator>> EXCLUSION_PAGE_FRAGMENTS_LIST) {
        PageControls.EXCLUSION_PAGE_FRAGMENTS_LIST = EXCLUSION_PAGE_FRAGMENTS_LIST;
    }

    public static void refreshPage() {
        Environment.getDriverService().getDriver().navigate().refresh();
        try {
            Environment.getDriverService().getDriver().switchTo().alert().accept();
        } catch (final Exception ignored) {
        }
        waitWhileNetworkActive();
        waitWhenPreloaderIsGone();
        detectBackendErrors();
        detectConsoleErrors();
        detectUps404Errors();
        DriverUtils.getPage(PageContext.getCurrentPage().getClass());
    }

    public static void waitWhileNetworkActive() {
        final BooleanSupplier preloaderTracking = PageControls::connectionIsActive;

        final BooleanSupplier waitWithDelay = () ->
                !Timer.executeTimerMillis(DriverConstants.FREEZE_500_MS * 2, preloaderTracking);

        final boolean firstWait = Timer.executeTimer(DriverConstants.TIMEOUT, waitWithDelay);

        final boolean isAppPresent = !DriverUtils
                .findElementsOnPageByXpath(APP_AND_LOGIN_XPATH).isEmpty();

        // Если не отображается прелоадер и отображается окно приложения, считаем что загрузка завершена,
        // даже если сеть активна
        if (isAppPresent) {
            final List<String> activeConnections = new ArrayList<>(NetworkControl
                    .getNetworkActiveConnectionInstance()
                    .getActiveConnection());
            if (!activeConnections.isEmpty()) {
                activeConnections.forEach(id -> {
                    final String request = NetworkControl
                            .getNetworkActiveConnectionInstance()
                            .getConnectionStat(id)
                            .getRequestBody();
                    Allure.addAttachment(
                            "Запрос не завершился вовремя: " + id,
                            request == null ? "Запрос не определён" : request.replaceAll("\\\\n", "\n")
                    );
                });
            }
            NetworkControl.getNetworkActiveConnectionInstance().purge();
            return;
        }

        if (!firstWait && "false".equals(Props.get("testing.safemode", "true"))) {
            final List<String> activeConnection = NetworkControl
                    .getNetworkActiveConnectionInstance()
                    .getActiveConnection();
            activeConnection.forEach(id -> {
                final String requestBody = NetworkControl
                        .getNetworkActiveConnectionInstance()
                        .getConnectionStat(id)
                        .getRequestBody()
                        .replaceAll("\\\\n", "\n");
                Allure.addAttachment("Запрос не закончился вовремя " + id, requestBody);
            });
//            final String message = "Прелоадер не завершился в течении времени таймаута " +
//                                   DriverConstants.TIMEOUT +
//                                   " сек";
//            throw new AutotestError(message);
        }

        if (!firstWait && CRASHED_TIME.get() < CRASH_TIMEOUT_5_MINUTES * 1000L) {
            waitOperations(PageControls::connectionIsActive);
        }

        if (CRASHED_TIME.get() < CRASH_TIMEOUT_5_MINUTES * 1000L) {
            waitOperations(PageControls::beeIsNotFlyingOverApp);
        }

        if (CRASHED_TIME.get() < CRASH_TIMEOUT_5_MINUTES * 1000L) {
            waitOperations(PageControls::noConnectionErrorDetected);
        }
    }

    public static void waitWhenPreloaderIsGone() {
        Timer.startTimer("waitWhenPreloaderIsGone");
        ESUtils.freeze(10);
        final BooleanSupplier preloaderTracking = PageControls::preloaderIsActive;
        final boolean result = Timer.executeTimer(DriverConstants.TIMEOUT, () ->
                !Timer.executeTimerMillis(DriverConstants.FREEZE_500_MS, preloaderTracking)
        );
        if (ALERT_FOR_PRELOADER && !result) {
            try {
                final List<WebElement> elements = DriverUtils.findElementsOnPageByXpath(PRELOADER);
                Mover.moveToElement(elements.get(0));
            } catch (final Throwable ignore) {}
            AllureUtils.attachScreenShotToAllure("Preloader");
            Allure.addAttachment("Содержимое страницы", Environment.getDriverService().getDriver().getPageSource());
            publicateLastBackResponse();
            NotCriticalErrorAccumulator.setNotCriticalError("Прелоадер на странице не завершился", JavaUiFailCategoriesEdu.PRELOADER);
            NotCriticalErrorAccumulator.setStepBroken();
        }
    }

    private static boolean connectionIsActive() {
        return !NetworkControl.isConnectionNotActive();
    }

    private static boolean preloaderIsActive() {
        final List<WebElement> elements = DriverUtils.findElementsOnPageByXpath(PRELOADER);
        if (elements.isEmpty()) {
            return false;
        }
        return DriverUtils.isElementDisplayed(elements.get(0));
    }

    private static boolean beeIsNotFlyingOverApp() {
        final String beeXpath = "//div[contains(text(), '\uD83D\uDC1D') and @class = 'emoji']";
        return DriverUtils.findElementsOnPageByXpath(beeXpath).isEmpty();
    }

    private static boolean noConnectionErrorDetected() {
        final String errorXpath = "//*[text() = 'Соединение не установлено']";
        return DriverUtils.findElementsOnPageByXpath(errorXpath).isEmpty();
    }

    private static void waitOperations(final BooleanSupplier booleanSupplier) {
        final String timerName = DataProcessing.generator("word30");
        Timer.startTimer(timerName);
        final AtomicBoolean lowAvailability = new AtomicBoolean(false);
        final int timeout = (int) (CRASH_TIMEOUT_5_MINUTES - (CRASHED_TIME.get() / 1000));
        final BooleanSupplier waitWithReload = () -> {
            final boolean waitResult = Timer.executeTimer(DriverConstants.TIMEOUT, booleanSupplier);
            if (!waitResult) {
                Environment.getDriverService().getDriver().navigate().refresh();
                DriverUtils.freeze(DriverConstants.FREEZE_500_MS * 2);
                log.info("Выполняется расход резервного времени по причине проблем с доступностью приложения");
                lowAvailability.set(true);
                return false;
            }
            return true;
        };
        final boolean waitResult = Timer.executeTimer(timeout, waitWithReload);
        CRASHED_TIME.set(CRASHED_TIME.get() + Timer.getDelta(timerName, "App crashed"));
        if (lowAvailability.get()) {
            log.info("Общее время недоступности приложения: {} минут", CRASHED_TIME.get() / 60000);
        }
        Timer.removeTimer(timerName);
        if (!waitResult) {
            throw new AutotestError("Приложение израсходовало лимит на 30 минут простоя");
        }
    }

    public static void detectBackendErrors() {
        if (Stash.asMap().containsKey("no_toast") && "true".equals(Stash.getValue("no_toast"))) {
            return;
        }
        final boolean result = Timer.executeTimerMillis(
                DriverConstants.FREEZE_250_MS,
                () -> !NetworkControl.getAppErrorLis().isEmpty()
        );
        final AtomicBoolean flagRequest = new AtomicBoolean(false);
        if (result) {
            final List<String> errorMessages = new ArrayList<>();
            NetworkControl.getAppErrorLis().forEach((requestId, trace) -> {
                String postData;
                try {
                    postData = DevTools.getNetwork().getRequestPostData(requestId);
                } catch (ChromeDevToolsInvocationException e) {
                    postData = "Запрос не определён";
                }
                Allure.addAttachment("Запрос в бэкенд ID " + requestId, postData
                        .replaceAll("\\\\n", "\n")
                );
                Allure.addAttachment("Стектрейс ошибки бэкенда ID " + requestId, trace);
                errorMessages.add(trace);
                flagRequest.set(true);
            });
            if (flagRequest.get()) {
                NotCriticalErrorAccumulator.setNotCriticalError(
                        "При выполнении операции с бэкенда переданы ошибки",
                        String.join(" ", errorMessages)
                );
            }
            NetworkControl.clearAppErrorList();
            NotCriticalErrorAccumulator.setStepBroken();
        }
    }

    public static void detectConsoleErrors() {
        if (!"true".equals(Props.get("js.console.error.tracking"))) {
            return;
        }
        final boolean result = Timer.executeTimerMillis(
                DriverConstants.FREEZE_250_MS,
                () -> !RuntimeControl.getErrors().isEmpty()
        );
        if (result) {
            final List<String> errorMessages = new ArrayList<>();
            RuntimeControl.getErrors().forEach((date, trace) -> {
                Allure.addAttachment("Стектрейс из консоли JS " + date, trace);
            });
            NotCriticalErrorAccumulator.setNotCriticalError(
                    "В консоли JS обнаружены ошибки",
                    String.join(" ", errorMessages)
            );
            RuntimeControl.clearErrors();
            NotCriticalErrorAccumulator.setStepBroken();
        }
    }

    public static void detectUps404Errors() {
        if ((Stash.asMap().containsKey("no_toast") && "true".equals(Stash.getValue("no_toast")))) {
            return;
        }
        if ((Stash.asMap().containsKey("no_404_ups") && "true".equals(Stash.getValue("no_404_ups")))) {
            return;
        }
        final String xpath = DriverUtils.getXpath(WIDGET_UPS_404_NAME);
        if (!DriverUtils.findElementsOnPageByXpath(xpath).isEmpty()) {
            try {
                final Widget upsElement = (Widget) FindUtils.getElementByNameOrPath(WIDGET_UPS_404_NAME);
                upsElement.init();
                NotCriticalErrorAccumulator
                        .setNotCriticalError(
                                String.format(
                                        "Обнаружен экран %s с сообщением \"%s\" и причиной \"%s\"",
                                        ((IsUps404Widget) upsElement).getHeaderText(),
                                        ((IsUps404Widget) upsElement).getMessageText(),
                                        ((IsUps404Widget) upsElement).getReasonText()

                                )
                        );
            } catch (final StaleElementReferenceException ignored) {
            }
            publicateLastBackResponse();
            NotCriticalErrorAccumulator.setStepBroken();
        }
    }

    public static void publicateLastBackResponse() {
        NetworkControl.getNetworkActiveConnectionInstance().getLastRequests(10)
                      .forEach(id -> {
                          final String requestBody = NetworkControl
                                  .getNetworkActiveConnectionInstance()
                                  .getConnectionStat(id)
                                  .getRequestBody();
                          final String responseBody = NetworkControl
                                  .getNetworkActiveConnectionInstance()
                                  .getConnectionStat(id)
                                  .getResponse();
                          Allure.addAttachment(
                                  "Подробности запроса " + id,
                                  String.format(
                                          "Запрос:\n%s\n\nОтвет:\n%s",
                                          requestBody == null ? "запрос не идентифицирован" : requestBody.replaceAll(
                                                  "\\\\n",
                                                  "\n"
                                          ),
                                          responseBody == null ? "ответ не идентифицирован" : responseBody
                                                  .replaceAll("\\\\n", "\n")
                                                  .replaceAll("\\\\t", "\t")
                                  )
                          );
                      });
    }

    static void removeToasts() {
        final BlockExtractor toast = new BlockExtractor(TOAST_LIST_NAME);
        final AtomicReference<Throwable> throwable = new AtomicReference<>();
        final BooleanSupplier waitWhenToastBeClosed = () -> {
            try {
                if (toast.reset().getBlockCollection().isEmpty()) {
                    return true;
                }
                log.info("На странице есть уведомление: {}", toast.getInitializedBlockCollection().get(0).getText());
                Mover.moveAndClick(((AbstractToastListItem) toast
                        .getInitializedBlockCollection()
                        .get(0)).closeToastTextBlock);
            } catch (final Throwable e) {
                throwable.set(e);
            }
            DriverUtils.freeze(DriverConstants.FREEZE_500_MS * 2);
            return false;
        };

        final boolean result = Timer.executeTimer(DriverConstants.TIMEOUT, waitWhenToastBeClosed);
        if (!result) {
            if (throwable.get() == null) {
                throw new AutotestError("Не удалось закрыть всплывающее уведомление");
            } else {
                throw new AutotestError(throwable.get());
            }
        }
    }

    static void detectHintAndRemove() {
        final String hintXpath = DriverUtils.getXpath("Всплывающая подсказка");
        if (!DriverUtils.findElementsOnPageByXpath(hintXpath).isEmpty()) {
            final int height = Environment.getDriverService().getDriver().manage().window().getSize().getHeight() / 2;
            final int width = Environment.getDriverService().getDriver().manage().window().getSize().getWidth() / 2;
            Mover.moveOffset(width,height);

            final BooleanSupplier waitWhenHintRemoved = () -> {
                Mover.getActions().moveByOffset(10, 10).build().perform();
                DriverUtils.freeze(DriverConstants.FREEZE_250_MS);
                if (DriverUtils.findElementsOnPageByXpath(hintXpath).isEmpty()) {
                    return true;
                }
                Mover.moveOffset(width,height);
                log.info("Пытаюсь закрыть хит");
                DriverUtils.freeze(DriverConstants.FREEZE_250_MS);
                return DriverUtils.findElementsOnPageByXpath(hintXpath).isEmpty();
            };

            Timer.executeTimerThrowable(
                    DriverConstants.ELEMENT_WAIT_5SEC,
                    "Хинт не исчез",
                    waitWhenHintRemoved
            );
        }
    }

    static int getHtmlSectionHashCode(final String UUID) {
        int hashCode = 0;
        try {
            final Document document = Jsoup.parse((String) DriverUtils.executeJS("return document.body.innerHTML"));
            EXCLUSION_PAGE_FRAGMENTS_LIST.get().forEach(ev -> document.select(ev).remove());
            hashCode = document.html().hashCode();
        } catch (final Throwable e) {
            DriverUtils.freeze(DriverConstants.FREEZE_500_MS);
            if (!Timer.isTimeoutThrowable(
                    UUID,
                    DriverConstants.ELEMENT_WAIT_5SEC * 2,
                    "Не удалось отследить загрузку страницы \n" +
                    DriverUtils.executeJS("return document.body.innerHTML")
            )) {
                DriverUtils.freeze(DriverConstants.FREEZE_500_MS);
                hashCode = getHtmlSectionHashCode(UUID);
            }
        }
        return hashCode;
    }
}
