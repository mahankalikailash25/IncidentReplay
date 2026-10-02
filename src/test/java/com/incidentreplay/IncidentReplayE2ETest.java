package com.incidentreplay;

import com.incidentreplay.model.*;
import com.incidentreplay.dto.CreateIncidentContextRequest;
import com.incidentreplay.controller.IncidentController.CreateIncidentRequest;
import com.incidentreplay.controller.IncidentController.AddCapturedRequestDto;
import com.incidentreplay.repository.IncidentRepository;
import com.incidentreplay.repository.IncidentEventRepository;
import com.incidentreplay.repository.ReplayResultRepository;
import com.incidentreplay.repository.ReplaySessionRepository;
import com.incidentreplay.config.ReplayProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class IncidentReplayE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private IncidentEventRepository incidentEventRepository;

    @Autowired
    private ReplaySessionRepository replaySessionRepository;

    @Autowired
    private ReplayResultRepository replayResultRepository;

    @Autowired
    private ReplayProperties replayProperties;

    @BeforeEach
    void setUp() {
        incidentRepository.deleteAll();
        incidentEventRepository.deleteAll();
        replaySessionRepository.deleteAll();
        replayResultRepository.deleteAll();
        replayProperties.setTargetBaseUrl("http://localhost:" + port);
    }

    @Test
    void testEndToEndIncidentReplayWorkflow() throws Exception {
        String baseUrl = "http://localhost:" + port;

        // 1. Create a new Incident
        CreateIncidentRequest incidentRequest = new CreateIncidentRequest();
        incidentRequest.setTitle("E2E Test Incident");
        incidentRequest.setServiceName("e2e-service");
        incidentRequest.setSeverity(Severity.HIGH);

        ResponseEntity<Incident> createIncidentResponse = restTemplate.postForEntity(
                baseUrl + "/api/incidents", incidentRequest, Incident.class);
        
        assertThat(createIncidentResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Incident incident = createIncidentResponse.getBody();
        assertThat(incident).isNotNull();
        assertThat(incident.getId()).isNotNull();

        // 2. Capture Incident Context
        String contextPath = baseUrl + "/api/incidents/" + incident.getId() + "/context";
        CreateIncidentContextRequest contextReq = new CreateIncidentContextRequest();
        contextReq.setAffectedService("e2e-service");
        contextReq.setEnvironment("TEST");
        contextReq.setServiceVersion("1.0");
        contextReq.setErrorMessage("Internal Server Error");
        ResponseEntity<IncidentContext> contextResponse = restTemplate.postForEntity(
                contextPath, contextReq, IncidentContext.class);
        
        assertThat(contextResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        
        // 3. Capture multiple HTTP requests
        String captureReqPath = baseUrl + "/api/incidents/" + incident.getId() + "/requests";
        
        // Request 1: GET /api/test-target/health
        AddCapturedRequestDto req1 = new AddCapturedRequestDto();
        req1.setMethod("GET");
        req1.setUrl("/api/test-target/health");
        req1.setOriginalStatusCode(200);
        req1.setOriginalResponseBody("OK");
        
        ResponseEntity<Incident> capReq1Res = restTemplate.postForEntity(
                captureReqPath, req1, Incident.class);
        assertThat(capReq1Res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        
        // Request 2: GET /api/test-target/health (Simulate a failure match to test mismatches)
        AddCapturedRequestDto req2 = new AddCapturedRequestDto();
        req2.setMethod("GET");
        req2.setUrl("/api/test-target/health");
        req2.setOriginalStatusCode(200);
        req2.setOriginalResponseBody("NOT OK"); // Intentional mismatch
        
        ResponseEntity<Incident> capReq2Res = restTemplate.postForEntity(
                captureReqPath, req2, Incident.class);
        assertThat(capReq2Res.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Request 3: GET /api/test-target/health (Uncomparable request)
        AddCapturedRequestDto req3 = new AddCapturedRequestDto();
        req3.setMethod("GET");
        req3.setUrl("/api/test-target/health");
        req3.setOriginalStatusCode(null);
        req3.setOriginalResponseBody(null);

        ResponseEntity<Incident> capReq3Res = restTemplate.postForEntity(
                captureReqPath, req3, Incident.class);
        assertThat(capReq3Res.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 4. Run incident-level batch replay
        String replayPath = baseUrl + "/api/incidents/" + incident.getId() + "/replay";
        ResponseEntity<BatchReplaySummary> replayRes = restTemplate.postForEntity(
                replayPath, null, BatchReplaySummary.class);
        
        assertThat(replayRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        BatchReplaySummary batchSummary = replayRes.getBody();
        assertThat(batchSummary).isNotNull();
        assertThat(batchSummary.getStatus()).isEqualTo("COMPLETED");
        assertThat(batchSummary.getSuccessfulReplays()).isEqualTo(3);
        assertThat(batchSummary.getFailedReplays()).isEqualTo(0);
        
        String sessionId = batchSummary.getSessionId();

        // 5. Verify Session state in DB
        ReplaySession session = replaySessionRepository.findById(sessionId).orElseThrow();
        assertThat(session.getStatus()).isEqualTo(ReplaySessionStatus.COMPLETED);
        
        List<ReplayResult> results = replayResultRepository.findByIncidentId(incident.getId());
        assertThat(results).hasSize(3);
        results.forEach(r -> {
            assertThat(r.getSessionId()).isEqualTo(sessionId);
            assertThat(r.getExecutionTimeMs()).isNotNull();
        });

        // 6. Verify session endpoints
        // a. List sessions
        ResponseEntity<ReplaySession[]> sessionsRes = restTemplate.getForEntity(
                baseUrl + "/api/incidents/" + incident.getId() + "/replay-sessions", ReplaySession[].class);
        assertThat(sessionsRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sessionsRes.getBody()).hasSize(1);
        
        // b. Session details
        ResponseEntity<ReplaySession> sessionDetailsRes = restTemplate.getForEntity(
                baseUrl + "/api/incidents/" + incident.getId() + "/replay-sessions/" + sessionId, ReplaySession.class);
        assertThat(sessionDetailsRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sessionDetailsRes.getBody().getId()).isEqualTo(sessionId);
        
        // c. Session replay results
        ResponseEntity<ReplayResult[]> sessionResultsRes = restTemplate.getForEntity(
                baseUrl + "/api/incidents/" + incident.getId() + "/replay-sessions/" + sessionId + "/replays", ReplayResult[].class);
        assertThat(sessionResultsRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sessionResultsRes.getBody()).hasSize(3);

        // d. Session comparison
        ResponseEntity<SessionComparisonResult> sessionCompRes = restTemplate.getForEntity(
                baseUrl + "/api/incidents/" + incident.getId() + "/replay-sessions/" + sessionId + "/compare", SessionComparisonResult.class);
        assertThat(sessionCompRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        SessionComparisonResult compResult = sessionCompRes.getBody();
        assertThat(compResult).isNotNull();
        assertThat(compResult.getTotalRequests()).isEqualTo(3);
        assertThat(compResult.getComparableRequests()).isEqualTo(2); // Request 3 has no original status/body
        assertThat(compResult.getStatusCodeMatches()).isEqualTo(2); // Both matched 200
        assertThat(compResult.getResponseBodyMatches()).isEqualTo(1); // One was OK, one was NOT OK
        assertThat(compResult.getOverallMatches()).isEqualTo(1);

        // e. Session summary
        ResponseEntity<ReplaySessionSummary> sessionSummaryRes = restTemplate.getForEntity(
                baseUrl + "/api/incidents/" + incident.getId() + "/replay-sessions/" + sessionId + "/summary", ReplaySessionSummary.class);
        assertThat(sessionSummaryRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        ReplaySessionSummary fullSummary = sessionSummaryRes.getBody();
        assertThat(fullSummary).isNotNull();
        assertThat(fullSummary.getStatus()).isEqualTo(ReplaySessionStatus.COMPLETED);
        assertThat(fullSummary.getComparableRequests()).isEqualTo(2);
        assertThat(fullSummary.getOverallMatches()).isEqualTo(1);

        // f. Comparison details
        ResponseEntity<SessionComparisonDetailsResult> compDetailsRes = restTemplate.getForEntity(
                baseUrl + "/api/incidents/" + incident.getId() + "/replay-sessions/" + sessionId + "/comparison-details", SessionComparisonDetailsResult.class);
        assertThat(compDetailsRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        SessionComparisonDetailsResult detailsResult = compDetailsRes.getBody();
        assertThat(detailsResult.getDetails()).hasSize(3);
        
        long comparableCount = detailsResult.getDetails().stream().filter(SessionComparisonDetail::isComparable).count();
        assertThat(comparableCount).isEqualTo(2);

        detailsResult.getDetails().forEach(detail -> {
            assertThat(detail.getExecutionTimeMs()).isNotNull();
        });

        // 7. Verify incident events
        List<IncidentEvent> events = incidentEventRepository.findByIncidentIdOrderByTimestampAsc(incident.getId());
        assertThat(events).isNotEmpty();
        
        long createdCount = events.stream().filter(e -> e.getEventType() == EventType.INCIDENT_CREATED).count();
        long contextCount = events.stream().filter(e -> e.getEventType() == EventType.CONTEXT_CAPTURED).count();
        long reqCapCount = events.stream().filter(e -> e.getEventType() == EventType.REQUEST_CAPTURED).count();
        long reqRepCount = events.stream().filter(e -> e.getEventType() == EventType.REQUEST_REPLAYED).count();

        assertThat(createdCount).isEqualTo(1);
        assertThat(contextCount).isEqualTo(1);
        assertThat(reqCapCount).isEqualTo(3);
        assertThat(reqRepCount).isEqualTo(3);
    }
}
