import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ChevronLeft, AlertTriangle, Play, Server, Clock, ServerCrash, Plus, ChevronDown, ChevronRight, History, List } from 'lucide-react';
import { Card, CardBody } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { IncidentService } from '../lib/api';
import { ContextForm } from '../components/ContextForm';
import { CaptureRequestForm } from '../components/CaptureRequestForm';
import { ReplayResultCard } from '../components/ReplayResultCard';
import { BatchReplaySummary } from '../components/BatchReplaySummary';
import { IncidentTimeline } from '../components/IncidentTimeline';
import { useCursorGlow } from '../hooks/useCursorGlow';

function RequestCard({ incidentId, request }) {
  const [expanded, setExpanded] = useState(false);
  const [replaying, setReplaying] = useState(false);
  const [replayHistory, setReplayHistory] = useState([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [showHistory, setShowHistory] = useState(false);
  
  const getMethodColor = (method) => {
    switch (method) {
      case 'GET': return 'var(--accent-primary)';
      case 'POST': return 'var(--accent-success)';
      case 'DELETE': return 'var(--accent-error)';
      case 'PUT':
      case 'PATCH': return 'var(--accent-warning)';
      default: return 'var(--text-primary)';
    }
  };

  const handleReplay = async (e) => {
    e.stopPropagation();
    if (replaying) return;
    
    setReplaying(true);
    try {
      const result = await IncidentService.replayRequest(incidentId, request.id);
      // Immediately unshift into history
      setReplayHistory(prev => [result, ...prev]);
      setShowHistory(true);
      if (!expanded) setExpanded(true);
    } catch (err) {
      console.error(err);
      // Handle global error visually or let result card show failure?
      // ReplayResultCard expects a structured object.
      // If the backend threw a 500, we can synthesize a failure card:
      setReplayHistory(prev => [{
        id: 'error-' + Date.now(),
        statusCode: 0,
        responseBody: err.message || 'Network error or target unavailable',
        replayedAt: new Date().toISOString(),
        executionTimeMs: 0,
        targetUrl: 'Unknown'
      }, ...prev]);
      setShowHistory(true);
      if (!expanded) setExpanded(true);
    } finally {
      setReplaying(false);
    }
  };

  const loadHistory = async (e) => {
    e.stopPropagation();
    if (showHistory) {
      setShowHistory(false);
      return;
    }
    
    setHistoryLoading(true);
    try {
      const history = await IncidentService.getRequestReplays(incidentId, request.id);
      setReplayHistory(history || []);
      setShowHistory(true);
      if (!expanded) setExpanded(true);
    } catch (err) {
      console.error(err);
    } finally {
      setHistoryLoading(false);
    }
  };

  const glowRef = useCursorGlow();

  return (
    <div ref={glowRef} className="surface interactive mb-4 cursor-glow-wrapper p-0" style={{ borderColor: 'var(--border-color)', backgroundColor: 'var(--bg-surface)' }}>
      <div 
        role="button"
        tabIndex={0}
        className="flex gap-4 p-4 items-center cursor-pointer select-none flex-wrap"
        onClick={() => setExpanded(!expanded)}
        onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') setExpanded(!expanded); }}
        style={{ borderBottom: expanded ? '1px solid var(--border-color)' : 'none' }}
      >
        {expanded ? <ChevronDown size={18} /> : <ChevronRight size={18} />}
        <span style={{ color: getMethodColor(request.method), fontWeight: 'bold' }}>{request.method}</span>
        <span className="flex-1 truncate" style={{ fontFamily: 'monospace' }}>{request.url}</span>
        <Badge variant={request.originalStatusCode >= 400 ? 'error' : 'success'}>
          {request.originalStatusCode}
        </Badge>
        
        <div className="flex gap-2 ml-auto" onClick={e => e.stopPropagation()}>
          <Button size="sm" variant="ghost" icon={History} onClick={loadHistory} disabled={historyLoading}>
            {showHistory ? 'Hide History' : 'History'}
          </Button>
          <Button size="sm" variant="secondary" icon={Play} onClick={handleReplay} loading={replaying}>
            Replay
          </Button>
        </div>
      </div>
      
      {expanded && (
        <div className="p-4" style={{ backgroundColor: 'var(--bg-base)' }}>
          {showHistory && replayHistory.length > 0 && (
            <div className="mb-6 p-4 rounded border" style={{ borderColor: 'var(--border-color)', backgroundColor: 'var(--bg-surface)' }}>
              <h4 className="m-0 mb-2">Replay History</h4>
              {replayHistory.map(res => (
                <ReplayResultCard key={res.id} result={res} />
              ))}
            </div>
          )}
          
          {showHistory && replayHistory.length === 0 && !historyLoading && (
            <div className="mb-6 p-4 rounded border text-center text-secondary text-sm" style={{ borderColor: 'var(--border-color)' }}>
              No replay history found.
            </div>
          )}

          <div className="mb-4">
            <h5 className="text-sm m-0 mb-2" style={{ color: 'var(--text-secondary)' }}>Headers</h5>
            <pre className="code-viewer">
              {JSON.stringify(request.headers, null, 2)}
            </pre>
          </div>
          
          {request.body && (
            <div className="mb-4">
              <h5 className="text-sm m-0 mb-2" style={{ color: 'var(--text-secondary)' }}>Request Body</h5>
              <pre className="code-viewer">
                {request.body}
              </pre>
            </div>
          )}
          
          {request.originalResponseBody && (
            <div>
              <h5 className="text-sm m-0 mb-2" style={{ color: 'var(--text-secondary)' }}>Original Response</h5>
              <pre className="code-viewer">
                {request.originalResponseBody}
              </pre>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export function IncidentDetail() {
  const { incidentId } = useParams();
  const navigate = useNavigate();

  const [incident, setIncident] = useState(null);
  const [context, setContext] = useState(null);
  const [requests, setRequests] = useState([]);
  
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  const [showContextForm, setShowContextForm] = useState(false);
  const [showRequestForm, setShowRequestForm] = useState(false);

  const [batchReplaying, setBatchReplaying] = useState(false);
  const [batchSummary, setBatchSummary] = useState(null);
  const [batchError, setBatchError] = useState(null);

  const fetchIncidentData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const incData = await IncidentService.getIncident(incidentId);
      setIncident(incData);
      
      try {
        const ctxData = await IncidentService.getContext(incidentId);
        setContext(ctxData);
      } catch (e) {
        console.error(e);
        setContext(null);
      }

      try {
        const reqData = await IncidentService.getRequests(incidentId);
        setRequests(reqData || []);
      } catch (e) {
        console.error(e);
        setRequests([]);
      }
      
    } catch (err) {
      setError(err.message || 'Failed to load incident details');
    } finally {
      setLoading(false);
    }
  }, [incidentId]);

  useEffect(() => {
    fetchIncidentData();
  }, [fetchIncidentData]);

  const reloadContext = async () => {
    try {
      const ctxData = await IncidentService.getContext(incidentId);
      setContext(ctxData);
    } catch(e) {
      console.error(e);
    }
  };

  const reloadRequests = async () => {
    try {
      const reqData = await IncidentService.getRequests(incidentId);
      setRequests(reqData || []);
    } catch(e) {
      console.error(e);
    }
  };

  const handleBatchReplay = async () => {
    if (batchReplaying) return;
    setBatchReplaying(true);
    setBatchSummary(null);
    setBatchError(null);
    try {
      const summary = await IncidentService.replayIncident(incidentId);
      setBatchSummary(summary);
    } catch (err) {
      console.error(err);
      setBatchError(err.message || 'Batch replay failed');
    } finally {
      setBatchReplaying(false);
    }
  };

  if (loading) {
    return (
      <div className="layout-container py-8">
        <div className="skeleton mb-8" style={{ height: '40px', width: '200px' }}></div>
        <div className="skeleton mb-6" style={{ height: '120px' }}></div>
        <div className="skeleton mb-6" style={{ height: '200px' }}></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="layout-container py-12 flex-center flex-col gap-4 text-center">
        <AlertTriangle size={48} style={{ color: 'var(--accent-error)' }} />
        <h2 className="m-0">{error}</h2>
        <div className="flex gap-4">
          <Button variant="ghost" icon={ChevronLeft} onClick={() => navigate('/incidents')}>Back to Dashboard</Button>
          <Button variant="secondary" onClick={fetchIncidentData}>Retry</Button>
        </div>
      </div>
    );
  }

  return (
    <>
      <div className="layout-container animate-page-enter py-8">
        <div role="button" tabIndex={0} className="nav-link mb-6 flex gap-2 items-center text-sm cursor-pointer inline-flex" style={{ color: 'var(--text-secondary)' }} onClick={() => navigate('/incidents')}>
          <ChevronLeft size={16} /> Back to Incidents
        </div>

        <header className="flex-between mb-8 pb-4" style={{ borderBottom: '1px solid var(--border-color)' }}>
          <div>
            <div className="flex items-center gap-3 mb-2">
              <h1 className="text-2xl m-0">{incident.title}</h1>
              <Badge variant={incident.status === 'RESOLVED' ? 'success' : 'error'}>{incident.status}</Badge>
              <Badge variant={incident.severity === 'CRITICAL' ? 'error' : 'neutral'}>{incident.severity}</Badge>
            </div>
            <div className="flex items-center gap-4 text-sm" style={{ color: 'var(--text-secondary)' }}>
              <span className="flex items-center gap-1"><Server size={14}/> {incident.serviceName}</span>
              <span className="flex items-center gap-1"><Clock size={14}/> {new Date(incident.createdAt).toLocaleString()}</span>
              <span>ID: <span style={{ fontFamily: 'monospace' }}>{incident.id}</span></span>
            </div>
          </div>
          <div className="flex gap-4">
            <Button variant="secondary" icon={List} onClick={() => navigate(`/incidents/${incidentId}/replay-sessions`)}>Replay Sessions</Button>
            <Button 
              variant="primary" 
              icon={Play} 
              onClick={handleBatchReplay} 
              loading={batchReplaying}
              disabled={requests.length === 0}
            >
              Replay Incident
            </Button>
          </div>
        </header>

        {batchError && (
          <div className="mb-8 p-4 rounded border badge-error flex items-center gap-3">
            <AlertTriangle size={20} />
            {batchError}
          </div>
        )}

        {batchSummary && (
          <BatchReplaySummary incidentId={incidentId} summary={batchSummary} />
        )}

        {incident.description && (
          <div className="mb-8 mt-4 text-secondary">
            {incident.description}
          </div>
        )}

        <div className="grid grid-cols-1 gap-8 animate-slide-up">
          {/* Context Section */}
          <section>
            <div className="flex-between mb-4">
              <h3 className="m-0 flex items-center gap-2"><ServerCrash size={18} /> Incident Context</h3>
              {!showContextForm && (
                <Button variant="ghost" size="sm" onClick={() => setShowContextForm(true)}>
                  {context ? 'Edit Context' : 'Capture Context'}
                </Button>
              )}
            </div>
            
            {showContextForm ? (
              <ContextForm 
                incidentId={incidentId} 
                existingContext={context} 
                onSuccess={() => { setShowContextForm(false); reloadContext(); }} 
                onCancel={() => setShowContextForm(false)} 
              />
            ) : context ? (
              <Card interactive={false}>
                <CardBody className="grid grid-cols-2 gap-4 p-4">
                  <div>
                    <div className="text-xs mb-1" style={{ color: 'var(--text-tertiary)' }}>Environment</div>
                    <div className="font-semibold">{context.environment}</div>
                  </div>
                  <div>
                    <div className="text-xs mb-1" style={{ color: 'var(--text-tertiary)' }}>Service Version</div>
                    <div className="font-semibold" style={{ fontFamily: 'monospace' }}>{context.serviceVersion}</div>
                  </div>
                  <div className="col-span-2">
                    <div className="text-xs mb-1" style={{ color: 'var(--text-tertiary)' }}>Error Message</div>
                    <div className="text-error" style={{ color: 'var(--accent-error)' }}>{context.errorMessage}</div>
                  </div>
                </CardBody>
              </Card>
            ) : (
              <div className="p-6 text-center border rounded" style={{ borderColor: 'var(--border-color)', backgroundColor: 'rgba(0,0,0,0.1)' }}>
                <p className="m-0 mb-4" style={{ color: 'var(--text-secondary)' }}>No incident context captured yet.</p>
                <Button variant="secondary" onClick={() => setShowContextForm(true)}>Capture Snapshot</Button>
              </div>
            )}
          </section>

          {/* Requests Section */}
          <section>
            <div className="flex-between mb-4">
              <h3 className="m-0">Captured Requests ({requests.length})</h3>
              {!showRequestForm && (
                <Button variant="secondary" size="sm" icon={Plus} onClick={() => setShowRequestForm(true)}>
                  Capture Request
                </Button>
              )}
            </div>

            {showRequestForm && (
              <CaptureRequestForm 
                incidentId={incidentId}
                onSuccess={() => { setShowRequestForm(false); reloadRequests(); }}
                onCancel={() => setShowRequestForm(false)}
              />
            )}

            <div className="mt-4">
              {requests.length === 0 && !showRequestForm ? (
                <div className="p-8 text-center border rounded" style={{ borderColor: 'var(--border-color)', backgroundColor: 'rgba(0,0,0,0.1)' }}>
                  <p className="m-0 mb-4" style={{ color: 'var(--text-secondary)' }}>No requests captured yet.</p>
                  <Button variant="secondary" icon={Plus} onClick={() => setShowRequestForm(true)}>Capture First Request</Button>
                </div>
              ) : (
                <div className="flex-col">
                  {requests.map((req, idx) => (
                    <RequestCard key={req.id || idx} request={req} incidentId={incidentId} />
                  ))}
                </div>
              )}
            </div>
          </section>

          {/* Timeline Section */}
          <section>
            <h3 className="m-0 mb-4">Event Timeline</h3>
            <div className="p-4 rounded border" style={{ borderColor: 'var(--border-color)', backgroundColor: 'var(--bg-base)' }}>
              <IncidentTimeline incidentId={incidentId} />
            </div>
          </section>
        </div>
      </div>
    </>
  );
}
