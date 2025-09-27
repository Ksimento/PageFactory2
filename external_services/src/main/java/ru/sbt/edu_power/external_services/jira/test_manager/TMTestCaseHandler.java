package ru.sbt.edu_power.external_services.jira.test_manager;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import ru.sbt.edu_power.external_services.jira.JiraConnect;
import ru.sbt.edu_power.external_services.jira.JiraConnectionException;
import ru.sbt.edu_power.external_services.jira.tc_verifier.NewJiraTCCollector;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;
import ru.sbt.edu_power.external_services.jira.test_manager.model.TestCase;

/**
 * Класс выполняет физические манипуляции с тест-кейсами - такими как изменение значений полей
 */
@Slf4j
public class TMTestCaseHandler {
    private static long jiraUpdateCounter;

    // Изменение значения поля Автоматизирован
    public static boolean updateAutomatedStatus(
            final String testCaseKey,
            final TCFields.AutomatedStatus status
    ) {
        final TCFields.AutomatedStatus currentStatus = NewJiraTCCollector
                .getInstance()
                .getById(testCaseKey)
                .getAutomatedStatus();
        if (currentStatus == status) {
            return true;
        }
        final TestCase testCase = new TestCase();
        testCase.addCustomFieldParam(TMFields.AUTOMATED, status.value);
        update(testCaseKey, testCase, TCFields.AUTOMATED_STATUS, status);
        return true;
    }

    // Изменение значения поля Framework
    public static void updateFrameworkField(
            final String testCaseKey,
            final String framework
    ) {
        final TestCase testCase = new TestCase();
        testCase.addCustomFieldParam(TMFields.FRAMEWORK, framework);
        update(testCaseKey, testCase, TCFields.FRAMEWORK, TCFields.Framework.getByValue(framework));
    }

    // Метод выполняет отправку в джиру запроса на изменение данных
    private static void update(
            final String testCaseKey,
            final TestCase testCase,
            final TCFields fieldName,
            final Object newValue
    ) {
        final HttpResponse<JsonNode> httpResponse = JiraConnect.testCaseUpdate(testCaseKey, testCase);
        if (httpResponse.getStatus() == HttpStatus.SC_NOT_FOUND) {
            throw new IllegalArgumentException(String.format("Тест-кейс \"%s\" не найден в РТМ, либо архивирован", testCaseKey));
        }
        if (!httpResponse.isSuccess()) {
            throw new JiraConnectionException(String.format(
                    "Ошибка при отправке данных\ntestCaseKey: %s\nfieldName: %s\n%s",
                    testCaseKey,
                    fieldName.value,
                    httpResponse.getBody().toPrettyString()
            ));
        }
        jiraUpdateCounter++;
        // Устанавливаем новое значение поля в кэш
        NewJiraTCCollector.getInstance().updateModel(testCaseKey, fieldName, newValue);
    }

    public static long getJiraUpdateCounter() {
        return jiraUpdateCounter;
    }

}
