package ru.sbt.edu_power.external_services.jira.test_manager.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

// Модель для создания директории в тест-менеджере
@Getter
@Setter
@AllArgsConstructor
public class TestCaseFolderCreation {
    private String name;
    private int parentId;
    private int projectId;
}
