import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ChevronLeft, AlertTriangle, Server, Clock, CheckCircle, XCircle } from 'lucide-react';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { Card, CardBody } from '../components/ui/Card';
import { ReplayResultCard } from '../components/ReplayResultCard';
import { ComparisonRequestCard } from '../components/ComparisonRequestCard';
import { IncidentService } from '../lib/api';

export function SessionDetail() {
  const { incidentId, sessionId } = useParams();
  const navigate = useNavigate();

  const [incident, setIncident] = useState(null);
  const [session, setSession] = useState(null);
  const [results, setResults] = useState([]);
  const [comparisonStats, setComparisonStats] = useState(null);
  const [comparisonDetails, setComparisonDetails] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [viewMode, setViewMode] = useState('RESULTS'); // 'RESULTS' or 'COMPARISON'

  const fetchSessionData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const incData = await IncidentService.getIncident(incidentId);
      setIncident(incData);

      const sessData = await IncidentService.getReplaySession(incidentId, sessionId);
      setSession(sessData);

      const resData = await IncidentService.getSessionReplays(incidentId, sessionId);
      setResults(resData || []);

      if (sessData.status === 'COMPLETED' || sessData.status === 'COMPLETED_WITH_ERRORS') {
        try {
          const compStats = await IncidentService.getSessionComparison(incidentId, sessionId);
          setComparisonStats(compStats);
          
          const compDetailsData = await IncidentService.getSessionComparisonDetails(incidentId, sessionId);
          setComparisonDetails(compDetailsData?.details || []);
        } catch (e) {
          console.error("Comparison data not available yet or failed:", e);
        }
      }
    } catch (err) {
      setError(err.message || 'Failed to load session details');
    } finally {
      setLoading(false);
    }
  }, [incidentId, sessionId]);

  useEffect(() => {
    fetchSessionData();
  }, [fetchSessionData]);

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
        <div className="skeleton mb-6" style={{ height: '150px' }}></div>
        <div className="skeleton mb-4" style={{ height: '60px' }}></div>
        <div className="skeleton mb-4" style={{ height: '60px' }}></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="layout-container py-12 flex-center flex-col gap-4 text-center">
        <AlertTriangle size={48} style={{ color: 'var(--accent-error)' }} />
        <h2 className="m-0">{error}</h2>
        <div className="flex gap-4">
          <Button variant="ghost" icon={ChevronLeft} onClick={() => navigate(`/incidents/${incidentId}/replay-sessions`)}>Back to Sessions</Button>
          <Button variant="secondary" onClick={fetchSessionData}>Retry</Button>
        </div>
      </div>
    );
  }

  return (
    <>
      <div className="layout-container animate-page-enter py-8">
        <div className="mb-6 flex gap-4 text-sm" style={{ color: 'var(--text-secondary)' }}>
          <span role="button" tabIndex={0} className="nav-link flex items-center gap-1 cursor-pointer" onClick={() => navigate(`/incidents/${incidentId}`)}>
            <ChevronLeft size={16} /> Back to Incident
          </span>
          <span role="button" tabIndex={0} className="nav-link flex items-center gap-1 cursor-pointer" onClick={() => navigate(`/incidents/${incidentId}/replay-sessions`)}>
            <ChevronLeft size={16} /> Back to Sessions
          </span>
        </div>

        <header className="mb-6 pb-4" style={{ borderBottom: '1px solid var(--border-color)' }}>
          <div className="flex-between flex-wrap gap-4 mb-2">
            <h1 className="text-xl m-0 flex items-center gap-2">
              Session <span style={{ fontFamily: 'monospace' }}>{session.id}</span>
            </h1>
            <Badge variant={getStatusVariant(session.status)}>{session.status}</Badge>
          </div>
          {incident && (
            <div className="text-sm" style={{ color: 'var(--text-secondary)' }}>
              Incident: <span className="font-semibold text-primary" style={{ color: 'var(--text-primary)' }}>{incident.title}</span>
            </div>
          )}
        </header>

        <Card interactive={false} className="mb-8 p-4">
          <CardBody className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div>
              <div className="text-xs mb-1" style={{ color: 'var(--text-tertiary)' }}>Started</div>
              <div className="text-sm font-semibold flex items-center gap-1"><Clock size={14}/> {new Date(session.startedAt).toLocaleString()}</div>
            </div>
            {session.completedAt && (
              <div>
                <div className="text-xs mb-1" style={{ color: 'var(--text-tertiary)' }}>Completed</div>
                <div className="text-sm font-semibold">{new Date(session.completedAt).toLocaleString()}</div>
              </div>
            )}
            <div className="col-span-2">
              <div className="text-xs mb-1" style={{ color: 'var(--text-tertiary)' }}>Replay Target</div>
              <div className="text-sm font-semibold flex items-center gap-1"><Server size={14}/> {session.replayTargetBaseUrl || 'Unknown'}</div>
            </div>

            <div className="mt-4 p-3 rounded border" style={{ borderColor: 'var(--border-color)', backgroundColor: 'var(--bg-base)' }}>
              <div className="text-xs mb-1" style={{ color: 'var(--text-tertiary)' }}>Total Requests</div>
              <div className="text-xl font-bold">{session.totalRequests}</div>
            </div>
            <div className="mt-4 p-3 rounded border" style={{ borderColor: 'var(--border-color)', backgroundColor: 'var(--bg-base)' }}>
              <div className="text-xs mb-1" style={{ color: 'var(--text-tertiary)' }}>Successful</div>
              <div className="text-xl font-bold flex items-center gap-2" style={{ color: 'var(--accent-success)' }}>
                <CheckCircle size={18} /> {session.successfulReplays}
              </div>
            </div>
            <div className="mt-4 p-3 rounded border" style={{ borderColor: 'var(--border-color)', backgroundColor: 'var(--bg-base)' }}>
              <div className="text-xs mb-1" style={{ color: 'var(--text-tertiary)' }}>Failed</div>
              <div className="text-xl font-bold flex items-center gap-2" style={{ color: 'var(--accent-error)' }}>
                <XCircle size={18} /> {session.failedReplays}
              </div>
            </div>
          </CardBody>
        </Card>

        {comparisonStats && (
          <Card interactive={false} className="mb-8 p-4 border cursor-glow-wrapper" style={{ borderColor: 'var(--accent-primary)' }}>
            <h3 className="m-0 mb-4 text-primary" style={{ color: 'var(--accent-primary)' }}>Comparison Summary</h3>
            <CardBody className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div>
                <div className="text-xs mb-1" style={{ color: 'var(--text-tertiary)' }}>Comparable Requests</div>
                <div className="text-xl font-bold">{comparisonStats.comparableRequests} / {comparisonStats.totalRequests}</div>
              </div>
              <div>
                <div className="text-xs mb-1" style={{ color: 'var(--text-tertiary)' }}>Overall Matches</div>
                <div className="text-xl font-bold" style={{ color: 'var(--accent-success)' }}>{comparisonStats.overallMatches}</div>
              </div>
              <div>
                <div className="text-xs mb-1" style={{ color: 'var(--text-tertiary)' }}>Status Mismatches</div>
                <div className="text-xl font-bold" style={{ color: 'var(--accent-warning)' }}>{comparisonStats.statusCodeMismatches}</div>
              </div>
              <div>
                <div className="text-xs mb-1" style={{ color: 'var(--text-tertiary)' }}>Body Mismatches</div>
                <div className="text-xl font-bold" style={{ color: 'var(--accent-error)' }}>{comparisonStats.responseBodyMismatches}</div>
              </div>
            </CardBody>
          </Card>
        )}

        <div className="flex gap-4 mb-6" style={{ borderBottom: '1px solid var(--border-color)' }}>
          <button 
            className={`tab-button ${viewMode === 'RESULTS' ? 'active' : ''}`}
            onClick={() => setViewMode('RESULTS')}
          >
            Replay Results ({results.length})
          </button>
          
          <button 
            className={`tab-button ${viewMode === 'COMPARISON' ? 'active' : ''}`}
            onClick={() => setViewMode('COMPARISON')}
          >
            Comparison Details ({comparisonDetails.length})
          </button>
        </div>

        <section className="animate-slide-up">
          {viewMode === 'RESULTS' && (
            <>
              {results.length === 0 ? (
                <div className="p-8 text-center border rounded text-secondary" style={{ borderColor: 'var(--border-color)', backgroundColor: 'var(--bg-surface)' }}>
                  No replay results found for this session.
                </div>
              ) : (
                <div className="flex-col gap-2">
                  {results.map(res => (
                    <div key={res.id}>
                      <div className="text-xs ml-2 mt-4" style={{ color: 'var(--text-tertiary)' }}>
                        Request ID: <span style={{ fontFamily: 'monospace' }}>{res.requestId}</span>
                      </div>
                      <ReplayResultCard result={res} />
                    </div>
                  ))}
                </div>
              )}
            </>
          )}

          {viewMode === 'COMPARISON' && (
            <>
              {comparisonDetails.length === 0 ? (
                <div className="p-8 text-center border rounded text-secondary" style={{ borderColor: 'var(--border-color)', backgroundColor: 'var(--bg-surface)' }}>
                  No comparison data available yet.
                </div>
              ) : (
                <div className="flex-col gap-2">
                  {comparisonDetails.map(detail => (
                    <ComparisonRequestCard key={detail.requestId} detail={detail} />
                  ))}
                </div>
              )}
            </>
          )}
        </section>
      </div>
    </>
  );
}
