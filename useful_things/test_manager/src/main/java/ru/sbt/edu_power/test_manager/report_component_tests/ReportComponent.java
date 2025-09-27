package ru.sbt.edu_power.test_manager.report_component_tests;

import lombok.Getter;
import lombok.Setter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import ru.sbt.edu_power.external_services.confluence.ConfluenceConnect;
import ru.sbt.edu_power.external_services.jenkins.allure.SuiteCollector;
import ru.sbt.edu_power.external_services.jenkins.allure.SuiteGrabber;
import ru.sbt.edu_power.external_services.jenkins.allure.enums.Children;
import ru.sbt.edu_power.external_services.jenkins.jobs.Job;
import ru.sbt.edu_power.external_services.jenkins.jobs.JobStatus;
import ru.sbt.edu_power.external_services.jenkins.model.StepPipe;
import ru.sbt.edu_power.external_services.jira.JiraConnect;
import ru.sbt.edu_power.external_services.jira.tc_verifier.NewJiraTCCollector;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFolder;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCQueryBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
public class ReportComponent {
    final String nameComponent;
    final String urlAllure;
    int passedTest = 0;
    int allTest = 0;
    int skipTest = 0;
    int failTest = 0;
    int rtmAllTest = 0;
    boolean isProject = true;
    private int idComponent;
    @Setter
    String team = "undefined";
    @Setter
    boolean statusTests = true;
    @Setter
    String statusMfe = "?";
    List<String> pipeError = new ArrayList<>();
    final String rootDir = "MFE";
    TCFolder tcFolder = NewJiraTCCollector
            .getInstance().getFolder(TCFields.ProjectId.EDU.id);

    ReportComponent(final String nameComponent, final String urlAllure) {
        this.nameComponent = nameComponent;
        this.urlAllure = urlAllure;
        if (!nameComponent.equals("MFE_assistant")) {
            idComponent = Integer.parseInt(Optional.ofNullable(JiraConnect.getProject(nameComponent.replace("MFE_", ""), true)).orElse("0"));
        } else {
            idComponent = 0;
        }
        if (idComponent == 0) {
            folderEdu();
            isProject = false;
        }

    }

    protected ReportComponent evaluate() {
        final SuiteCollector collector = new SuiteCollector();
        new SuiteGrabber().grab(collector, urlAllure);
        Map<String, List<Children>> collection = collector.getCollection();
        final List<Children> steps = collection.values()
                .stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
        if (isRunningTests(urlAllure)) {
            passedTest = (int) steps.stream()
                    .filter(s -> "passed".equals(s.getStatus()))
                    .count();
            skipTest = (int) steps.stream()
                    .filter(s -> "skipped".equals(s.getStatus()))
                    .count();
            failTest = steps.size() - passedTest - skipTest;
            allTest = steps.size();
        }
        TCQueryBuilder queryBuilder = new TCQueryBuilder();
        final NewJiraTCCollector newJiraTCCollector = new NewJiraTCCollector();
        if (isProject) {
            queryBuilder.addField(TCFields.STATUS, TCFields.Status.APPROVED)
                    .addField(TCFields.ARCHIVED, false).addField(TCFields.PROJECT_ID, idComponent);
            if (newJiraTCCollector.collect(queryBuilder).asMap().values().isEmpty()) {
                folderEdu();
                queryBuilder = getSizeTests();
            }
        } else {
            queryBuilder = getSizeTests();
        }
        rtmAllTest = newJiraTCCollector.collect(queryBuilder).asMap().values().size();
        return this;
    }

    private boolean isRunningTests(final String urlAllure) {
        List<StepPipe> pipeMfe = Job.getPipeMfe(String.valueOf(SuiteGrabber.getBuildId(urlAllure)));

        for (StepPipe pipe : pipeMfe) {
            if ("run component tests".equals(pipe.getDisplayName())) {
                if (JobStatus.FAILURE.name().equals(pipe.getState())) {
                    statusTests = false;
                }
                if ("SKIPPED".equals(pipe.getState())) {
                    statusTests = false;
                }
            }
            if (JobStatus.FAILURE.name().equals(pipe.getResult())) {
                pipeError.add(String.format("Ошибки в шаге %s", pipe.getDisplayName()));
            }
        }
        return statusTests;
    }

    private TCQueryBuilder getSizeTests() {
        return new TCQueryBuilder().addField(TCFields.STATUS, TCFields.Status.APPROVED)
                .addField(TCFields.ARCHIVED, false).addField(TCFields.PROJECT_ID, TCFields.ProjectId.EDU.id).addField(TCFields.FOLDER,
                        idComponent
                );
    }

    private void folderEdu() {
        final List<String> parentPages = new ArrayList<>();
        parentPages.add(rootDir);
        parentPages.add(nameComponent);
        final Optional<TCFolder> folder = tcFolder.getChildByName(rootDir).get().getChildByName(nameComponent);
        if (!folder.isPresent()) {
            idComponent = tcFolder.createIfNotExists(parentPages.toArray(new String[]{}));
        } else {
            idComponent = folder.get().getId();
        }
    }
}
