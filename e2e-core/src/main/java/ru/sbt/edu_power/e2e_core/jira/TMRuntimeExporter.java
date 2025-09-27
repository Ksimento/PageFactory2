package ru.sbt.edu_power.e2e_core.jira;

import cucumber.api.Scenario;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.e2e_core.test_runner.ScenarioDataStorage;
import ru.sbt.edu_power.external_services.ExternalServicesException;
import ru.sbt.edu_power.external_services.PropReader;
import ru.sbt.edu_power.external_services.jira.JiraConnect;
import ru.sbt.edu_power.external_services.jira.JiraExporterUtils;
import ru.sbt.edu_power.external_services.jira.JiraQueueExecutorRunner;
import ru.sbt.edu_power.external_services.jira.tc_verifier.NewJiraTCCollector;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;
import ru.sbt.edu_power.external_services.jira.test_manager.TMTestCaseHandler;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.TestRunSearch;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.Execution;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.TestRunModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

// Класс выполняет обновление некоторых данных в тест-кейсах и тест-сетах
@Slf4j
public final class TMRuntimeExporter {
    private final List<String> exportedTags = new ArrayList<>();
    private final Map<Integer, Long> startMap = new ConcurrentHashMap<>();
    private final String testSetId = System.getProperty("jira.testset.id");
    private final List<String> failedTestList = new ArrayList<>();
    private static TMRuntimeExporter instance;
    private String version;
    private final Consumer<Map<String, ?>> sendReportFunction = (data) -> {
        final Collection<String> queueTags = data.keySet();
        for (final String tag : queueTags) {
            final String decodedTag = JiraQueueExecutorRunner.decodeTag(tag);
            try {
                if (sendReport(decodedTag, (Execution) data.get(tag))) {
                    exportedTags.add(decodedTag);
                    data.remove(tag);
                }
            } catch (final IllegalArgumentException e) {
                log.error("", e);
                data.remove(tag);
            } catch (final Throwable e) {
                log.info("При отправке отчёта {} 'sendReportFunction' возникла ошибка: {}", decodedTag, e.toString());
            }
        }
    };
    private final Consumer<Map<String, ?>> updateAutomatedStatusFunction = (data) -> {
        final Collection<String> statusUpdateQueueTags = data.keySet();
        for (final String tag : statusUpdateQueueTags) {
            final String decodedTag = JiraQueueExecutorRunner.decodeTag(tag);
            try {
                if (TMTestCaseHandler.updateAutomatedStatus(decodedTag,
                        (TCFields.AutomatedStatus) data.get(tag)
                )) {
                    data.remove(tag);
                }
            } catch (final IllegalArgumentException e) {
                log.error("", e);
                data.remove(tag);
            } catch (final Throwable e) {
                log.info("При отправке отчёта {} 'updateAutomatedStatusFunction' возникла ошибка: {}", decodedTag, e.toString());
            }
        }
    };

    private TMRuntimeExporter() {
        instance = this;
    }

    public static TMRuntimeExporter getInstance() {
        if (null == instance) {
            new TMRuntimeExporter();
        }
        return instance;
    }

    // Добавление в очередь на отправку статуса прохождения тест-кейса в тест-сет
    public void exportCases(final Scenario scenario, final Integer id, final String status) {
        final Collection<String> tags = scenario.getSourceTagNames();
        final Map<String, Execution> data = tags
                .stream()
                .filter(tag -> tag.startsWith("@" + ScenarioDataStorage.PROJECT))
                .collect(Collectors.toMap(tag -> tag.replace("@", ""),
                        tag -> getExecution(startMap.get(id), status),
                        (a, b) -> b));
        JiraQueueExecutorRunner.getInstance().putToQueue(sendReportFunction, data);
    }

    // Запоминаем время старта теста
    public void setStart(final Integer id) {
        startMap.put(id, System.currentTimeMillis());
    }

    // Возвращаем список всех отправленных тест-кейсов
    public List<String> getExportedTags() {
        return exportedTags;
    }

    //    Добавляем в список упавших только один EDU тег, этого достаточно для запуска сценария
    public void addFailedCases(final Scenario scenario) {
        failedTestList.add(
                scenario.getSourceTagNames()
                        .stream()
                        .filter(v -> v.startsWith("@" + ScenarioDataStorage.PROJECT))
                        .findFirst()
                        .orElse(null));
    }

    // Метод добавляет в очередь теги и новые статусы, которые нужно проставить в тест-кейсах в джире
    public void addTestCasesToUpdateStatus(final Scenario scenario, final boolean isPassed) {
        final TCFields.AutomatedStatus automatedStatus = isPassed ? TCFields.AutomatedStatus.YES :
                TCFields.AutomatedStatus.ON_AUTOMATE;

        final Map<String, TCFields.AutomatedStatus> data = new HashMap<>();
        scenario.getSourceTagNames()
                .stream()
                .filter(tag -> tag.startsWith("@" + ScenarioDataStorage.PROJECT))
                .map(tag -> tag.replace("@", ""))
                .map(t -> NewJiraTCCollector.getInstance().getById(t))
                .filter(t ->
                        t.getAutomatedStatus() == TCFields.AutomatedStatus.NOT ||
                        t.getAutomatedStatus() == TCFields.AutomatedStatus.ON_AUTOMATE ||
                        t.getAutomatedStatus() == TCFields.AutomatedStatus.YES
                )
                .forEach(tag -> data.put(tag.getKey(), automatedStatus));
        JiraQueueExecutorRunner.getInstance().putToQueue(updateAutomatedStatusFunction, data);
    }

    public String getFailedScenariosTagList() {
        return String.join(" or ", failedTestList);
    }

    private Execution getExecution(final long startTime, final String status) {
        final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        final long currentTime = System.currentTimeMillis();
        return new Execution(
                status,
                df.format(new Date(startTime)),
                df.format(new Date(currentTime)),
                JiraExporterUtils.decodeData(PropReader.get("jira.login")),
                (int) (currentTime - startTime),
                getVersion()
        );
    }

    private String getVersion() {
        if (Objects.isNull(version)) {
            final TestRunSearch search = new TestRunSearch(testSetId);
            final Optional<TestRunModel> modelOptional = search.getTestRunByKey();
            try {
                version = modelOptional.map(t -> t.getJiraVersionModel().getName()).orElse("");
            } catch (final ExternalServicesException e) {
                version = "";
            }
        }
        return version;
    }

    private boolean sendReport(final String tag, final Execution execution) {
        final HttpResponse<JsonNode> response = JiraConnect.reportTestCaseToTestSet(
                tag,
                execution.toString(),
                testSetId
        );
        return response.isSuccess();
    }
}
