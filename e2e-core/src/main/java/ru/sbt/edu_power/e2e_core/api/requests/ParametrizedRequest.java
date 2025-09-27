package ru.sbt.edu_power.e2e_core.api.requests;

import org.junit.Assert;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbt.edu_power.e2e_core.resource_repository.ResourceRepository;
import ru.sbtqa.tag.datajack.Stash;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Класс выполняет подстановку параметров в запрос, взятый из файла request-map.resources
 */
public class ParametrizedRequest {
    private final String requestID;
    private final Map<String, String> params;
    public static final String SCHOOL_ID_HEADER = "school.id.header";
    public static final String REQUEST_HEADERS = "REQUEST_HEADERS";

    public ParametrizedRequest(final String requestID, final Map<String, String> params) {
        this.requestID = requestID;
        this.params = params;
    }

    @Override
    public String toString() {
        final AtomicReference<String> request = new AtomicReference<>(getRequestString());
        final Map<String, String> headers = new HashMap<>();
        params.forEach((k, v) -> {
                    if (k.startsWith("HEADER_")) {
                        headers.put(k.replace("HEADER_", ""), v);
                        return;
                    }
                    if ("SCHOOL_ID_HEADER".equals(k)) {
                        headers.put("schoolid", v);
                        return;
                    }
                    if(k.startsWith("JSON_")){
                        request.set(request.get().replace(k, DataProcessing.decodeValue(v)));
                        return;
                    }
                    if (!request.get().contains(k)) {
                        throw new AutotestError(
                                String.format("В запросе нет параметра %s", k)
                        );
                    }
                    if (v.replace(" ", "").trim().equals("[]")) {
                        request.set(request.get().replace(k, ""));
                        return;
                    }
                    String result = getResultParameter(v);
                    final String quotedParamName = "\"" + k + "\"";
                    if (request.get().contains(quotedParamName)) {
                        request.set(request
                                .get()
                                .replace(quotedParamName, !result.contains(",") ? "\"" + result + "\"" : result));
                    } else {
                        request.set(request.get().replace(k, !result.contains(",") ? result : result.replace("\"", "")));
                    }

                }
        );
        if (!headers.isEmpty()) {
            Stash.put(REQUEST_HEADERS, headers);
        }
        return request.get();
    }

    private String getResultParameter(final String value) {
        String result;
        if (value.contains(",")) {
            final AtomicReference<String> array = new AtomicReference<>("");
            Arrays.stream(value.split(",")).forEach(e -> {
                array.set(array.get() + "\"" + DataProcessing.decodeValue(e.trim()) + "\" ,");
            });
            result = array.get().substring(0, array.get().length() - 2);
        } else {
            result = DataProcessing.decodeValue(value);
        }
        return result;
    }

    private String getRequestString() {
        final Map<String, String> repository = ResourceRepository.getResource(ResourceRepository.AvailableResource.REQUEST_MAP);
        Assert.assertTrue(
                String.format("Не найдены ресурсы для ID \"%s\"", requestID),
                repository.containsKey(requestID)
        );
        return repository.get(requestID);
    }
}
