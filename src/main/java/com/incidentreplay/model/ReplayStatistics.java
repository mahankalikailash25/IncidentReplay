package com.incidentreplay.model;

import java.time.Instant;

public class ReplayStatistics {
    private String incidentId;
    private String requestId;
    private int totalReplays;
    private int successfulReplays;
    private int statusCodeMatches;
    private int statusCodeMismatches;
    private int responseBodyMatches;
    private int responseBodyMismatches;
    private int overallMatches;
    private int overallMismatches;
    private Instant latestReplayAt;

    public ReplayStatistics() {}

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

    public int getTotalReplays() {
        return totalReplays;
    }

    public void setTotalReplays(int totalReplays) {
        this.totalReplays = totalReplays;
    }

    public int getSuccessfulReplays() {
        return successfulReplays;
    }

    public void setSuccessfulReplays(int successfulReplays) {
        this.successfulReplays = successfulReplays;
    }

    public int getStatusCodeMatches() {
        return statusCodeMatches;
    }

    public void setStatusCodeMatches(int statusCodeMatches) {
        this.statusCodeMatches = statusCodeMatches;
    }

    public int getStatusCodeMismatches() {
        return statusCodeMismatches;
    }

    public void setStatusCodeMismatches(int statusCodeMismatches) {
        this.statusCodeMismatches = statusCodeMismatches;
    }

    public int getResponseBodyMatches() {
        return responseBodyMatches;
    }

    public void setResponseBodyMatches(int responseBodyMatches) {
        this.responseBodyMatches = responseBodyMatches;
    }

    public int getResponseBodyMismatches() {
        return responseBodyMismatches;
    }

    public void setResponseBodyMismatches(int responseBodyMismatches) {
        this.responseBodyMismatches = responseBodyMismatches;
    }

    public int getOverallMatches() {
        return overallMatches;
    }

    public void setOverallMatches(int overallMatches) {
        this.overallMatches = overallMatches;
    }

    public int getOverallMismatches() {
        return overallMismatches;
    }

    public void setOverallMismatches(int overallMismatches) {
        this.overallMismatches = overallMismatches;
    }

    public Instant getLatestReplayAt() {
        return latestReplayAt;
    }

    public void setLatestReplayAt(Instant latestReplayAt) {
        this.latestReplayAt = latestReplayAt;
    }
}
