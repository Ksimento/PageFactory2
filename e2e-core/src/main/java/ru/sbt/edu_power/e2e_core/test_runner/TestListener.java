package ru.sbt.edu_power.e2e_core.test_runner;

import lombok.extern.slf4j.Slf4j;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.e2e_core.smoke_layout.report.CoverReport;
import ru.sbt.edu_power.external_services.PropReader;
import ru.sbt.edu_power.e2e_core.confluence_integration.UpdateRequestStatTable;
import ru.sbt.edu_power.e2e_core.devtools.network.ConnectionStatRepository;
import ru.sbt.edu_power.external_services.jira.JiraConnect;
import ru.sbt.edu_power.external_services.jira.JiraQueueExecutorRunner;
import ru.sbt.edu_power.external_services.jira.test_manager.TMConnectionTest;
import ru.sbt.edu_power.e2e_core.jira.TMRuntimeExporter;
import ru.sbt.edu_power.external_services.jira.test_manager.TMTestCaseHandler;
import ru.sbt.edu_power.e2e_core.layout.enums.DimensionEnum;
import ru.sbt.edu_power.e2e_core.layout.enums.UserAgent;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

@Slf4j
public abstract class TestListener extends RunListener {
    private final FutureTask<Boolean> task = new FutureTask<>(JiraQueueExecutorRunner.getInstance());

    protected TestListener(final Runnable runnable) {
        runnable.run();
    }

    @Override
    public void testRunStarted(final Description description) {

        // Конфигурация Unirest выполняется только один раз на все соединения
        JiraConnect.configureConnection();

        // Запуск синхронизации с джирой в отдельном потоке
        final Thread queueService = new Thread(task);
        queueService.start();
        final String screenSizePreset = System.getProperty("webdriver.browser.size.preset", "DEFAULT");
        if (DimensionEnum.valueOf(screenSizePreset).isMobileDimension()) {
            if ("safari".equalsIgnoreCase(System.getProperty("webdriver.browser.name"))) {
//                Assert.assertTrue(System.getProperty("os.name").startsWith("Mac"));
//                SeleniumGridHandler.getGrid().start();
                throw new AutotestError("Не поддерживается тестирование мобильной версии на сафари");
            } else {
                final String BROWSER_OPTIONS = System.getProperty("webdriver.chrome.capability.options.args");
                final String USER_AGENT = ",user-agent=" + UserAgent.MOBILE.getUserAgentName();
                System.setProperty(
                        "webdriver.chrome.capability.options.args",
                        BROWSER_OPTIONS + USER_AGENT
                );
            }
        }
        log.info("Время запуска тестов: {}", new SimpleDateFormat().format(new Date()));
        log.info("Параметры запуска браузера: {}", System.getProperty("webdriver.chrome.capability.options.args"));
        // Запуск тестов функционала экспорта
        if ("none".equals(System.getProperty("jira.testset.id"))) {
            return;
        }
        final TMConnectionTest TMConnectionTest = new TMConnectionTest();
        TMConnectionTest.checkTestSetStatus();
        TMConnectionTest.executionExportFailed();
        TMConnectionTest.executionExportPassSuccess();
        TMConnectionTest.executionExportFailSuccess();
    }

    @Override
    public void testRunFinished(final Result result) throws ExecutionException, InterruptedException {
        if ("jenkins".equals(System.getProperty("execution.environment"))) {
            // Сохраняем файл со списком тегов упавших тестов для передачи данных в джобу
            FailedScenarioTagsCollector.getInstance().saveData();
        }
        // Останавливаем синхронизацию с джирой
        JiraQueueExecutorRunner.getInstance().stopQueue();
        while (!task.get()) {
            DriverUtils.freeze(1000);
            log.info("Ждём завершения синхронизации");
        }
        log.info("Всего обновлено тест-кейсов за прогон: {}", TMTestCaseHandler.getJiraUpdateCounter());
        if ("true".equals(System.getProperty("enableStat"))) {
            new UpdateRequestStatTable(
                    PropReader.get("conf.request.stat.table"),
                    ConnectionStatRepository.getRequestStat()
            )
                    .updatePage();
        }
        final String coverProperty = System.getProperty("smoke.layout.get.screenshots");
        if (Objects.nonNull(coverProperty) && !coverProperty.isEmpty()) {
            new CoverReport().generate();
        }
        if ("none".equals(System.getProperty("jira.testset.id"))) {
            return;
        }
        log.info(
                "Отчитано {} тест-кейсов:\n{}",
                TMRuntimeExporter.getInstance().getExportedTags().size(),
                String.join("\n", TMRuntimeExporter.getInstance().getExportedTags())
        );
        log.info(
                "Строка тегов для повторного запуска упавших кейсов:\n{}",
                TMRuntimeExporter.getInstance().getFailedScenariosTagList()
        );
    }
}
