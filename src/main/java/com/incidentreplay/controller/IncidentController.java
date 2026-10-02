package com.incidentreplay.controller;

import com.incidentreplay.model.Incident;
import com.incidentreplay.model.Severity;
import com.incidentreplay.model.CapturedRequest;
import com.incidentreplay.service.IncidentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping
    public ResponseEntity<Incident> createIncident(@Valid @RequestBody CreateIncidentRequest request) {
        Incident incident = new Incident();
        incident.setTitle(request.getTitle());
        incident.setDescription(request.getDescription());
        incident.setServiceName(request.getServiceName());
        incident.setSeverity(request.getSeverity());

        Incident created = incidentService.createIncident(incident);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Incident>> getAllIncidents() {
        List<Incident> incidents = incidentService.getAllIncidents();
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Incident> getIncidentById(@PathVariable String id) {
        Optional<Incident> incidentOpt = incidentService.getIncidentById(id);
        return incidentOpt.map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));
    }

    @PostMapping("/{incidentId}/requests")
    public ResponseEntity<Incident> addCapturedRequest(
            @PathVariable String incidentId, 
            @Valid @RequestBody AddCapturedRequestDto requestDto) {
        
        CapturedRequest request = new CapturedRequest();
        request.setMethod(requestDto.getMethod());
        request.setUrl(requestDto.getUrl());
        request.setHeaders(requestDto.getHeaders());
        request.setBody(requestDto.getBody());
        request.setOriginalStatusCode(requestDto.getOriginalStatusCode());
        request.setOriginalResponseBody(requestDto.getOriginalResponseBody());

        Optional<Incident> updatedIncident = incidentService.addCapturedRequest(incidentId, request);
        return updatedIncident.map(incident -> new ResponseEntity<>(incident, HttpStatus.CREATED))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));
    }

    @GetMapping("/{incidentId}/requests")
    public ResponseEntity<List<CapturedRequest>> getCapturedRequests(@PathVariable String incidentId) {
        Optional<Incident> incidentOpt = incidentService.getIncidentById(incidentId);
        return incidentOpt.map(incident -> ResponseEntity.ok(incident.getRequests()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));
    }

    // DTO for incoming request to encapsulate validation
    public static class CreateIncidentRequest {
        @NotBlank(message = "Title must not be empty")
        private String title;

        private String description;

        @NotBlank(message = "Service name must not be empty")
        private String serviceName;

        @NotNull(message = "Severity must not be null")
        private Severity severity;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
        
        public Severity getSeverity() { return severity; }
        public void setSeverity(Severity severity) { this.severity = severity; }
    }

    public static class AddCapturedRequestDto {
        @NotBlank(message = "Method must not be empty")
        private String method;

        @NotBlank(message = "URL must not be empty")
        private String url;

        private java.util.Map<String, String> headers;
        private String body;
        private Integer originalStatusCode;
        private String originalResponseBody;

        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public java.util.Map<String, String> getHeaders() { return headers; }
        public void setHeaders(java.util.Map<String, String> headers) { this.headers = headers; }
        
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
        
        public Integer getOriginalStatusCode() { return originalStatusCode; }
        public void setOriginalStatusCode(Integer originalStatusCode) { this.originalStatusCode = originalStatusCode; }
        
        public String getOriginalResponseBody() { return originalResponseBody; }
        public void setOriginalResponseBody(String originalResponseBody) { this.originalResponseBody = originalResponseBody; }
    }
}
