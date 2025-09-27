package ru.sbt.edu_power.e2e_core.devtools.network;

import com.github.kklisura.cdt.protocol.types.network.Request;
import com.google.gson.JsonParser;
import ru.sbtqa.tag.qautils.errors.AutotestError;

public class ConnectionStat {
    private final String id;
    private final long start;
    private final String requestName;
    private final String requestBody;
    private int duration;
    private boolean isFailed;
    private String response;
    private final ContentType contentType;
    private String stackTrace;

    public ConnectionStat(final Request request, final String requestId) {
        id = requestId;
        start = System.currentTimeMillis();
        final JsonParser parser = new JsonParser();
        contentType = ContentType.getContentTypeByPath(request.getUrl());
        try {
            if (contentType == ContentType.GRAPHQL) {
                if (parser.parse(request.getPostData()).getAsJsonObject().has("operationName"))  {
                    requestName = parser.parse(request.getPostData()).getAsJsonObject().getAsJsonPrimitive("operationName").getAsString();
                } else {
                    final String[] parts = parser.parse(request.getPostData()).getAsJsonObject().getAsJsonPrimitive("query").getAsString().split("\\{__typename");
                    requestName = parts.length > 1 ? parts[1].trim() : parts[0];
                }
            } else {
                requestName = contentType.toString();
            }
        } catch (final Throwable e) {
            throw new AutotestError(e);
        }

        try {
            requestBody = request.getHasPostData() != null
                          && request.getHasPostData()
                          && request.getPostData() != null ?
                    request.getPostData() : request.getUrl();
        } catch (final Throwable e) {
            throw new AutotestError(e);
        }
    }

    public void setResponse(final String response) {
        this.response = response;
    }

    public void setStackTrace(final String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public void finished() {
        duration = (int) (System.currentTimeMillis() - start);
    }

    public void failed() {
        duration = 0;
        isFailed = true;
    }

    public String getResponse() {
        return response;
    }

    public String getId() {
        return id;
    }

    public String getRequestName() {
        return requestName;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public int getDuration() {
        return duration;
    }

    public boolean isFailed() {
        return isFailed;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public String getStackTrace() {
        return stackTrace;
    }
}
