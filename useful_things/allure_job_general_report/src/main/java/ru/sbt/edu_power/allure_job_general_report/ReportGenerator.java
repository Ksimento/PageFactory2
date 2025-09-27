package ru.sbt.edu_power.allure_job_general_report;

import com.slack.api.Slack;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.model.Attachment;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.jenkins.allure.SuiteCollector;
import ru.sbt.edu_power.external_services.jenkins.allure.SuiteGrabber;
import ru.sbt.edu_power.external_services.jenkins.allure.enums.Children;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class ReportGenerator {
    private final String buildUrl = Objects.requireNonNull(System.getProperty("buildUrl"));
    private final String channel = Objects.requireNonNull(System.getProperty("slackChannel"));
    private final String botToken = Objects.requireNonNull(System.getProperty("bot.token"));
    private final SuiteCollector suiteCollector = new SuiteCollector();
    private final Map<String, Data> teamToData = new HashMap<>();

    public void generate() {
        final SuiteGrabber grabber = new SuiteGrabber();
        grabber.grab(suiteCollector, buildUrl);
        separateData();
        sendReport();
    }

    private void separateData() {
        suiteCollector.getCollection().keySet().forEach(k -> teamToData.put(k, new Data()));
        suiteCollector.getCollection().forEach((team, list) -> {
            list.forEach(ch -> {
                switch (ch.getStatus()) {
                    case "failed":
                        teamToData.get(team).addFailed(ch);
                        break;
                    case "broken":
                        teamToData.get(team).addBroken(ch);
                        break;
                    case "passed":
                        teamToData.get(team).addPassed(ch);
                        break;
                    case "skipped":
                        teamToData.get(team).addSkipped(ch);
                        break;
                }
            });
        });
    }

    private void sendReport() {
        final String total = String.format("%s\n%s\n%s\n<%s|Ссылка на джобу>", getAllPassed(), getAllNotPassed(), getAllSkipped(), buildUrl);
        final List<String> byTeams = teamToData.keySet()
                                               .stream()
                                               .map(this::getByTeam)
                                               .filter(s -> !s.isEmpty())
                                               .sorted()
                                               .collect(Collectors.toList());
        try (final Slack slack = Slack.getInstance()) {
            final ChatPostMessageResponse response = slack
                    .methods(botToken)
                    .chatPostMessage(r -> r
                            .text(total)
                            .attachments(
                                    Collections.singletonList(
                                            Attachment.builder()
                                                      .text(String.join("\n", byTeams))
                                                      .fallback("Данные по командам")
                                                      .color(getLineColor())
                                                      .build()
                                    )
                            )
                            .mrkdwn(true)
                            .channel(channel)
                    );
            if (!response.isOk()) {
                log.error("{}", response);
            }
        } catch (final Exception e) {
            throw new AllureJobGeneralReportException(e);
        }
    }

    private String getLineColor() {
        final int count = teamToData.values()
                                    .stream()
                                    .map(Data::getPassed)
                                    .mapToInt(List::size)
                                    .reduce(Integer::sum)
                                    .orElse(0);
        final int total = suiteCollector.getTestCount();
        final double percent = total == 0 ? 0 : count * 100d / total;
        if (percent < 70) {
            return "#EC275F";
        }
        if (percent < 90) {
            return "#2780EC";
        }
        return "#3AD781";
    }

    private String getByTeam(final String team) {
        final int failed = teamToData.get(team)
                                     .getNotPassed()
                                     .size();
        final int total = suiteCollector.getCollection().get(team).size();
        final double percent = total == 0 ? 0 : failed * 100d / total;
        return failed == 0 ? "" : String.format(
                "*%s*: %d/%d (%.2f%%) - <%s|build> - <%s|allure>",
                team,
                failed,
                total,
                percent,
                getBuildLink(suiteCollector.getCollection().get(team).get(0)),
                getAllureLink(suiteCollector.getCollection().get(team).get(0))
        );
    }

    private String getAllNotPassed() {
        return getTotal("Статистика (Failed/Tests)", Data::getNotPassed);
    }

    private String getAllSkipped() {
        return getTotal("Статистика (Skipped/Tests)", Data::getSkipped);
    }

    private String getAllPassed() {
        return getTotal("Статистика (Passed/Tests)", Data::getPassed);
    }

    private String getTotal(final String message, final Function<Data, List<Children>> mapFunction) {
        final int count = teamToData.values()
                                    .stream()
                                    .map(mapFunction)
                                    .mapToInt(List::size)
                                    .reduce(Integer::sum)
                                    .orElse(0);
        final int total = suiteCollector.getTestCount();
        final double percent = total == 0 ? 0 : count * 100d / total;
        return String.format("%s: %d/%d (%.2f%%)", message, count, total, percent);
    }

    private String getAllureLink(final Children child) {
        return child.getLink().split("#suites")[0];
    }

    private String getBuildLink(final Children child) {
        return child.getLink().split("allure")[0];
    }

    @Getter
    private static class Data {
        private final List<Children> passed = new ArrayList<>();
        private final List<Children> skipped = new ArrayList<>();
        private final List<Children> failed = new ArrayList<>();
        private final List<Children> broken = new ArrayList<>();

        public void addPassed(final Children item) {
            passed.add(item);
        }

        public void addSkipped(final Children item) {
            skipped.add(item);
        }

        public void addFailed(final Children item) {
            failed.add(item);
        }

        public void addBroken(final Children item) {
            broken.add(item);
        }

        public List<Children> getNotPassed() {
            final List<Children> notPassed = new ArrayList<>();
            notPassed.addAll(failed);
            notPassed.addAll(broken);
            return notPassed;
        }
    }
}
