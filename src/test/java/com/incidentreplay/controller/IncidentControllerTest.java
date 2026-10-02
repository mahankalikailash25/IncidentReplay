package com.incidentreplay.controller;

import com.incidentreplay.model.CapturedRequest;
import com.incidentreplay.model.Incident;
import com.incidentreplay.model.Severity;
import com.incidentreplay.repository.IncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class IncidentControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private IncidentRepository incidentRepository;

    @BeforeEach
    void setUp() {
        incidentRepository.deleteAll();
    }

    private String createIncident() {
        IncidentController.CreateIncidentRequest req = new IncidentController.CreateIncidentRequest();
        req.setTitle("Test Incident");
        req.setServiceName("test-service");
        req.setSeverity(Severity.HIGH);

        ResponseEntity<Incident> createResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents", req, Incident.class);
        return createResponse.getBody().getId();
    }

    @Test
    void testCaptureRequestWithOriginalResponse() {
        String incidentId = createIncident();

        IncidentController.AddCapturedRequestDto captureReq = new IncidentController.AddCapturedRequestDto();
        captureReq.setMethod("POST");
        captureReq.setUrl("/api/test-capture-response");
        captureReq.setBody("test-body");
        captureReq.setOriginalStatusCode(201);
        captureReq.setOriginalResponseBody("{\"status\":\"created\"}");

        ResponseEntity<Incident> captureResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents/" + incidentId + "/requests", captureReq, Incident.class);

        assertThat(captureResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Incident postReturnedIncident = captureResponse.getBody();
        assertThat(postReturnedIncident).isNotNull();
        CapturedRequest postReturnedReq = postReturnedIncident.getRequests().get(0);
        assertThat(postReturnedReq.getOriginalStatusCode()).isEqualTo(201);
        assertThat(postReturnedReq.getOriginalResponseBody()).isEqualTo("{\"status\":\"created\"}");

        // Also hit the direct GET /requests endpoint as requested
        ResponseEntity<CapturedRequest[]> getRequestsResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + incidentId + "/requests", CapturedRequest[].class);

        assertThat(getRequestsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        CapturedRequest[] requests = getRequestsResponse.getBody();
        assertThat(requests).hasSize(1);
        CapturedRequest savedReq = requests[0];
        assertThat(savedReq.getMethod()).isEqualTo("POST");
        assertThat(savedReq.getUrl()).isEqualTo("/api/test-capture-response");
        assertThat(savedReq.getOriginalStatusCode()).isEqualTo(201);
        assertThat(savedReq.getOriginalResponseBody()).isEqualTo("{\"status\":\"created\"}");
    }

    @Test
    void testCaptureRequestWithoutOriginalResponse() {
        String incidentId = createIncident();

        IncidentController.AddCapturedRequestDto captureReq = new IncidentController.AddCapturedRequestDto();
        captureReq.setMethod("GET");
        captureReq.setUrl("/api/test-no-response");

        ResponseEntity<Incident> captureResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents/" + incidentId + "/requests", captureReq, Incident.class);

        assertThat(captureResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Incident incident = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + incidentId, Incident.class).getBody();

        assertThat(incident.getRequests()).hasSize(1);
        CapturedRequest savedReq = incident.getRequests().get(0);
        assertThat(savedReq.getMethod()).isEqualTo("GET");
        assertThat(savedReq.getUrl()).isEqualTo("/api/test-no-response");
        assertThat(savedReq.getOriginalStatusCode()).isNull();
        assertThat(savedReq.getOriginalResponseBody()).isNull();
    }
}
