package ru.sbt.edu_power.e2e_core.api.connect;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.lang.Nullable;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@AllArgsConstructor
public class RestService {

    private final RestTemplate restTemplate;

    public <T> ResponseEntity<T> post(
            final String url,
            final HttpEntity<?> httpEntity,
            final Class<T> responseType
    ) {
        return doRequest(url, httpEntity, HttpMethod.POST, responseType, null);
    }

    public <T> ResponseEntity<T> post(
            final String url,
            final HttpEntity<?> httpEntity,
            final Class<T> responseType,
            final RedirectStrategy redirectStrategy
    ) {
        return doRequest(url, httpEntity, HttpMethod.POST, responseType, redirectStrategy);
    }

    public <T> ResponseEntity<T> get(
            final String url,
            final HttpEntity<?> httpEntity,
            final Class<T> responseType
    ) {
        return doRequest(url, httpEntity, HttpMethod.GET, responseType, null);
    }

    public <T> ResponseEntity<T> get(
            final String url,
            final HttpEntity<?> httpEntity,
            final Class<T> responseType,
            final RedirectStrategy redirectStrategy
    ) {
        return doRequest(url, httpEntity, HttpMethod.GET, responseType, redirectStrategy);
    }

    public <T> ResponseEntity<T> delete(
            final String url,
            final HttpEntity<?> httpEntity,
            final Class<T> responseType
    ) {
        return doRequest(url, httpEntity, HttpMethod.DELETE, responseType, null);
    }

    @SuppressWarnings("unchecked")
    private <T> ResponseEntity<T> doRequest(
            final String url,
            final HttpEntity<?> httpEntity,
            final HttpMethod httpMethod,
            final Class<T> responseType,
            @Nullable final RedirectStrategy redirectStrategy
    ) {
        final ClientHttpRequestFactory defaultFactory = restTemplate.getRequestFactory();
        if (redirectStrategy != null) {
            final HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
            final HttpClient httpClient = HttpClientBuilder.create()
                                                           .setRedirectStrategy(redirectStrategy)
                                                           .build();
            factory.setHttpClient(httpClient);
            restTemplate.setRequestFactory(factory);
        }

        try {
            log.debug("doRequest: Method{}, url {}", httpMethod, url);

            final ResponseEntity<T> entity;

            if (httpEntity != null &&
                httpEntity.hasBody() &&
                httpEntity.getBody() instanceof Map &&
                !((Map) httpEntity.getBody()).isEmpty()) {
                final Map<String, String> queryParameters = (Map<String, String>) httpEntity.getBody();
                final UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
                for (final Map.Entry<String, String> entry : queryParameters.entrySet()) {
                    builder.queryParam(entry.getKey(), entry.getValue());
                }
                entity = restTemplate.exchange(
                        builder.buildAndExpand(new HashMap<>()).toUri(),
                        httpMethod,
                        httpEntity,
                        responseType
                );
            } else {
                entity = restTemplate.exchange(url, httpMethod, httpEntity, responseType);
            }

            final String message = String.format("Method=%s, url=%s, code=%s, body=%s",
                    httpMethod.name(), url,
                    entity.getStatusCodeValue(), entity.getBody()
            );

            log.debug(message);

            return entity;

        } catch (final HttpServerErrorException | HttpClientErrorException e) {
            final String message = String.format("Method=%s, url=%s, code=%s,  message=%s",
                    httpMethod.name(), url,
                    e.getStatusCode(), e.getResponseBodyAsString()
            );

            log.debug(message, e);
            throw new IllegalStateException(e);
        } catch (final RuntimeException e) {
            log.error("RuntimeException", e);
            throw new IllegalStateException(e);
        } finally {
            if (redirectStrategy != null) {
                restTemplate.setRequestFactory(defaultFactory);
            }
        }
    }
}