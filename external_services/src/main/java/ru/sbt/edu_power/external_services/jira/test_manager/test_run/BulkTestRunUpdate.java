package ru.sbt.edu_power.external_services.jira.test_manager.test_run;

import com.google.gson.Gson;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.ESUtils;
import ru.sbt.edu_power.external_services.jira.JiraConnect;
import ru.sbt.edu_power.external_services.jira.JiraConnectionException;
import ru.sbt.edu_power.external_services.jira.test_manager.model.TestCaseModel;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.Execution;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.TestRunBulkUpdateItems;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.TestRunModel;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.TestRunResult;
import ru.sbt.edu_power.external_services.timer.Timer;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

@Slf4j
public class BulkTestRunUpdate {
    private final TestRunModel testRunModel;
    private final List<Integer> addItems = new ArrayList<>();
    private final List<Integer> itemsToRemove = new ArrayList<>();
    private final Map<Integer, String> testCaseToOwnerMap = new HashMap<>();
    public static final int LIMIT = 300;

    public BulkTestRunUpdate(final TestRunModel testRunModel) {
        this.testRunModel = testRunModel;
    }

    public BulkTestRunUpdate addItems(final Collection<TestCaseModel> testCaseList) {
        addItems.addAll(testCaseList.stream().map(TestCaseModel::getId).collect(Collectors.toList()));
        // получаю список из тест-кейсов и их владельцев
        testCaseToOwnerMap.putAll(testCaseList
                .stream()
                .filter(e -> e.getOwner() != null)
                .collect(Collectors.toMap(
                        TestCaseModel::getId,
                        TestCaseModel::getOwner
                ))
        );
        return this;
    }

    public BulkTestRunUpdate removeItems(final List<Execution> itemsForRemove) {
        itemsForRemove.forEach(e -> itemsToRemove.add(e.getId()));
        return this;
    }

    public void execute() {
        // заполнение тест-сета выполняем постранично
        if (!addItems.isEmpty()) {
            for (int i = 0; i < Math.ceil(addItems.size() / (double) LIMIT); i++) {
                if (pageableAddItems(addItems, i)) {
                    break;
                }
            }
        } else {
            removeItems();
        }

    }

    private boolean pageableAddItems(
            final List<Integer> testCaseList,
            final int page
    ) {
        if (page * LIMIT > testCaseList.size()) {
            return true;
        }
        final int to = Math.min((page + 1) * LIMIT, testCaseList.size());

        final TestRunBulkUpdateItems bulkUpdateItems = new TestRunBulkUpdateItems(testRunModel.getId());
        addedItems(testCaseList.subList(page * LIMIT, to), bulkUpdateItems);
        update(bulkUpdateItems);
        log.info("Добавлено {} записей в тест-сет", to);
        return false;
    }

    private void removeItems() {
        final AtomicReference<HttpResponse<JsonNode>> response = new AtomicReference<>();
        final AtomicReference<Throwable> error = new AtomicReference<>();

        final BooleanSupplier waitWhenDataBeLoaded = () -> {
            try {
                error.set(null);
                response.set(JiraConnect.testRunLastTestResultGet(testRunModel.getId()));
            } catch (final Throwable e) {
                error.set(e);
                log.error("", e);
                ESUtils.freeze(10000);
                return false;
            }
            if (response.get().isSuccess()) {
                return true;
            }
            log.error(response.get().getBody() == null ? response.get().getStatusText() : response.get().getBody().toString());
            ESUtils.freeze(10000);
            return false;
        };
        final boolean result = Timer.executeTimer(180, waitWhenDataBeLoaded);
        if (!result) {
            if (Objects.nonNull(error.get())) {
                throw new JiraConnectionException("Фатальная ошибка связи при получении данных из тест-сета", error.get());
            }
            throw new JiraConnectionException("Не удалось получить данные из тест-сета\n" +
                    (response.get().getBody() == null ?
                            response.get().getStatusText() :
                            response.get().getBody().toString()
                    ));
        }
        final List<TestRunResult> testRunResults = new ArrayList<>();
        final Gson gson = new Gson();
        for (final Object o : response.get().getBody().getArray()) {
            testRunResults.add(gson.fromJson(o.toString(), TestRunResult.class));
        }
        final TestRunBulkUpdateItems bulkUpdateItems = new TestRunBulkUpdateItems(testRunModel.getId());
        bulkUpdateItems.itemsToRemove(
                testRunResults.stream()
                .filter(r -> itemsToRemove.contains(r.getLastTestResult().getId()))
                .map(TestRunResult::getId)
                .collect(Collectors.toList())
        );
        update(bulkUpdateItems);
    }

    private void update(final TestRunBulkUpdateItems bulkUpdateItems) {
        final AtomicReference<HttpResponse<?>> response = new AtomicReference<>();
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final BooleanSupplier waitWhenDataBeUpdated = () -> {
            try {
                error.set(null);
                response.set(JiraConnect.testRunItemsBulkUpdate(bulkUpdateItems));
            } catch (final Throwable e) {
                error.set(e);
                log.error("", e);
                ESUtils.freeze(3000);
                return false;
            }
            if (response.get().isSuccess()) {
                return true;
            }
            log.error(response.get().getBody() == null ? String.valueOf(response.get().getStatus()) : response.get().getBody().toString());
            ESUtils.freeze(3000);
            return false;
        };
        final boolean result = Timer.executeTimer(20, waitWhenDataBeUpdated);
        if (!result) {
            if (Objects.nonNull(response.get())) {
                throw new JiraConnectionException("Фатальная ошибка связи при обновлении данных в тест-сете", error.get());
            }
            throw new JiraConnectionException("Не удалось обновить данные в тест-сете\n" +
                    (response.get().getBody() == null ?
                            response.get().getStatusText() :
                            response.get().getBody().toString()
                    ));
        }
    }

    private void addedItems(final List<Integer> items, final TestRunBulkUpdateItems bulkUpdateItems) {
        items.forEach(item ->
                bulkUpdateItems.itemToAdd(
                        new TestRunBulkUpdateItems.AddedItem.LastTestResult(testCaseToOwnerMap.get(item), item)
                )
        );
    }
}
