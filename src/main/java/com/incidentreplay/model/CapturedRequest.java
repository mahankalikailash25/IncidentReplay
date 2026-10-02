package com.incidentreplay.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class CapturedRequest {
    private String id;
    private String method;
    private String url;
    private Map<String, String> headers;
    private String body;
    private Integer originalStatusCode;
    private String originalResponseBody;
    private Instant capturedAt;

    public CapturedRequest() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(Instant capturedAt) {
        this.capturedAt = capturedAt;
    }

    public Integer getOriginalStatusCode() {
        return originalStatusCode;
    }

    public void setOriginalStatusCode(Integer originalStatusCode) {
        this.originalStatusCode = originalStatusCode;
    }

    public String getOriginalResponseBody() {
        return originalResponseBody;
    }

    public void setOriginalResponseBody(String originalResponseBody) {
        this.originalResponseBody = originalResponseBody;
    }
}
