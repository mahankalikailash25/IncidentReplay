package com.incidentreplay.model;

import java.util.List;
import java.util.ArrayList;
import java.time.Instant;

public class BatchReplaySummary {
    private String sessionId;
    private String incidentId;
    private String status;
    private int totalRequests;
    private int successfulReplays;
    private int failedReplays;
    private Instant startedAt;
    private Instant completedAt;
    private String replayTargetBaseUrl;
    private List<BatchReplayResult> results = new ArrayList<>();

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(int totalRequests) {
        this.totalRequests = totalRequests;
    }

    public int getSuccessfulReplays() {
        return successfulReplays;
    }

    public void setSuccessfulReplays(int successfulReplays) {
        this.successfulReplays = successfulReplays;
    }

    public int getFailedReplays() {
        return failedReplays;
    }

    public void setFailedReplays(int failedReplays) {
        this.failedReplays = failedReplays;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public List<BatchReplayResult> getResults() {
        return results;
    }

    public void setResults(List<BatchReplayResult> results) {
        this.results = results;
    }

    public String getReplayTargetBaseUrl() {
        return replayTargetBaseUrl;
    }

    public void setReplayTargetBaseUrl(String replayTargetBaseUrl) {
        this.replayTargetBaseUrl = replayTargetBaseUrl;
    }
}
