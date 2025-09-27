package ru.sbt.edu_power.assist_bot.reporting;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import ru.sbt.edu_power.external_services.confluence.ConfluenceConnect;
import ru.sbt.edu_power.external_services.confluence.ConfluenceContentModel;
import ru.sbt.edu_power.external_services.confluence.ConfluenceSearch;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.TestRunModel;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Класс генерирует структуру вложенных документов по данным из TestRunModel
 * Проектная область
 *  - Тестирование
 *   - Информация по РТМ и регрессу
 *    - Итоговые отчеты о проведении регресса
 *     - r/33.0.0
 *      - r/33.0.0-rc5
 *       - EDU-C1285 Название отчета
 */
public class ConfluenceRegressReporting {
    private final TestRunModel testRunModel;
    private static final String FINAL_REPORTS_PATH = "Итоговые отчеты о проведении регресса";
    private static final String RTM_INFO_PATH = "Информация по РТМ и регрессу";
    private static final String TESTING_PATH = "Тестирование";

    public ConfluenceRegressReporting(final TestRunModel testRunModel) {
        this.testRunModel = testRunModel;
    }

    public String getReportPage(final String reportName) {
        final String generatedReportName = generateReportName(reportName);
        final Optional<String> reportPage = search(generatedReportName);
        return reportPage.orElseGet(() -> createPage(generatedReportName, getReleaseCandidatePage()));
    }

    private String getReleaseCandidatePage() {
        final String release = testRunModel.getJiraVersionModel().getName();
        final Optional<String> releasePage = search(release);
        return releasePage.orElseGet(() -> createPage(release, getVersionPage()));
    }

    private String getVersionPage() {
        final String version = testRunModel.getJiraVersionModel().getName().split("-")[0];
        final Optional<String> versionPage = search(version);
        return versionPage.orElseGet(() -> createPage(version, getFinalReportsPage()));
    }

    private String getFinalReportsPage() {
        final Optional<String> finalReportPage = search(FINAL_REPORTS_PATH);
        return finalReportPage.orElseGet(() -> createPage(FINAL_REPORTS_PATH, getRtmInfoPage()));
    }

    private String getRtmInfoPage() {
        final Optional<String> rtmInfoPage = search(RTM_INFO_PATH);
        return rtmInfoPage.orElseGet(() -> createPage(RTM_INFO_PATH, getTestingPage()));
    }

    private String getTestingPage() {
        final Optional<String> testingPage = search(TESTING_PATH);
        return testingPage.orElseGet(() -> createPage(TESTING_PATH, null));
    }

    private String createPage(final String title, final String ancestor) {
        final ConfluenceContentModel model = new ConfluenceContentModel();
        model.setSpace(new ConfluenceContentModel.Space(testRunModel.getProject().confluenceKey));
        model.setTitle(title);
        if (Objects.nonNull(ancestor) && !ancestor.isEmpty()) {
            model.setAncestors(Collections.singletonList(new ConfluenceContentModel.Ancestors(Integer.parseInt(ancestor))));
        }
        final HttpResponse<JsonNode> response = ConfluenceConnect.createEmptyPage(model);
        return response.getBody().getObject().getString("id");
    }

    private Optional<String> search(final String pageName) {
        final ConfluenceSearch search = new ConfluenceSearch();
        search.setSpace(testRunModel.getProject());
        search.setTitle(pageName);
        search.search();
        final List<ConfluenceContentModel> result = search.getResults();
        if (result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(result.get(0).getId());
    }

    private String generateReportName(final String reportName) {
        return testRunModel.getKey() + " " + reportName;
    }
}
