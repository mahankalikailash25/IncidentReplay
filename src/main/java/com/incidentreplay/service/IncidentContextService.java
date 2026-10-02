package com.incidentreplay.service;

import com.incidentreplay.model.Incident;
import com.incidentreplay.model.IncidentContext;
import com.incidentreplay.repository.IncidentContextRepository;
import com.incidentreplay.repository.IncidentRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class IncidentContextService {

    private final IncidentContextRepository incidentContextRepository;
    private final IncidentRepository incidentRepository;
    private final IncidentEventService incidentEventService;

    public IncidentContextService(IncidentContextRepository incidentContextRepository, IncidentRepository incidentRepository, IncidentEventService incidentEventService) {
        this.incidentContextRepository = incidentContextRepository;
        this.incidentRepository = incidentRepository;
        this.incidentEventService = incidentEventService;
    }

    public Optional<IncidentContext> createOrUpdateIncidentContext(String incidentId, IncidentContext contextData) {
        Optional<Incident> incidentOpt = incidentRepository.findById(incidentId);
        if (incidentOpt.isEmpty()) {
            return Optional.empty();
        }

        Optional<IncidentContext> existingContextOpt = incidentContextRepository.findByIncidentId(incidentId);
        IncidentContext contextToSave;

        if (existingContextOpt.isPresent()) {
            contextToSave = existingContextOpt.get();
            // Update fields
            contextToSave.setAffectedService(contextData.getAffectedService());
            contextToSave.setEnvironment(contextData.getEnvironment());
            contextToSave.setServiceVersion(contextData.getServiceVersion());
            contextToSave.setErrorMessage(contextData.getErrorMessage());
            contextToSave.setStackTrace(contextData.getStackTrace());
            contextToSave.setMetadata(contextData.getMetadata());
            contextToSave.setCapturedAt(Instant.now());
        } else {
            contextToSave = contextData;
            contextToSave.setIncidentId(incidentId);
            contextToSave.setCapturedAt(Instant.now());
        }

        IncidentContext savedContext = incidentContextRepository.save(contextToSave);
        incidentEventService.createEvent(incidentId, com.incidentreplay.model.EventType.CONTEXT_CAPTURED, "Incident context was captured");
        
        return Optional.of(savedContext);
    }

    public Optional<IncidentContext> getIncidentContext(String incidentId) {
        Optional<Incident> incidentOpt = incidentRepository.findById(incidentId);
        if (incidentOpt.isEmpty()) {
            // Differentiating between Incident not found and Context not found isn't strictly requested to have different codes, both return 404, but doing it via Optional is clean.
            // Wait, if incident exists but context doesn't, we return Optional.empty() as well. The controller will handle mapping to 404.
            // But wait, what if incident doesn't exist? Also 404.
            // However, we can just call findByIncidentId. If incident doesn't exist, it naturally returns empty.
            // But the instructions say: "If the incident does not exist: 404 Not Found. If the incident exists but has no context: return 404 Not Found for now."
            // So we can just find by incidentId.
            return incidentContextRepository.findByIncidentId(incidentId);
        }
        return incidentContextRepository.findByIncidentId(incidentId);
    }
}
