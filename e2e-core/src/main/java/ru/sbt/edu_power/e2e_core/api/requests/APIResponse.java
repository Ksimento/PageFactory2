package ru.sbt.edu_power.e2e_core.api.requests;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;

import java.util.ArrayList;
import java.util.List;

public class APIResponse {

    public static String getDataByKey(final String response, final String fields) {
        return getDataByKeyAndIndex(response, fields, 0);
    }
    /**
     * Вовращает список значений из результата API запроса по имени параметра, исключая null значения
     *
     * @param fields    - уникальное поле в ответе, например "student:getCodeReviewRoundsByStudentGoalId:eventId"
     * @param response  - строка ответа
     */
    public static List<String> getDataListByKey(final String response, final String fields) {
        final JsonParser parser = new JsonParser();
        final String responseDecoded = DataProcessing.decodeValue(response);
        final String[] arr_fields = fields.split(":");

        JsonElement element = parser.parse(responseDecoded);
        element = element.getAsJsonObject().get("data");
        final List<String> result = new ArrayList<>();
        for (final String field : arr_fields) {
            if (element.isJsonObject()) {
                element = element.getAsJsonObject().get(field);
            } else if (element.isJsonArray()) {
                element.getAsJsonArray().forEach(e -> {
                    final JsonElement data = e.getAsJsonObject().get(field);
                    if (!data.isJsonNull()) {
                        result.add(data.toString().replace("\"", ""));
                    }
                });
            }
        }
        if (!element.isJsonArray() && !element.isJsonNull()) {
            result.add(element.toString().replace("\"", ""));
        }
        return result;
    }

    /**
     * Иногда API запрос возвращает ответ, данные из которого нужны для следующего API запроса
     * Ответ может иметь вид {"data":{"student":{"getCodeReviewRoundsByStudentGoalId":[{"eventId":"0"},{"eventId":"141"}]}}}
     * где нужно получить значение только параметра "eventId" из массива под индексом 1
     *
     * @param fields   - уникальные поля в ответе, в примере выше это "student:getCodeReviewRoundsByStudentGoalId:eventId"
     * @param response - строка ответа
     * @param index    - индекс в json массиве, для который нужно получить данные
     */
    public static String getDataByKeyAndIndex(final String response, final String fields, final int index) {
        final JsonParser parser = new JsonParser();
        final String responseDecoded = DataProcessing.decodeValue(response);
        final String[] arr_fields = fields.split(":");

        JsonElement element = parser.parse(responseDecoded);
        element = element.getAsJsonObject().get("data");

        for (final String field : arr_fields) {
            if (element.isJsonObject()) {
                element = element.getAsJsonObject().get(field);
            } else if (element.isJsonArray()) {
                element = element.getAsJsonArray().get(index).getAsJsonObject().get(field);
            }
        }
        return element.toString().replace("\"", "");
    }

    /**
     * Получение размера json массива из ответа на запрос
     * Ответ может иметь вид {"data":{"student":{"getCodeReviewRoundsByStudentGoalId":[{"eventId":"0"},{"eventId":"141"}]}}}
     * где нужно получить размер массива [{"eventId":"0"},{"eventId":"141"}]
     *
     * @param fields   - пусть к массиву, в примере выше это "student:getCodeReviewRoundsByStudentGoalId"
     * @param response - строка ответа
     */
    public static int getArraySize(final String response, final String fields) {
        final String json = APIResponse.getDataByKey(response, fields);
        final JsonParser parser = new JsonParser();
        JsonElement element = parser.parse(json);
        int size = 0;
        if (element.isJsonArray()) {
            size = element.getAsJsonArray().size();
        }
        return size;
    }
}
