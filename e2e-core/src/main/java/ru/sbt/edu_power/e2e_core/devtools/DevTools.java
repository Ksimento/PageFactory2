package ru.sbt.edu_power.e2e_core.devtools;

import com.github.kklisura.cdt.protocol.commands.Emulation;
import com.github.kklisura.cdt.protocol.commands.Network;
import com.github.kklisura.cdt.protocol.commands.Page;
import com.github.kklisura.cdt.protocol.commands.Runtime;
import com.github.kklisura.cdt.services.ChromeDevToolsService;
import com.github.kklisura.cdt.services.impl.ChromeServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbtqa.tag.datajack.Stash;
import ru.sbtqa.tag.pagefactory.environment.Environment;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/**
 * Класс реализует подключение к запущенному браузеру java клиента для Chrome DevTools Protocol
 */
@Slf4j
public final class DevTools {
    private ChromeDevToolsService devToolsService;
    private Network network;
    private Page page;
    private Emulation emulation;
    private Runtime runtime;

    private DevTools() {
        log.info("Инициализация DevTools");
    }

    // В каждом исполняемом потоке будет свой DevTools со своим ID сессии браузера
    public static DevTools getInstance() {
        final String driverHash = String.valueOf(Environment.getDriverService().hashCode());
        if (!Stash.asMap().containsKey(driverHash)) {
            Stash.put(driverHash, new DevTools());
        }
        return Stash.getValue(driverHash);
    }

    // Инициализация DevTools протокола
    @SuppressWarnings("unchecked")
    public static ChromeDevToolsService getDevToolsService() {
        if (getInstance().devToolsService == null) {
            final AtomicReference<WebDriver> driver = new AtomicReference<>(Environment.getDriverService().getDriver());
            final AtomicReference<Throwable> throwableAtomicReference = new AtomicReference<>();
            final BooleanSupplier waitWhenDevToolsUp = () -> {
                try {
                    final Map<String, String> capability = (Map<String, String>) ((RemoteWebDriver) driver.get())
                            .getCapabilities()
                            .getCapability("goog:chromeOptions");
                    final int port = Integer.parseInt(capability.get("debuggerAddress").replaceAll("\\D", ""));
                    final ChromeServiceImpl service = new ChromeServiceImpl(port);
                    getInstance().devToolsService = service.createDevToolsService(service.getTabs().get(0));
                } catch (final Throwable e) {
                    Environment.getDriverService().mountDriver();
                    driver.set(Environment.getDriverService().getDriver());
                    throwableAtomicReference.set(e);
                    DriverUtils.freeze(1000);
                    return false;
                }
                return true;
            };
            final boolean result = Timer.executeTimer(30, waitWhenDevToolsUp);
            if (!result) {
                if (Objects.nonNull(throwableAtomicReference.get())) {
                    throw new AutotestError(throwableAtomicReference.get());
                }
            }
        }
        return getInstance().devToolsService;
    }

    // Получение объекта Network из активного протокола
    public static Network getNetwork() {
        if (getInstance().network == null) {
            getInstance().network = getDevToolsService().getNetwork();
            getInstance().network.enable();
        }
        return getInstance().network;
    }

    // Получение объекта Page из активного протокола
    public static Page getPage() {
        if (getInstance().page == null) {
            getInstance().page = getDevToolsService().getPage();
        }
        return getInstance().page;
    }

    // Получение объекта Emulation из активного протокола
    public static Emulation getEmulation() {
        if (getInstance().emulation == null) {
            getInstance().emulation = getDevToolsService().getEmulation();
        }
        return getInstance().emulation;
    }

    // Получение объекта Runtime из активного протокола
    public static Runtime getRuntime() {
        if (getInstance().runtime == null) {
            getInstance().runtime = getDevToolsService().getRuntime();
            getInstance().runtime.enable();
        }
        return getInstance().runtime;
    }

    // Идентификатор инстанса DevTools протокла для хранения объектов Network и подобных с привязкой к этому протоколу
    public static int getId() {
        return getInstance().hashCode();
    }

    public Network getNetworkInstance() {
        return network;
    }

    public ChromeDevToolsService getDevToolsServiceInstance() {
        return devToolsService;
    }
}
