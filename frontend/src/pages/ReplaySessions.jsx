import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ChevronLeft, AlertTriangle, List, Server, Clock } from 'lucide-react';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { Card } from '../components/ui/Card';
import { IncidentService } from '../lib/api';

export function ReplaySessions() {
  const { incidentId } = useParams();
  const navigate = useNavigate();

  const [incident, setIncident] = useState(null);
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchSessions = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const incData = await IncidentService.getIncident(incidentId);
      setIncident(incData);

      const sessData = await IncidentService.getReplaySessions(incidentId);
      setSessions(sessData || []);
    } catch (err) {
      setError(err.message || 'Failed to load sessions');
    } finally {
      setLoading(false);
    }
  }, [incidentId]);

  useEffect(() => {
    fetchSessions();
  }, [fetchSessions]);

  const getStatusVariant = (status) => {
    if (status === 'COMPLETED') return 'success';
    if (status === 'COMPLETED_WITH_ERRORS') return 'warning';
    if (status === 'RUNNING') return 'primary';
    return 'neutral';
  };

  if (loading) {
    return (
      <div className="layout-container py-8">
        <div className="skeleton mb-8" style={{ height: '40px', width: '300px' }}></div>
        <div className="skeleton mb-4" style={{ height: '100px' }}></div>
        <div className="skeleton mb-4" style={{ height: '100px' }}></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="layout-container py-12 flex-center flex-col gap-4 text-center">
        <AlertTriangle size={48} style={{ color: 'var(--accent-error)' }} />
        <h2 className="m-0">{error}</h2>
        <div className="flex gap-4">
          <Button variant="ghost" icon={ChevronLeft} onClick={() => navigate(`/incidents/${incidentId}`)}>Back to Incident</Button>
          <Button variant="secondary" onClick={fetchSessions}>Retry</Button>
        </div>
      </div>
    );
  }

  return (
    <>
      <div className="layout-container animate-page-enter py-8">
        <div role="button" tabIndex={0} className="nav-link mb-6 flex gap-2 items-center text-sm cursor-pointer inline-flex" style={{ color: 'var(--text-secondary)' }} onClick={() => navigate(`/incidents/${incidentId}`)}>
          <ChevronLeft size={16} /> Back to Incident
        </div>

        <header className="mb-8 pb-4" style={{ borderBottom: '1px solid var(--border-color)' }}>
          <h1 className="text-2xl m-0 flex items-center gap-3">
            <List size={24} style={{ color: 'var(--accent-primary)' }} />
            Replay Sessions
          </h1>
          {incident && (
            <div className="mt-2 text-sm" style={{ color: 'var(--text-secondary)' }}>
              Incident: <span className="font-semibold text-primary" style={{ color: 'var(--text-primary)' }}>{incident.title}</span>
            </div>
          )}
        </header>

        <main className="animate-slide-up">
          {sessions.length === 0 ? (
            <div className="p-12 text-center border rounded" style={{ borderColor: 'var(--border-color)', backgroundColor: 'var(--bg-surface)' }}>
              <List size={48} className="mb-4" style={{ color: 'var(--text-tertiary)', margin: '0 auto' }} />
              <h3 className="m-0 mb-2">No replay sessions yet</h3>
              <p className="m-0 text-sm mb-6" style={{ color: 'var(--text-secondary)' }}>Run a replay for this incident to create a replay session.</p>
              <Button variant="secondary" onClick={() => navigate(`/incidents/${incidentId}`)}>Back to Incident</Button>
            </div>
          ) : (
            <div className="flex-col gap-4">
              {sessions.map(session => (
                  <Card 
                    key={session.id} 
                    interactive={true} 
                    role="button"
                    tabIndex={0}
                    className="p-4 cursor-pointer cursor-glow-wrapper" 
                    onClick={() => navigate(`/incidents/${incidentId}/replay-sessions/${session.id}`)}
                  >
                  <div className="flex-between flex-wrap gap-4">
                    <div className="flex-col gap-2">
                      <div className="flex items-center gap-3">
                        <span className="font-semibold" style={{ fontFamily: 'monospace' }}>{session.id.substring(0, 8)}...</span>
                        <Badge variant={getStatusVariant(session.status)}>{session.status}</Badge>
                      </div>
                      <div className="flex items-center gap-4 text-xs" style={{ color: 'var(--text-secondary)' }}>
                        <span className="flex items-center gap-1"><Clock size={12} /> {new Date(session.startedAt).toLocaleString()}</span>
                        {session.replayTargetBaseUrl && (
                          <span className="flex items-center gap-1"><Server size={12} /> {session.replayTargetBaseUrl}</span>
                        )}
                      </div>
                    </div>
                    
                    <div className="flex gap-4">
                      <div className="text-center">
                        <div className="text-xs mb-1" style={{ color: 'var(--text-tertiary)' }}>Total</div>
                        <div className="font-bold">{session.totalRequests}</div>
                      </div>
                      <div className="text-center">
                        <div className="text-xs mb-1" style={{ color: 'var(--text-tertiary)' }}>Success</div>
                        <div className="font-bold text-success" style={{ color: 'var(--accent-success)' }}>{session.successfulReplays}</div>
                      </div>
                      <div className="text-center">
                        <div className="text-xs mb-1" style={{ color: 'var(--text-tertiary)' }}>Failed</div>
                        <div className="font-bold text-error" style={{ color: 'var(--accent-error)' }}>{session.failedReplays}</div>
                      </div>
                    </div>
                  </div>
                </Card>
              ))}
            </div>
          )}
        </main>
      </div>
    </>
  );
}
