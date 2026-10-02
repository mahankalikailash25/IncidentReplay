package com.incidentreplay.controller;

import com.incidentreplay.model.IncidentEvent;
import com.incidentreplay.repository.IncidentEventRepository;
import com.incidentreplay.service.IncidentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
public class IncidentEventController {

    private final IncidentEventRepository incidentEventRepository;
    private final IncidentService incidentService;

    public IncidentEventController(IncidentEventRepository incidentEventRepository, IncidentService incidentService) {
        this.incidentEventRepository = incidentEventRepository;
        this.incidentService = incidentService;
    }

    @GetMapping("/{incidentId}/events")
    public ResponseEntity<List<IncidentEvent>> getIncidentEvents(@PathVariable String incidentId) {
        if (incidentService.getIncidentById(incidentId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found");
        }

        List<IncidentEvent> events = incidentEventRepository.findByIncidentIdOrderByTimestampAsc(incidentId);
        return ResponseEntity.ok(events);
    }
}
