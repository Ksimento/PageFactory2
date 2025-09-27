package ru.sbt.edu_power.e2e_core.layout;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import ru.sbtqa.tag.pagefactory.environment.Environment;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Класс выполняет разбор веб-элементов на ближайших предков и потомков
 */
@Slf4j
public class ElementsTree extends HashMap<String, WebElement> {
    private static final long serialVersionUID = -713260300271691911L;
    private final List<Node> nodeList = new ArrayList<>();
    private final Map<String, List<String>> parentToChildrenMap = new HashMap<>();

    @SuppressWarnings("unchecked")
    public void requestElementsTree() {
        final String js = "let elementTree = function(map) {let a = [];for (parentId in map) {for (childId in map) {" +
                          "if (parentId === childId) {continue;}if (map[parentId].contains(map[childId])) {" +
                          "a.push({'parent': parentId, 'child': childId })}}};return a;};return elementTree(arguments[0]);";
        final List<Map<String, String>> response = (List<Map<String, String>>) ((JavascriptExecutor) Environment
                .getDriverService()
                .getDriver()).executeScript(js, this);
        response.forEach(map -> nodeList.add(new Node(map.get("parent"), map.get("child"))));
        collectMap();
    }

    // метод возвращает предка по id потомка из уже сформированного дерева
    public String getParent(final String child) {
        final List<String> parents = parentToChildrenMap.entrySet()
                .stream()
                .filter(e -> e.getValue().contains(child))
                .map(Entry::getKey)
                .collect(Collectors.toList());
        if (parents.size() > 1) {
            throw new AutotestError("У элемента более одного предка");
        }
        return parents.isEmpty() ? null : parents.get(0);
    }

    public Map<String, List<String>> getParentToChildrenMap() {
        return parentToChildrenMap;
    }

    // Получаю корневые элементы - они не должны содержаться в списке потомков
    private Set<String> getRootElements() {
        final List<String> children = nodeList.stream().map(Node::getChild).collect(Collectors.toList());
        return nodeList.stream()
                       .map(Node::getParent)
                       .filter(t -> !children.contains(t))
                       .collect(Collectors.toSet());
    }

    private void collectMap() {
        final Set<String> roots = getRootElements();
        if (roots.isEmpty()) {
            return;
        }
        roots.forEach(parent -> parentToChildrenMap.put(parent, getChildren(parent)));
        roots.forEach(this::removeNodes);
        collectMap();
    }

    // возвращает список всех потомков из nodeList (включая вложенных) по предку
    private List<String> getChildren(final String parent) {
        return nodeList.stream()
                       .filter(t -> parent.equals(t.getParent()))
                       .filter(this::childIsUnique)
                       .map(Node::getChild)
                       .collect(Collectors.toList());
    }

    // метод удаляет все ноды из nodeList содержащие выбранного родителя
    private void removeNodes(final String parent) {
        nodeList.removeIf(t -> parent.equals(t.getParent()));
    }

    // метод проверяет, что содержащийся в ноде потомок единственный из всех нод
    // это позволяет определить, что нода содержит прямого потомка и предка
    private boolean childIsUnique(final Node node) {
        return nodeList.stream()
                       .map(Node::getChild)
                       .filter(node.getChild()::equals)
                       .count() == 1;
    }

    @Override
    public String toString() {
        return new Gson().toJson(parentToChildrenMap);
    }

    @Getter
    private static class Node {
        private final String parent;
        private final String child;

        Node(final String parent, final String child) {
            this.parent = parent;
            this.child = child;
        }
    }
}
