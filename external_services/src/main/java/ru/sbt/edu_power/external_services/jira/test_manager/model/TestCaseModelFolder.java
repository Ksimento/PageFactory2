package ru.sbt.edu_power.external_services.jira.test_manager.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@NoArgsConstructor
@Getter
@Setter
public class TestCaseModelFolder {
    private TestCaseModelFolder parent;
    private String name;
    private Integer id;

    public List<TestCaseModelFolder> getParentsFolders() {
        final List<TestCaseModelFolder> parents = new ArrayList<>();
        getParentsFolder(parents, this);
        Collections.reverse(parents);
        return parents;
    }

    private void getParentsFolder(final List<TestCaseModelFolder> parents, final TestCaseModelFolder folder) {
        parents.add(folder);
        if (folder.parent != null) {
            getParentsFolder(parents, folder.parent);
        }
    }

    public List<String> getParents() {
        final List<String> parents = new ArrayList<>();
        getParents(parents, this);
        return parents;
    }

    private void getParents(final List<String> parents, final TestCaseModelFolder folder) {
        parents.add(folder.id.toString());
        if (folder.parent != null) {
            getParents(parents, folder.parent);
        }
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return id.equals(((TestCaseModelFolder) obj).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
