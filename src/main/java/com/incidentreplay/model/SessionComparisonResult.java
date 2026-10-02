package com.incidentreplay.model;

public class SessionComparisonResult {
    private String sessionId;
    private String incidentId;
    private int totalRequests;
    private int comparableRequests;
    private int statusCodeMatches;
    private int statusCodeMismatches;
    private int responseBodyMatches;
    private int responseBodyMismatches;
    private int overallMatches;
    private int overallMismatches;

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

    public int getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(int totalRequests) {
        this.totalRequests = totalRequests;
    }

    public int getComparableRequests() {
        return comparableRequests;
    }

    public void setComparableRequests(int comparableRequests) {
        this.comparableRequests = comparableRequests;
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
}
