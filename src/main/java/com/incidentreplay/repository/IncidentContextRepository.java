package com.incidentreplay.repository;

import com.incidentreplay.model.IncidentContext;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface IncidentContextRepository extends MongoRepository<IncidentContext, String> {
    Optional<IncidentContext> findByIncidentId(String incidentId);
}
