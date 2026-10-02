package com.incidentreplay.controller;

import com.incidentreplay.model.CapturedRequest;
import com.incidentreplay.model.Incident;
import com.incidentreplay.model.ReplayResult;
import com.incidentreplay.model.ReplayStatistics;
import com.incidentreplay.repository.IncidentRepository;
import com.incidentreplay.repository.ReplayResultRepository;
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
public class ReplayStatisticsControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private ReplayResultRepository replayResultRepository;

    @BeforeEach
    void setUp() {
        incidentRepository.deleteAll();
        replayResultRepository.deleteAll();
    }

    private Incident createIncidentWithRequest(Integer origStatus, String origBody) {
        Incident incident = new Incident();
        incident.setTitle("Stats test incident");
        incident = incidentRepository.save(incident);

        CapturedRequest req = new CapturedRequest();
        req.setId(UUID.randomUUID().toString());
        req.setMethod("GET");
        req.setUrl("/test-stats");
        req.setOriginalStatusCode(origStatus);
        req.setOriginalResponseBody(origBody);
        
        if (incident.getRequests() == null) {
            incident.setRequests(new ArrayList<>());
        }
        incident.getRequests().add(req);
        return incidentRepository.save(incident);
    }

    private void createReplayResult(String incidentId, String requestId, int status, String body, Instant time) {
        ReplayResult result = new ReplayResult();
        result.setIncidentId(incidentId);
        result.setRequestId(requestId);
        result.setStatusCode(status);
        result.setResponseBody(body);
        result.setReplayedAt(time);
        replayResultRepository.save(result);
    }

    @Test
    void testStatsNoReplayResults() {
        Incident incident = createIncidentWithRequest(200, "{}");
        String reqId = incident.getRequests().get(0).getId();

        ResponseEntity<ReplayStatistics> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/requests/" + reqId + "/replay-stats", ReplayStatistics.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ReplayStatistics stats = response.getBody();
        assertThat(stats.getTotalReplays()).isEqualTo(0);
        assertThat(stats.getSuccessfulReplays()).isEqualTo(0);
        assertThat(stats.getOverallMatches()).isEqualTo(0);
        assertThat(stats.getLatestReplayAt()).isNull();
    }

    @Test
    void testStatsOneMatch() {
        Incident incident = createIncidentWithRequest(200, "{\"success\":true}");
        String reqId = incident.getRequests().get(0).getId();
        createReplayResult(incident.getId(), reqId, 200, "{\"success\":true}", Instant.now());

        ResponseEntity<ReplayStatistics> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/requests/" + reqId + "/replay-stats", ReplayStatistics.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ReplayStatistics stats = response.getBody();
        assertThat(stats.getTotalReplays()).isEqualTo(1);
        assertThat(stats.getStatusCodeMatches()).isEqualTo(1);
        assertThat(stats.getResponseBodyMatches()).isEqualTo(1);
        assertThat(stats.getOverallMatches()).isEqualTo(1);
    }

    @Test
    void testStatsOneMismatch() {
        Incident incident = createIncidentWithRequest(200, "{\"success\":true}");
        String reqId = incident.getRequests().get(0).getId();
        createReplayResult(incident.getId(), reqId, 500, "{}", Instant.now());

        ResponseEntity<ReplayStatistics> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/requests/" + reqId + "/replay-stats", ReplayStatistics.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ReplayStatistics stats = response.getBody();
        assertThat(stats.getTotalReplays()).isEqualTo(1);
        assertThat(stats.getStatusCodeMismatches()).isEqualTo(1);
        assertThat(stats.getResponseBodyMismatches()).isEqualTo(1);
        assertThat(stats.getOverallMismatches()).isEqualTo(1);
    }

    @Test
    void testStatsMultipleReplaysMatchesAndMismatches() {
        Incident incident = createIncidentWithRequest(200, "{\"success\":true}");
        String reqId = incident.getRequests().get(0).getId();
        
        createReplayResult(incident.getId(), reqId, 200, "{\"success\":true}", Instant.now().minus(3, ChronoUnit.MINUTES)); // match
        createReplayResult(incident.getId(), reqId, 500, "{}", Instant.now().minus(2, ChronoUnit.MINUTES)); // mismatch (both)
        createReplayResult(incident.getId(), reqId, 200, "{}", Instant.now().minus(1, ChronoUnit.MINUTES)); // match status, mismatch body
        createReplayResult(incident.getId(), reqId, 404, "{\"success\":true}", Instant.now()); // match body, mismatch status

        ResponseEntity<ReplayStatistics> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/requests/" + reqId + "/replay-stats", ReplayStatistics.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ReplayStatistics stats = response.getBody();
        assertThat(stats.getTotalReplays()).isEqualTo(4);
        
        assertThat(stats.getStatusCodeMatches()).isEqualTo(2); // 1st, 3rd
        assertThat(stats.getStatusCodeMismatches()).isEqualTo(2); // 2nd, 4th
        
        assertThat(stats.getResponseBodyMatches()).isEqualTo(2); // 1st, 4th
        assertThat(stats.getResponseBodyMismatches()).isEqualTo(2); // 2nd, 3rd
        
        assertThat(stats.getOverallMatches()).isEqualTo(1); // 1st only
        assertThat(stats.getOverallMismatches()).isEqualTo(3); // 2nd, 3rd, 4th
        
        assertThat(stats.getLatestReplayAt()).isNotNull();
    }

    @Test
    void testStatsMissingOriginalResponse() {
        Incident incident = createIncidentWithRequest(null, null);
        String reqId = incident.getRequests().get(0).getId();
        
        createReplayResult(incident.getId(), reqId, 200, "{}", Instant.now());
        createReplayResult(incident.getId(), reqId, 404, "error", Instant.now());

        ResponseEntity<ReplayStatistics> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/requests/" + reqId + "/replay-stats", ReplayStatistics.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ReplayStatistics stats = response.getBody();
        
        // Count replays
        assertThat(stats.getTotalReplays()).isEqualTo(2);
        
        // Everything comparison-related should be zero
        assertThat(stats.getStatusCodeMatches()).isEqualTo(0);
        assertThat(stats.getStatusCodeMismatches()).isEqualTo(0);
        assertThat(stats.getResponseBodyMatches()).isEqualTo(0);
        assertThat(stats.getResponseBodyMismatches()).isEqualTo(0);
        assertThat(stats.getOverallMatches()).isEqualTo(0);
        assertThat(stats.getOverallMismatches()).isEqualTo(0);
    }

    @Test
    void testStatsInvalidIncident() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/invalid/requests/req-id/replay-stats", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testStatsInvalidRequest() {
        Incident incident = createIncidentWithRequest(200, "{}");
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/requests/invalid/replay-stats", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
