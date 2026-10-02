package com.incidentreplay.controller;

import com.incidentreplay.model.*;
import com.incidentreplay.repository.IncidentRepository;
import com.incidentreplay.repository.IncidentEventRepository;
import com.incidentreplay.repository.ReplayResultRepository;
import com.incidentreplay.repository.ReplaySessionRepository;
import com.incidentreplay.service.ReplayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.incidentreplay.config.ReplayProperties;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ReplayControllerTest {

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

    @Autowired
    private ReplayProperties replayProperties;

    @Autowired
    private IncidentEventRepository incidentEventRepository;

    @Autowired
    private ReplayService replayService;

    @BeforeEach
    void setUp() {
        incidentRepository.deleteAll();
        replayResultRepository.deleteAll();
        replaySessionRepository.deleteAll();
        incidentEventRepository.deleteAll();
        replayProperties.setTargetBaseUrl("http://localhost:" + port);
    }

    private Incident createIncidentWithRequests(int numRequests) {
        Incident incident = new Incident();
        incident.setTitle("Batch Replay Test Incident");
        incident.setServiceName("test-service");
        incident.setSeverity(Severity.MEDIUM);

        List<CapturedRequest> requests = new ArrayList<>();
        Instant now = Instant.now();
        for (int i = 0; i < numRequests; i++) {
            CapturedRequest req = new CapturedRequest();
            req.setId(UUID.randomUUID().toString());
            req.setMethod("POST");
            req.setUrl("/api/test-target/echo");
            req.setBody("test-body-" + i);
            req.setCapturedAt(now.plus(i, ChronoUnit.SECONDS)); // Sequential capture order
            requests.add(req);
        }
        incident.setRequests(requests);
        return incidentRepository.save(incident);
    }

    @Test
    void testBatchReplayAllRequests() {
        Incident incident = createIncidentWithRequests(3);

        ResponseEntity<BatchReplaySummary> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/replay",
                null, BatchReplaySummary.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        BatchReplaySummary summary = response.getBody();
        assertThat(summary).isNotNull();
        assertThat(summary.getIncidentId()).isEqualTo(incident.getId());
        assertThat(summary.getTotalRequests()).isEqualTo(3);
        assertThat(summary.getSuccessfulReplays()).isEqualTo(3);
        assertThat(summary.getFailedReplays()).isEqualTo(0);
        assertThat(summary.getSessionId()).isNotNull();
        assertThat(summary.getReplayTargetBaseUrl()).isEqualTo("http://localhost:" + port);
        assertThat(summary.getStatus()).isEqualTo(ReplaySessionStatus.COMPLETED.name());
        assertThat(summary.getStartedAt()).isNotNull();
        assertThat(summary.getCompletedAt()).isNotNull();
        
        List<BatchReplayResult> results = summary.getResults();
        assertThat(results).hasSize(3);

        // Ensure returned in capture order
        for (int i = 0; i < 3; i++) {
            assertThat(results.get(i).getRequestId()).isEqualTo(incident.getRequests().get(i).getId());
            assertThat(results.get(i).getStatus()).isEqualTo("SUCCESS");
            assertThat(results.get(i).getStatusCode()).isEqualTo(200);
            assertThat(results.get(i).getFailureReason()).isNull();
        }

        // Verify ReplayResult persistence (Condition 1) and chronological execution order (Condition 3)
        List<ReplayResult> persistedResults = replayResultRepository.findByIncidentId(incident.getId());
        assertThat(persistedResults).hasSize(3);
        
        persistedResults.sort((r1, r2) -> r1.getReplayedAt().compareTo(r2.getReplayedAt()));
        for (int i = 0; i < 3; i++) {
            assertThat(persistedResults.get(i).getRequestId()).isEqualTo(incident.getRequests().get(i).getId());
            assertThat(persistedResults.get(i).getSessionId()).isEqualTo(summary.getSessionId());
            assertThat(persistedResults.get(i).getExecutionTimeMs()).isNotNull().isGreaterThanOrEqualTo(0L);
        }

        // Verify REQUEST_REPLAYED events
        List<IncidentEvent> events = incidentEventRepository.findByIncidentIdOrderByTimestampAsc(incident.getId());
        // Depending on if the test created INCIDENT_CREATED, here we didn't use the endpoint so events should just be the 3 REPLAY events
        assertThat(events).hasSize(3);
        assertThat(events.get(0).getEventType()).isEqualTo(EventType.REQUEST_REPLAYED);
        assertThat(events.get(1).getEventType()).isEqualTo(EventType.REQUEST_REPLAYED);
        assertThat(events.get(2).getEventType()).isEqualTo(EventType.REQUEST_REPLAYED);

        // Verify ReplaySession
        List<ReplaySession> sessions = replaySessionRepository.findAll();
        assertThat(sessions).hasSize(1);
        ReplaySession session = sessions.get(0);
        assertThat(session.getIncidentId()).isEqualTo(incident.getId());
        assertThat(session.getTotalRequests()).isEqualTo(3);
        assertThat(session.getSuccessfulReplays()).isEqualTo(3);
        assertThat(session.getFailedReplays()).isEqualTo(0);
        assertThat(session.getStatus()).isEqualTo(ReplaySessionStatus.COMPLETED);
        assertThat(session.getReplayTargetBaseUrl()).isEqualTo("http://localhost:" + port);
    }

    @Test
    void testBatchReplayDeterministicOrder() {
        Incident incident = new Incident();
        incident.setTitle("Deterministic Order Test");
        
        Instant sameTime = Instant.now();
        List<CapturedRequest> requests = new ArrayList<>();
        
        CapturedRequest req1 = new CapturedRequest();
        req1.setId("B");
        req1.setMethod("POST");
        req1.setUrl("/api/test-target/echo");
        req1.setCapturedAt(sameTime);
        
        CapturedRequest req2 = new CapturedRequest();
        req2.setId("C");
        req2.setMethod("POST");
        req2.setUrl("/api/test-target/echo");
        req2.setCapturedAt(sameTime);
        
        CapturedRequest req3 = new CapturedRequest();
        req3.setId("A");
        req3.setMethod("POST");
        req3.setUrl("/api/test-target/echo");
        req3.setCapturedAt(sameTime);
        
        // Add out of order
        requests.add(req1);
        requests.add(req2);
        requests.add(req3);
        incident.setRequests(requests);
        incident = incidentRepository.save(incident);

        ResponseEntity<BatchReplaySummary> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/replay",
                null, BatchReplaySummary.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        BatchReplaySummary summary = response.getBody();
        assertThat(summary.getResults()).hasSize(3);
        
        // Should be ordered by ID: A, B, C
        assertThat(summary.getResults().get(0).getRequestId()).isEqualTo("A");
        assertThat(summary.getResults().get(1).getRequestId()).isEqualTo("B");
        assertThat(summary.getResults().get(2).getRequestId()).isEqualTo("C");
    }

    @Test
    void testBatchReplayWithZeroRequestsReturnsEmptySummary() {
        Incident incident = createIncidentWithRequests(0);

        ResponseEntity<BatchReplaySummary> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/replay",
                null, BatchReplaySummary.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        BatchReplaySummary summary = response.getBody();
        assertThat(summary).isNotNull();
        assertThat(summary.getIncidentId()).isEqualTo(incident.getId());
        assertThat(summary.getTotalRequests()).isEqualTo(0);
        assertThat(summary.getSuccessfulReplays()).isEqualTo(0);
        assertThat(summary.getFailedReplays()).isEqualTo(0);
        assertThat(summary.getResults()).isEmpty();
    }

    @Test
    void testBatchReplayInvalidIncidentReturns404() {
        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents/invalid-id/replay",
                null, ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).isEqualTo("Incident not found");
    }

    @Test
    void testBatchReplayFailsWhenTargetUnreachable() {
        Incident incident = createIncidentWithRequests(2);

        // Point to an unreachable port to simulate failure
        replayProperties.setTargetBaseUrl("http://localhost:1");

        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/replay",
                null, ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        
        ApiErrorResponse error = response.getBody();
        assertThat(error).isNotNull();
        assertThat(error.getStatus()).isEqualTo(502);
        assertThat(error.getMessage()).contains("Configured replay target is unavailable at http://localhost:1");
        
        // Verify no fake ReplayResult was created
        List<ReplayResult> persistedResults = replayResultRepository.findAll();
        assertThat(persistedResults).isEmpty();

        // Verify no ReplaySession was created
        List<ReplaySession> sessions = replaySessionRepository.findAll();
        assertThat(sessions).isEmpty();
    }

    @Test
    void testSingleReplayTimeout() {
        Incident incident = createIncidentWithRequests(1);
        incident.getRequests().get(0).setUrl("/api/test-target/delay");
        incident.getRequests().get(0).setMethod("GET");
        incidentRepository.save(incident);

        int originalTimeout = replayProperties.getTimeoutMs();
        replayProperties.setTimeoutMs(100);
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(100);
        factory.setReadTimeout(100);
        org.springframework.web.client.RestClient fastTimeoutClient = org.springframework.web.client.RestClient.builder().requestFactory(factory).build();
        org.springframework.web.client.RestClient originalClient = (org.springframework.web.client.RestClient) org.springframework.test.util.ReflectionTestUtils.getField(replayService, "restClient");
        org.springframework.test.util.ReflectionTestUtils.setField(replayService, "restClient", fastTimeoutClient);

        try {
            ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                    "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/requests/" + incident.getRequests().get(0).getId() + "/replay",
                    null, ApiErrorResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
            assertThat(response.getBody().getMessage()).contains("Replay request timed out after 100 ms");

            // Verify no fake ReplayResult was created
            List<ReplayResult> persistedResults = replayResultRepository.findAll();
            assertThat(persistedResults).isEmpty();
        } finally {
            // Reset timeout and client
            replayProperties.setTimeoutMs(originalTimeout);
            org.springframework.test.util.ReflectionTestUtils.setField(replayService, "restClient", originalClient);
        }
    }

    @Test
    void testBatchReplayWithTimeout() {
        Incident incident = new Incident();
        incident.setTitle("Batch Replay Timeout Test");
        List<CapturedRequest> requests = new ArrayList<>();
        
        CapturedRequest req1 = new CapturedRequest();
        req1.setId("1");
        req1.setMethod("GET");
        req1.setUrl("/api/test-target/delay"); // will timeout
        req1.setCapturedAt(Instant.now());
        
        CapturedRequest req2 = new CapturedRequest();
        req2.setId("2");
        req2.setMethod("POST");
        req2.setUrl("/api/test-target/echo"); // will succeed
        req2.setCapturedAt(Instant.now().plusSeconds(1));
        
        requests.add(req1);
        requests.add(req2);
        incident.setRequests(requests);
        incidentRepository.save(incident);
        
        int originalTimeout = replayProperties.getTimeoutMs();
        replayProperties.setTimeoutMs(100);
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(100);
        factory.setReadTimeout(100);
        org.springframework.web.client.RestClient fastTimeoutClient = org.springframework.web.client.RestClient.builder().requestFactory(factory).build();
        org.springframework.web.client.RestClient originalClient = (org.springframework.web.client.RestClient) org.springframework.test.util.ReflectionTestUtils.getField(replayService, "restClient");
        org.springframework.test.util.ReflectionTestUtils.setField(replayService, "restClient", fastTimeoutClient);

        try {
            ResponseEntity<BatchReplaySummary> response = restTemplate.postForEntity(
                    "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/replay",
                    null, BatchReplaySummary.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            BatchReplaySummary summary = response.getBody();
            assertThat(summary.getResults()).hasSize(2);
            
            // Req1 should be failed with 504
            assertThat(summary.getResults().get(0).getStatus()).isEqualTo("FAILED");
            assertThat(summary.getResults().get(0).getStatusCode()).isEqualTo(504);
            assertThat(summary.getResults().get(0).getFailureReason()).contains("Replay request timed out after 100 ms");
            
            // Req2 should be SUCCESS
            assertThat(summary.getResults().get(1).getStatus()).isEqualTo("SUCCESS");
            assertThat(summary.getResults().get(1).getStatusCode()).isEqualTo(200);

            assertThat(summary.getSuccessfulReplays()).isEqualTo(1);
            assertThat(summary.getFailedReplays()).isEqualTo(1);

            // Verify no fake ReplayResult was created for the timeout
            List<ReplayResult> persistedResults = replayResultRepository.findAll();
            assertThat(persistedResults).hasSize(1);
            assertThat(persistedResults.get(0).getRequestId()).isEqualTo("2");
        } finally {
            // Reset timeout
            replayProperties.setTimeoutMs(originalTimeout);
            org.springframework.test.util.ReflectionTestUtils.setField(replayService, "restClient", originalClient);
        }
    }

    @Test
    void testBatchReplayFailsWhenTargetReturnsNon2xx() {
        Incident incident = createIncidentWithRequests(2);

        // We can point the target base URL to the application's own port, but to a path that returns 404
        // So the health check /api/test-target/health will be appended to it.
        // Let's set base URL to localhost:port/not-found-prefix
        replayProperties.setTargetBaseUrl("http://localhost:" + port + "/not-found-prefix");

        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/replay",
                null, ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        
        ApiErrorResponse error = response.getBody();
        assertThat(error).isNotNull();
        assertThat(error.getStatus()).isEqualTo(502);
        assertThat(error.getMessage()).contains("Configured replay target is unavailable at http://localhost:" + port + "/not-found-prefix");
        
        // Verify no fake ReplayResult was created
        List<ReplayResult> persistedResults = replayResultRepository.findAll();
        assertThat(persistedResults).isEmpty();

        // Verify no ReplaySession was created
        List<ReplaySession> sessions = replaySessionRepository.findAll();
        assertThat(sessions).isEmpty();
    }

    @Test
    void testBatchReplayWithUnexpectedExceptionOnOneRequest() {
        Incident incident = new Incident();
        incident.setTitle("Batch Replay Unexpected Exception Test");
        List<CapturedRequest> requests = new ArrayList<>();
        
        CapturedRequest req1 = new CapturedRequest();
        req1.setId("1");
        req1.setMethod(null); // This will cause NullPointerException in replayRequest
        req1.setUrl("/api/test-target/echo");
        req1.setCapturedAt(Instant.now());
        
        CapturedRequest req2 = new CapturedRequest();
        req2.setId("2");
        req2.setMethod("POST");
        req2.setUrl("/api/test-target/echo"); // This will succeed
        req2.setCapturedAt(Instant.now().plusSeconds(1));
        
        requests.add(req1);
        requests.add(req2);
        incident.setRequests(requests);
        incidentRepository.save(incident);
        
        ResponseEntity<BatchReplaySummary> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/replay",
                null, BatchReplaySummary.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        BatchReplaySummary summary = response.getBody();
        assertThat(summary.getResults()).hasSize(2);
        
        // Req1 should be FAILED due to unexpected NPE
        assertThat(summary.getResults().get(0).getStatus()).isEqualTo("FAILED");
        assertThat(summary.getResults().get(0).getStatusCode()).isEqualTo(500);
        assertThat(summary.getResults().get(0).getFailureReason()).contains("is null");
        
        // Req2 should be SUCCESS (batch continued!)
        assertThat(summary.getResults().get(1).getStatus()).isEqualTo("SUCCESS");
        assertThat(summary.getResults().get(1).getStatusCode()).isEqualTo(200);

        assertThat(summary.getSuccessfulReplays()).isEqualTo(1);
        assertThat(summary.getFailedReplays()).isEqualTo(1);
        assertThat(summary.getStatus()).isEqualTo("COMPLETED_WITH_ERRORS");

        // Verify session is COMPLETED_WITH_ERRORS and not RUNNING
        ReplaySession session = replaySessionRepository.findById(summary.getSessionId()).orElseThrow();
        assertThat(session.getStatus().name()).isEqualTo("COMPLETED_WITH_ERRORS");
        assertThat(session.getFailedReplays()).isEqualTo(1);
        assertThat(session.getSuccessfulReplays()).isEqualTo(1);

        // Verify no fake ReplayResult was created for req1
        List<ReplayResult> persistedResults = replayResultRepository.findAll();
        assertThat(persistedResults).hasSize(1);
        assertThat(persistedResults.get(0).getRequestId()).isEqualTo("2");
    }

    @Test
    void testBatchReplayNormalReachesCompleted() {
        Incident incident = createIncidentWithRequests(2);
        
        ResponseEntity<BatchReplaySummary> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/replay",
                null, BatchReplaySummary.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        BatchReplaySummary summary = response.getBody();
        
        assertThat(summary.getStatus()).isEqualTo("COMPLETED");
        
        ReplaySession session = replaySessionRepository.findById(summary.getSessionId()).orElseThrow();
        assertThat(session.getStatus().name()).isEqualTo("COMPLETED");
    }

    @Test
    void testReplayTargetIsolation_absoluteUrlRejected() {
        Incident incident = createIncidentWithRequests(1);
        incident.getRequests().get(0).setUrl("http://example.com/api/test");
        incidentRepository.save(incident);

        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/requests/" + incident.getRequests().get(0).getId() + "/replay",
                null, ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("Replay target URL is not allowed");
    }

    @Test
    void testReplayTargetIsolation_atHostOverrideRejected() {
        Incident incident = createIncidentWithRequests(1);
        incident.getRequests().get(0).setUrl("@localhost:9999/api/test");
        incidentRepository.save(incident);

        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/requests/" + incident.getRequests().get(0).getId() + "/replay",
                null, ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("Replay target URL is not allowed");
    }

    @Test
    void testReplayTargetIsolation_userInfoRejected() {
        Incident incident = createIncidentWithRequests(1);
        incident.getRequests().get(0).setUrl("http://user:pass@localhost:8081/api/test");
        incidentRepository.save(incident);

        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/requests/" + incident.getRequests().get(0).getId() + "/replay",
                null, ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("Replay target URL is not allowed");
    }

    @Test
    void testReplayTargetIsolation_protocolRelativeRejected() {
        Incident incident = createIncidentWithRequests(1);
        incident.getRequests().get(0).setUrl("//example.com/api/test");
        incidentRepository.save(incident);

        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/requests/" + incident.getRequests().get(0).getId() + "/replay",
                null, ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("Replay target URL is not allowed");
    }

    @Test
    void testReplayTargetIsolation_batchReplayContinuesOnRejectedUrl() {
        Incident incident = new Incident();
        incident.setTitle("SSRF Batch Test");
        List<CapturedRequest> requests = new ArrayList<>();
        
        CapturedRequest req1 = new CapturedRequest();
        req1.setId("1");
        req1.setMethod("GET");
        req1.setUrl("@example.com/api"); // bad
        req1.setCapturedAt(Instant.now());
        
        CapturedRequest req2 = new CapturedRequest();
        req2.setId("2");
        req2.setMethod("POST");
        req2.setUrl("/api/test-target/echo?query=1"); // good with query string
        req2.setCapturedAt(Instant.now().plusSeconds(1));
        
        requests.add(req1);
        requests.add(req2);
        incident.setRequests(requests);
        incidentRepository.save(incident);
        
        ResponseEntity<BatchReplaySummary> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/replay",
                null, BatchReplaySummary.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        BatchReplaySummary summary = response.getBody();
        assertThat(summary.getResults()).hasSize(2);
        
        // Req1 should be failed with 400
        assertThat(summary.getResults().get(0).getStatus()).isEqualTo("FAILED");
        assertThat(summary.getResults().get(0).getStatusCode()).isEqualTo(400);
        assertThat(summary.getResults().get(0).getFailureReason()).contains("Replay target URL is not allowed");
        
        // Req2 should be SUCCESS
        assertThat(summary.getResults().get(1).getStatus()).isEqualTo("SUCCESS");
        assertThat(summary.getResults().get(1).getStatusCode()).isEqualTo(200);
    }

    @Test
    void testGetReplaySessionsForIncident() {
        Incident incident = createIncidentWithRequests(1);
        
        // Trigger two replays
        restTemplate.postForEntity("http://localhost:" + port + "/api/incidents/" + incident.getId() + "/replay", null, BatchReplaySummary.class);
        restTemplate.postForEntity("http://localhost:" + port + "/api/incidents/" + incident.getId() + "/replay", null, BatchReplaySummary.class);

        ResponseEntity<ReplaySession[]> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/replay-sessions",
                ReplaySession[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ReplaySession[] sessions = response.getBody();
        assertThat(sessions).hasSize(2);
        // Ensure sorted by startedAt descending
        assertThat(sessions[0].getStartedAt()).isAfterOrEqualTo(sessions[1].getStartedAt());
    }

    @Test
    void testGetReplaySessionsEmpty() {
        Incident incident = createIncidentWithRequests(0);

        ResponseEntity<ReplaySession[]> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/replay-sessions",
                ReplaySession[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void testGetReplaySessionsInvalidIncidentReturns404() {
        ResponseEntity<ApiErrorResponse> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/invalid-id/replay-sessions",
                ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).isEqualTo("Incident not found");
    }

    @Test
    void testGetIndividualReplaySession() {
        Incident incident = createIncidentWithRequests(1);
        ResponseEntity<BatchReplaySummary> postResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/replay",
                null, BatchReplaySummary.class);
        String sessionId = postResponse.getBody().getSessionId();

        ResponseEntity<ReplaySession> getResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/replay-sessions/" + sessionId,
                ReplaySession.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().getId()).isEqualTo(sessionId);
        assertThat(getResponse.getBody().getStatus()).isEqualTo(ReplaySessionStatus.COMPLETED);
    }

    @Test
    void testGetIndividualReplaySessionInvalidIncidentReturns404() {
        ResponseEntity<ApiErrorResponse> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/invalid-id/replay-sessions/some-session",
                ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testGetIndividualReplaySessionInvalidSessionReturns404() {
        Incident incident = createIncidentWithRequests(1);
        ResponseEntity<ApiErrorResponse> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/replay-sessions/invalid-session",
                ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testGetIndividualReplaySessionWrongIncidentReturns404() {
        Incident incident = createIncidentWithRequests(1);
        Incident otherIncident = createIncidentWithRequests(1);

        ResponseEntity<BatchReplaySummary> postResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/replay",
                null, BatchReplaySummary.class);
        String sessionId = postResponse.getBody().getSessionId();

        ResponseEntity<ApiErrorResponse> getResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + otherIncident.getId() + "/replay-sessions/" + sessionId,
                ApiErrorResponse.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(getResponse.getBody().getMessage()).isEqualTo("Replay session does not belong to this incident");
    }

    @Test
    void testReplayResultsForSessionReturnsCorrectRecordsAndOrder() {
        Incident incident = createIncidentWithRequests(2);
        ResponseEntity<BatchReplaySummary> postResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/replay",
                null, BatchReplaySummary.class);
        String sessionId = postResponse.getBody().getSessionId();

        ResponseEntity<ReplayResult[]> getResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/replay-sessions/" + sessionId + "/replays",
                ReplayResult[].class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        ReplayResult[] results = getResponse.getBody();
        assertThat(results).hasSize(2);
        
        // Ensure ordered by replayedAt ascending
        assertThat(results[0].getReplayedAt().isBefore(results[1].getReplayedAt()) || 
                   results[0].getReplayedAt().equals(results[1].getReplayedAt())).isTrue();
        
        // Ensure all belong to the session
        assertThat(results[0].getSessionId()).isEqualTo(sessionId);
        assertThat(results[1].getSessionId()).isEqualTo(sessionId);
    }

    @Test
    void testReplayResultsFromDifferentSessionsRemainSeparated() {
        Incident incident = createIncidentWithRequests(1);
        
        // Trigger session 1
        ResponseEntity<BatchReplaySummary> postResponse1 = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/replay",
                null, BatchReplaySummary.class);
        String sessionId1 = postResponse1.getBody().getSessionId();

        // Trigger session 2
        ResponseEntity<BatchReplaySummary> postResponse2 = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/replay",
                null, BatchReplaySummary.class);
        String sessionId2 = postResponse2.getBody().getSessionId();

        ResponseEntity<ReplayResult[]> getResponse1 = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/replay-sessions/" + sessionId1 + "/replays",
                ReplayResult[].class);
        assertThat(getResponse1.getStatusCode()).isEqualTo(HttpStatus.OK);
        ReplayResult[] results1 = getResponse1.getBody();
        assertThat(results1).hasSize(1);
        assertThat(results1[0].getSessionId()).isEqualTo(sessionId1);

        ResponseEntity<ReplayResult[]> getResponse2 = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/replay-sessions/" + sessionId2 + "/replays",
                ReplayResult[].class);
        assertThat(getResponse2.getStatusCode()).isEqualTo(HttpStatus.OK);
        ReplayResult[] results2 = getResponse2.getBody();
        assertThat(results2).hasSize(1);
        assertThat(results2[0].getSessionId()).isEqualTo(sessionId2);
    }

    @Test
    void testReplayResultsForSessionEmptySessionReturnsEmptyList() {
        Incident incident = createIncidentWithRequests(0);
        ResponseEntity<BatchReplaySummary> postResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/replay",
                null, BatchReplaySummary.class);
        String sessionId = postResponse.getBody().getSessionId();

        ResponseEntity<ReplayResult[]> getResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/replay-sessions/" + sessionId + "/replays",
                ReplayResult[].class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isEmpty();
    }

    @Test
    void testReplayResultsForSessionInvalidIncidentReturns404() {
        ResponseEntity<ApiErrorResponse> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/invalid-id/replay-sessions/some-session/replays",
                ApiErrorResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testReplayResultsForSessionInvalidSessionReturns404() {
        Incident incident = createIncidentWithRequests(1);
        ResponseEntity<ApiErrorResponse> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/replay-sessions/invalid-session/replays",
                ApiErrorResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testReplayResultsForSessionWrongIncidentReturns404() {
        Incident incident = createIncidentWithRequests(1);
        Incident otherIncident = createIncidentWithRequests(1);

        ResponseEntity<BatchReplaySummary> postResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/replay",
                null, BatchReplaySummary.class);
        String sessionId = postResponse.getBody().getSessionId();

        ResponseEntity<ApiErrorResponse> getResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + otherIncident.getId() + "/replay-sessions/" + sessionId + "/replays",
                ApiErrorResponse.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
