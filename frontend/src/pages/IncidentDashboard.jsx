import React, { useState, useEffect, useCallback } from 'react';
import { Play, ShieldAlert, AlertTriangle, Plus, Server } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { Card, CardHeader, CardTitle, CardBody } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { IncidentService } from '../lib/api';
import { CreateIncidentModal } from '../components/CreateIncidentModal';

export function IncidentDashboard() {
  const navigate = useNavigate();
  const [incidents, setIncidents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);

  const fetchIncidents = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await IncidentService.getIncidents();
      setIncidents(data);
    } catch (err) {
      console.error(err);
      setError('Unable to load incidents');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchIncidents();
  }, [fetchIncidents]);

  const handleCreateSuccess = () => {
    setIsCreateModalOpen(false);
    fetchIncidents();
  };

  const getSeverityBadge = (severity) => {
    switch (severity) {
      case 'CRITICAL':
      case 'HIGH':
        return <Badge variant="error">{severity}</Badge>;
      case 'MEDIUM':
        return <Badge variant="warning">{severity}</Badge>;
      case 'LOW':
      default:
        return <Badge variant="neutral">{severity}</Badge>;
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'OPEN':
        return <Badge variant="error">{status}</Badge>;
      case 'INVESTIGATING':
        return <Badge variant="warning">{status}</Badge>;
      case 'RESOLVED':
        return <Badge variant="success">{status}</Badge>;
      default:
        return <Badge variant="neutral">{status}</Badge>;
    }
  };

  return (
    <>
      <div className="layout-container animate-page-enter">
        <header className="flex-between mb-8 pb-4" style={{ borderBottom: '1px solid var(--border-color)' }}>
          <div className="flex-center gap-4">
            <ShieldAlert size={28} style={{ color: 'var(--accent-primary)' }} />
            <div>
              <h1 className="text-2xl m-0">IncidentReplay</h1>
              <p className="text-secondary m-0" style={{ color: 'var(--text-secondary)' }}>Developer API Debugging Platform</p>
            </div>
          </div>
          <div className="flex-center gap-4">
            <Button variant="primary" icon={Plus} onClick={() => setIsCreateModalOpen(true)}>New Incident</Button>
          </div>
        </header>

        <main className="grid grid-cols-1 gap-6 animate-slide-up">
          <Card interactive={false}>
            <CardHeader>
              <CardTitle>Captured Incidents</CardTitle>
              {!loading && !error && <Badge variant="neutral">{incidents.length} Total</Badge>}
            </CardHeader>
            <CardBody>
              {loading ? (
                <div className="flex-col gap-4">
                  {[1, 2, 3].map(i => (
                    <div key={i} className="skeleton" style={{ height: '80px' }}></div>
                  ))}
                </div>
              ) : error ? (
                <div className="flex-center flex-col gap-4 py-8 text-center" style={{ color: 'var(--text-secondary)' }}>
                  <AlertTriangle size={48} style={{ color: 'var(--accent-error)', opacity: 0.8 }} />
                  <p>{error}</p>
                  <Button variant="secondary" onClick={fetchIncidents}>Retry</Button>
                </div>
              ) : incidents.length === 0 ? (
                <div className="flex-center flex-col gap-4 py-12 text-center" style={{ color: 'var(--text-secondary)' }}>
                  <Server size={48} style={{ opacity: 0.3 }} />
                  <div>
                    <h3 className="m-0 mb-2 text-primary" style={{ color: 'var(--text-primary)' }}>No incidents found</h3>
                    <p className="m-0 text-sm">Create an incident to start capturing and replaying requests.</p>
                  </div>
                  <Button variant="secondary" icon={Plus} onClick={() => setIsCreateModalOpen(true)}>Create First Incident</Button>
                </div>
              ) : (
                <div className="flex-col gap-4">
                  {incidents.map((incident) => (
                      <Card 
                        key={incident.id} 
                        interactive={true} 
                        role="button"
                        tabIndex={0}
                        className="p-4 cursor-pointer" 
                        style={{ backgroundColor: 'var(--bg-surface-hover)' }}
                        onClick={() => navigate(`/incidents/${incident.id}`)}
                      >
                      <div className="flex-between">
                        <div>
                          <div className="flex gap-2 items-center mb-1" style={{ display: 'flex', alignItems: 'center' }}>
                            <h4 className="m-0 text-md">{incident.title}</h4>
                            {getStatusBadge(incident.status)}
                          </div>
                          <div className="flex gap-4 items-center text-sm" style={{ display: 'flex', alignItems: 'center', color: 'var(--text-secondary)' }}>
                            <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}><Server size={14} /> {incident.serviceName}</span>
                            <span>{new Date(incident.createdAt).toLocaleString()}</span>
                            {getSeverityBadge(incident.severity)}
                          </div>
                          {incident.description && (
                            <p className="mt-2 text-sm m-0" style={{ color: 'var(--text-tertiary)' }}>{incident.description}</p>
                          )}
                        </div>
                        <div className="flex-center gap-2">
                          <Button variant="secondary" icon={Play}>Replay</Button>
                        </div>
                      </div>
                    </Card>
                  ))}
                </div>
              )}
            </CardBody>
          </Card>
        </main>
      </div>

      {isCreateModalOpen && (
        <CreateIncidentModal 
          onClose={() => setIsCreateModalOpen(false)} 
          onSuccess={handleCreateSuccess} 
        />
      )}
    </>
  );
}

