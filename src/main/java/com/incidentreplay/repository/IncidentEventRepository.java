package com.incidentreplay.repository;

import com.incidentreplay.model.IncidentEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface IncidentEventRepository extends MongoRepository<IncidentEvent, String> {
    List<IncidentEvent> findByIncidentIdOrderByTimestampAsc(String incidentId);
}
