package ru.sbt.edu_power.external_services.jira.test_manager.test_run.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Модель данных для создания нового тест-сета
 */
@Data
public class TestRunCreateModel {
    private final String name;
    private final Integer projectId;
    private final Integer folderId;
    private final String plannedStartDate;
    private final String plannedEndDate;
    private final String projectVersionId;
    private final Integer statusId = 50;
    private Integer id;

    public TestRunCreateModel(final String name, final Integer projectId, final Integer folderId, final String projectVersionId) {
        this.name = name;
        this.projectId = projectId;
        this.folderId = folderId;
        final LocalDateTime localDateTime = LocalDateTime.now(ZoneId.of("UTC"));
        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.S'Z'");
        plannedStartDate = localDateTime.format(dateTimeFormatter);
        plannedEndDate = localDateTime.plusDays(1L).format(dateTimeFormatter);
        this.projectVersionId = projectVersionId;
    }

    public TestRunCreateModel(
            final String name,
            final Integer projectId,
            final Integer folderId,
            final String plannedStartDate,
            final String plannedEndDate,
            final String projectVersionId
    ) {
        this.name = name;
        this.projectId = projectId;
        this.folderId = folderId;
        this.plannedStartDate = plannedStartDate;
        this.plannedEndDate = plannedEndDate;
        this.projectVersionId = projectVersionId;
    }
}
