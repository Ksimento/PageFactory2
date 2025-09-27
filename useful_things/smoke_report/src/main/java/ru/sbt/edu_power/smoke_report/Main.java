package ru.sbt.edu_power.smoke_report;

import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.apache.commons.io.FileUtils;
import ru.sbt.edu_power.external_services.jenkins.allure.SuiteCollector;
import ru.sbt.edu_power.external_services.jenkins.allure.SuiteGrabber;
import ru.sbt.edu_power.external_services.jenkins.allure.enums.AllureReport;
import ru.sbt.edu_power.external_services.jenkins.allure.enums.Children;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Log
public class Main {
    private static final String JOB_URL = System.getProperty("smokeJobUrl");

    @SneakyThrows
    public static void main(final String[] args) {

        if (Objects.isNull(JOB_URL) || JOB_URL.isEmpty()) {
            throw new SmokeReportError("Не задан параметр jobUrl");
        }
        final File report = new File("target/smokeReport.txt");
        final SuiteCollector collector = new SuiteCollector();
        new SuiteGrabber().grab(collector, JOB_URL);
        Map<String, List<Children>> collection = collector.getCollection();
        final List<AllureReport.Step> steps = collection.values()
                                                        .stream()
                                                        .flatMap(List::stream)
                                                        .map(Children::getReport)
                                                        .map(AllureReport::getTestStage)
                                                        .map(AllureReport.TestStage::getSteps)
                                                        .flatMap(Set::stream)
                                                        .filter(s -> s.getName().contains("роут"))
                                                        .collect(Collectors.toList());
        final int passed = (int) steps.stream()
                                      .filter(s -> "passed".equals(s.getStatus()))
                                      .count();
        final int skip = (int) steps.stream()
                                    .filter(s -> "skipped".equals(s.getStatus()))
                                    .count();
        final int fail = steps.size() - passed - skip;
        final String line = fail == 0 && skip == 0  ?
                "<div style=\"color:#9ACD32\">\uD83C\uDF86\uD83C\uDF86Ура, нет падений\uD83C\uDF86\uD83C\uDF86 <br> Всего экранов: " +
                steps.size() +
                "</div>" :
                "<div style=\"color:#9ACD32\">Всего количество экранов: " +
                steps.size() +
                "</div> <div style=\"color:#fd5a3e\">Упало экранов: " +
                fail +
                "</div><div style=\"color:#97cc64\">Успешно прошли: " +
                passed +
                "</div>" +
                "</div><div style=\"color:#aaa\">Пропущено: " +
                skip +
                "</div>" +
                TableStatistic.getTableFailRout(collection);

        FileUtils.write(
                report,
                "<b>" + line + TableStatistic.getLineRoutUntagged(collection) + "</b>",
                StandardCharsets.UTF_8
        );


    }
}