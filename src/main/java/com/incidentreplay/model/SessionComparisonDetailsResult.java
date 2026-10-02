package com.incidentreplay.model;

import java.util.List;

public class SessionComparisonDetailsResult {
    private String sessionId;
    private String incidentId;
    private int totalRequests;
    private List<SessionComparisonDetail> details;

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public int getTotalRequests() { return totalRequests; }
    public void setTotalRequests(int totalRequests) { this.totalRequests = totalRequests; }

    public List<SessionComparisonDetail> getDetails() { return details; }
    public void setDetails(List<SessionComparisonDetail> details) { this.details = details; }
}
