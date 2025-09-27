package ru.sbt.edu_power.e2e_core.devtools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import ru.sbt.edu_power.e2e_core.devtools.network.ConnectionStat;
import ru.sbt.edu_power.e2e_core.devtools.network.NetworkControl;
import ru.sbt.edu_power.e2e_core.error_processing.ErrorCollector;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Клас предназначен для проврки параметров запроса в нетворке
 */
public class CheckingRequests {
    final ObjectMapper mapper = new ObjectMapper();

    /**
     * Проверка запроса на параметры и значения
     * Подерживается проверка значений в массиве
     * Для отфильтровки запростов нужно указать уникальное значение которое есть в нужном запросе, например SchoolList.Button.create
     * @param requestName - любое уникальное значение в запросе
     * @param data - key - путь до нужного значения, пример event;type (если нужно проверить массив то путь до массива)
     *              value  - значение (для массива доступна множественная проверка пример: nodeName:SPAN , class:MuiButton-label && nodeName:DIV , id:app-root && nodeName:DIV,class :*PageWrapper* )
     */
    public void checkRequests(String requestName, final Map<String, String> data) {
        final ErrorCollector errorCollector = new ErrorCollector();
        final Collection<ConnectionStat> listRequest = NetworkControl.getNetworkActiveConnectionInstance().getStatMap().values().stream().filter(e -> e.getRequestBody().contains(requestName)).collect(Collectors.toList());
        Assert.assertFalse("Найдено более одного запроса, уточните значение по которому будет осуществляться поиск", listRequest.size() > 1);
        Assert.assertFalse("Не найдено ни одного запроса, уточните значение по которому будет осуществляться поиск", listRequest.isEmpty());
        for (final Map.Entry<String, String> dataEntry : data.entrySet()) {
            JsonNode value = getNode(dataEntry.getKey(), listRequest.stream().findFirst().get().getRequestBody());
            final String dataValue = dataEntry.getValue();
            try {
                if (value.isArray()) {
                    errorCollector.assertTrue(String.format("Часть значений не было найдено \"%s\" ", dataValue), findValueInArray(value, dataValue));
                } else {
                    errorCollector.assertTrue(String.format(
                            "Ожидаемое значение \"%s\" не соответствует актуальному \"%s\"", dataValue, value), checkEntry(dataValue, validateValues(dataValue), value.asText()));
                }

            } catch (NullPointerException e) {
                throw new RuntimeException(String.format("Не удалось \"%s\" найти значение по пути \"%s\"", dataValue, dataEntry.getKey()));
            }
        }
        errorCollector.assertAll();
    }

    /**
     * Получаем ноду по пути
     * @param path - Путь до ноды
     * @param request - Запрос из нетворка
     * @return
     */
    public JsonNode getNode(final String path, final String request) {
        final String[] pathParam = path.split(";");
        try {
            JsonNode node = mapper.readTree(request).get(pathParam[0]);
            if (pathParam.length > 1) {
                for (int i = 1; i < pathParam.length; i++) {
                    node = node.get(pathParam[i]);
                }
            }
            return node;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(String.format("Не удалось \"%s\" распарсить значение", path));
        }
    }

    /**
     * Проверка параметров внутри массива
     * @param array - массив ноды
     * @param findValues - значение или список значений которые должны быть в массиве
     * @return
     */
    private boolean findValueInArray(final JsonNode array, final String findValues) {
        final String[] values = findValues.split("&&");
        int count = 0;
        for (final String value : values) {
            for (final JsonNode valuesArray : array) {
                if (equalsArrayInValue(mapper.convertValue(valuesArray, Map.class), value)) {
                    count++;
                    break;
                }
            }
        }
        return count == values.length;
    }

    /**
     * Ищим соответствия между значением и элементами массива
     * @param actualMap - актуальные значение key - параметр value - значение параметра
     * @param findValues - значение или список значений которые должны быть в массиве
     * @return
     */
    private boolean equalsArrayInValue(final Map<String, String> actualMap, final String findValues) {
        final Map<String, String> expectedMap = convert(findValues.split(","));
        if (actualMap.size() != expectedMap.size()) {
            return false;
        }
        for (final Map.Entry<String, String> expected : expectedMap.entrySet()) {
            final String expectedValue = validateValues(expected.getValue());
            final String actualValue = validateValues(String.valueOf(actualMap.get(expected.getKey())));
            if (expected.getValue().contains("*")) {
                if (checkEntry(expected.getValue(), expectedValue, actualValue)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Проверка на вхождение
     * @param noValidateValue - значение которое не проходило валидацию
     * @param expected - значение после валидации
     * @param actual - актуальное значение
     * @return
     */
    private boolean checkEntry(final String noValidateValue, final String expected, final String actual) {
        if (noValidateValue.contains("*")) {
            if (noValidateValue.startsWith("*") && noValidateValue.endsWith("*") && !actual.contains(expected)) {
                return false;
            }
            if (noValidateValue.startsWith("*") && !noValidateValue.endsWith("*") && !actual.endsWith(expected)) {
                return false;
            }
            if (!noValidateValue.startsWith("*") && noValidateValue.endsWith("*") && !actual.startsWith(expected)) {
                return false;
            }
        } else {
            if (!actual.equals(expected)) {
                return false;
            }
        }
        return true;
    }

    /**
     * конвертация из массива в карту
     * @param values - массив
     * @return Map
     */
    private Map<String, String> convert(final String[] values) {
        final Map<String, String> mapValue = new HashMap<>();
        for (String value : values) {
            final String[] keyAndValue = value.split(":");
            mapValue.put(keyAndValue[0].trim(), keyAndValue[1].trim());
        }
        return mapValue;
    }

    private String validateValues(final String values) {
        return values.replaceAll(" ", "").toLowerCase().replace("*", "");
    }

}
