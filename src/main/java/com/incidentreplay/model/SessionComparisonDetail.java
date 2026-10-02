package com.incidentreplay.model;

public class SessionComparisonDetail {
    private String requestId;
    private Integer replayStatusCode;
    private Integer originalStatusCode;
    private boolean statusCodeMatch;
    private String replayResponseBody;
    private String originalResponseBody;
    private boolean responseBodyMatch;
    private boolean overallMatch;
    private boolean comparable;
    private Long executionTimeMs;

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public Integer getReplayStatusCode() { return replayStatusCode; }
    public void setReplayStatusCode(Integer replayStatusCode) { this.replayStatusCode = replayStatusCode; }

    public Integer getOriginalStatusCode() { return originalStatusCode; }
    public void setOriginalStatusCode(Integer originalStatusCode) { this.originalStatusCode = originalStatusCode; }

    public boolean isStatusCodeMatch() { return statusCodeMatch; }
    public void setStatusCodeMatch(boolean statusCodeMatch) { this.statusCodeMatch = statusCodeMatch; }

    public String getReplayResponseBody() { return replayResponseBody; }
    public void setReplayResponseBody(String replayResponseBody) { this.replayResponseBody = replayResponseBody; }

    public String getOriginalResponseBody() { return originalResponseBody; }
    public void setOriginalResponseBody(String originalResponseBody) { this.originalResponseBody = originalResponseBody; }

    public boolean isResponseBodyMatch() { return responseBodyMatch; }
    public void setResponseBodyMatch(boolean responseBodyMatch) { this.responseBodyMatch = responseBodyMatch; }

    public boolean isOverallMatch() { return overallMatch; }
    public void setOverallMatch(boolean overallMatch) { this.overallMatch = overallMatch; }

    public boolean isComparable() { return comparable; }
    public void setComparable(boolean comparable) { this.comparable = comparable; }

    public Long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(Long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
}
