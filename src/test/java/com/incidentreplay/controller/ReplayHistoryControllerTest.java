package com.incidentreplay.controller;

import com.incidentreplay.model.CapturedRequest;
import com.incidentreplay.model.Incident;
import com.incidentreplay.model.ReplayResult;
import com.incidentreplay.model.ReplaySession;
import com.incidentreplay.repository.IncidentRepository;
import com.incidentreplay.repository.ReplayResultRepository;
import com.incidentreplay.repository.ReplaySessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ReplayHistoryControllerTest {

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

    private Incident createIncidentWithRequest() {
        Incident incident = new Incident();
        incident.setTitle("History test incident");
        incident = incidentRepository.save(incident);

        CapturedRequest req = new CapturedRequest();
        req.setId(UUID.randomUUID().toString());
        req.setMethod("GET");
        req.setUrl("/test-history");
        
        if (incident.getRequests() == null) {
            incident.setRequests(new ArrayList<>());
        }
        incident.getRequests().add(req);
        return incidentRepository.save(incident);
    }

    private ReplayResult createReplayResult(String incidentId, String requestId, Instant time) {
        ReplayResult result = new ReplayResult();
        result.setIncidentId(incidentId);
        result.setRequestId(requestId);
        result.setStatusCode(200);
        result.setResponseBody("Response");
        result.setReplayedAt(time);
        return replayResultRepository.save(result);
    }

    @Test
    void testHistoryValidRequestNoReplayHistory() {
        Incident incident = createIncidentWithRequest();
        String reqId = incident.getRequests().get(0).getId();

        ResponseEntity<List<ReplayResult>> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/requests/" + reqId + "/replays",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<ReplayResult>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void testHistoryOneReplayResult() {
        Incident incident = createIncidentWithRequest();
        String reqId = incident.getRequests().get(0).getId();
        createReplayResult(incident.getId(), reqId, Instant.now());

        ResponseEntity<List<ReplayResult>> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/requests/" + reqId + "/replays",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<ReplayResult>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void testHistoryMultipleReplaysOrderingNewestFirst() {
        Incident incident = createIncidentWithRequest();
        String reqId = incident.getRequests().get(0).getId();
        
        Instant now = Instant.now();
        // Create out of order in database
        createReplayResult(incident.getId(), reqId, now.minus(5, ChronoUnit.MINUTES)); // Oldest
        createReplayResult(incident.getId(), reqId, now); // Newest
        createReplayResult(incident.getId(), reqId, now.minus(2, ChronoUnit.MINUTES)); // Middle

        ResponseEntity<List<ReplayResult>> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/requests/" + reqId + "/replays",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<ReplayResult>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<ReplayResult> replays = response.getBody();
        assertThat(replays).hasSize(3);
        
        // Assert descending order
        assertThat(replays.get(0).getReplayedAt()).isAfter(replays.get(1).getReplayedAt());
        assertThat(replays.get(1).getReplayedAt()).isAfter(replays.get(2).getReplayedAt());
    }

    @Test
    void testHistoryInvalidIncident() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/invalid/requests/reqId/replays", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testHistoryInvalidRequest() {
        Incident incident = createIncidentWithRequest();
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/requests/invalid/replays", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testGetReplaySessionReturnsTargetUrl() {
        Incident incident = createIncidentWithRequest();

        ReplaySession session = new ReplaySession();
        session.setIncidentId(incident.getId());
        session.setStatus(com.incidentreplay.model.ReplaySessionStatus.COMPLETED);
        session.setTotalRequests(1);
        session.setSuccessfulReplays(1);
        session.setFailedReplays(0);
        session.setReplayTargetBaseUrl("http://localhost:8081");
        session = replaySessionRepository.save(session);

        ResponseEntity<ReplaySession> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/replay-sessions/" + session.getId(),
                ReplaySession.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getReplayTargetBaseUrl()).isEqualTo("http://localhost:8081");
    }

    @Test
    void testHistoryRequestBelongingToAnotherIncident() {
        Incident incident1 = createIncidentWithRequest();
        Incident incident2 = createIncidentWithRequest();

        String reqId1 = incident1.getRequests().get(0).getId();

        // Query incident2 using reqId from incident1
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident2.getId() + "/requests/" + reqId1 + "/replays", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
