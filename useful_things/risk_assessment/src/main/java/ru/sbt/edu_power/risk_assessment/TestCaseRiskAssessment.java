package ru.sbt.edu_power.risk_assessment;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import ru.sbt.edu_power.external_services.jira.JiraConnect;
import ru.sbt.edu_power.external_services.jira.tc_verifier.NewJiraTCCollector;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFilter;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCQueryBuilder;
import ru.sbt.edu_power.external_services.jira.test_manager.TMFields;
import ru.sbt.edu_power.external_services.jira.test_manager.model.TestCase;
import ru.sbt.edu_power.external_services.jira.test_manager.model.TestCaseModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Stream;

@Slf4j
public class TestCaseRiskAssessment {
    private final TCQueryBuilder queryBuilder = new TCQueryBuilder();
    private final LinkedIssueCollector linkedIssueCollector = new LinkedIssueCollector(queryBuilder);
    private final Map<TestCaseModel, TCFields.Risk> testCaseToRiskMap = new HashMap<>();
    private final SeparatorRules separatorRules = new SeparatorRules(linkedIssueCollector);
    private final LongAdder updatedTestCaseCounter = new LongAdder();

    public void collect(final String projectKey) {
        final TCFields.ProjectId projectId = TCFields.ProjectId.valueOf(projectKey);
        queryBuilder.addField(TCFields.ARCHIVED, false)
                    .addField(TCFields.PROJECT_ID, projectId)
                    .addField(TCFields.TEST_VIEW, TCFields.TestView.U_I)
                    .addField(TCFields.TEST_TYPE, TCFields.TestType.REGRESS)
                    .addField(TCFields.FOLDER, NewJiraTCCollector.getInstance().getFolderId("Регресс", projectId.id));
        NewJiraTCCollector.getInstance().collect(queryBuilder);
        linkedIssueCollector.collectIssues();
        separator();
        updateData();
        printStatistic();
    }

    private void separator() {
        TCFilter.of(NewJiraTCCollector.getInstance().asMap())
                .filter(queryBuilder)
                .toMap()
                .values()
                .forEach(tc -> {
                    final TCFields.Risk risk = separatorRules.getRisk(tc);
                    if (Objects.nonNull(risk)) {
                        testCaseToRiskMap.put(tc, risk);
                    }
                });
        log.info("Тест-кейсам назначены риски");
    }

    private void updateData() {
        testCaseToRiskMap.forEach((tc, risk) -> {
            if (tc.getRisk() == risk) {
                return;
            }
            final TestCase testCase = new TestCase();
            testCase.addCustomFieldParam(TMFields.RISK, risk.value);
            update(tc.getKey(), testCase);
        });
        log.info("Обновлено {} кейсов", updatedTestCaseCounter.sum());
    }

    private void printStatistic() {
        log.info("Всего обновлено кейсов: {}", updatedTestCaseCounter.sum());
        log.info("------- statistic by risk -------");
        final AtomicInteger r5 = new AtomicInteger(0);
        final AtomicInteger r4 = new AtomicInteger(0);
        final AtomicInteger r3 = new AtomicInteger(0);
        final AtomicInteger r2 = new AtomicInteger(0);
        final AtomicInteger r1 = new AtomicInteger(0);
        testCaseToRiskMap.forEach((tc, risk) -> {
            switch (risk) {
                case CRITICAL:
                    r5.incrementAndGet();
                    break;
                case HIGH:
                    r4.incrementAndGet();
                    break;
                case MEDIUM:
                    r3.incrementAndGet();
                    break;
                case LOW:
                    r2.incrementAndGet();
                    break;
                case LOWEST:
                    r1.incrementAndGet();
                    break;
                case UNDEFINED:
                    break;
            }
        });
        log.info("{}: {}", TCFields.Risk.CRITICAL.getValue(), r5.get());
        log.info("{}: {}", TCFields.Risk.HIGH.getValue(), r4.get());
        log.info("{}: {}", TCFields.Risk.MEDIUM.getValue(), r3.get());
        log.info("{}: {}", TCFields.Risk.LOW.getValue(), r2.get());
        log.info("{}: {}", TCFields.Risk.LOWEST.getValue(), r1.get());
        log.info("------- statistic by rules -------");
        Stream.of(SeparatorRules.Rules.values()).forEach(rule ->
                log.info("{}: {}", rule.getDescription(), separatorRules.getAccum().get(rule.name()))
        );
    }

    private void update(
            final String testCaseKey,
            final TestCase testCase
    ) {
        final HttpResponse<JsonNode> httpResponse = JiraConnect.testCaseUpdate(testCaseKey, testCase);
        if (httpResponse.getStatus() == HttpStatus.SC_NOT_FOUND) {
            log.error("Тест-кейс {} не найден в РТМ, либо архивирован", testCaseKey);
            return;
        }
        if (!httpResponse.isSuccess()) {
            log.error(
                    "Ошибка при записи риска\ntestCaseKey: {}\n{}",
                    testCaseKey,
                    httpResponse.getBody().toPrettyString()
            );
            return;
        }
        updatedTestCaseCounter.increment();
        if (updatedTestCaseCounter.sum() % 100 == 0) {
            log.info("Обновлено {} кейсов", updatedTestCaseCounter.sum());
        }
    }
}
