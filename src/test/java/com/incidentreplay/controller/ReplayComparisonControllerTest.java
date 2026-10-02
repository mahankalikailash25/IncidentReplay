package com.incidentreplay.controller;

import com.incidentreplay.model.CapturedRequest;
import com.incidentreplay.model.ComparisonResult;
import com.incidentreplay.model.Incident;
import com.incidentreplay.model.ReplayResult;
import com.incidentreplay.model.ReplaySession;
import com.incidentreplay.model.SessionComparisonResult;
import com.incidentreplay.model.ReplaySessionSummary;
import com.incidentreplay.model.SessionComparisonDetail;
import com.incidentreplay.model.SessionComparisonDetailsResult;
import com.incidentreplay.repository.IncidentRepository;
import com.incidentreplay.repository.ReplayResultRepository;
import com.incidentreplay.repository.ReplaySessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ReplayComparisonControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private ReplayResultRepository replayResultRepository;

    @Autowired
    private ReplaySessionRepository replaySessionRepository;

    @BeforeEach
    void setUp() {
        incidentRepository.deleteAll();
        replayResultRepository.deleteAll();
        replaySessionRepository.deleteAll();
    }

    private Incident createIncidentWithRequest(Integer origStatus, String origBody) {
        Incident incident = new Incident();
        incident.setTitle("Test incident for comparison");
        incident = incidentRepository.save(incident);

        CapturedRequest req = new CapturedRequest();
        req.setId(UUID.randomUUID().toString());
        req.setMethod("GET");
        req.setUrl("/test");
        req.setOriginalStatusCode(origStatus);
        req.setOriginalResponseBody(origBody);
        
        if (incident.getRequests() == null) {
            incident.setRequests(new ArrayList<>());
        }
        incident.getRequests().add(req);
        return incidentRepository.save(incident);
    }

    private void createReplayResult(String incidentId, String requestId, int status, String body, Instant time, Long executionTimeMs) {
        ReplayResult result = new ReplayResult();
        result.setIncidentId(incidentId);
        result.setRequestId(requestId);
        result.setStatusCode(status);
        result.setResponseBody(body);
        result.setReplayedAt(time);
        result.setExecutionTimeMs(executionTimeMs);
        replayResultRepository.save(result);
    }

    @Test
    void testComparisonIdenticalMatch() {
        Incident incident = createIncidentWithRequest(200, "{\"success\":true}");
        String reqId = incident.getRequests().get(0).getId();
        createReplayResult(incident.getId(), reqId, 200, "{\"success\":true}", Instant.now(), 125L);

        ResponseEntity<ComparisonResult> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/requests/" + reqId + "/compare", ComparisonResult.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ComparisonResult comp = response.getBody();
        assertThat(comp.isStatusCodeMatch()).isTrue();
        assertThat(comp.isResponseBodyMatch()).isTrue();
        assertThat(comp.isOverallMatch()).isTrue();
        assertThat(comp.getExecutionTimeMs()).isEqualTo(125L);
    }

    @Test
    void testComparisonStatusDiffers() {
        Incident incident = createIncidentWithRequest(200, "{\"success\":true}");
        String reqId = incident.getRequests().get(0).getId();
        createReplayResult(incident.getId(), reqId, 201, "{\"success\":true}", Instant.now(), 55L);

        ResponseEntity<ComparisonResult> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/requests/" + reqId + "/compare", ComparisonResult.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ComparisonResult comp = response.getBody();
        assertThat(comp.isStatusCodeMatch()).isFalse();
        assertThat(comp.isResponseBodyMatch()).isTrue();
        assertThat(comp.isOverallMatch()).isFalse();
        assertThat(comp.getExecutionTimeMs()).isEqualTo(55L);
    }

    @Test
    void testComparisonBodyDiffers() {
        Incident incident = createIncidentWithRequest(200, "{\"success\":true}");
        String reqId = incident.getRequests().get(0).getId();
        createReplayResult(incident.getId(), reqId, 200, "{\"success\":false}", Instant.now(), 200L);

        ResponseEntity<ComparisonResult> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/requests/" + reqId + "/compare", ComparisonResult.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ComparisonResult comp = response.getBody();
        assertThat(comp.isStatusCodeMatch()).isTrue();
        assertThat(comp.isResponseBodyMatch()).isFalse();
        assertThat(comp.isOverallMatch()).isFalse();
        assertThat(comp.getExecutionTimeMs()).isEqualTo(200L);
    }

    @Test
    void testComparisonBothDiffer() {
        Incident incident = createIncidentWithRequest(200, "{\"success\":true}");
        String reqId = incident.getRequests().get(0).getId();
        createReplayResult(incident.getId(), reqId, 500, "{\"error\":\"fail\"}", Instant.now(), 500L);

        ResponseEntity<ComparisonResult> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/requests/" + reqId + "/compare", ComparisonResult.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ComparisonResult comp = response.getBody();
        assertThat(comp.isStatusCodeMatch()).isFalse();
        assertThat(comp.isResponseBodyMatch()).isFalse();
        assertThat(comp.isOverallMatch()).isFalse();
        assertThat(comp.getExecutionTimeMs()).isEqualTo(500L);
    }

    @Test
    void testComparisonOriginalMissing() {
        Incident incident = createIncidentWithRequest(null, null);
        String reqId = incident.getRequests().get(0).getId();
        createReplayResult(incident.getId(), reqId, 200, "{}", Instant.now(), 10L);

        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/requests/" + reqId + "/compare", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void testComparisonReplayMissing() {
        Incident incident = createIncidentWithRequest(200, "{}");
        String reqId = incident.getRequests().get(0).getId();

        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/requests/" + reqId + "/compare", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testComparisonInvalidIncident() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/invalid-incident/requests/req-id/compare", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testComparisonInvalidRequest() {
        Incident incident = createIncidentWithRequest(200, "{}");
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/requests/invalid-req/compare", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testComparisonMultipleReplaysUsesLatest() {
        Incident incident = createIncidentWithRequest(200, "{}");
        String reqId = incident.getRequests().get(0).getId();
        
        // older replay that matches
        createReplayResult(incident.getId(), reqId, 200, "{}", Instant.now().minus(10, ChronoUnit.MINUTES), 100L);
        // newer replay that doesn't match
        createReplayResult(incident.getId(), reqId, 404, "{\"error\": \"not_found\"}", Instant.now(), 150L);

        ResponseEntity<ComparisonResult> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/requests/" + reqId + "/compare", ComparisonResult.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ComparisonResult comp = response.getBody();
        assertThat(comp.isStatusCodeMatch()).isFalse();
        assertThat(comp.isOverallMatch()).isFalse();
        assertThat(comp.getExecutionTimeMs()).isEqualTo(150L);
    }

    @Test
    void testComparisonOldReplayWithoutExecutionTimeMs() {
        Incident incident = createIncidentWithRequest(200, "{}");
        String reqId = incident.getRequests().get(0).getId();
        createReplayResult(incident.getId(), reqId, 200, "{}", Instant.now(), null); // Simulate old record

        ResponseEntity<ComparisonResult> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/requests/" + reqId + "/compare", ComparisonResult.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ComparisonResult comp = response.getBody();
        assertThat(comp.isStatusCodeMatch()).isTrue();
        assertThat(comp.isOverallMatch()).isTrue();
        assertThat(comp.getExecutionTimeMs()).isNull();
    }

    @Test
    void testSessionComparison() {
        Incident incident = new Incident();
        incident.setTitle("Session comparison test");
        incident.setRequests(new ArrayList<>());
        incident = incidentRepository.save(incident);

        CapturedRequest r1 = createRequest(incident, 200, "OK");
        CapturedRequest r2 = createRequest(incident, 200, "OK");
        CapturedRequest r3 = createRequest(incident, 200, "OK");
        CapturedRequest r4 = createRequest(incident, 200, "OK");
        CapturedRequest r5 = createRequest(incident, 200, null);
        
        incidentRepository.save(incident);

        ReplaySession session = new ReplaySession();
        session.setIncidentId(incident.getId());
        session = replaySessionRepository.save(session);
        String sessionId = session.getId();

        createReplayResult(incident.getId(), r1.getId(), sessionId, 200, "OK"); // Match
        createReplayResult(incident.getId(), r2.getId(), sessionId, 500, "OK"); // Status mismatch
        createReplayResult(incident.getId(), r3.getId(), sessionId, 200, "FAIL"); // Body mismatch
        createReplayResult(incident.getId(), r4.getId(), sessionId, 404, "NOT FOUND"); // Both mismatch
        createReplayResult(incident.getId(), r5.getId(), sessionId, 200, "OK"); // Non-comparable

        ResponseEntity<SessionComparisonResult> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/replay-sessions/" + sessionId + "/compare",
                SessionComparisonResult.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        SessionComparisonResult result = response.getBody();
        assertThat(result).isNotNull();
        assertThat(result.getTotalRequests()).isEqualTo(5);
        assertThat(result.getComparableRequests()).isEqualTo(4);
        
        assertThat(result.getStatusCodeMatches()).isEqualTo(2); // r1, r3
        assertThat(result.getStatusCodeMismatches()).isEqualTo(2); // r2, r4
        
        assertThat(result.getResponseBodyMatches()).isEqualTo(2); // r1, r2
        assertThat(result.getResponseBodyMismatches()).isEqualTo(2); // r3, r4
        
        assertThat(result.getOverallMatches()).isEqualTo(1); // r1
        assertThat(result.getOverallMismatches()).isEqualTo(3); // r2, r3, r4
    }

    @Test
    void testSessionComparisonInvalidSession() {
        Incident incident = createIncidentWithRequest(200, "OK");
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/replay-sessions/invalid-id/compare",
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testSessionSummary() {
        Incident incident = new Incident();
        incident.setTitle("Session summary test");
        incident.setRequests(new ArrayList<>());
        incident = incidentRepository.save(incident);

        CapturedRequest r1 = createRequest(incident, 200, "OK");
        CapturedRequest r2 = createRequest(incident, 200, "OK");
        CapturedRequest r3 = createRequest(incident, 200, "OK");
        CapturedRequest r4 = createRequest(incident, 200, "OK");
        CapturedRequest r5 = createRequest(incident, 200, null);
        
        incidentRepository.save(incident);

        ReplaySession session = new ReplaySession();
        session.setIncidentId(incident.getId());
        session.setStatus(com.incidentreplay.model.ReplaySessionStatus.COMPLETED);
        session.setTotalRequests(6);
        session.setSuccessfulReplays(5);
        session.setFailedReplays(1);
        session.setStartedAt(Instant.now().minusSeconds(60));
        session.setCompletedAt(Instant.now());
        session.setReplayTargetBaseUrl("http://localhost:8081");
        session = replaySessionRepository.save(session);
        String sessionId = session.getId();

        createReplayResult(incident.getId(), r1.getId(), sessionId, 200, "OK"); // Match
        createReplayResult(incident.getId(), r2.getId(), sessionId, 500, "OK"); // Status mismatch
        createReplayResult(incident.getId(), r3.getId(), sessionId, 200, "FAIL"); // Body mismatch
        createReplayResult(incident.getId(), r4.getId(), sessionId, 404, "NOT FOUND"); // Both mismatch
        createReplayResult(incident.getId(), r5.getId(), sessionId, 200, "OK"); // Non-comparable

        ResponseEntity<ReplaySessionSummary> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/replay-sessions/" + sessionId + "/summary",
                ReplaySessionSummary.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ReplaySessionSummary result = response.getBody();
        assertThat(result).isNotNull();
        
        // Check session data
        assertThat(result.getSessionId()).isEqualTo(sessionId);
        assertThat(result.getIncidentId()).isEqualTo(incident.getId());
        assertThat(result.getStatus()).isEqualTo(com.incidentreplay.model.ReplaySessionStatus.COMPLETED);
        assertThat(result.getTotalRequests()).isEqualTo(6);
        assertThat(result.getSuccessfulReplays()).isEqualTo(5);
        assertThat(result.getFailedReplays()).isEqualTo(1);
        assertThat(result.getReplayTargetBaseUrl()).isEqualTo("http://localhost:8081");
        
        // Check comparison data
        assertThat(result.getComparableRequests()).isEqualTo(4);
        assertThat(result.getStatusCodeMatches()).isEqualTo(2); // r1, r3
        assertThat(result.getStatusCodeMismatches()).isEqualTo(2); // r2, r4
        assertThat(result.getResponseBodyMatches()).isEqualTo(2); // r1, r2
        assertThat(result.getResponseBodyMismatches()).isEqualTo(2); // r3, r4
        assertThat(result.getOverallMatches()).isEqualTo(1); // r1
        assertThat(result.getOverallMismatches()).isEqualTo(3); // r2, r3, r4
    }

    @Test
    void testSessionSummaryNoReplays() {
        Incident incident = new Incident();
        incident.setTitle("Session summary no replays test");
        incident = incidentRepository.save(incident);

        ReplaySession session = new ReplaySession();
        session.setIncidentId(incident.getId());
        session.setStatus(com.incidentreplay.model.ReplaySessionStatus.COMPLETED_WITH_ERRORS);
        session = replaySessionRepository.save(session);

        ResponseEntity<ReplaySessionSummary> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/replay-sessions/" + session.getId() + "/summary",
                ReplaySessionSummary.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ReplaySessionSummary result = response.getBody();
        assertThat(result).isNotNull();
        assertThat(result.getTotalRequests()).isEqualTo(0);
        assertThat(result.getComparableRequests()).isEqualTo(0);
        assertThat(result.getOverallMatches()).isEqualTo(0);
    }

    @Test
    void testSessionComparisonDetails() {
        Incident incident = new Incident();
        incident.setTitle("Session comparison details test");
        incident.setRequests(new ArrayList<>());
        incident = incidentRepository.save(incident);

        CapturedRequest r1 = createRequest(incident, 200, "OK"); // Match
        CapturedRequest r2 = createRequest(incident, 200, "OK"); // Status mismatch
        CapturedRequest r3 = createRequest(incident, 200, "OK"); // Body mismatch
        CapturedRequest r4 = createRequest(incident, 200, "OK"); // Both mismatch
        CapturedRequest r5 = createRequest(incident, 200, null); // Non-comparable
        CapturedRequest r6 = createRequest(incident, 200, "OK"); // No replay
        
        incidentRepository.save(incident);

        ReplaySession session = new ReplaySession();
        session.setIncidentId(incident.getId());
        session = replaySessionRepository.save(session);
        String sessionId = session.getId();

        createReplayResult(incident.getId(), r1.getId(), sessionId, 200, "OK", 100L);
        createReplayResult(incident.getId(), r2.getId(), sessionId, 500, "OK", 110L);
        createReplayResult(incident.getId(), r3.getId(), sessionId, 200, "FAIL", 120L);
        createReplayResult(incident.getId(), r4.getId(), sessionId, 404, "NOT FOUND", 130L);
        createReplayResult(incident.getId(), r5.getId(), sessionId, 200, "OK", 140L);

        ResponseEntity<SessionComparisonDetailsResult> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/replay-sessions/" + sessionId + "/comparison-details",
                SessionComparisonDetailsResult.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        SessionComparisonDetailsResult result = response.getBody();
        assertThat(result).isNotNull();
        assertThat(result.getSessionId()).isEqualTo(sessionId);
        assertThat(result.getIncidentId()).isEqualTo(incident.getId());
        assertThat(result.getTotalRequests()).isEqualTo(5); // 5 replays
        assertThat(result.getDetails()).hasSize(5);

        // Check r1
        SessionComparisonDetail d1 = result.getDetails().stream().filter(d -> d.getRequestId().equals(r1.getId())).findFirst().get();
        assertThat(d1.isComparable()).isTrue();
        assertThat(d1.isStatusCodeMatch()).isTrue();
        assertThat(d1.isResponseBodyMatch()).isTrue();
        assertThat(d1.isOverallMatch()).isTrue();
        assertThat(d1.getExecutionTimeMs()).isEqualTo(100L);

        // Check r2
        SessionComparisonDetail d2 = result.getDetails().stream().filter(d -> d.getRequestId().equals(r2.getId())).findFirst().get();
        assertThat(d2.isStatusCodeMatch()).isFalse();
        assertThat(d2.isResponseBodyMatch()).isTrue();
        assertThat(d2.isOverallMatch()).isFalse();

        // Check r3
        SessionComparisonDetail d3 = result.getDetails().stream().filter(d -> d.getRequestId().equals(r3.getId())).findFirst().get();
        assertThat(d3.isStatusCodeMatch()).isTrue();
        assertThat(d3.isResponseBodyMatch()).isFalse();
        assertThat(d3.isOverallMatch()).isFalse();

        // Check r4
        SessionComparisonDetail d4 = result.getDetails().stream().filter(d -> d.getRequestId().equals(r4.getId())).findFirst().get();
        assertThat(d4.isStatusCodeMatch()).isFalse();
        assertThat(d4.isResponseBodyMatch()).isFalse();
        assertThat(d4.isOverallMatch()).isFalse();

        // Check r5
        SessionComparisonDetail d5 = result.getDetails().stream().filter(d -> d.getRequestId().equals(r5.getId())).findFirst().get();
        assertThat(d5.isComparable()).isFalse();
        assertThat(d5.isStatusCodeMatch()).isFalse();
        assertThat(d5.isOverallMatch()).isFalse();

        // r6 should not be present
        boolean hasR6 = result.getDetails().stream().anyMatch(d -> d.getRequestId().equals(r6.getId()));
        assertThat(hasR6).isFalse();
    }

    private CapturedRequest createRequest(Incident incident, Integer status, String body) {
        CapturedRequest req = new CapturedRequest();
        req.setId(UUID.randomUUID().toString());
        req.setOriginalStatusCode(status);
        req.setOriginalResponseBody(body);
        incident.getRequests().add(req);
        return req;
    }

    private ReplayResult createReplayResult(String incidentId, String requestId, String sessionId, int status, String body) {
        return createReplayResult(incidentId, requestId, sessionId, status, body, null);
    }

    private ReplayResult createReplayResult(String incidentId, String requestId, String sessionId, int status, String body, Long executionTimeMs) {
        ReplayResult rr = new ReplayResult();
        rr.setIncidentId(incidentId);
        rr.setRequestId(requestId);
        rr.setSessionId(sessionId);
        rr.setStatusCode(status);
        rr.setResponseBody(body);
        rr.setExecutionTimeMs(executionTimeMs);
        rr.setReplayedAt(Instant.now());
        return replayResultRepository.save(rr);
    }
}
