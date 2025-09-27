package ru.sbt.edu_power.e2e_core.api.requests;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.qameta.allure.Allure;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Assert;
import ru.sbt.edu_power.e2e_core.api.connect.HttpRequestClient;
import ru.sbt.edu_power.e2e_core.api.connect.Stand;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbt.edu_power.e2e_core.resource_repository.ResourceRepository;
import ru.sbtqa.tag.datajack.Stash;
import ru.sbtqa.tag.qautils.errors.AutotestError;
import ru.sbtqa.tag.qautils.properties.Props;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class APIRequest {
    public static String doRequestByRequestID(final String requestId, final boolean isSynchronized) {
        return doRequestByRequestID(Stand.STAND_URL, requestId, true, isSynchronized);
    }

    public static String doRequest(final String request, final boolean checkResponse, final boolean isSynchronized) {
        return doRequest(Stand.STAND_URL, request, checkResponse, isSynchronized);
    }

    public static String doRequestByRequestID(
            final String stand,
            final String requestId,
            final boolean checkResponse,
            final boolean isSynchronized
    ) {
        final Map<String, String> resource = ResourceRepository
                .getResource(ResourceRepository.AvailableResource.REQUEST_MAP);
        Assert.assertTrue(
                String.format("Не найдены ресурсы для ID \"%s\"", requestId),
                resource.containsKey(requestId)
        );
        return doRequest(stand, resource.get(requestId), checkResponse, isSynchronized);
    }

    public static String doRequest(
            final String stand,
            final String request,
            final boolean checkResponse,
            final boolean isSynchronized
    ) {
        final String response = HttpRequestClient.post(
                getServiceUrl(stand),
                request,
                getAuthData(),
                isSynchronized
        );
        Allure.addAttachment("Ответ на запрос", response);
        if (checkResponse) {
            checkResponse(response);
        }
        return response;
    }

    public static String doRequestByMethod(
            final String url,
            final String domain,
            final String method,
            final String request
    ) {
        final String domainUrl = getUrlByDomain(Stand.STAND_URL, domain) + url;
        final String response = HttpRequestClient.requestByMethod(domainUrl, request, getAuthData(), method);
        checkResponse(response);
        return response;
    }

    public static String doRequestByMethodUrl(final String url, final String domain, final String method) {
        final String domainUrl = getUrlByDomain(Stand.STAND_URL, domain) + url;
        return HttpRequestClient.requestByMethod(domainUrl, null, getAuthData(), method);
    }

    public static void doRequestUrlWithoutResponse(
            final String url,
            final String request,
            final boolean isSynchronized
    ) {
        HttpRequestClient.post(url, request, getAuthData(), isSynchronized);
    }

    public static String getUrlByDomain(final String url, final String domain) {
        if (domain.isEmpty()) {
            return url;
        }
        final String[] lvlDomain = url.split("\\.", 2);
        return lvlDomain[0] + domain + "." + lvlDomain[1];
    }

    private static String getServiceUrl(final String stand) {
        final String serviceUrl = Props.get("service.url");
        return !serviceUrl.equals("") ? stand + serviceUrl : stand + "/services/graphql";
    }

    private static List<NameValuePair> getAuthData() {
        final ArrayList<NameValuePair> data = new ArrayList<>();
        data.add(new BasicNameValuePair("Authorization", "Bearer " + Stash.getValue(APIAuth.TOKEN_STASH_KEY)));
        if (Stash.asMap().containsKey(ParametrizedRequest.REQUEST_HEADERS)) {
            final Map<String, String> headers = Stash.getValue(ParametrizedRequest.REQUEST_HEADERS);
            headers.forEach((k, v) -> data.add(new BasicNameValuePair(k, v)));
            Stash.remove(ParametrizedRequest.REQUEST_HEADERS);
        }
        return data;
    }

    private static void checkResponse(final String response) {
        final JsonParser parser = new JsonParser();
        final JsonElement element = parser.parse(response);
        if (element.getAsJsonObject().has("errors")) {
            final String errorMessage = element
                    .getAsJsonObject()
                    .getAsJsonArray("errors")
                    .get(0)
                    .getAsJsonObject()
                    .getAsJsonPrimitive("message")
                    .getAsString();
            final String trace = element
                    .getAsJsonObject()
                    .getAsJsonArray("errors")
                    .get(0)
                    .getAsJsonObject()
                    .getAsJsonObject("extensions")
                    .getAsJsonPrimitive("trace")
                    .getAsString()
                    .replaceAll("\\n", "\n")
                    .replaceAll("\\t", "");
            Allure.addAttachment(errorMessage, trace);
            throw new AutotestError("Ошибка выполнения запроса API " + element);
        }
    }
}
