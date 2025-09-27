package ru.sbt.edu_power.allure_comparator;

import com.slack.api.Slack;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.model.Attachment;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import ru.sbt.edu_power.allure_comparator.failed_tags.FailedTags;
import ru.sbt.edu_power.allure_comparator.report.Report;
import ru.sbt.edu_power.allure_comparator.report.ReportSection;
import ru.sbt.edu_power.external_services.jenkins.allure.SuiteCollector;
import ru.sbt.edu_power.external_services.jenkins.allure.SuiteGrabber;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class SuiteCollectorComparator extends AllureComparator {
    @Getter
    private final SuiteCollector actualCollector = new SuiteCollector();
    private final SuiteCollector expectedCollector = new SuiteCollector();
    private SuiteCollectionDiff diff;
    private final Report report = new Report("index.html");
    private final FailedTags failedTags = new FailedTags("failed_tags.html");

    public void compareData() {
        prepareData();
        new SuiteGrabber().grab(actualCollector, getActualJobUrl());
        if (!getExpectedJobUrl().isEmpty()) {
            new SuiteGrabber().grab(expectedCollector, getExpectedJobUrl());
            diff = new SuiteCollectionDiff(getJiraProjectKey(), actualCollector, expectedCollector);
            diff.compare();
        }
    }

    public void generateReport() {
        if (Objects.isNull(diff)) {
            return;
        }
        report.addHtmlMessage(
                String.format(
                        "<p>Эталонные данные для сравнения предоставлены джобой %s<br><a href='%s'>%s</a> (%d)",
                        getExpectedJobType(),
                        getExpectedJobUrl(),
                        getExpectedJobUrl(),
                        expectedCollector.getTestCount()
                ));
        report.addHtmlMessage(
                String.format(
                        "<p>Данные для проверки предоставлены джобой %s<br><a href='%s'>%s</a> (%d)",
                        getActualJobType(),
                        getActualJobUrl(),
                        getActualJobUrl(),
                        actualCollector.getTestCount()
                ));
        addSection("Упавшие тесты, которые раньше работали", "failed", diff.getBroken(), Report.class);
        addSection("Успешные тесты, которые раньше падали", "fixed", diff.getFixed(), Report.class);
        addSection("Пропущенные тесты, которые раньше запускались", "skipped", diff.getSkipped(), Report.class);
        addSection("Добавленные тесты", "added", diff.getAdded(), Report.class);
        addSection("Удаленные тесты", "removed", diff.getRemoved(), Report.class);
        report.generate();
    }

    public void generateReportFailedTags() {
        if (Objects.isNull(diff)) {
            return;
        }
        addSection("Список тегов упавших тестов", "failed", diff.getBroken(), FailedTags.class);
        failedTags.generate(getJiraProjectKey());
    }

    @SneakyThrows
    public void saveIndexHtml() {
        if (Objects.isNull(diff)) {
            return;
        }
        final Path reportPath = Paths.get(System.getProperty("user.dir"), "target", "diff-report");
        final Path reportFile = Paths.get(reportPath.toString(), "index.html");
        final Path reportFailedTagsFile = Paths.get(reportPath.toString(), "failed-tags.html");
        Files.createDirectories(reportPath);
        Files.write(reportFile, report.getContent().getBytes(StandardCharsets.UTF_8));
        Files.write(reportFailedTagsFile, failedTags.getContent().getBytes(StandardCharsets.UTF_8));

        try (final InputStream is = Objects.requireNonNull(this
                .getClass()
                .getClassLoader()
                .getResourceAsStream("style.css"))
        ) {
            final Path cssPath = Paths.get(reportPath.toString(), "style.css");
            FileUtils.copyInputStreamToFile(is, cssPath.toFile());
        }
    }

    public void slackNotification() {
        if (Objects.isNull(diff)) {
            return;
        }
        final String channel = System.getProperty("slackChannel");
        final String botToken = System.getProperty("bot.token");
        final String buildId = System.getProperty("buildId");
        if (Objects.isNull(channel) || Objects.isNull(botToken) || Objects.isNull(buildId)) {
            String message = "Нотификация в слак не выполнена\n";
            if (Objects.isNull(channel)) {
                message += "Не указан канал слака\n";
            }
            if (Objects.isNull(botToken)) {
                message += "Не указан токен для бота\n";
            }
            if (Objects.isNull(buildId)) {
                message += "Не указан buildId";
            }
            log.info(message);
            return;
        }
        final List<String> broken = diff
                .getBroken()
                .entrySet()
                .stream()
                .filter(e -> !e.getValue().isEmpty())
                .map(e -> e.getKey() + ": " + e.getValue().size())
                .collect(Collectors.toList());
        if (!broken.isEmpty()) {
            final String message = ":boom: Allure Diff обнаружил появление новых сбойных тестов относительно предыдущего прогона";
            final String attachment = String.format(
                    "%s\n<https://jenkins3-dev.pcbltools.ru/job/EduPower/job/QA/job/allure-diff/%s" +
                    "/artifact/diff-report/index.html|Allure Diff>",
                    String.join("\n", broken),
                    buildId
            );
            try (final Slack slack = Slack.getInstance()) {
                final ChatPostMessageResponse response = slack.methods(botToken).chatPostMessage(m -> m
                        .mrkdwn(true)
                        .channel(channel.replace("#", ""))
                        .text(message)
                        .token(botToken)
                        .attachments(Collections.singletonList(Attachment.builder().text(attachment).color("#A82921").build()))
                );
                if (!response.isOk()) {
                    log.error("{}", response);
                }
            } catch (final Exception e) {
                throw new AllureComparatorException(e);
            }
        }
    }

    private void addSection(
            final String name,
            final String sectionClass,
            final TeamCollection map,
            Class classReport
    ) {
        final ReportSection topSection = new ReportSection(name, sectionClass);
        map.forEach((tag, list) -> {
            final ReportSection tagSection = new ReportSection(tag);
            list.forEach(tagSection::addChildren);
            topSection.addSection(tagSection);
        });
        if (classReport == Report.class) {
            report.addSection(topSection);
        } else {
            failedTags.addSection(topSection);
        }
    }

}