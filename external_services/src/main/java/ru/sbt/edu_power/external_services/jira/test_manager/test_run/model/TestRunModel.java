package ru.sbt.edu_power.external_services.jira.test_manager.test_run.model;

import lombok.Getter;
import lombok.Setter;
import ru.sbt.edu_power.external_services.ExternalServicesException;
import ru.sbt.edu_power.external_services.jira.agile.JiraVersion;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraVersionModel;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Getter
@Setter
public class TestRunModel implements Serializable {
    private static final long serialVersionUID = 1391089838540845071L;
    private String name;
    private String key;
    private Integer id;
    private String projectKey;
    private Integer projectId;
    private String version;
    private String projectVersionId;
    private String plannedStartDate;
    private String plannedEndDate;
    private String owner;
    private Integer testCaseCount;
    private Integer statusId;

    private TCFields.ProjectId project;
    private JiraVersionModel jiraVersionModel;
    private Integer folderId;
    private List<Execution> items = new ArrayList<>();

    public TestRunModel() {}

    public TestRunModel(
            final TCFields.ProjectId project,
            final JiraVersionModel jiraVersionModel
    ) {
        this.project = project;
        this.jiraVersionModel = jiraVersionModel;
        this.projectKey = project.name();
        this.version = jiraVersionModel.getName();
        this.projectVersionId = jiraVersionModel.getId();
        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        plannedStartDate = LocalDateTime.now().format(dateTimeFormatter);
        plannedEndDate = LocalDateTime.now().plusDays(1L).format(dateTimeFormatter);
    }

    public TestRunCreateModel getTestRunCreateModel() {
        return new TestRunCreateModel(
                name,
                getProject().id,
                folderId,
                plannedStartDate,
                plannedEndDate,
                getJiraVersionModel().getId()
        );
    }

    public JiraVersionModel getJiraVersionModel() {
        if (Objects.isNull(jiraVersionModel)) {
            if (Objects.nonNull(version)) {
                jiraVersionModel = new JiraVersion().getJiraVersionByName(getProject().id, version);
            } else if (Objects.nonNull(projectVersionId)) {
                jiraVersionModel = new JiraVersion().getJiraVersionById(getProject().id, projectVersionId);
            } else {
                throw new ExternalServicesException("В модели отсутствуют данные о версии");
            }
        }
        return jiraVersionModel;
    }

    public TCFields.ProjectId getProject() {
        if (Objects.isNull(project)) {
            if (Objects.nonNull(projectKey)) {
                project = TCFields.ProjectId.valueOf(projectKey);
            } else if (Objects.nonNull(projectId)) {
                project = TCFields.ProjectId.getById(projectId);
            } else {
                throw new ExternalServicesException("В модели отсутствуют данные о проекте");
            }
        }
        return project;
    }

    @Override
    public String toString() {
        return "TestRunModel{" +
               "name='" + name + '\'' +
               ", key='" + key + '\'' +
               ", id=" + id +
               ", projectKey='" + projectKey + '\'' +
               ", projectId=" + projectId +
               ", version='" + version + '\'' +
               ", projectVersionId='" + projectVersionId + '\'' +
               ", plannedStartDate='" + plannedStartDate + '\'' +
               ", plannedEndDate='" + plannedEndDate + '\'' +
               ", owner='" + owner + '\'' +
               ", testCaseCount=" + testCaseCount +
               ", statusId=" + statusId +
               ", project=" + project +
               ", jiraVersionModel=" + jiraVersionModel +
               ", folderId=" + folderId +
               ", items=" + items +
               '}';
    }
}
