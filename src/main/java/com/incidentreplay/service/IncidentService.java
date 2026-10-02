package com.incidentreplay.service;

import com.incidentreplay.model.Incident;
import com.incidentreplay.model.IncidentStatus;
import com.incidentreplay.model.CapturedRequest;
import com.incidentreplay.model.EventType;
import com.incidentreplay.repository.IncidentRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentEventService incidentEventService;

    public IncidentService(IncidentRepository incidentRepository, IncidentEventService incidentEventService) {
        this.incidentRepository = incidentRepository;
        this.incidentEventService = incidentEventService;
    }

    public Incident createIncident(Incident incident) {
        incident.setStatus(IncidentStatus.OPEN);
        incident.setCreatedAt(Instant.now());
        Incident savedIncident = incidentRepository.save(incident);
        incidentEventService.createEvent(savedIncident.getId(), EventType.INCIDENT_CREATED, "Incident created");
        return savedIncident;
    }

    public List<Incident> getAllIncidents() {
        return incidentRepository.findAll();
    }

    public Optional<Incident> getIncidentById(String id) {
        return incidentRepository.findById(id);
    }

    public Optional<Incident> addCapturedRequest(String incidentId, CapturedRequest request) {
        Optional<Incident> incidentOpt = incidentRepository.findById(incidentId);
        if (incidentOpt.isPresent()) {
            Incident incident = incidentOpt.get();
            request.setId(java.util.UUID.randomUUID().toString());
            request.setCapturedAt(Instant.now());
            incident.getRequests().add(request);
            incidentRepository.save(incident);
            incidentEventService.createEvent(incident.getId(), EventType.REQUEST_CAPTURED, "HTTP request was captured");
            return Optional.of(incident);
        }
        return Optional.empty();
    }
}
