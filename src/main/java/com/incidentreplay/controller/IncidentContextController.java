package com.incidentreplay.controller;

import com.incidentreplay.dto.CreateIncidentContextRequest;
import com.incidentreplay.model.IncidentContext;
import com.incidentreplay.service.IncidentContextService;
import com.incidentreplay.service.IncidentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@RestController
@RequestMapping("/api/incidents")
public class IncidentContextController {

    private final IncidentContextService incidentContextService;
    private final IncidentService incidentService;

    public IncidentContextController(IncidentContextService incidentContextService, IncidentService incidentService) {
        this.incidentContextService = incidentContextService;
        this.incidentService = incidentService;
    }

    @PostMapping("/{incidentId}/context")
    public ResponseEntity<IncidentContext> createOrUpdateContext(
            @PathVariable String incidentId,
            @Valid @RequestBody CreateIncidentContextRequest request) {

        IncidentContext contextData = new IncidentContext();
        contextData.setAffectedService(request.getAffectedService());
        contextData.setEnvironment(request.getEnvironment());
        contextData.setServiceVersion(request.getServiceVersion());
        contextData.setErrorMessage(request.getErrorMessage());
        contextData.setStackTrace(request.getStackTrace());
        contextData.setMetadata(request.getMetadata());

        Optional<IncidentContext> savedContextOpt = incidentContextService.createOrUpdateIncidentContext(incidentId, contextData);

        return savedContextOpt.map(context -> new ResponseEntity<>(context, HttpStatus.CREATED))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));
    }

    @GetMapping("/{incidentId}/context")
    public ResponseEntity<IncidentContext> getContext(@PathVariable String incidentId) {
        // We can optionally check if incident exists here, or rely on service
        if (incidentService.getIncidentById(incidentId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found");
        }

        Optional<IncidentContext> contextOpt = incidentContextService.getIncidentContext(incidentId);

        return contextOpt.map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident context not found"));
    }
}
