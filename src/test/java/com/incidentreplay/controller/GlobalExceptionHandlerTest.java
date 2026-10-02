package com.incidentreplay.controller;

import com.incidentreplay.model.ApiErrorResponse;
import com.incidentreplay.model.Incident;
import com.incidentreplay.model.Severity;
import com.incidentreplay.repository.IncidentRepository;
import com.incidentreplay.repository.ReplayResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GlobalExceptionHandlerTest {

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

    @Test
    void testInvalidIncidentReturns404WithStandardizedResponse() {
        ResponseEntity<ApiErrorResponse> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/invalid-id", ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        
        ApiErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(404);
        assertThat(body.getError()).isEqualTo("NOT_FOUND");
        assertThat(body.getMessage()).isEqualTo("Incident not found");
        assertThat(body.getPath()).isEqualTo("/api/incidents/invalid-id");
        assertThat(body.getTimestamp()).isNotNull();
    }

    @Test
    void testInvalidRequestReturns404WithStandardizedResponse() {
        Incident incident = new Incident();
        incident.setTitle("Test");
        incident = incidentRepository.save(incident);

        ResponseEntity<ApiErrorResponse> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/requests/invalid-req/replays", ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        
        ApiErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(404);
        assertThat(body.getError()).isEqualTo("NOT_FOUND");
        assertThat(body.getMessage()).isEqualTo("Request not found");
        assertThat(body.getPath()).contains("/requests/invalid-req/replays");
        assertThat(body.getTimestamp()).isNotNull();
    }

    @Test
    void testInvalidContextReturns404WithStandardizedResponse() {
        Incident incident = new Incident();
        incident.setTitle("Test");
        incident = incidentRepository.save(incident);

        ResponseEntity<ApiErrorResponse> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/context", ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        
        ApiErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(404);
        assertThat(body.getError()).isEqualTo("NOT_FOUND");
        assertThat(body.getMessage()).isEqualTo("Incident context not found");
        assertThat(body.getPath()).contains("/context");
        assertThat(body.getTimestamp()).isNotNull();
    }

    @Test
    void testInvalidRequestPayloadReturns400WithStandardizedResponse() {
        // Missing title, severity etc
        String invalidPayload = "{\"description\":\"only desc\"}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(invalidPayload, headers);

        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents", request, ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        
        ApiErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(400);
        assertThat(body.getError()).isEqualTo("BAD_REQUEST");
        assertThat(body.getMessage()).isNotNull(); // Will be a validation message like "Title must not be empty"
        assertThat(body.getPath()).isEqualTo("/api/incidents");
        assertThat(body.getTimestamp()).isNotNull();
    }

    @Test
    void testReplayRelatedFailureReturnsStandardizedResponse() {
        Incident incident = new Incident();
        incident.setTitle("Test");
        incident = incidentRepository.save(incident);

        // Try to compare a non-existent request
        ResponseEntity<ApiErrorResponse> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/incidents/" + incident.getId() + "/requests/invalid-id/compare", ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        
        ApiErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(404);
        assertThat(body.getError()).isEqualTo("NOT_FOUND");
        assertThat(body.getMessage()).isEqualTo("Request not found");
        assertThat(body.getPath()).contains("/compare");
        assertThat(body.getTimestamp()).isNotNull();
    }

    @Test
    void testSuccessfulEndpointRemainsUnaffected() {
        String validPayload = "{\"title\":\"Success\",\"serviceName\":\"test\",\"severity\":\"HIGH\"}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(validPayload, headers);

        ResponseEntity<Incident> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incidents", request, Incident.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Incident incident = response.getBody();
        assertThat(incident).isNotNull();
        assertThat(incident.getTitle()).isEqualTo("Success");
    }
}
