package ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.test_case_repartition;

import ru.sbt.edu_power.external_services.jira.test_manager.model.TestCaseModelFolder;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.Execution;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TestCaseRepartitionUtils {
    // получаем полный путь до конечной папки с тестами в виде строки
    public static String getFolderPath(final TestCaseModelFolder folder) {
        return folder.getParentsFolders()
                     .stream()
                     .map(TestCaseModelFolder::getName)
                     .collect(Collectors.joining("/"));
    }

    private static void addExecutionsFromFolder(
            final TestCaseRepartition testCaseRepartition,
            final String folder,
            final List<Execution> executions
    ) {
        final Integer folderId = Integer.parseInt(folder);
        executions.addAll(testCaseRepartition
                .getFunctionalParts()
                .entrySet()
                .stream()
                .filter(e -> e.getKey().getId().equals(folderId))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(new ArrayList<>()));
    }

    public static List<Execution> getExecutions(
            final TestCaseRepartition testCaseRepartition,
            final List<String> folderList
    ) {
        final List<Execution> executions = new ArrayList<>();
        folderList.forEach(f -> addExecutionsFromFolder(testCaseRepartition, f, executions));
        return executions;
    }
}
