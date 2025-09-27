package ru.sbt.edu_power.warm_up_job;

import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.jenkins.allure.SuiteCollector;
import ru.sbt.edu_power.external_services.jenkins.allure.SuiteGrabber;
import ru.sbt.edu_power.external_services.jenkins.allure.enums.AllureReport;
import ru.sbt.edu_power.external_services.jenkins.allure.enums.Children;
import ru.sbt.edu_power.external_services.mattermost.Regressman;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class Main {
    private static final String JOB_URL = System.getProperty("jobUrl");
    private static final String EXPECTED_PASSED_PERCENT = System.getProperty("expectedPassedPercent");

    public static void main(final String[] args) {
        if (Objects.isNull(JOB_URL) || JOB_URL.isEmpty()) {
            throw new SmokeTestPassPercent("Не задан параметр jobUrl");
        }
        if (Objects.isNull(EXPECTED_PASSED_PERCENT) || EXPECTED_PASSED_PERCENT.isEmpty()) {
            throw new SmokeTestPassPercent("Не задан параметр expectedPassedPercent");
        }
        final SuiteCollector collector = new SuiteCollector();
        new SuiteGrabber().grab(collector, JOB_URL);

        final List<AllureReport.Step> steps = collector.getCollection().values()
                                                       .stream()
                                                       .flatMap(List::stream)
                                                       .map(Children::getReport)
                                                       .map(AllureReport::getTestStage)
                                                       .map(AllureReport.TestStage::getSteps)
                                                       .flatMap(Set::stream)
                                                       .collect(Collectors.toList());

        if (JOB_URL.toLowerCase().contains("java-ui-smoke") || JOB_URL.toLowerCase().contains("qa-java-ui-auto")) {
            final int passedStep = (int) steps.stream()
                                              .filter(s -> "passed".equals(s.getStatus()))
                                              .count();
            final double passedPercent = steps.isEmpty() ? 0 : passedStep * 100d / steps.size();
            final int passed = steps.size() - (int) steps.stream()
                                                         .filter(s -> (s.getStatusMessage() ==
                                                                       null ? "" : s.getStatusMessage()).contains(
                                                                 "PRELOADER"))
                                                         .count();
            final double passedPercentPreloader = steps.isEmpty() ? 0 : passed * 100d / steps.size();
            if (passedPercent < 50) {
                Regressman.getInstance().sendPostByChannelName("Warm_up_notifi", String.format(
                        "Текущий процент успешного выполнения тестов %.2f%% меньше 50, возможны большое количество скопление дефектов на экранах, проверьте прогон \"%s\"",
                        passedPercent,
                        JOB_URL));
                throw new SmokeTestPassPercent(String.format(
                        "Текущий процент успешного выполнения тестов %.2f%% меньше ожидаемого %s%%",
                        passedPercent,
                        50
                ));
            }
            if (passedPercentPreloader < Integer.parseInt(EXPECTED_PASSED_PERCENT)) {
                throw new SmokeTestPassPercent(String.format(
                        "Текущий процент успешного выполнения тестов по прелоадерам %.2f%% меньше ожидаемого %s%%",
                        passedPercentPreloader,
                        EXPECTED_PASSED_PERCENT
                ));
            }
            log.info(String.format("Процент прохождения шагов по прелоадерам: %.2f%%", passedPercent));
        }
        if (JOB_URL.toLowerCase().contains("java-api")) {
            final int passed = (int) steps.stream()
                                          .filter(s -> "passed".equals(s.getStatus()))
                                          .count();
            final double passedPercent = steps.isEmpty() ? 0 : passed * 100d / steps.size();
            if (passedPercent < Integer.parseInt(EXPECTED_PASSED_PERCENT)) {
                throw new SmokeTestPassPercent(String.format(
                        "Текущий процент успешного выполнения тестов api %.2f%% меньше ожидаемого %s%%",
                        passedPercent,
                        EXPECTED_PASSED_PERCENT
                ));
            }
            log.info(String.format("Процент прохождения шагов api: %.2f%%", passedPercent));
        }
    }
}
