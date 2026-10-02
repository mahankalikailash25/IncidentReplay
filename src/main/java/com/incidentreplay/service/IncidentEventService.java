package com.incidentreplay.service;

import com.incidentreplay.model.EventType;
import com.incidentreplay.model.IncidentEvent;
import com.incidentreplay.repository.IncidentEventRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class IncidentEventService {

    private final IncidentEventRepository incidentEventRepository;

    public IncidentEventService(IncidentEventRepository incidentEventRepository) {
        this.incidentEventRepository = incidentEventRepository;
    }

    public IncidentEvent createEvent(String incidentId, EventType eventType, String description) {
        IncidentEvent event = new IncidentEvent(incidentId, eventType, description, Instant.now());
        return incidentEventRepository.save(event);
    }
}
