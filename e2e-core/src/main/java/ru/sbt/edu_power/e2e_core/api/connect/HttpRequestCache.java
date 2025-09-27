package ru.sbt.edu_power.e2e_core.api.connect;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class HttpRequestCache {

    private static HttpRequestCache cache;

    private final Map<Long, Request> requests = new ConcurrentHashMap<>();

    private HttpRequestCache() {
    }

    public static synchronized HttpRequestCache getCache() {
        if (cache == null) {
            cache = new HttpRequestCache();
        }
        return cache;
    }

    @AllArgsConstructor
    @Getter
    public static class Request {

        private final String url;
        private final String httpMethod;
        private final Map<String, List<Object>> headers;
        private final Object body;
        private final Object response;

        @Override
        public String toString() {
            return "Url='" + url + '\'' + "\n" +
                   "HttpMethod='" + httpMethod + '\'' + "\n" +
                   "Headers=" + headers + "\n" +
                   "Body=" + body + "\n" +
                   "Response=" + response;
        }
    }

    public void put(final Long threadId, final Request request) {
        requests.put(threadId, request);
    }

    public Request get(final Long threadId) {
        return requests.get(threadId);
    }

    public void remove(final Long threadId) {
        requests.remove(threadId);
    }
}
