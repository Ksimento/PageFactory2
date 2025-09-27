package ru.sbt.edu_power.e2e_core.test_runner;

import com.github.kklisura.cdt.protocol.types.emulation.UserAgentMetadata;
import cucumber.api.Scenario;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.openqa.selenium.JavascriptExecutor;
import ru.sbt.edu_power.e2e_core.devtools.DevTools;
import ru.sbt.edu_power.e2e_core.devtools.RuntimeControl;
import ru.sbt.edu_power.e2e_core.devtools.network.NetworkControl;
import ru.sbt.edu_power.e2e_core.layout.LayoutUtils;
import ru.sbt.edu_power.e2e_core.layout.enums.DimensionEnum;
import ru.sbt.edu_power.e2e_core.resource_repository.ResourceRepository;
import ru.sbtqa.tag.pagefactory.Page;
import ru.sbtqa.tag.pagefactory.context.PageContext;
import ru.sbtqa.tag.pagefactory.drivers.DriverService;
import ru.sbtqa.tag.pagefactory.environment.Environment;
import ru.sbtqa.tag.pagefactory.web.drivers.WebDriverService;
import ru.sbtqa.tag.qautils.errors.AutotestError;
import ru.sbtqa.tag.qautils.properties.Props;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class WebDriverSwitcher {
    private static final String STASH_DRIVER_NAME = "STASH_DRIVER_NAME_";
    private static final Map<DriverService, DevTools> DEV_TOOLS_MAP = new ConcurrentHashMap<>();
    private static final Map<String, DriverService> SERVICE_STASH = new ConcurrentHashMap<>();

    public static void enableBrowserSwitcher() {
        if (!SERVICE_STASH.containsKey(getDriverName(1))) {
            final DriverService driverService = Environment.getDriverService();
            SERVICE_STASH.put(getDriverName(1), driverService);
        }
    }

    public static void startNetwork() {
        DEV_TOOLS_MAP.put(Environment.getDriverService(), DevTools.getInstance());
        // Запуск отслеживания сетевой активности
        if ("network".equals(Props.get("load.tracking.mode"))) {
            NetworkControl.networkActiveConnectionEnable();
        }
    }

    private static void startErrorTracking() {
        final boolean hasNoToastTag = Environment.getScenario().getSourceTagNames().contains("@no_toast");
        if (!hasNoToastTag) {
            NetworkControl.errorToastControlEnable();
            NetworkControl.getNetworkActiveConnectionInstance().initErrorExclusion(Environment.getScenario());
            log.info("Запущен контроль бэкенд ошибок");
            if ("true".equals(Props.get("js.console.error.tracking"))) {
                RuntimeControl.getRuntimeConsoleErrorTrackingInstance().enable();
                RuntimeControl.getRuntimeConsoleErrorTrackingInstance().initErrorExclusion(Environment.getScenario());
                log.info("Запущен контроль фронтэнд ошибок");
            }
        }
    }

    public static void changeBrowser(final int browserName, final String userAgentKey) {
        changeBrowser(browserName);
        setUserAgent(userAgentKey);
    }

    public static void changeBrowser(final int browserNumber) {
        final String name = getDriverName(browserNumber);
        final Class<? extends Page> page = PageContext.getCurrentPage().getClass();
        enableBrowserSwitcher();
        if (!SERVICE_STASH.containsKey(name)) {
            SERVICE_STASH.put(name, new WebDriverService());
            Environment.setDriverService(SERVICE_STASH.get(name));
            hideScrollBars();
            setWindowSize();
            startNetwork();
            startErrorTracking();
        } else {
            Environment.setDriverService(SERVICE_STASH.get(name));
        }
//         Выполняю смену контекста, что бы сбросились кэши элементов, иначе бывают проблемы если работаешь в одном контексте в разных браузерах
        PageContext.clearPageContext();
        try {
            PageContext.setCurrentPage(page.getConstructor().newInstance());
        } catch (final Throwable e) {
            throw new AutotestError(e);
        }
    }

    public static void shutDownBrowsers() {
        if (switchToFirstBrowser()) {
            return;
        }
        SERVICE_STASH.forEach((key, drv) -> {
            if (key.contains(String.valueOf(Thread.currentThread().hashCode()))
                && !drv.isDriverEmpty()) {
                try {
                    DEV_TOOLS_MAP.get(drv).getNetworkInstance().disable();
                    DEV_TOOLS_MAP.get(drv).getDevToolsServiceInstance().close();
                } catch (final Throwable ignored) {
                }
                drv.demountDriver();
            }
        });
    }

    public static boolean switchToFirstBrowser() {
        if (SERVICE_STASH.containsKey(getDriverName(1))) {
            Environment.setDriverService(SERVICE_STASH.get(getDriverName(1)));
            SERVICE_STASH.remove(getDriverName(1));
            return false;
        }
        return true;
    }

    private static String getDriverName(final int browserNumber) {
        final int hash = Thread.currentThread().hashCode();
        return String.format("%s%s%03d", STASH_DRIVER_NAME, hash, browserNumber);
    }

    public static void setUserAgentFromTag(final Scenario scenario) {
        scenario.getSourceTagNames()
                .stream()
                .filter(tag -> tag.startsWith("@user-agent"))
                .map(tag -> tag.split(":", 2)[1])
                .findFirst()
                .ifPresent(WebDriverSwitcher::setUserAgent);
    }

    public static void setUserAgent(final String userAgentKey) {
        final Map<String, String> userAgentMap = ResourceRepository.getResource(ResourceRepository.AvailableResource.USER_AGENT);
        final String userAgentString = userAgentMap
                .get(userAgentKey);
        Assert.assertNotNull(
                String.format("\"%s\" не найден в файле user-agent.resources", userAgentKey),
                userAgentString
        );
        DevTools.getEmulation().setUserAgentOverride(userAgentString);
        Environment.getDriverService().getDriver().navigate().refresh();
    }

    public static void setLanguage(final String languageKey) {
        JavascriptExecutor js = Environment.getDriverService().getDriver();
        UserAgentMetadata userAgentMetadata = new UserAgentMetadata();
        userAgentMetadata.setPlatform(js.executeScript("return navigator.userAgentData.platform;").toString());
        userAgentMetadata.setPlatformVersion("");
        userAgentMetadata.setMobile(Boolean.parseBoolean(js.executeScript("return navigator.userAgentData.mobile;").toString()));
        String userAgent = js.executeScript("return navigator.userAgent;").toString();
        String platform = js.executeScript("return navigator.platform;").toString();
        userAgentMetadata.setArchitecture(platform);
        userAgentMetadata.setModel(platform);
        DevTools.getEmulation().setUserAgentOverride(userAgent, languageKey, platform, userAgentMetadata);
        Environment.getDriverService().getDriver().navigate().refresh();
    }

    public static void setWindowSize() {
        final String screenSizePreset = System.getProperty(
                "webdriver.browser.size.preset",
                "DEFAULT"
        );
        if (!"DEFAULT".equals(screenSizePreset)) {
            final DimensionEnum preset = DimensionEnum.valueOf(screenSizePreset);
            LayoutUtils.setWindowSize(preset);
        }
    }

    public static void hideScrollBars() {
        DevTools.getEmulation().setScrollbarsHidden(true);
    }
}
