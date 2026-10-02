import React, { useState, useEffect, useCallback } from 'react';
import { AlertTriangle, PlusCircle, Network, Camera, PlayCircle, ShieldAlert } from 'lucide-react';
import { IncidentService } from '../lib/api';

export function IncidentTimeline({ incidentId }) {
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchEvents = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await IncidentService.getIncidentEvents(incidentId);
      setEvents(data || []);
    } catch (err) {
      setError(err.message || 'Failed to load timeline events');
    } finally {
      setLoading(false);
    }
  }, [incidentId]);

  useEffect(() => {
    fetchEvents();
  }, [fetchEvents]);

  if (loading) {
    return (
      <div className="flex-col gap-4 animate-pulse">
        <div className="skeleton rounded" style={{ height: '60px', width: '100%' }}></div>
        <div className="skeleton rounded" style={{ height: '60px', width: '80%' }}></div>
        <div className="skeleton rounded" style={{ height: '60px', width: '90%' }}></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-4 border rounded badge-error flex items-center gap-3">
        <AlertTriangle size={18} />
        <span className="text-sm">{error}</span>
        <button onClick={fetchEvents} className="ml-auto text-xs underline cursor-pointer bg-transparent border-none text-white">Retry</button>
      </div>
    );
  }

  if (events.length === 0) {
    return (
      <div className="p-6 text-center border rounded text-secondary text-sm" style={{ borderColor: 'var(--border-color)', backgroundColor: 'var(--bg-surface)' }}>
        No events recorded yet.
      </div>
    );
  }

  const getEventIcon = (type) => {
    switch(type) {
      case 'INCIDENT_CREATED': return <ShieldAlert size={14} className="text-white" />;
      case 'CONTEXT_CAPTURED': return <Camera size={14} className="text-white" />;
      case 'REQUEST_CAPTURED': return <Network size={14} className="text-white" />;
      case 'REQUEST_REPLAYED': return <PlayCircle size={14} className="text-white" />;
      default: return <PlusCircle size={14} className="text-white" />;
    }
  };

  return (
    <div className="relative pt-2 pb-2 pl-4">
      <div className="absolute left-[1.35rem] top-4 bottom-4 w-px" style={{ backgroundColor: 'var(--border-color)' }}></div>
      
      <div className="flex-col gap-6">
        {events.map((evt, idx) => (
          <div key={evt.id || idx} className="relative pl-10 animate-fade-in" style={{ animationDelay: `${idx * 0.1}s` }}>
            {/* Timeline icon dot */}
            <div 
              className="absolute left-[-0.25rem] top-0 w-7 h-7 rounded-full z-10 flex-center" 
              style={{ backgroundColor: 'var(--accent-primary)', boxShadow: '0 0 0 4px var(--bg-surface)' }}
            >
              {getEventIcon(evt.eventType)}
            </div>
            
            <div className="text-sm font-bold tracking-wide" style={{ color: 'var(--text-primary)' }}>
              {evt.eventType.replace(/_/g, ' ')}
            </div>
            
            <div className="text-sm mt-1 mb-1" style={{ color: 'var(--text-secondary)' }}>
              {evt.description}
            </div>
            
            <div className="text-xs" style={{ color: 'var(--text-tertiary)' }}>
              {new Date(evt.timestamp).toLocaleString()}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
