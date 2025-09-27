package ru.sbt.edu_power.external_services.jira.test_manager.test_run.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class TestRunBulkUpdateItems {
    private final Integer testRunId;
    private Set<AddedItem> addedTestRunItems;
    private Set<DeletedItems> deletedTestRunItems;

    public TestRunBulkUpdateItems(final Integer testRunId) {
        this.testRunId = testRunId;
    }

    public void itemToAdd(final AddedItem.LastTestResult item) {
        if (Objects.isNull(addedTestRunItems)) {
            addedTestRunItems = new HashSet<>();
        }
        addedTestRunItems.add(new AddedItem(addedTestRunItems.size(), item));
    }

    public void itemsToRemove(final List<Integer> items) {
        if (Objects.isNull(deletedTestRunItems)) {
            deletedTestRunItems = new HashSet<>();
        }
        items.forEach(i -> deletedTestRunItems.add(new DeletedItems(i)));
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class AddedItem {
        private Integer index;
        private LastTestResult lastTestResult;

        @Getter
        @AllArgsConstructor
        @Setter
        public static class LastTestResult {
            private String assignedTo;
            private Integer testCaseId;
        }
    }

    @Getter
    @AllArgsConstructor
    public static class DeletedItems {
        private final Integer id;
    }
}
