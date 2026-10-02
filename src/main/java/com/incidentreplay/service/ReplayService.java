package com.incidentreplay.service;

import com.incidentreplay.model.CapturedRequest;
import com.incidentreplay.model.ComparisonResult;
import com.incidentreplay.model.Incident;
import com.incidentreplay.model.ReplayResult;
import com.incidentreplay.model.ReplaySession;
import com.incidentreplay.model.ReplaySessionStatus;
import com.incidentreplay.model.ReplayStatistics;
import com.incidentreplay.model.BatchReplaySummary;
import com.incidentreplay.model.BatchReplayResult;
import com.incidentreplay.model.SessionComparisonResult;
import com.incidentreplay.model.ReplaySessionSummary;
import com.incidentreplay.model.SessionComparisonDetail;
import com.incidentreplay.model.SessionComparisonDetailsResult;
import com.incidentreplay.repository.IncidentRepository;
import com.incidentreplay.repository.ReplaySessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import com.incidentreplay.config.ReplayProperties;

import java.time.Instant;
import com.incidentreplay.repository.ReplayResultRepository;
import java.util.List;
import java.util.Set;

@Service
public class ReplayService {

    private final IncidentRepository incidentRepository;
    private final ReplayResultRepository replayResultRepository;
    private final RestClient restClient;
    private final IncidentEventService incidentEventService;

    private final ReplaySessionRepository replaySessionRepository;
    private final ReplayProperties replayProperties;

    public ReplayService(IncidentRepository incidentRepository, ReplayResultRepository replayResultRepository, ReplaySessionRepository replaySessionRepository, RestClient.Builder restClientBuilder, IncidentEventService incidentEventService, ReplayProperties replayProperties) {
        this.incidentRepository = incidentRepository;
        this.replayResultRepository = replayResultRepository;
        this.replaySessionRepository = replaySessionRepository;
        this.incidentEventService = incidentEventService;
        this.replayProperties = replayProperties;

        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(replayProperties.getTimeoutMs());
        factory.setReadTimeout(replayProperties.getTimeoutMs());
        this.restClient = restClientBuilder.requestFactory(factory).build();
    }

    public ReplayResult replayRequest(String incidentId, String requestId) {
        return replayRequest(incidentId, requestId, null);
    }

    public ReplayResult replayRequest(String incidentId, String requestId, String sessionId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));

        CapturedRequest capturedRequest = incident.getRequests().stream()
                .filter(r -> r.getId().equals(requestId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        String capturedUrl = capturedRequest.getUrl();
        String targetUrl = replayProperties.getTargetBaseUrl() + capturedUrl;
        validateTargetUrl(targetUrl, capturedUrl);
        HttpMethod httpMethod;
        try {
            httpMethod = HttpMethod.valueOf(capturedRequest.getMethod().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported HTTP method: " + capturedRequest.getMethod());
        }

        HttpHeaders headers = new HttpHeaders();
        if (capturedRequest.getHeaders() != null) {
            Set<String> excludeHeaders = Set.of("host", "content-length", "connection");
            capturedRequest.getHeaders().forEach((k, v) -> {
                if (!excludeHeaders.contains(k.toLowerCase())) {
                    headers.add(k, v);
                }
            });
        }

        ReplayResult result = new ReplayResult();
        result.setIncidentId(incidentId);
        result.setRequestId(requestId);
        if (sessionId != null) {
            result.setSessionId(sessionId);
        }
        result.setTargetUrl(targetUrl);
        result.setReplayedAt(Instant.now());

        long startTime = 0;
        try {
            RestClient.RequestBodySpec requestSpec = restClient.method(httpMethod)
                    .uri(targetUrl)
                    .headers(h -> h.addAll(headers));

            if (capturedRequest.getBody() != null && !capturedRequest.getBody().isEmpty()) {
                requestSpec.body(capturedRequest.getBody());
            }

            startTime = System.currentTimeMillis();
            ResponseEntity<String> response = requestSpec.retrieve().toEntity(String.class);
            long endTime = System.currentTimeMillis();
            
            result.setExecutionTimeMs(endTime - startTime);
            result.setStatusCode(response.getStatusCode().value());
            result.setResponseBody(response.getBody());
            result = replayResultRepository.save(result);
            incidentEventService.createEvent(incidentId, com.incidentreplay.model.EventType.REQUEST_REPLAYED, "HTTP request was replayed");

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            long endTime = System.currentTimeMillis();
            if (startTime > 0) {
                result.setExecutionTimeMs(endTime - startTime);
            }
            result.setStatusCode(e.getStatusCode().value());
            result.setResponseBody(e.getResponseBodyAsString());
            result = replayResultRepository.save(result);
            incidentEventService.createEvent(incidentId, com.incidentreplay.model.EventType.REQUEST_REPLAYED, "HTTP request was replayed");
        } catch (org.springframework.web.client.ResourceAccessException e) {
            if (e.getCause() instanceof java.net.SocketTimeoutException) {
                throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Replay request timed out after " + replayProperties.getTimeoutMs() + " ms");
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Target endpoint cannot be reached: " + e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Target endpoint cannot be reached: " + e.getMessage());
        }

        return result;
    }

    private void validateTargetUrl(String targetUrl, String capturedUrl) {
        if (capturedUrl != null && (capturedUrl.startsWith("//") || capturedUrl.startsWith("http://") || capturedUrl.startsWith("https://") || capturedUrl.startsWith("@"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Replay target URL is not allowed");
        }

        try {
            java.net.URI configuredUri = new java.net.URI(replayProperties.getTargetBaseUrl());
            java.net.URI targetUri = new java.net.URI(targetUrl);

            if (!java.util.Objects.equals(configuredUri.getScheme(), targetUri.getScheme())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Replay target URL is not allowed");
            }
            if (!java.util.Objects.equals(configuredUri.getHost(), targetUri.getHost())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Replay target URL is not allowed");
            }

            int expectedPort = configuredUri.getPort();
            if (expectedPort == -1) {
                expectedPort = "https".equalsIgnoreCase(configuredUri.getScheme()) ? 443 : 80;
            }
            int actualPort = targetUri.getPort();
            if (actualPort == -1) {
                actualPort = "https".equalsIgnoreCase(targetUri.getScheme()) ? 443 : 80;
            }

            if (expectedPort != actualPort) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Replay target URL is not allowed");
            }

            if (targetUri.getUserInfo() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Replay target URL is not allowed");
            }

            if (!java.util.Objects.equals(configuredUri.getAuthority(), targetUri.getAuthority())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Replay target URL is not allowed");
            }
        } catch (java.net.URISyntaxException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Replay target URL is not allowed");
        }
    }

    private void checkTargetHealth() {
        String healthUrl = replayProperties.getTargetBaseUrl() + "/api/test-target/health";
        try {
            ResponseEntity<String> response = restClient.get()
                    .uri(healthUrl)
                    .retrieve()
                    .toEntity(String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Configured replay target is unavailable at " + replayProperties.getTargetBaseUrl());
            }
        } catch (Exception e) {
            if (e instanceof ResponseStatusException) {
                throw (ResponseStatusException) e;
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Configured replay target is unavailable at " + replayProperties.getTargetBaseUrl());
        }
    }


    public BatchReplaySummary replayAllRequests(String incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));

        checkTargetHealth();

        List<CapturedRequest> requests = incident.getRequests();
        int totalRequests = (requests != null) ? requests.size() : 0;

        ReplaySession session = new ReplaySession();
        session.setIncidentId(incidentId);
        session.setStatus(ReplaySessionStatus.RUNNING);
        session.setTotalRequests(totalRequests);
        session.setSuccessfulReplays(0);
        session.setFailedReplays(0);
        session.setStartedAt(Instant.now());
        session.setReplayTargetBaseUrl(replayProperties.getTargetBaseUrl());
        session = replaySessionRepository.save(session);

        BatchReplaySummary summary = new BatchReplaySummary();
        summary.setSessionId(session.getId());
        summary.setIncidentId(incidentId);
        summary.setStatus(session.getStatus().name());
        summary.setTotalRequests(totalRequests);
        summary.setSuccessfulReplays(0);
        summary.setFailedReplays(0);
        summary.setStartedAt(session.getStartedAt());
        summary.setReplayTargetBaseUrl(session.getReplayTargetBaseUrl());

        if (totalRequests == 0) {
            session.setStatus(ReplaySessionStatus.COMPLETED);
            session.setCompletedAt(Instant.now());
            session = replaySessionRepository.save(session);
            
            summary.setStatus(session.getStatus().name());
            summary.setCompletedAt(session.getCompletedAt());
            return summary;
        }

        try {
            // requests must be replayed sequentially, in capture order
            requests.sort(java.util.Comparator
                .comparing(CapturedRequest::getCapturedAt, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
                .thenComparing(CapturedRequest::getId, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));

            int successfulReplays = 0;
            int failedReplays = 0;

            for (CapturedRequest req : requests) {
                BatchReplayResult bResult = new BatchReplayResult();
                bResult.setRequestId(req.getId());
                try {
                    ReplayResult replayResult = replayRequest(incidentId, req.getId(), session.getId());
                    bResult.setStatus("SUCCESS");
                    bResult.setStatusCode(replayResult.getStatusCode());
                    successfulReplays++;
                } catch (Exception ex) {
                    bResult.setStatus("FAILED");
                    if (ex instanceof ResponseStatusException) {
                        bResult.setStatusCode(((ResponseStatusException) ex).getStatusCode().value());
                        bResult.setFailureReason(((ResponseStatusException) ex).getReason());
                    } else {
                        bResult.setStatusCode(500);
                        bResult.setFailureReason(ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
                    }
                    failedReplays++;
                }
                summary.getResults().add(bResult);
            }

            session.setSuccessfulReplays(successfulReplays);
            session.setFailedReplays(failedReplays);
            if (successfulReplays == totalRequests && failedReplays == 0) {
                session.setStatus(ReplaySessionStatus.COMPLETED);
            } else {
                session.setStatus(ReplaySessionStatus.COMPLETED_WITH_ERRORS);
            }
        } catch (Exception e) {
            session.setStatus(ReplaySessionStatus.COMPLETED_WITH_ERRORS);
            throw e;
        } finally {
            if (session.getStatus() == ReplaySessionStatus.RUNNING) {
                session.setStatus(ReplaySessionStatus.COMPLETED_WITH_ERRORS);
            }
            session.setCompletedAt(Instant.now());
            session = replaySessionRepository.save(session);

            summary.setSuccessfulReplays(session.getSuccessfulReplays());
            summary.setFailedReplays(session.getFailedReplays());
            summary.setStatus(session.getStatus().name());
            summary.setCompletedAt(session.getCompletedAt());
        }

        return summary;
    }

    public List<ReplaySession> getReplaySessions(String incidentId) {
        if (!incidentRepository.existsById(incidentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found");
        }
        return replaySessionRepository.findByIncidentIdOrderByStartedAtDesc(incidentId);
    }

    public ReplaySession getReplaySession(String incidentId, String sessionId) {
        if (!incidentRepository.existsById(incidentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found");
        }
        ReplaySession session = replaySessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Replay session not found"));
        if (!session.getIncidentId().equals(incidentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Replay session does not belong to this incident");
        }
        return session;
    }

    public List<ReplayResult> getReplaysForSession(String incidentId, String sessionId) {
        if (!incidentRepository.existsById(incidentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found");
        }
        ReplaySession session = replaySessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Replay session not found"));
        if (!session.getIncidentId().equals(incidentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Replay session does not belong to this incident");
        }
        return replayResultRepository.findBySessionIdOrderByReplayedAtAsc(sessionId);
    }

    public List<ReplayResult> getReplaysForIncident(String incidentId) {
        if (!incidentRepository.existsById(incidentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found");
        }
        return replayResultRepository.findByIncidentId(incidentId);
    }

    public List<ReplayResult> getReplaysForRequest(String incidentId, String requestId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));

        boolean requestExists = incident.getRequests().stream().anyMatch(r -> r.getId().equals(requestId));
        if (!requestExists) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found");
        }

        return replayResultRepository.findByIncidentIdAndRequestIdOrderByReplayedAtDesc(incidentId, requestId);
    }

    public ComparisonResult compareRequest(String incidentId, String requestId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));

        CapturedRequest capturedRequest = incident.getRequests().stream()
                .filter(r -> r.getId().equals(requestId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        if (capturedRequest.getOriginalStatusCode() == null || capturedRequest.getOriginalResponseBody() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Original response data is missing, cannot compare.");
        }

        ReplayResult replayResult = replayResultRepository.findFirstByIncidentIdAndRequestIdOrderByReplayedAtDesc(incidentId, requestId);
        if (replayResult == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No replay result found for this request");
        }

        ComparisonResult comparison = new ComparisonResult();
        comparison.setIncidentId(incidentId);
        comparison.setRequestId(requestId);
        
        comparison.setOriginalStatusCode(capturedRequest.getOriginalStatusCode());
        comparison.setReplayStatusCode(replayResult.getStatusCode());
        comparison.setExecutionTimeMs(replayResult.getExecutionTimeMs());
        
        comparison.setOriginalResponseBody(capturedRequest.getOriginalResponseBody());
        comparison.setReplayResponseBody(replayResult.getResponseBody());

        boolean statusCodeMatch = capturedRequest.getOriginalStatusCode().equals(replayResult.getStatusCode());
        
        boolean responseBodyMatch = false;
        if (capturedRequest.getOriginalResponseBody() != null && replayResult.getResponseBody() != null) {
            responseBodyMatch = capturedRequest.getOriginalResponseBody().equals(replayResult.getResponseBody());
        } else if (capturedRequest.getOriginalResponseBody() == null && replayResult.getResponseBody() == null) {
            responseBodyMatch = true;
        }

        comparison.setStatusCodeMatch(statusCodeMatch);
        comparison.setResponseBodyMatch(responseBodyMatch);
        comparison.setOverallMatch(statusCodeMatch && responseBodyMatch);
        
        return comparison;
    }

    public SessionComparisonResult compareSession(String incidentId, String sessionId) {
        if (!incidentRepository.existsById(incidentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found");
        }
        
        Incident incident = incidentRepository.findById(incidentId).get();

        ReplaySession session = replaySessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Replay session not found"));
        
        if (!session.getIncidentId().equals(incidentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Replay session does not belong to this incident");
        }

        List<ReplayResult> replayResults = replayResultRepository.findBySessionIdOrderByReplayedAtAsc(sessionId);

        SessionComparisonResult result = new SessionComparisonResult();
        result.setSessionId(sessionId);
        result.setIncidentId(incidentId);
        result.setTotalRequests(replayResults.size());

        for (ReplayResult replayResult : replayResults) {
            CapturedRequest request = incident.getRequests().stream()
                    .filter(r -> r.getId().equals(replayResult.getRequestId()))
                    .findFirst()
                    .orElse(null);

            if (request == null || request.getOriginalStatusCode() == null || request.getOriginalResponseBody() == null) {
                continue;
            }

            result.setComparableRequests(result.getComparableRequests() + 1);

            boolean statusMatch = request.getOriginalStatusCode().equals(replayResult.getStatusCode());
            boolean bodyMatch = false;
            
            if (request.getOriginalResponseBody() != null && replayResult.getResponseBody() != null) {
                bodyMatch = request.getOriginalResponseBody().equals(replayResult.getResponseBody());
            } else if (request.getOriginalResponseBody() == null && replayResult.getResponseBody() == null) {
                bodyMatch = true;
            }

            if (statusMatch) {
                result.setStatusCodeMatches(result.getStatusCodeMatches() + 1);
            } else {
                result.setStatusCodeMismatches(result.getStatusCodeMismatches() + 1);
            }

            if (bodyMatch) {
                result.setResponseBodyMatches(result.getResponseBodyMatches() + 1);
            } else {
                result.setResponseBodyMismatches(result.getResponseBodyMismatches() + 1);
            }

            if (statusMatch && bodyMatch) {
                result.setOverallMatches(result.getOverallMatches() + 1);
            } else {
                result.setOverallMismatches(result.getOverallMismatches() + 1);
            }
        }

        return result;
    }

    public ReplaySessionSummary getReplaySessionSummary(String incidentId, String sessionId) {
        ReplaySession session = replaySessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Replay session not found"));
        
        if (!session.getIncidentId().equals(incidentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Replay session does not belong to this incident");
        }

        SessionComparisonResult comparison = compareSession(incidentId, sessionId);

        ReplaySessionSummary summary = new ReplaySessionSummary();
        summary.setSessionId(session.getId());
        summary.setIncidentId(session.getIncidentId());
        summary.setStatus(session.getStatus());
        summary.setTotalRequests(session.getTotalRequests());
        summary.setSuccessfulReplays(session.getSuccessfulReplays());
        summary.setFailedReplays(session.getFailedReplays());
        summary.setStartedAt(session.getStartedAt());
        summary.setCompletedAt(session.getCompletedAt());
        summary.setReplayTargetBaseUrl(session.getReplayTargetBaseUrl());

        summary.setComparableRequests(comparison.getComparableRequests());
        summary.setStatusCodeMatches(comparison.getStatusCodeMatches());
        summary.setStatusCodeMismatches(comparison.getStatusCodeMismatches());
        summary.setResponseBodyMatches(comparison.getResponseBodyMatches());
        summary.setResponseBodyMismatches(comparison.getResponseBodyMismatches());
        summary.setOverallMatches(comparison.getOverallMatches());
        summary.setOverallMismatches(comparison.getOverallMismatches());

        return summary;
    }

    public SessionComparisonDetailsResult getReplaySessionComparisonDetails(String incidentId, String sessionId) {
        ReplaySession session = replaySessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Replay session not found"));
        
        if (!session.getIncidentId().equals(incidentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Replay session does not belong to this incident");
        }

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));

        List<ReplayResult> replayResults = replayResultRepository.findBySessionIdOrderByReplayedAtAsc(sessionId);

        SessionComparisonDetailsResult result = new SessionComparisonDetailsResult();
        result.setSessionId(sessionId);
        result.setIncidentId(incidentId);
        result.setTotalRequests(replayResults.size());
        
        List<SessionComparisonDetail> details = new java.util.ArrayList<>();

        for (ReplayResult replayResult : replayResults) {
            CapturedRequest request = incident.getRequests().stream()
                    .filter(r -> r.getId().equals(replayResult.getRequestId()))
                    .findFirst()
                    .orElse(null);

            SessionComparisonDetail detail = new SessionComparisonDetail();
            detail.setRequestId(replayResult.getRequestId());
            detail.setReplayStatusCode(replayResult.getStatusCode());
            detail.setReplayResponseBody(replayResult.getResponseBody());
            detail.setExecutionTimeMs(replayResult.getExecutionTimeMs());

            if (request != null) {
                detail.setOriginalStatusCode(request.getOriginalStatusCode());
                detail.setOriginalResponseBody(request.getOriginalResponseBody());
                
                boolean comparable = request.getOriginalStatusCode() != null && request.getOriginalResponseBody() != null;
                detail.setComparable(comparable);

                if (comparable) {
                    boolean statusMatch = request.getOriginalStatusCode().equals(replayResult.getStatusCode());
                    boolean bodyMatch = false;
                    if (request.getOriginalResponseBody() != null && replayResult.getResponseBody() != null) {
                        bodyMatch = request.getOriginalResponseBody().equals(replayResult.getResponseBody());
                    } else if (request.getOriginalResponseBody() == null && replayResult.getResponseBody() == null) {
                        bodyMatch = true;
                    }
                    
                    detail.setStatusCodeMatch(statusMatch);
                    detail.setResponseBodyMatch(bodyMatch);
                    detail.setOverallMatch(statusMatch && bodyMatch);
                } else {
                    detail.setStatusCodeMatch(false);
                    detail.setResponseBodyMatch(false);
                    detail.setOverallMatch(false);
                }
            } else {
                detail.setComparable(false);
                detail.setStatusCodeMatch(false);
                detail.setResponseBodyMatch(false);
                detail.setOverallMatch(false);
            }
            
            details.add(detail);
        }
        
        result.setDetails(details);
        return result;
    }

    public ReplayStatistics getReplayStats(String incidentId, String requestId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));

        CapturedRequest capturedRequest = incident.getRequests().stream()
                .filter(r -> r.getId().equals(requestId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        List<ReplayResult> replays = replayResultRepository.findByIncidentIdAndRequestIdOrderByReplayedAtDesc(incidentId, requestId);

        ReplayStatistics stats = new ReplayStatistics();
        stats.setIncidentId(incidentId);
        stats.setRequestId(requestId);
        stats.setTotalReplays(replays.size());
        stats.setSuccessfulReplays(replays.size());

        Integer origStatus = capturedRequest.getOriginalStatusCode();
        String origBody = capturedRequest.getOriginalResponseBody();
        boolean hasOriginalResponse = origStatus != null && origBody != null;

        int statusCodeMatches = 0;
        int statusCodeMismatches = 0;
        int responseBodyMatches = 0;
        int responseBodyMismatches = 0;
        int overallMatches = 0;
        int overallMismatches = 0;
        Instant latestReplayAt = null;

        for (ReplayResult replay : replays) {
            if (latestReplayAt == null || replay.getReplayedAt().isAfter(latestReplayAt)) {
                latestReplayAt = replay.getReplayedAt();
            }

            if (hasOriginalResponse) {
                boolean statusMatch = origStatus.equals(replay.getStatusCode());
                boolean bodyMatch = origBody.equals(replay.getResponseBody());

                if (statusMatch) statusCodeMatches++;
                else statusCodeMismatches++;

                if (bodyMatch) responseBodyMatches++;
                else responseBodyMismatches++;

                if (statusMatch && bodyMatch) overallMatches++;
                else overallMismatches++;
            }
        }

        stats.setStatusCodeMatches(statusCodeMatches);
        stats.setStatusCodeMismatches(statusCodeMismatches);
        stats.setResponseBodyMatches(responseBodyMatches);
        stats.setResponseBodyMismatches(responseBodyMismatches);
        stats.setOverallMatches(overallMatches);
        stats.setOverallMismatches(overallMismatches);
        stats.setLatestReplayAt(latestReplayAt);

        return stats;
    }
}
