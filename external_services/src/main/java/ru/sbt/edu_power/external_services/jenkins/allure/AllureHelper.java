package ru.sbt.edu_power.external_services.jenkins.allure;

import com.google.gson.Gson;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import ru.sbt.edu_power.external_services.ESUtils;
import ru.sbt.edu_power.external_services.jenkins.JenkinsHttpConnection;
import ru.sbt.edu_power.external_services.jenkins.allure.enums.Children;
import ru.sbt.edu_power.external_services.jenkins.allure.enums.AllureReport;
import ru.sbt.edu_power.external_services.jenkins.allure.enums.Suite;
import ru.sbt.edu_power.external_services.jira.JiraConnect;
import ru.sbt.edu_power.external_services.jira.JiraConnectionException;
import ru.sbt.edu_power.external_services.timer.Timer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

@Slf4j
public class AllureHelper {
    private final String allureUrl;
    private static final String SUMMARY_JSON = "widgets/summary.json";
    private static final String SUITES = "data/suites.json";
    private static final String REPORT = "data/test-cases/{UUID}.json";
    private Integer summaryPercent;
    private Integer total;
    private Integer passed;
    private final List<Suite> suites = new ArrayList<>();
    private final List<Children> children = new ArrayList<>();
    private final Gson gson = new Gson();

    public AllureHelper(final String buildUrl) {
        this.allureUrl = buildUrl.replaceFirst("/$", "") + "/allure/";
    }

    public List<Suite> getSuites() {
        if (suites.isEmpty()) {
            collectSuites();
        }
        return suites;
    }

    public List<Children> getChildrenList() {
        if (children.isEmpty()) {
            getSuites().forEach(s -> s.getChildren().forEach(this::collectChildrenDeep));
        }
        return children;
    }

    private void collectChildrenDeep(final Children ch) {
        if (ch.getChildren().isEmpty()) {
            children.add(ch);
        } else {
            ch.getChildren().forEach(this::collectChildrenDeep);
        }
    }

    private void collectSuites() {
        waitWhenAllureBeDone();
        final HttpResponse<JsonNode> response = JenkinsHttpConnection
                .getClusterByUrl(allureUrl)
                .getJsonData(allureUrl + SUITES);
        if (response.getStatus() == HttpStatus.SC_NOT_FOUND) {
            log.error("Билд по указанной ссылке больше недоступен {}{}", allureUrl, SUITES);
            return;
//            throw new JiraConnectionException("Билд по указанной ссылке больше недоступен " + allureUrl + SUITES);
        }
        JiraConnect.checkResponse(response, allureUrl + SUITES);
        for (final Object o : response.getBody().getObject().getJSONArray("children")) {
            suites.add(gson.fromJson(o.toString(), Suite.class));
        }
    }

    public void enrichmentChild() {
        getChildrenList().parallelStream().forEach(c -> {
            c.setReport(getReport(c.getUid()));
            c.setLink(getTestReportUrl(c));
        });
    }

    public Suite getSuiteByName(final String name) {
        return getSuites().stream()
                .filter(s -> name.equals(s.getName()))
                .findFirst()
                .orElse(null);
    }

    private void waitWhenAllureBeDone() {
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final AtomicInteger status = new AtomicInteger();
        final BooleanSupplier waitWhenAllureBePrepared = () -> {
            try {
                final HttpResponse<JsonNode> response = JenkinsHttpConnection
                        .getClusterByUrl(allureUrl)
                        .getJsonData(allureUrl + SUMMARY_JSON);
                if (response.getStatus() < HttpStatus.SC_BAD_REQUEST) {
                    return true;
                }
                status.set(response.getStatus());
            } catch (final Throwable e) {
                log.warn("Не удалось получить Allure отчёт");
                ESUtils.freeze(2000);
            }
            return false;
        };
        final boolean result = Timer.executeTimer(20, waitWhenAllureBePrepared);
        if (!result) {
            if (Objects.nonNull(error.get())) {
                throw new JiraConnectionException(error.get());
            } else {
                if (status.get() != HttpStatus.SC_NOT_FOUND) {
                    throw new JiraConnectionException(String.format(
                            "Ошибка получения аллюр отчёта %d по адресу %s",
                            status.get(),
                            allureUrl + SUMMARY_JSON
                    ));
                }
            }
        }
    }

    public int getSummaryPercent() {
        waitWhenAllureBeDone();
        if (summaryPercent == null) {
            final HttpResponse<JsonNode> response = JenkinsHttpConnection
                    .getClusterByUrl(allureUrl)
                    .getJsonData(allureUrl + SUMMARY_JSON);
            JiraConnect.checkResponse(response, allureUrl + SUMMARY_JSON);
            final Summary summary = new Gson().fromJson(response.getBody().toString(), Summary.class);
            if (summary.getStatistic().getTotal() == 0) {
                summaryPercent = 0;
                total = 0;
                passed = 0;
            } else if (summary.getStatistic().getPassed() == 0) {
                summaryPercent = 0;
                total = summary.getStatistic().total - summary.getStatistic().getSkipped();
                passed = 0;
            } else {
                total = summary.getStatistic().total - summary.getStatistic().getSkipped();
                passed = summary.getStatistic().getPassed();
                summaryPercent = total == 0 ? 0 : passed * 100 / total;
            }
        }
        return summaryPercent;
    }

    public Integer getTotal() {
        if (total == null) {
            getSummaryPercent();
        }
        return total;
    }

    public Integer getPassed() {
        if (passed == null) {
            getSummaryPercent();
        }
        return passed;
    }

    public String getTestReportUrl(final Children test) {
        return allureUrl + "#suites/" + test.getParentUid() + "/" + test.getUid();
    }

    private AllureReport getReport(final String uuid) {
        final String url = allureUrl + REPORT.replace("{UUID}", uuid);
        final AtomicReference<HttpResponse<JsonNode>> response = new AtomicReference<>();
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final BooleanSupplier waitWhenReportBeDownload = () -> {
            try {
                final HttpResponse<JsonNode> r = JenkinsHttpConnection.getClusterByUrl(url).getJsonData(url);
                if (r.getStatus() >= HttpStatus.SC_BAD_REQUEST) {
                    log.warn("Неверный статус получения отчета: {}\n{}\n{}", r.getStatus(), url, r);
                }
                response.set(r);
                return true;
            } catch (final Throwable e) {
                error.set(e);
                log.warn("Ошибка получения отчёта Allure");
                ESUtils.freeze(3000);
                return false;
            }
        };
        final boolean result = Timer.executeTimer(60, waitWhenReportBeDownload);
        if (!result) {
            throw new JiraConnectionException(error.get());
        }
        JiraConnect.checkResponse(response.get(), url);
        return gson.fromJson(response.get().getBody().getObject().toString(), AllureReport.class);
    }

    @NoArgsConstructor
    @Getter
    @Setter
    private static class Summary {
        private Statistic statistic;
    }

    @NoArgsConstructor
    @Getter
    @Setter
    private static class Statistic {
        private int total;
        private int skipped;
        private int passed;
    }

}
