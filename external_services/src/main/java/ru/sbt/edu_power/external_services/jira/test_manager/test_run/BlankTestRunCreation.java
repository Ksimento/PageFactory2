package ru.sbt.edu_power.external_services.jira.test_manager.test_run;

import com.google.gson.Gson;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.jira.JiraConnect;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraVersionModel;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFolder;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.TestRunModel;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class BlankTestRunCreation {
    private final TCFields.ProjectId project;
    private final JiraVersionModel version;
    private final List<String> path;
    private final Map<String, TCFolder> pathMap = new LinkedHashMap<>();
    private TestRunModel testRunModel;

    public BlankTestRunCreation(
            final TCFields.ProjectId project,
            final JiraVersionModel version,
            final String... path
    ) {
        this.project = project;
        this.version = version;
        this.path = Arrays.asList(path);
    }

    public void setTestRunModel(final TestRunModel testRunModel) {
        this.testRunModel = testRunModel;
    }

    public TestRunModel getTestRunModel() {
        return testRunModel;
    }

    // Создание пустого тест-сета
    public TestRunModel create(final String testSetName) {
        generateTestSetDirectory();
        testRunModel = new TestRunModel(project, version);
        testRunModel.setName(testSetName);
        testRunModel.setFolderId(getTargetFolderId());
        final HttpResponse<JsonNode> response = JiraConnect.testRunCreate(testRunModel.getTestRunCreateModel());
        testRunModel.setKey(response.getBody().getObject().getString("key"));
        testRunModel.setId(response.getBody().getObject().getInt("id"));
        log.info("Тест сет {} с ID {} создан", testRunModel.getKey(), testRunModel.getId());
        return testRunModel;
    }

    // получаем ID папки назначения (где должен быть создан тест-сет)
    private Integer getTargetFolderId() {
        return pathMap.get(path.get(path.size() - 1)).getId();
    }

    // метод пытается найти директорию для размещения тест-сета
    // если таковой нет - она будет создана
    private void generateTestSetDirectory() {
        pathMap.clear();
        final HttpResponse<JsonNode> response = JiraConnect.getTestCyclesFolderTree(project.id.toString());
        final Gson gson = new Gson();
        final TCFolder folder = gson.fromJson(response.getBody().toString(), TCFolder.class);
        for (int i = 0; i < path.size(); i++) {
            if (pathMap.containsKey(path.get(i))) {
                continue;
            }
            final TCFolder last = i > 0 ? pathMap.get(path.get(i - 1)) : folder;
            final Optional<TCFolder> itemFolder = last.getChildByName(path.get(i));
            if (itemFolder.isPresent()) {
                pathMap.put(path.get(i), itemFolder.get());
            } else {
                createSubfolder(last.getId(), path.get(i));
                generateTestSetDirectory();
            }
        }
    }

    // создаём поддиректорию
    private void createSubfolder(final Integer parentId, final String subfolderName) {
        final Map<String, Object> subfolderMap = new HashMap<>();
        subfolderMap.put("index", 100);
        subfolderMap.put("name", subfolderName);
        if (parentId != null) {
            subfolderMap.put("parentId", parentId);
        }
        subfolderMap.put("projectId", project.id);
        JiraConnect.createSubfolderInTestCycles(subfolderMap);
        log.info("Создана директория {}", subfolderName);
    }
}
