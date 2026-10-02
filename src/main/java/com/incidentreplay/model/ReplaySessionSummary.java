package com.incidentreplay.model;

import java.time.Instant;

public class ReplaySessionSummary {
    private String sessionId;
    private String incidentId;
    private ReplaySessionStatus status;
    private int totalRequests;
    private int successfulReplays;
    private int failedReplays;
    private Instant startedAt;
    private Instant completedAt;
    private String replayTargetBaseUrl;
    
    private int comparableRequests;
    private int statusCodeMatches;
    private int statusCodeMismatches;
    private int responseBodyMatches;
    private int responseBodyMismatches;
    private int overallMatches;
    private int overallMismatches;

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public ReplaySessionStatus getStatus() { return status; }
    public void setStatus(ReplaySessionStatus status) { this.status = status; }

    public int getTotalRequests() { return totalRequests; }
    public void setTotalRequests(int totalRequests) { this.totalRequests = totalRequests; }

    public int getSuccessfulReplays() { return successfulReplays; }
    public void setSuccessfulReplays(int successfulReplays) { this.successfulReplays = successfulReplays; }

    public int getFailedReplays() { return failedReplays; }
    public void setFailedReplays(int failedReplays) { this.failedReplays = failedReplays; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public String getReplayTargetBaseUrl() { return replayTargetBaseUrl; }
    public void setReplayTargetBaseUrl(String replayTargetBaseUrl) { this.replayTargetBaseUrl = replayTargetBaseUrl; }

    public int getComparableRequests() { return comparableRequests; }
    public void setComparableRequests(int comparableRequests) { this.comparableRequests = comparableRequests; }

    public int getStatusCodeMatches() { return statusCodeMatches; }
    public void setStatusCodeMatches(int statusCodeMatches) { this.statusCodeMatches = statusCodeMatches; }

    public int getStatusCodeMismatches() { return statusCodeMismatches; }
    public void setStatusCodeMismatches(int statusCodeMismatches) { this.statusCodeMismatches = statusCodeMismatches; }

    public int getResponseBodyMatches() { return responseBodyMatches; }
    public void setResponseBodyMatches(int responseBodyMatches) { this.responseBodyMatches = responseBodyMatches; }

    public int getResponseBodyMismatches() { return responseBodyMismatches; }
    public void setResponseBodyMismatches(int responseBodyMismatches) { this.responseBodyMismatches = responseBodyMismatches; }

    public int getOverallMatches() { return overallMatches; }
    public void setOverallMatches(int overallMatches) { this.overallMatches = overallMatches; }

    public int getOverallMismatches() { return overallMismatches; }
    public void setOverallMismatches(int overallMismatches) { this.overallMismatches = overallMismatches; }
}
