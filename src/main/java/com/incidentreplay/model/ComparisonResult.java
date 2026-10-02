package com.incidentreplay.model;

public class ComparisonResult {
    private String incidentId;
    private String requestId;
    private Integer originalStatusCode;
    private Integer replayStatusCode;
    private boolean statusCodeMatch;
    private String originalResponseBody;
    private String replayResponseBody;
    private boolean responseBodyMatch;
    private boolean overallMatch;
    private Long executionTimeMs;

    public ComparisonResult() {}

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Integer getOriginalStatusCode() {
        return originalStatusCode;
    }

    public void setOriginalStatusCode(Integer originalStatusCode) {
        this.originalStatusCode = originalStatusCode;
    }

    public Integer getReplayStatusCode() {
        return replayStatusCode;
    }

    public void setReplayStatusCode(Integer replayStatusCode) {
        this.replayStatusCode = replayStatusCode;
    }

    public boolean isStatusCodeMatch() {
        return statusCodeMatch;
    }

    public void setStatusCodeMatch(boolean statusCodeMatch) {
        this.statusCodeMatch = statusCodeMatch;
    }

    public String getOriginalResponseBody() {
        return originalResponseBody;
    }

    public void setOriginalResponseBody(String originalResponseBody) {
        this.originalResponseBody = originalResponseBody;
    }

    public String getReplayResponseBody() {
        return replayResponseBody;
    }

    public void setReplayResponseBody(String replayResponseBody) {
        this.replayResponseBody = replayResponseBody;
    }

    public boolean isResponseBodyMatch() {
        return responseBodyMatch;
    }

    public void setResponseBodyMatch(boolean responseBodyMatch) {
        this.responseBodyMatch = responseBodyMatch;
    }

    public boolean isOverallMatch() {
        return overallMatch;
    }

    public void setOverallMatch(boolean overallMatch) {
        this.overallMatch = overallMatch;
    }
}
