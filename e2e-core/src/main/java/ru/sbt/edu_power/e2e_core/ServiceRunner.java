package ru.sbt.edu_power.e2e_core;

import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.jira.JiraConnect;

import java.io.IOException;

@Slf4j
public class ServiceRunner {
    public static void main(String[] args) throws IOException {
        JiraConnect.configureConnection();
        // класс используется для тестового запуска произвольного кода
    }
}
