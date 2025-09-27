package ru.sbt.edu_power.e2e_core.test_runner;

import com.sun.jndi.toolkit.url.UrlUtil;
import cucumber.api.Scenario;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import lombok.extern.slf4j.Slf4j;
import org.junit.AssumptionViolatedException;
import ru.sbt.edu_power.e2e_core.data.CheckPathRules;
import ru.sbt.edu_power.e2e_core.devtools.DevTools;
import ru.sbt.edu_power.e2e_core.devtools.RuntimeControl;
import ru.sbt.edu_power.e2e_core.devtools.network.ConnectionStatRepository;
import ru.sbt.edu_power.e2e_core.devtools.network.NetworkControl;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbt.edu_power.e2e_core.error_processing.NotCriticalErrorAccumulator;
import ru.sbt.edu_power.e2e_core.jira.TMRuntimeExporter;
import ru.sbt.edu_power.external_services.ESUtils;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.ExecutionStatus;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbtqa.tag.datajack.Stash;
import ru.sbtqa.tag.qautils.errors.AutotestError;
import ru.sbtqa.tag.qautils.properties.Props;

import java.io.File;
import java.net.MalformedURLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class AfterTest implements IHooks {
    private boolean testIsSkipped;

    @Override
    @Before(value = "@skip", order = 1)
    public void beforeSkipTest(final Scenario scenario) {
        FailedScenarioTagsCollector.getInstance().removeTags(scenario);
        if ("true".equals(System.getProperty("disableSkippedTests"))) {
            testIsSkipped = true;
            throw new AssumptionViolatedException("test skipped");
        }
    }

    // Запоминаем время старта сценария для возможности указать его при отчёте в тест-сет
    @Override
    @Before(order = 2)
    public void beforeRememberStartTime(final Scenario scenario) {
        log.info("Выполняется тест: {}", scenario.getName());
        if (testIsSkipped) {
            return;
        }
        if ("none".equals(ScenarioDataStorage.TEST_SET_ID)) {
            return;
        }
        TMRuntimeExporter.getInstance().setStart(scenario.getUri().hashCode());
    }

    // Настройка разделения прогона на два стенда
    // Если фича должна вся проходить на одном стенде - над функционалом нужно проставить тег @first_stand
    @Override
    @Before
    public void beforeSplitStand(final Scenario scenario) throws MalformedURLException {
        if (testIsSkipped) {
            return;
        }
        CheckPathRules.checkPath(UrlUtil.decode(scenario.getUri()).replace("file:", File.separator));
        if (!scenario.getSourceTagNames().contains("@first_stand")) {
            StandSplitter.getStand();
        }
    }

    // Чтобы установить в браузер кастомный user agent для эмуляции разных браузеров
    // нужно добавить user agent в файл user-agent.resources, например
    // agent-short-name=Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_1 like Mac OS X) AppleWebKit/603.1.30 (KHTML like Gecko).....
    // и добавить в сценарий тег вида @user-agent:agent-short-name
    @Override
    @Before(value = "not @jira_report", order = 10)
    public void beforeSetUserAgentAndWindowSize(final Scenario scenario) {
        if (testIsSkipped) {
            return;
        }
        // ожидаем инициализации класса констант
        Timer.executeTimer(30, () -> {
            try {
                DriverConstants.init();
                return true;
            } catch (final Throwable ignored) {
                ESUtils.freeze(5000);
                return false;
            }
        });
        WebDriverSwitcher.hideScrollBars();
        // Иногда приложение падает с ошибкой, вызванной отсутствием файла конфигурации.
        final AtomicReference<RuntimeException> error = new AtomicReference<>();
        final boolean result = Timer.executeTimer(60, () -> {
            try {
                WebDriverSwitcher.setUserAgentFromTag(scenario);
                return true;
            } catch (final RuntimeException e) {
                error.set(e);
                return false;
            }
        });
        if (!result) {
            throw new AutotestError("Не удалось инициализировать конфигурацию приложения", error.get());
        }

        WebDriverSwitcher.setWindowSize();
    }

    @Override
    @Before(order = 20)
    public void beforeDevToolsUp() {
        if (testIsSkipped) {
            return;
        }
        WebDriverSwitcher.startNetwork();
        // Принудительное ограничение сети
//        NetworkControl.throttleNetwork();
    }

    @Override
    @Before(value = "not @no_toast", order = 30)
    public void beforeDevToolsNetworkingUp(final Scenario scenario) {
        if (testIsSkipped) {
            return;
        }
        if ("true".equals(Props.get("js.console.error.tracking"))) {
            RuntimeControl.getRuntimeConsoleErrorTrackingInstance().enable();
            RuntimeControl.getRuntimeConsoleErrorTrackingInstance().initErrorExclusion(scenario);
            log.info("Запущен контроль фронтэнд ошибок");
        }
        // Запуск отслеживания ошибок только для сценариев, которые не содержат тега @no_toast
        if ("network".equals(Props.get("load.tracking.mode"))) {
            NetworkControl.errorToastControlEnable();
            NetworkControl.getNetworkActiveConnectionInstance().initErrorExclusion(scenario);
            log.info("Запущен контроль бэкенд ошибок");
        }
    }

    @Override
    @After(order = 10)
    public void afterCollectStatistic() {
        if (testIsSkipped) {
            return;
        }
        ConnectionStatRepository.putAll(
                NetworkControl.getNetworkActiveConnectionInstance().getStatMap()
        );
    }

    @Override
    @After(order = 100)
    public void afterNetworkDown() {
        if (testIsSkipped) {
            return;
        }
        try {
            DevTools.getRuntime().disable();
            if ("network".equals(Props.get("load.tracking.mode"))) {
                // Отключаем Network и завершаем работу DevTools для текущего потока
                DevTools.getNetwork().disable();
                DevTools.getDevToolsService().close();
            }
        } catch (final Throwable ignored) {
        }
        // Выкидываем все Not Critical исключения, накопленные за время исполнения теста
        NotCriticalErrorAccumulator.throwErrors();
    }

    @Override
    @After(order = 500)
    public void afterCloseAllDrivers() {
        if (testIsSkipped) {
            return;
        }
        WebDriverSwitcher.shutDownBrowsers();
    }

    @Override
    @Before(value = "@moon_only")
    public void beforeMoonSeparator() {
        if (testIsSkipped) {
            return;
        }

        if ("".equals(System.getProperty("webdriver.url"))) {
            throw new AutotestError("Тест пригоден для работы только в MOON. " +
                                    "Необходимо список тегов указать следующим образом:\n" +
                                    "\"-DTAGS='@your_tag and not @moon_only'\"\n" +
                                    "для исключения таких тестов из прогона в данном окружении");
        }
    }

    @Override
    @Before(value = "@no_headless")
    public void beforeNoHeadlessSeparator() {
        if (testIsSkipped) {
            return;
        }
        if (System.getProperty("webdriver.chrome.capability.options.args").contains("headless")) {
            throw new AutotestError("Тест не пригоден для работы в режиме HEADLESS. " +
                                    "Необходимо список тегов указать следующим образом:\n" +
                                    "\"-DTAGS='@your_tag and not @no_headless'\"\n" +
                                    "для исключения таких тестов из прогона в данном окружении");
        }
    }

    @Override
    @Before(value = "@no_toast")
    public void beforeDisableRedToastDetected() {
        if (testIsSkipped) {
            return;
        }
        Stash.put("no_toast", "true");
    }

    @Override
    @Before(value = "@no_404 or no_ups")
    public void beforeDisable404UpsDetected() {
        if (testIsSkipped) {
            return;
        }
        Stash.put("no_404_ups", "true");
    }

    // отчитывание в тест-сет по статусу прохождения тест-кейса
    @Override
    @After(order = 10001)
    public void afterResultReport(final Scenario scenario) {
        FailedScenarioTagsCollector.getInstance().collectFailedTag(scenario);
        if (testIsSkipped) {
            return;
        }
        if (!"none".equals(ScenarioDataStorage.TEST_SET_ID)) {
            if (scenario.isFailed()) {
                TMRuntimeExporter.getInstance().exportCases(
                        scenario,
                        scenario.getUri().hashCode(),
                        ExecutionStatus.AUTO_FAIL.getStatusName()
                );
                TMRuntimeExporter.getInstance().addFailedCases(scenario);
                TMRuntimeExporter.getInstance().addTestCasesToUpdateStatus(scenario, false);
            } else {
                TMRuntimeExporter.getInstance().exportCases(
                        scenario,
                        scenario.getUri().hashCode(),
                        ExecutionStatus.AUTO_PASS.getStatusName()
                );
                TMRuntimeExporter.getInstance().addTestCasesToUpdateStatus(scenario, true);
            }
        }
    }

    // Метод включает режим маркировки кликов и ховеров если в application.properties
    // в параметр mouse.hover.visual.marker передан тег из запущенного теста
    @Override
    @Before
    public void beforeMarkMousePosition(final Scenario scenario) {
        if (testIsSkipped) {
            return;
        }
        final List<String> tags = Stream.of(Props.get("mouse.hover.visual.marker", "").split(","))
                .map(String::trim)
                .collect(Collectors.toList());
        final boolean result = scenario.getSourceTagNames()
                .stream()
                .anyMatch(tags::contains);
        if (result) {
            System.setProperty("markMousePosition", "true");
        }
    }

    // Этот метод должен быть самым последним
    @Override
    @Before(order = 10001)
    public void beforeTestExecuted(final Scenario scenario) {
        FailedScenarioTagsCollector.getInstance().removeTags(scenario);
    }
}
