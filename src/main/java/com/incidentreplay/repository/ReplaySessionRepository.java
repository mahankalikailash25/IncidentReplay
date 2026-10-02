package com.incidentreplay.repository;

import com.incidentreplay.model.ReplaySession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReplaySessionRepository extends MongoRepository<ReplaySession, String> {
    List<ReplaySession> findByIncidentIdOrderByStartedAtDesc(String incidentId);
}
