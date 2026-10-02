package com.incidentreplay.controller;

import com.incidentreplay.model.ComparisonResult;
import com.incidentreplay.model.ReplayResult;
import com.incidentreplay.model.ReplayStatistics;
import com.incidentreplay.model.BatchReplaySummary;
import com.incidentreplay.model.SessionComparisonResult;
import com.incidentreplay.model.ReplaySessionSummary;
import com.incidentreplay.model.SessionComparisonDetailsResult;
import com.incidentreplay.model.ReplaySession;
import com.incidentreplay.service.ReplayService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/incidents")
public class ReplayController {

    private final ReplayService replayService;

    public ReplayController(ReplayService replayService) {
        this.replayService = replayService;
    }

    @PostMapping("/{incidentId}/requests/{requestId}/replay")
    public ResponseEntity<ReplayResult> replayRequest(
            @PathVariable String incidentId,
            @PathVariable String requestId) {
        
        ReplayResult result = replayService.replayRequest(incidentId, requestId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{incidentId}/replay")
    public ResponseEntity<BatchReplaySummary> replayAllRequests(@PathVariable String incidentId) {
        BatchReplaySummary summary = replayService.replayAllRequests(incidentId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/{incidentId}/replay-sessions")
    public ResponseEntity<List<ReplaySession>> getReplaySessions(@PathVariable String incidentId) {
        List<ReplaySession> sessions = replayService.getReplaySessions(incidentId);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/{incidentId}/replay-sessions/{sessionId}")
    public ResponseEntity<ReplaySession> getReplaySession(
            @PathVariable String incidentId,
            @PathVariable String sessionId) {
        ReplaySession session = replayService.getReplaySession(incidentId, sessionId);
        return ResponseEntity.ok(session);
    }

    @GetMapping("/{incidentId}/replay-sessions/{sessionId}/replays")
    public ResponseEntity<List<ReplayResult>> getReplaysForSession(
            @PathVariable String incidentId,
            @PathVariable String sessionId) {
        List<ReplayResult> replays = replayService.getReplaysForSession(incidentId, sessionId);
        return ResponseEntity.ok(replays);
    }

    @GetMapping("/{incidentId}/replay-sessions/{sessionId}/compare")
    public ResponseEntity<SessionComparisonResult> compareReplaySession(
            @PathVariable String incidentId,
            @PathVariable String sessionId) {
        SessionComparisonResult result = replayService.compareSession(incidentId, sessionId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{incidentId}/replay-sessions/{sessionId}/summary")
    public ResponseEntity<ReplaySessionSummary> getReplaySessionSummary(
            @PathVariable String incidentId,
            @PathVariable String sessionId) {
        ReplaySessionSummary summary = replayService.getReplaySessionSummary(incidentId, sessionId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/{incidentId}/replay-sessions/{sessionId}/comparison-details")
    public ResponseEntity<SessionComparisonDetailsResult> getReplaySessionComparisonDetails(
            @PathVariable String incidentId,
            @PathVariable String sessionId) {
        SessionComparisonDetailsResult result = replayService.getReplaySessionComparisonDetails(incidentId, sessionId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{incidentId}/replays")
    public ResponseEntity<List<ReplayResult>> getReplaysForIncident(@PathVariable String incidentId) {
        List<ReplayResult> replays = replayService.getReplaysForIncident(incidentId);
        return ResponseEntity.ok(replays);
    }

    @GetMapping("/{incidentId}/requests/{requestId}/replays")
    public ResponseEntity<List<ReplayResult>> getReplaysForRequest(
            @PathVariable String incidentId,
            @PathVariable String requestId) {
        List<ReplayResult> replays = replayService.getReplaysForRequest(incidentId, requestId);
        return ResponseEntity.ok(replays);
    }

    @GetMapping("/{incidentId}/requests/{requestId}/compare")
    public ResponseEntity<ComparisonResult> compareRequest(
            @PathVariable String incidentId,
            @PathVariable String requestId) {
        ComparisonResult comparison = replayService.compareRequest(incidentId, requestId);
        return ResponseEntity.ok(comparison);
    }

    @GetMapping("/{incidentId}/requests/{requestId}/replay-stats")
    public ResponseEntity<ReplayStatistics> getReplayStats(
            @PathVariable String incidentId,
            @PathVariable String requestId) {
        ReplayStatistics stats = replayService.getReplayStats(incidentId, requestId);
        return ResponseEntity.ok(stats);
    }
}
