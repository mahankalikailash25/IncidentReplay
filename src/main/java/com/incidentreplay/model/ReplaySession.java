package com.incidentreplay.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "replay_sessions")
public class ReplaySession {
    
    @Id
    private String id;
    
    private String incidentId;
    private ReplaySessionStatus status;
    private int totalRequests;
    private int successfulReplays;
    private int failedReplays;
    private Instant startedAt;
    private Instant completedAt;
    private String replayTargetBaseUrl;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }

    public ReplaySessionStatus getStatus() {
        return status;
    }

    public void setStatus(ReplaySessionStatus status) {
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

    public String getReplayTargetBaseUrl() {
        return replayTargetBaseUrl;
    }

    public void setReplayTargetBaseUrl(String replayTargetBaseUrl) {
        this.replayTargetBaseUrl = replayTargetBaseUrl;
    }
}
