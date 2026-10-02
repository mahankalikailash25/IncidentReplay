package com.incidentreplay.repository;

import com.incidentreplay.model.ReplayResult;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReplayResultRepository extends MongoRepository<ReplayResult, String> {
    List<ReplayResult> findByIncidentId(String incidentId);
    List<ReplayResult> findByIncidentIdAndRequestIdOrderByReplayedAtDesc(String incidentId, String requestId);
    ReplayResult findFirstByIncidentIdAndRequestIdOrderByReplayedAtDesc(String incidentId, String requestId);
    List<ReplayResult> findBySessionIdOrderByReplayedAtAsc(String sessionId);
}
