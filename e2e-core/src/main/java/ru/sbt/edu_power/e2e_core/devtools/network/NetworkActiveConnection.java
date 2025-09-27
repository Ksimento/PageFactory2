package ru.sbt.edu_power.e2e_core.devtools.network;

import com.github.kklisura.cdt.protocol.commands.Network;
import com.github.kklisura.cdt.services.exceptions.ChromeDevToolsInvocationException;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cucumber.api.Scenario;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import ru.sbt.edu_power.e2e_core.devtools.DevToolsException;
import ru.sbt.edu_power.e2e_core.devtools.ErrorExclusion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Класс реализует слежение за активными коннектами к серверу
 */
@Slf4j
public class NetworkActiveConnection {
    private final Network network;
    private final ErrorExclusion errorExclusion;
    private final Map<String, ConnectionStat> statMap = new ConcurrentHashMap<>();
    private final List<String> activeConnection = new ArrayList<>();
    private final List<String> allConnection = new ArrayList<>();
    private final List<String> backendErrors = new ArrayList<>();
    private boolean isErrorControlEnable;
    // типы файлов, загрузка которых будет игнорироваться
    private List<String> notControlledMediaTypes = Arrays.asList(".png", ".jpg");

    public NetworkActiveConnection(final Network network) {
        this.network = network;
        errorExclusion = new ErrorExclusion();
    }

    public void initErrorExclusion(final Scenario scenario) {
        errorExclusion.init(scenario);
    }

    public void enable() {
        // собираем все запущенные коннекты, которые удовлетворяют ALLOWABLE_CONTENT_TYPE
        network.onRequestWillBeSent(request -> {
            final ContentType contentType = ContentType.getContentTypeByPath(request.getRequest().getUrl());
            if (contentType != ContentType.UNDEFINED && !activeConnection.contains(request.getRequestId())) {
                // игнорируем загрузку некоторых типов медиа данных
                if (contentType == ContentType.STORAGE && notControlledMediaTypes.stream().anyMatch(t -> request.getRequest().getUrl().contains(t))) {
                    return;
                }
                allConnection.add(request.getRequestId());
                activeConnection.add(request.getRequestId());
                statMap.put(request.getRequestId(), new ConnectionStat(request.getRequest(), request.getRequestId()));
            }
        });
        // Если коннект оборвался, исключаем его
        network.onLoadingFailed(event -> {
            activeConnection.remove(event.getRequestId());
//            allActiveConnection.remove(event.getRequestId());
            if (statMap.containsKey(event.getRequestId())) {
                statMap.get(event.getRequestId()).failed();
            }
        });

        network.onDataReceived(event -> {
            activeConnection.remove(event.getRequestId());
            if (statMap.containsKey(event.getRequestId())) {
                statMap.get(event.getRequestId()).finished();
            }
        });


        network.onResponseReceived(response -> {
            activeConnection.remove(response.getRequestId());
            if (statMap.containsKey(response.getRequestId())) {
                statMap.get(response.getRequestId()).finished();
            }
            if (response.getResponse().getStatus() == HttpStatus.SC_OK && statMap.containsKey(response.getRequestId())) {
                if (
                        response.getResponse().getMimeType() != null
                                && "application/json".equals(response.getResponse().getMimeType())
                ) {
                    try {
                        if (network.getResponseBody(response.getRequestId()) == null) {
                            return;
                        }
                        final String responseBody = network.getResponseBody(response.getRequestId()).getBody();
                        final JsonObject jsonObject = new JsonParser().parse(responseBody).getAsJsonObject();
                        if (isErrorControlEnable && jsonObject.has("errors")) {
                            final String message = getMessage(jsonObject);
                            if (errorExclusion.isErrorNotAllowed(message)) {
                                final String trace = getTrace(jsonObject);
                                if (errorExclusion.isErrorNotAllowed(trace)) {
                                    backendErrors.add(response.getRequestId());
                                    statMap.get(response.getRequestId()).setStackTrace(trace);
                                }
                            }
                        }
                        statMap.get(response.getRequestId()).setResponse(responseBody);
                    } catch (final ChromeDevToolsInvocationException e) {
                        if (
                                !"No data found for resource with given identifier".equals(e.getMessage())
                                && !"No resource with given identifier found".equals(e.getMessage())
                        ) {
                            throw new DevToolsException(e);
                        }
                    }
                }
            }
        });
    }

    public void enableErrorControl() {
        isErrorControlEnable = true;
    }

    public List<String> getActiveConnection() {
        return activeConnection;
    }


    // Возвращает true если на момент запроса нет активных коннектов к серверу
    public boolean isConnectionNotActive() {
        return activeConnection.isEmpty();
    }

    public void purge() {
        activeConnection.clear();
    }

    public ConnectionStat getConnectionStat(final String id) {
        return statMap.get(id);
    }

    public List<String> getErrors() {
        return backendErrors;
    }

    public List<String> getLastRequests(final int lastReqNumber) {
        if (allConnection.size() <= lastReqNumber) {
            return allConnection;
        }
        return allConnection.subList(allConnection.size() - lastReqNumber - 1, allConnection.size() - 1);
    }

    public void clearErrors() {
        backendErrors.clear();
    }

    // Получаем стектрейс из джейсона ответа сервера
    private String getTrace(final JsonObject json) {
        final JsonArray errors = json.getAsJsonArray("errors");
        if (errors.size() == 0) {
            return "";
        }
        final JsonObject extensions = errors.get(0)
                                           .getAsJsonObject()
                                           .getAsJsonObject("extensions");
        if (extensions.has("trace")) {
            return extensions.get("trace")
                    .getAsString();
        }
        return "";
    }

    // получаем текст ошибки
    private String getMessage(final JsonObject json) {
        final JsonArray errors = json.getAsJsonArray("errors");
        if (errors.size() == 0) {
            return "";
        }
        if (errors.get(0).getAsJsonObject().has("message")) {
            return errors.get(0)
                         .getAsJsonObject()
                         .getAsJsonPrimitive("message")
                         .getAsString();
        }
        return "";
    }

    public Map<String, ConnectionStat> getStatMap() {
        return statMap;
    }
}
