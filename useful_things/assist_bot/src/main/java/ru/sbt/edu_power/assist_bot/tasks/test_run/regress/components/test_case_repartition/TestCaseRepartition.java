package ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.test_case_repartition;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.AnalyticUtils;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.test_run.TestRunSlice;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraUser;
import ru.sbt.edu_power.external_services.jira.tc_verifier.NewJiraTCCollector;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.Execution;
import ru.sbt.edu_power.external_services.jira.test_manager.model.TestCaseModel;
import ru.sbt.edu_power.external_services.jira.test_manager.model.TestCaseModelFolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

// Класс выполняет построение наборов тест-кейсов разбитых по папкам по выбранному пользователю
// полученные наборы далее можно использовать для перемещения
public class TestCaseRepartition {
    private final TestRunSlice testRunSlice;
    private final JiraUser user;
    private final MutableGraph<TestCaseModelFolder> graph = GraphBuilder.directed().allowsSelfLoops(false).build();
    // мапа для экзекушены разбитые по своим папкам
    private final Map<TestCaseModelFolder, List<Execution>> folderToExecutionsListMap = new HashMap<>();
    // мапа хранит папки и все экзекушены из неё и всех вложенных в неё папок
    private final Map<TestCaseModelFolder, List<Execution>> folderToExecutionDeepListMap = new HashMap<>();
    // мапа хранить ID папки и объект папки (для возможности быстро строить полный путь до папки по её ID)
    private final Map<String, TestCaseModelFolder> folderIdToFolderMap = new HashMap<>();

    public TestCaseRepartition(final TestRunSlice testRunSlice, final JiraUser user) {
        this.testRunSlice = testRunSlice;
        this.user = user;
    }

    // мапа возвращает значимые наборы из папок и вложенных в неё экзекушенов
    public Map<TestCaseModelFolder, List<Execution>> getFunctionalParts() {
        if (folderToExecutionDeepListMap.isEmpty()) {
            updateGraph();
            search();
            removeFolderWithOutsideCaseCountRange();
            updateFolderIdMap();
        }
        return folderToExecutionDeepListMap;
    }

    public Map<String, TestCaseModelFolder> getFolderIdToFolderMap() {
        return folderIdToFolderMap;
    }

    private void updateFolderIdMap() {
        folderToExecutionsListMap.keySet().forEach(model -> folderIdToFolderMap.put(model.getId().toString(), model));
    }

    // метод возвращает граф директорий в которых расположены тест-кейсы выбранного пользователя
    // находящиеся в тест-сете в статусе not started и autofail
    private void updateGraph() {
        testRunSlice.getUserToExecutionStatusToExecutionsMap()
                    .get(user)
                    .entrySet()
                    .stream()
                    .filter(e -> AnalyticUtils.isInNotStartedStatus(e.getKey()))
                    .map(Map.Entry::getValue)
                    .flatMap(List::stream)
                    .forEach(this::updateGraph);
    }

    // метод определяет родительскую директорию для тест-кейса и добавляет её в граф
    private void updateGraph(final Execution execution) {
        final TestCaseModel testCaseModel = NewJiraTCCollector.getInstance().getById(execution.getTestCaseKey());
        final TestCaseModelFolder folder = testCaseModel.getFolder();
        if (Objects.nonNull(folder) && Objects.nonNull(folder.getParent())) {
            if (!folderToExecutionsListMap.containsKey(folder)) {
                folderToExecutionsListMap.put(folder, new ArrayList<>());
            }
            folderToExecutionsListMap.get(folder).add(execution);
            graph.putEdge(folder, folder.getParent());
        }
    }

    // перебираю все директории и заполняю folderToExecutionDeepListMap
    private void search() {
        graph.nodes()
             .forEach(f -> folderToExecutionDeepListMap.put(f, getExecutionsDeep(f)));
    }

    private void removeFolderWithOutsideCaseCountRange() {
        final List<TestCaseModelFolder> resultList = folderToExecutionDeepListMap
                .entrySet()
                .stream()
                .filter(e -> !e.getValue().isEmpty())
                .sorted((a, b) -> Integer.compare(b
                        .getValue()
                        .size(), a.getValue().size()))
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        new ArrayList<>(folderToExecutionDeepListMap.keySet())
                .forEach(f -> {
                    if (!resultList.contains(f)) {
                        folderToExecutionDeepListMap.remove(f);
                    }
                });

    }

    // метод рекурсивно переходит во все внутренние директории и возвращает список всех экзекушенов из них
    private List<Execution> getExecutionsDeep(
            final TestCaseModelFolder folder
    ) {
        final List<Execution> executions = new ArrayList<>();
        scanFolders(executions, folder);
        return executions;
    }

    private void scanFolders(
            final List<Execution> executions,
            final TestCaseModelFolder folder
    ) {
        if (folderToExecutionsListMap.containsKey(folder)) {
            executions.addAll(folderToExecutionsListMap.get(folder));
            graph.predecessors(folder).forEach(f -> scanFolders(executions, f));
        }
    }

}
