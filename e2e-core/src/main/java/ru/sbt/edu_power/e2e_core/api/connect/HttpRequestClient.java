package ru.sbt.edu_power.e2e_core.api.connect;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import io.qameta.allure.Allure;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.junit.Assert;
import ru.sbt.edu_power.e2e_core.api.requests.ParametrizedRequest;
import ru.sbtqa.tag.datajack.Stash;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class HttpRequestClient {

    // утилитный класс
    private HttpRequestClient() {
    }

    public static String post(
            final String url,
            final String query,
            final List<NameValuePair> headers,
            final boolean isSynchronized
    ) {
        Preconditions.checkArgument(query != null && !query.isEmpty(), "Http request query cannot by empty");

        final HttpEntity httpEntity = setRequestHttpEntity(query);

        return isSynchronized ? synchronizedPost(url, httpEntity, headers) : post(url, httpEntity, headers);
    }

    public static String requestByMethod(final String url, final String request, final List<NameValuePair> headers, final String method) {
        HttpEntity httpEntity = null;
        if (!(request == null)) {
            httpEntity = setRequestHttpEntity(request);
        }
        try (final CloseableHttpClient httpClient = createHttpClient()) {

            final HttpRequestBase httpRequestBase = createBaseRequest(url, httpEntity, method);
            setHeaders(httpRequestBase, headers);
            final CloseableHttpResponse response = httpClient.execute(httpRequestBase);
            final HttpEntity entity = response.getEntity();
            checkStatus(response.getStatusLine().getStatusCode(), response.getEntity(), response);
            return readFromEntity(entity);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static HttpEntity setRequestHttpEntity(final String request) {
        return EntityBuilder.create()
                .setContentType(ContentType.APPLICATION_JSON)
                .setText(request)
                .build();
    }


    private static HttpRequestBase createBaseRequest(final String url, HttpEntity httpEntity, final String method) {
        if ("DELETE".equals(method)) {
            return new HttpDelete(url);
        }
        if ("GET".equals(method)) {
            return new HttpGet(url);
        }
        Assert.assertNotNull("Параметры пустые", httpEntity);
        if ("PUT".equals(method)) {
            final HttpPut httpPut = new HttpPut(url);
            httpPut.setEntity(httpEntity);
            return httpPut;
        }
        if ("POST".equals(method)) {
            final HttpPost httppost = new HttpPost(url);
            httppost.setEntity(httpEntity);
            return httppost;
        }
        throw new AutotestError(String.format(
                "Тип запроса \"%s\" не реализован",
                method
        ));
    }

    private static void setHeaders(final HttpRequestBase httpRequestBase, final List<NameValuePair> headers) {
        if (headers != null) {
            httpRequestBase.setHeaders(headers
                    .stream()
                    .map(header -> new BasicHeader(header.getName(), header.getValue()))
                    .toArray(Header[]::new)
            );
        }
    }

    public static synchronized String synchronizedPost(
            final String serviceUrl,
            final HttpEntity httpEntity,
            final List<NameValuePair> headers
    ) {
        return post(serviceUrl, httpEntity, headers);
    }

    public static String post(
            final String serviceUrl,
            final HttpEntity httpEntity,
            final List<NameValuePair> headers
    ) {

        try (final CloseableHttpClient httpClient = createHttpClient()) {
            final HttpPost post = new HttpPost(serviceUrl);
            setHeaders(post, headers);
            post.setEntity(httpEntity);

            Allure.addAttachment("POST запрос", post.toString());
            Allure.addAttachment("POST headers", Arrays.stream(post.getAllHeaders()).map(Object::toString).collect(
                    Collectors.joining("\n")));
            try (final InputStream in = post.getEntity().getContent()) {
                final int n = in.available();
                final byte[] bytes = new byte[n];
                in.read(bytes, 0, n);
                Allure.addAttachment("POST body", new String(bytes, StandardCharsets.UTF_8));
            }

            return doRequest(httpClient, post);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static CloseableHttpClient createHttpClient() {
        final HttpClientBuilder builder = HttpClientBuilder.create();
        return builder.build();
    }

    private static String doRequest(
            final CloseableHttpClient httpClient,
            final HttpPost post
    ) throws IOException {

        try (final CloseableHttpResponse response = httpClient.execute(post)) {
            // удаляем из стэша данные о school_id
            Stash.remove(ParametrizedRequest.SCHOOL_ID_HEADER);
            final int status = response.getStatusLine().getStatusCode();
            final HttpEntity entity = response.getEntity();
            cacheRequest(post, response);
            checkStatus(status, entity, response);
            return readFromEntity(entity);
        }
    }

    private static void checkStatus(final int status, final HttpEntity entity, final CloseableHttpResponse response)
            throws IOException {
        if (!(status >= HttpStatus.SC_OK && status <= HttpStatus.SC_MULTI_STATUS)) {
            throw new IllegalStateException(String.format(
                    "Response code is: %s. Body: %s. %s",
                    status,
                    IOUtils.toString(entity.getContent(), Charsets.UTF_8),
                    response.containsHeader("X-Bad-Request") &&
                            response
                                    .getFirstHeader("X-Bad-Request")
                                    .toString()
                                    .contains("Denied by graphql query filter") ?
                            response.getFirstHeader("X-Bad-Request") :
                            "Или неверно указан адрес сервиса keycloak"
            ));
        }
    }

    private static void cacheRequest(final HttpPost post, final CloseableHttpResponse response) {
        HttpRequestCache
                .getCache()
                .put(
                        Thread.currentThread().getId(),
                        new HttpRequestCache.Request(
                                post.getURI().toString(),
                                post.getMethod(),
                                Arrays
                                        .stream(post.getAllHeaders())
                                        .collect(Collectors.toMap(
                                                Header::getName,
                                                h -> Collections.singletonList(h.getValue())
                                        )),
                                readFromEntity(post.getEntity()),
                                response.toString()
                        )
                );
    }

    private static String readFromEntity(final HttpEntity entity) {
        try {
            return IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
