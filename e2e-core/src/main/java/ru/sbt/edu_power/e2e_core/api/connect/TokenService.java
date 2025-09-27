package ru.sbt.edu_power.e2e_core.api.connect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolException;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TokenService {
    private static final String KC_REALM = "EduPowerKeycloak";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public String getToken(final String username, final String password) {
        return getToken(Stand.STAND_URL, username, password);
    }

    public String getToken(final String stand, final String username, final String password) {
        if (isStandWithProdSecurityConfig(stand)) {
            return getTokenForStandWithProdSecurityConfig(stand, username, password);
        }

        final UrlEncodedFormEntity form = new UrlEncodedFormEntity(
                createFormParams(username, password),
                StandardCharsets.UTF_8
        );
        final String response = HttpRequestClient.post(getTokenUrl(stand), form, null);
        try {
            return OBJECT_MAPPER
                    .readValue(response, new TypeReference<Map<String, String>>() {
                    })
                    .get("access_token");
        } catch (final JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private boolean isStandWithProdSecurityConfig(final String stand) {
        return !(stand.startsWith("http://localhost") ||
                 stand.startsWith("https://localhost") ||
                 stand.startsWith("http://dev") ||
                 stand.startsWith("https://dev"));
    }

    private String getTokenServicePath(final String stand) {
        return isStandWithProdSecurityConfig(stand) ?
                "/services/rest/login-to/" + KC_REALM :
                "/auth/realms/" + KC_REALM + "/protocol/openid-connect/token";
    }

    private String getTokenUrl(final String stand) {
        return stand + getTokenServicePath(stand);
    }

    private String getTokenForStandWithProdSecurityConfig(
            final String stand,
            final String username,
            final String password
    ) {
        final Map<String, String> redirectCookies = new HashMap<>();
        final RedirectStrategy redirectStrategy = new LaxRedirectStrategy() {
            @Override
            public boolean isRedirected(
                    final HttpRequest httpRequest, final HttpResponse httpResponse, final HttpContext httpContext
            ) throws ProtocolException {
                final Header[] cookieHeaders = httpResponse.getHeaders("Set-Cookie");
                for (final Header cookieHeader : cookieHeaders) {
                    final String cookieHeaderValue = cookieHeader.getValue();
                    final int index = cookieHeaderValue.indexOf("=");
                    redirectCookies.put(
                            cookieHeaderValue.substring(0, index),
                            cookieHeaderValue.substring(index + 1).split(";")[0]
                    );
                }
                return super.isRedirected(httpRequest, httpResponse, httpContext);
            }
        };

        final RestService restService = new RestService(new RestTemplate());

        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.addAll(HttpHeaders.ACCEPT, Arrays.asList(
                "text/html",
                "application/xhtml+xm",
                "application/xml",
                "image/webp",
                "image/apng",
                "*/*",
                "application/signed-exchange"
                )
        );
        httpHeaders.addAll(HttpHeaders.ACCEPT_LANGUAGE, Arrays.asList("ru", "en"));
        httpHeaders.addAll(HttpHeaders.ACCEPT_ENCODING, Arrays.asList("gzip", "deflate", "br"));

        final String tokenUrl = getTokenUrl(stand);
        final ResponseEntity responseEntity = restService.get(tokenUrl +
                                                              "?returnTo=" +
                                                              tokenUrl.substring(
                                                                      0,
                                                                      tokenUrl.indexOf("/services/") + 1
                                                              ), null, String.class, redirectStrategy);

        try {
            httpHeaders.remove(HttpHeaders.CONTENT_TYPE);
            httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
            final List cookies = Objects.requireNonNull(responseEntity.getHeaders().get("Set-Cookie"));
            httpHeaders.addAll(HttpHeaders.COOKIE, Arrays.asList(
                    cookies.get(0).toString().split(";")[0],
                    cookies.get(1).toString().split(";")[0]
            ));

            restService.post(
                    new HtmlParser()
                            .parseTagAttribute(
                                    Objects.requireNonNull(responseEntity.getBody()).toString(),
                                    "form",
                                    "action"
                            )
                            .get(0),
                    new HttpEntity<>(getTokenRequestBody(username, password), httpHeaders),
                    String.class,
                    redirectStrategy
            );

            return redirectCookies.get("tokenId");
        } catch (final RuntimeException e) {
            throw new IllegalStateException("Empty body", e);
        }
    }

    private List<NameValuePair> createFormParams(final String username, final String password) {
        final List<NameValuePair> formParams = new ArrayList<>();

        formParams.add(new BasicNameValuePair("client_id", "edupower"));
        formParams.add(new BasicNameValuePair("grant_type", "password"));
        formParams.add(new BasicNameValuePair("username", username));
        formParams.add(new BasicNameValuePair("password", password));

        return formParams;
    }

    private MultiValueMap<String, String> getTokenRequestBody(final String username, final String password) {
        final MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("username", username);
        body.add("password", password);

        return body;
    }
}
