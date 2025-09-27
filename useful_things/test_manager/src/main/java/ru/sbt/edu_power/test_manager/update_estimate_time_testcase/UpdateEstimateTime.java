package ru.sbt.edu_power.test_manager.update_estimate_time_testcase;

import com.google.gson.Gson;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import ru.sbt.edu_power.external_services.jira.JiraConnect;
import ru.sbt.edu_power.external_services.jira.tc_verifier.NewJiraTCCollector;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCQueryBuilder;
import ru.sbt.edu_power.external_services.jira.test_manager.model.TestCaseModel;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.TestResults;
import ru.sbt.edu_power.external_services.timer.Timer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
public class UpdateEstimateTime {
    private final TCQueryBuilder queryBuilder = new TCQueryBuilder();

    public void loadTestSet() {
        String projectKey = System.getProperty("jiraProjectKey");
        String testSet = System.getProperty("testRunKey");
        collect(projectKey);
        loadTestResult();
        updateEstimateTime(testSet, TCFields.ProjectId.valueOf(projectKey).id.toString());
    }

    /**
     * Выгружаем все тесты подходящие под условия queryBuilder из папки регресс
     *
     * @param projectKey - id проекта
     */
    public void collect(final String projectKey) {
        log.info("Начинаем выгрузку всех тест кейсов");
        Timer.startTimer("downloadMany");
        final TCFields.ProjectId projectId = TCFields.ProjectId.valueOf(projectKey);
        queryBuilder.addField(TCFields.ARCHIVED, false)
                .addField(TCFields.PROJECT_ID, projectId)
                .addField(TCFields.TEST_VIEW, TCFields.TestView.U_I)
                .addField(TCFields.TEST_TYPE, TCFields.TestType.REGRESS)
                .addField(TCFields.FOLDER, NewJiraTCCollector.getInstance().getFolderId("Регресс", projectId.id));
        NewJiraTCCollector.getInstance().collect(queryBuilder);
        log.info("Завершаем выгрузку всех тест кейсов");
    }

    /**
     * Догружаем в модель тест кейса результаты прогонов
     */
    public void loadTestResult() {
        log.info("Добавляем в тесты результаты прогонов");

        List<TestCaseModel> listTestCase = new ArrayList<>(NewJiraTCCollector.getInstance().asMap().values());
        Map<String,TestResults> rezMapTestResult = startExecutors(listTestCase, 10);

        NewJiraTCCollector.getInstance().asMap().values().parallelStream().forEach(tc -> {
            tc.setTestResults(rezMapTestResult.get(tc.getKey()));
        });

        Map<String, TestCaseModel> map = NewJiraTCCollector.getInstance().asMap();

        log.info("Завершили добавление результатов прогона");
    }

    /**
     * Получаем список тестов по которым необходимо выполнить запросы и количество частей на которые будем его делить,
     * делим список, и по каждой части запускаем отдельный поток в котором все запросы будут идти последовательно
     * @param list
     * @param countThread
     * @return
     */
    private Map<String,TestResults> startExecutors(List<TestCaseModel> list, int countThread){
        int count = list.size() / countThread;
        ExecutorService service = Executors.newFixedThreadPool(countThread);
        List<Future> futureList = new ArrayList<>();
        Map<String,TestResults> resultMap = new HashMap<>();
        int i = 0;
        while (i < countThread){
            int start = count * i;
            int end = count * (i + 1);
            if ((i + 1) == countThread){
                end = list.size();
            }
            Callable call = requestOneThread(list.subList(start, end));
            Future future = service.submit(call);
            futureList.add(future);
            i++;
        }

        futureList.forEach(future -> {
            try {
                resultMap.putAll((Map<? extends String, ? extends TestResults>) future.get());
            } catch (final InterruptedException | ExecutionException ignored) {}
        });
        service.shutdown();

        resultMap.forEach((key, value) -> {
            if (Objects.isNull(value)){
                final HttpResponse<JsonNode> response = JiraConnect.testCaseExecutionsGet(key);
                if (Objects.nonNull(response)){
                    final Gson gson = new Gson();
                    TestResults testResults = gson.fromJson(response.getBody().getObject().toString(), TestResults.class);
                    resultMap.put(key, testResults);
                } {
                    log.info("Не удалось получить данные по тесту = " + key);
                }
            }
        });
        return resultMap;
    }


    private Callable<Map<String,TestResults>> requestOneThread(List<TestCaseModel> list){
        JiraConnect connect = new JiraConnect();
        return ()->{
            final Map<String,TestResults> rezMap = new HashMap<>();
            list.forEach(tc -> {
                try{
                    final HttpResponse<JsonNode> response = connect.testCaseExecutionsGetNoStatic(tc.getId().toString());
                    if (Objects.isNull(response)){
                        rezMap.put(tc.getKey(), null);
                    } else {
                        final Gson gson = new Gson();
                        TestResults testResult = gson.fromJson(response.getBody().getObject().toString(), TestResults.class);
                        rezMap.put(tc.getKey(), testResult);
                    }
                } catch (Exception e) {
                    rezMap.put(tc.getKey(), null);
                    log.error("Возникла ошибка с тестом = " + tc.getId());
                }
            });
            return rezMap;
        };
    }

    /**
     * Выполняем обновление поля EstimatedTime значением из поля ExecutionTime из указанного тест сета
     *
     * @param testSet - номер тест сета
     */
    public void updateEstimateTime(String testSet, String projectId) {
        log.info("Выполняем заполнение поля EstimatedTime");
        NewJiraTCCollector.getInstance().asMap().values().parallelStream().forEach(tc -> {
                if (Objects.nonNull(tc.getTestResults()) && Objects.nonNull(tc.getTestResults().getResultsList())){

                    tc.getTestResults().getResultsList().parallelStream().forEach(rez -> {
                        if (Objects.nonNull(rez.getTestRun()) && Objects.equals(rez.getTestRun().getKey(), testSet)) {
                            if (Objects.nonNull(tc.getEstimatedTime()) && Objects.equals(tc.getEstimatedTime(), rez.getExecutionTime())) {
                                return;
                            }
                            try {
                                int resultTime = 0;
                                if (Objects.isNull(rez.getExecutionTime())) {
                                    resultTime = 60000;
                                } else {
                                    resultTime = rez.getExecutionTime();
                                }
                                update(tc.getId().toString(), projectId, TCFields.ESTIMATED_TIME.value, String.valueOf(resultTime));
                            } catch (Exception e) {
                                log.info("Не удалось обновить тест кейс Key = " + tc.getKey() + " Team = " + tc.getTeam());
                            }
                        }
                    });
                }
        });
        log.info("Завершаем заполнение поля EstimatedTime");
    }

    /**
     * Выполняем POST запрос на обновление поля в тесте
     *
     * @param testCaseId - id тест кейса
     * @param projectId  - id проекта
     * @param field      - название поля которое будем обновлять
     * @param fieldValue - значение обновляемого поля
     */
    private void update(
            String testCaseId, String projectId, String field, String fieldValue
    ) {
        final HttpResponse<JsonNode> httpResponse = JiraConnect.testCaseUpdateRest(testCaseId, projectId, field, fieldValue);
        if (httpResponse.getStatus() == HttpStatus.SC_NOT_FOUND) {
            log.error("Тест-кейс {} не найден в РТМ, либо архивирован", testCaseId);
            return;
        }
        if (!httpResponse.isSuccess()) {
            log.error(
                    "Ошибка при записи риска\ntestCaseKey: {}\n{}",
                    testCaseId,
                    httpResponse.getBody().toPrettyString()
            );
            return;
        }
    }
}
