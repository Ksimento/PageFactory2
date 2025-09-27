package ru.sbt.edu_power.external_services.jira.tc_verifier;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.sbt.edu_power.external_services.ExternalServicesException;
import ru.sbt.edu_power.external_services.jira.JiraConnect;
import ru.sbt.edu_power.external_services.jira.JiraConnectionException;
import ru.sbt.edu_power.external_services.jira.test_manager.model.TestCaseFolderCreation;
import ru.sbt.edu_power.external_services.validator.Validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@NoArgsConstructor
@Getter
@Setter
public class TCFolder {
    private Integer id;
    private String name;
    private Integer projectId;
    private Integer parentId;
    private Set<TCFolder> children = new HashSet<>();

    public String getNameById(final Integer id) {
        final String result = getNameById(id, this);
        if (result.isEmpty()) {
            throw new JiraConnectionException("Не найдена папка с ID" + id);
        }
        return result;
    }

    // Выполняет поиск по одному уровню вложенности папок по имени папки, можно использовать маску
    public Optional<TCFolder> getChildByName(final String name) {
        return children
                .stream()
                .filter(f -> Validator.matchValues(f.name, name))
                .findFirst();
    }

    public Integer getIdByName(final String name) {
        final Integer result = getIdByName(name, this);
        if (result == null) {
            throw new JiraConnectionException("Не найдена папка с именем" + name);
        }
        return result;
    }

    // метод проходится по пути path и если элементы пути отсутствуют в РТМ - они будут созданы.
    // возвращается ID последней созданной директории
    public Integer createIfNotExists(final String... path) {
        if (path.length == 0) {
            throw new ExternalServicesException("Передан пустой путь");
        }
        final List<String> pathList = new ArrayList<>(Arrays.asList(path));
        final String element = pathList.get(0);
        pathList.remove(0);
        if (pathList.isEmpty()) {
            if (element.equals(name)) {
                return id;
            }
            return createFolder(id, element);
        }
        final Optional<TCFolder> folder = getChildByName(element);
        if (folder.isPresent()) {
            return folder.get().createIfNotExists(pathList.toArray(new String[]{}));
        }
        final TCFolder newFolder = new TCFolder();
        newFolder.setName(element);
        newFolder.setParentId(id);
        newFolder.setProjectId(projectId);
        newFolder.setId(createFolder(id, element));
        return newFolder.createIfNotExists(pathList.toArray(new String[]{}));
    }

    // метод создаёт новую директорию
    private Integer createFolder(final int parentId, final String folderName) {
        final TestCaseFolderCreation model = new TestCaseFolderCreation(
                folderName,
                parentId,
                projectId
        );
        final HttpResponse<JsonNode> response = JiraConnect.testCaseFolderCreate(model);
        return response.getBody().getObject().getInt("id");
    }

    private String getNameById(final Integer id, final TCFolder folder) {
        if (folder.getId() != null && folder.getId().equals(id)) {
            return folder.getName();
        } else {
            return folder
                    .getChildren()
                    .stream()
                    .filter(child -> child.getId() != null && child.getId().equals(id))
                    .findFirst()
                    .map(TCFolder::getName)
                    .orElse("");
        }
    }

    private Integer getIdByName(final String name, final TCFolder folder) {
        if (folder.getName() != null && folder.getName().equals(name)) {
            return folder.getId();
        } else {
            return folder
                    .getChildren()
                    .stream()
                    .filter(child -> child.getName() != null && child.getName().equals(name))
                    .findFirst()
                    .map(TCFolder::getId)
                    .orElse(null);
        }
    }
}
