package com.incidentreplay.controller;

import com.incidentreplay.model.EventType;
import com.incidentreplay.model.Incident;
import com.incidentreplay.model.IncidentEvent;
import com.incidentreplay.model.Severity;
import com.incidentreplay.repository.IncidentEventRepository;
import com.incidentreplay.repository.IncidentRepository;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import com.incidentreplay.service.ReplayService;
import com.incidentreplay.model.ReplayResult;
import com.incidentreplay.config.ReplayProperties;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class IncidentEventControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private IncidentEventRepository incidentEventRepository;

    @Autowired
    private ReplayService replayService;

    @Autowired
    private ReplayProperties replayProperties;

    @BeforeEach
    void setUp() {
        incidentRepository.deleteAll();
        incidentEventRepository.deleteAll();
        replayProperties.setTargetBaseUrl("http://localhost:" + port);
    }

    @Test
    void testCreateIncidentGeneratesEvent() {
        IncidentController.CreateIncidentRequest request = new IncidentController.CreateIncidentRequest();
        request.setTitle("Event Test Incident");
        request.setServiceName("test-service");
        request.setSeverity(Severity.MEDIUM);

        ResponseEntity<Incident> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents", request, Incident.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Incident createdIncident = response.getBody();
        assertThat(createdIncident).isNotNull();

        // Verify event was created
        List<IncidentEvent> events = incidentEventRepository.findByIncidentIdOrderByTimestampAsc(createdIncident.getId());
        assertThat(events).hasSize(1);
        
        IncidentEvent event = events.get(0);
        assertThat(event.getEventType()).isEqualTo(EventType.INCIDENT_CREATED);
        assertThat(event.getDescription()).isEqualTo("Incident created");
        assertThat(event.getIncidentId()).isEqualTo(createdIncident.getId());
        assertThat(event.getTimestamp()).isNotNull();
    }

    @Test
    void testGetEventsForIncident() {
        IncidentController.CreateIncidentRequest request = new IncidentController.CreateIncidentRequest();
        request.setTitle("Event Test Incident 2");
        request.setServiceName("test-service-2");
        request.setSeverity(Severity.LOW);

        ResponseEntity<Incident> createResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents", request, Incident.class);

        String incidentId = createResponse.getBody().getId();

        ResponseEntity<List<IncidentEvent>> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/incidents/" + incidentId + "/events",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<IncidentEvent>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getEventType()).isEqualTo(EventType.INCIDENT_CREATED);
    }

    @Test
    void testGetEventsForInvalidIncidentReturns404() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/invalid-id/events", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testCreateAndUpdateContextGeneratesEvents() {
        // Create Incident
        IncidentController.CreateIncidentRequest request = new IncidentController.CreateIncidentRequest();
        request.setTitle("Context Event Test Incident");
        request.setServiceName("test-service-3");
        request.setSeverity(Severity.HIGH);

        ResponseEntity<Incident> createResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents", request, Incident.class);

        String incidentId = createResponse.getBody().getId();

        // Check initially 1 event (INCIDENT_CREATED)
        List<IncidentEvent> events = incidentEventRepository.findByIncidentIdOrderByTimestampAsc(incidentId);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getEventType()).isEqualTo(EventType.INCIDENT_CREATED);

        // Create Context
        com.incidentreplay.dto.CreateIncidentContextRequest ctxRequest = new com.incidentreplay.dto.CreateIncidentContextRequest();
        ctxRequest.setAffectedService("auth-service");
        ctxRequest.setEnvironment("prod");
        ctxRequest.setServiceVersion("1.0.0");
        ctxRequest.setErrorMessage("timeout");

        ResponseEntity<com.incidentreplay.model.IncidentContext> ctxCreateResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents/" + incidentId + "/context", ctxRequest, com.incidentreplay.model.IncidentContext.class);

        assertThat(ctxCreateResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Check 2 events now
        events = incidentEventRepository.findByIncidentIdOrderByTimestampAsc(incidentId);
        assertThat(events).hasSize(2);
        assertThat(events.get(1).getEventType()).isEqualTo(EventType.CONTEXT_CAPTURED);
        assertThat(events.get(1).getDescription()).isEqualTo("Incident context was captured");
        assertThat(events.get(1).getIncidentId()).isEqualTo(incidentId);

        // Update Context
        ctxRequest.setErrorMessage("updated timeout");
        ResponseEntity<com.incidentreplay.model.IncidentContext> ctxUpdateResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents/" + incidentId + "/context", ctxRequest, com.incidentreplay.model.IncidentContext.class);

        assertThat(ctxUpdateResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        
        // Ensure same context document is updated
        assertThat(ctxCreateResponse.getBody().getId()).isEqualTo(ctxUpdateResponse.getBody().getId());

        // Check 3 events now
        events = incidentEventRepository.findByIncidentIdOrderByTimestampAsc(incidentId);
        assertThat(events).hasSize(3);
        assertThat(events.get(2).getEventType()).isEqualTo(EventType.CONTEXT_CAPTURED);
        assertThat(events.get(2).getDescription()).isEqualTo("Incident context was captured");
        assertThat(events.get(2).getIncidentId()).isEqualTo(incidentId);
        assertThat(events.get(2).getTimestamp()).isAfter(events.get(1).getTimestamp());
    }

    @Test
    void testInvalidIncidentContextCreationDoesNotGenerateEvent() {
        com.incidentreplay.dto.CreateIncidentContextRequest ctxRequest = new com.incidentreplay.dto.CreateIncidentContextRequest();
        ctxRequest.setAffectedService("auth-service");
        ctxRequest.setEnvironment("prod");
        ctxRequest.setServiceVersion("1.0.0");
        ctxRequest.setErrorMessage("timeout");

        ResponseEntity<com.incidentreplay.model.IncidentContext> ctxCreateResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents/invalid-incident-id/context", ctxRequest, com.incidentreplay.model.IncidentContext.class);

        assertThat(ctxCreateResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        List<IncidentEvent> events = incidentEventRepository.findByIncidentIdOrderByTimestampAsc("invalid-incident-id");
        assertThat(events).isEmpty();
    }

    @Test
    void testCaptureRequestGeneratesEvent() {
        IncidentController.CreateIncidentRequest req = new IncidentController.CreateIncidentRequest();
        req.setTitle("Request Event Test Incident");
        req.setServiceName("test-service-4");
        req.setSeverity(Severity.MEDIUM);

        ResponseEntity<Incident> createResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents", req, Incident.class);

        String incidentId = createResponse.getBody().getId();

        IncidentController.AddCapturedRequestDto captureReq = new IncidentController.AddCapturedRequestDto();
        captureReq.setMethod("POST");
        captureReq.setUrl("/api/test");
        captureReq.setBody("test-body");

        ResponseEntity<Incident> captureResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents/" + incidentId + "/requests", captureReq, Incident.class);

        assertThat(captureResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        List<IncidentEvent> events = incidentEventRepository.findByIncidentIdOrderByTimestampAsc(incidentId);
        assertThat(events).hasSize(2);
        assertThat(events.get(0).getEventType()).isEqualTo(EventType.INCIDENT_CREATED);
        assertThat(events.get(1).getEventType()).isEqualTo(EventType.REQUEST_CAPTURED);
        assertThat(events.get(1).getDescription()).isEqualTo("HTTP request was captured");
        assertThat(events.get(1).getIncidentId()).isEqualTo(incidentId);
    }

    @Test
    void testCaptureMultipleRequestsGeneratesMultipleEvents() {
        IncidentController.CreateIncidentRequest req = new IncidentController.CreateIncidentRequest();
        req.setTitle("Multi Request Event Test Incident");
        req.setServiceName("test-service-5");
        req.setSeverity(Severity.LOW);

        ResponseEntity<Incident> createResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents", req, Incident.class);

        String incidentId = createResponse.getBody().getId();

        IncidentController.AddCapturedRequestDto captureReq1 = new IncidentController.AddCapturedRequestDto();
        captureReq1.setMethod("GET");
        captureReq1.setUrl("/api/test1");

        IncidentController.AddCapturedRequestDto captureReq2 = new IncidentController.AddCapturedRequestDto();
        captureReq2.setMethod("POST");
        captureReq2.setUrl("/api/test2");

        restTemplate.postForEntity("http://localhost:" + port + "/api/incidents/" + incidentId + "/requests", captureReq1, Incident.class);
        restTemplate.postForEntity("http://localhost:" + port + "/api/incidents/" + incidentId + "/requests", captureReq2, Incident.class);

        List<IncidentEvent> events = incidentEventRepository.findByIncidentIdOrderByTimestampAsc(incidentId);
        assertThat(events).hasSize(3);
        assertThat(events.get(0).getEventType()).isEqualTo(EventType.INCIDENT_CREATED);
        assertThat(events.get(1).getEventType()).isEqualTo(EventType.REQUEST_CAPTURED);
        assertThat(events.get(2).getEventType()).isEqualTo(EventType.REQUEST_CAPTURED);
    }

    @Test
    void testInvalidIncidentCaptureRequestDoesNotGenerateEvent() {
        IncidentController.AddCapturedRequestDto captureReq = new IncidentController.AddCapturedRequestDto();
        captureReq.setMethod("POST");
        captureReq.setUrl("/api/test");

        ResponseEntity<String> captureResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents/invalid-incident-id/requests", captureReq, String.class);

        assertThat(captureResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        List<IncidentEvent> events = incidentEventRepository.findByIncidentIdOrderByTimestampAsc("invalid-incident-id");
        assertThat(events).isEmpty();
    }

    @Test
    void testInvalidCaptureRequestDataDoesNotGenerateEvent() {
        IncidentController.CreateIncidentRequest req = new IncidentController.CreateIncidentRequest();
        req.setTitle("Invalid Request Data Test");
        req.setServiceName("test-service-6");
        req.setSeverity(Severity.MEDIUM);

        ResponseEntity<Incident> createResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents", req, Incident.class);

        String incidentId = createResponse.getBody().getId();

        IncidentController.AddCapturedRequestDto captureReq = new IncidentController.AddCapturedRequestDto();
        // Method is blank, which violates @NotBlank
        captureReq.setMethod("");
        captureReq.setUrl("/api/test");

        ResponseEntity<String> captureResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents/" + incidentId + "/requests", captureReq, String.class);

        assertThat(captureResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        List<IncidentEvent> events = incidentEventRepository.findByIncidentIdOrderByTimestampAsc(incidentId);
        // Only INCIDENT_CREATED should exist
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getEventType()).isEqualTo(EventType.INCIDENT_CREATED);
    }

    private String setupIncidentWithRequest(String url) {
        IncidentController.CreateIncidentRequest req = new IncidentController.CreateIncidentRequest();
        req.setTitle("Replay Event Test Incident");
        req.setServiceName("test-service-7");
        req.setSeverity(Severity.HIGH);

        ResponseEntity<Incident> createResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents", req, Incident.class);
        String incidentId = createResponse.getBody().getId();

        IncidentController.AddCapturedRequestDto captureReq = new IncidentController.AddCapturedRequestDto();
        captureReq.setMethod("POST");
        captureReq.setUrl(url);
        captureReq.setBody("test-body");

        restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents/" + incidentId + "/requests", captureReq, Incident.class);

        ResponseEntity<Incident> getResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + incidentId, Incident.class);
        
        return getResponse.getBody().getRequests().get(0).getId();
    }
    
    private String createIncident() {
        IncidentController.CreateIncidentRequest req = new IncidentController.CreateIncidentRequest();
        req.setTitle("Replay Event Test Incident");
        req.setServiceName("test-service-7");
        req.setSeverity(Severity.HIGH);

        ResponseEntity<Incident> createResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents", req, Incident.class);
        return createResponse.getBody().getId();
    }

    @Test
    void testSuccessfulReplayCreatesEvent() {
        String incidentId = createIncident();
        IncidentController.AddCapturedRequestDto captureReq = new IncidentController.AddCapturedRequestDto();
        captureReq.setMethod("POST");
        captureReq.setUrl("/api/test-target/echo");
        captureReq.setBody("test-body");

        restTemplate.postForEntity("http://localhost:" + port + "/api/incidents/" + incidentId + "/requests", captureReq, Incident.class);
        String requestId = restTemplate.getForEntity("http://localhost:" + port + "/api/incidents/" + incidentId, Incident.class).getBody().getRequests().get(0).getId();

        ResponseEntity<ReplayResult> replayResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents/" + incidentId + "/requests/" + requestId + "/replay", null, ReplayResult.class);

        assertThat(replayResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        List<IncidentEvent> events = incidentEventRepository.findByIncidentIdOrderByTimestampAsc(incidentId);
        assertThat(events).hasSize(3);
        assertThat(events.get(0).getEventType()).isEqualTo(EventType.INCIDENT_CREATED);
        assertThat(events.get(1).getEventType()).isEqualTo(EventType.REQUEST_CAPTURED);
        assertThat(events.get(2).getEventType()).isEqualTo(EventType.REQUEST_REPLAYED);
        assertThat(events.get(2).getDescription()).isEqualTo("HTTP request was replayed");
        assertThat(events.get(2).getIncidentId()).isEqualTo(incidentId);
    }

    @Test
    void testMultipleSuccessfulReplays() {
        String incidentId = createIncident();
        IncidentController.AddCapturedRequestDto captureReq = new IncidentController.AddCapturedRequestDto();
        captureReq.setMethod("POST");
        captureReq.setUrl("/api/test-target/echo");
        captureReq.setBody("test-body");

        restTemplate.postForEntity("http://localhost:" + port + "/api/incidents/" + incidentId + "/requests", captureReq, Incident.class);
        String requestId = restTemplate.getForEntity("http://localhost:" + port + "/api/incidents/" + incidentId, Incident.class).getBody().getRequests().get(0).getId();

        restTemplate.postForEntity("http://localhost:" + port + "/api/incidents/" + incidentId + "/requests/" + requestId + "/replay", null, ReplayResult.class);
        restTemplate.postForEntity("http://localhost:" + port + "/api/incidents/" + incidentId + "/requests/" + requestId + "/replay", null, ReplayResult.class);

        List<IncidentEvent> events = incidentEventRepository.findByIncidentIdOrderByTimestampAsc(incidentId);
        assertThat(events).hasSize(4);
        assertThat(events.get(0).getEventType()).isEqualTo(EventType.INCIDENT_CREATED);
        assertThat(events.get(1).getEventType()).isEqualTo(EventType.REQUEST_CAPTURED);
        assertThat(events.get(2).getEventType()).isEqualTo(EventType.REQUEST_REPLAYED);
        assertThat(events.get(3).getEventType()).isEqualTo(EventType.REQUEST_REPLAYED);
    }

    @Test
    void testReplayInvalidIncident() {
        ResponseEntity<String> replayResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents/invalid-incident/requests/invalid-request/replay", null, String.class);

        assertThat(replayResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        
        List<IncidentEvent> events = incidentEventRepository.findByIncidentIdOrderByTimestampAsc("invalid-incident");
        assertThat(events).isEmpty();
    }

    @Test
    void testReplayInvalidRequest() {
        String incidentId = createIncident();
        ResponseEntity<String> replayResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents/" + incidentId + "/requests/invalid-request/replay", null, String.class);

        assertThat(replayResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        List<IncidentEvent> events = incidentEventRepository.findByIncidentIdOrderByTimestampAsc(incidentId);
        // Only INCIDENT_CREATED
        assertThat(events).hasSize(1);
    }

    @Test
    void testReplayUnreachableTarget() {
        String incidentId = createIncident();
        IncidentController.AddCapturedRequestDto captureReq = new IncidentController.AddCapturedRequestDto();
        captureReq.setMethod("POST");
        captureReq.setUrl("/api/test-target/echo");
        captureReq.setBody("test-body");

        restTemplate.postForEntity("http://localhost:" + port + "/api/incidents/" + incidentId + "/requests", captureReq, Incident.class);
        String requestId = restTemplate.getForEntity("http://localhost:" + port + "/api/incidents/" + incidentId, Incident.class).getBody().getRequests().get(0).getId();

        // Make it unreachable
        replayProperties.setTargetBaseUrl("http://localhost:1");

        ResponseEntity<String> replayResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents/" + incidentId + "/requests/" + requestId + "/replay", null, String.class);

        assertThat(replayResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);

        List<IncidentEvent> events = incidentEventRepository.findByIncidentIdOrderByTimestampAsc(incidentId);
        // No REQUEST_REPLAYED, only INCIDENT_CREATED and REQUEST_CAPTURED
        assertThat(events).hasSize(2);
        assertThat(events.get(0).getEventType()).isEqualTo(EventType.INCIDENT_CREATED);
        assertThat(events.get(1).getEventType()).isEqualTo(EventType.REQUEST_CAPTURED);
    }

    @Test
    void testReplay404ResponseCreatesEvent() {
        String incidentId = createIncident();
        IncidentController.AddCapturedRequestDto captureReq = new IncidentController.AddCapturedRequestDto();
        captureReq.setMethod("POST");
        // /api/invalid-path doesn't exist, will return 404 from the Test Server!
        captureReq.setUrl("/api/invalid-path");

        restTemplate.postForEntity("http://localhost:" + port + "/api/incidents/" + incidentId + "/requests", captureReq, Incident.class);
        String requestId = restTemplate.getForEntity("http://localhost:" + port + "/api/incidents/" + incidentId, Incident.class).getBody().getRequests().get(0).getId();

        ResponseEntity<ReplayResult> replayResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents/" + incidentId + "/requests/" + requestId + "/replay", null, ReplayResult.class);

        // the replay mechanism returns 200 OK containing the ReplayResult with statusCode=404
        assertThat(replayResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(replayResponse.getBody().getStatusCode()).isEqualTo(404);

        List<IncidentEvent> events = incidentEventRepository.findByIncidentIdOrderByTimestampAsc(incidentId);
        // 3 events including REQUEST_REPLAYED
        assertThat(events).hasSize(3);
        assertThat(events.get(2).getEventType()).isEqualTo(EventType.REQUEST_REPLAYED);
    }
}
