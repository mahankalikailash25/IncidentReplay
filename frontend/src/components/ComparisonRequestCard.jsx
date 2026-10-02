import React, { useState } from 'react';
import { Badge } from './ui/Badge';
import { ChevronDown, ChevronRight, Activity, Check, X } from 'lucide-react';
import { useCursorGlow } from '../hooks/useCursorGlow';

export function ComparisonRequestCard({ detail }) {
  const [expanded, setExpanded] = useState(false);
  const glowRef = useCursorGlow();

  if (!detail.comparable) {
    return (
      <div className="border rounded mt-4 p-4 flex gap-4 items-center" style={{ borderColor: 'var(--border-color)', backgroundColor: 'var(--bg-surface)' }}>
        <Badge variant="neutral">Not Comparable</Badge>
        <span className="flex-1 text-sm font-monospace" style={{ color: 'var(--text-secondary)' }}>ID: {detail.requestId.substring(0, 12)}...</span>
        <span className="text-xs text-tertiary">Original response information unavailable.</span>
      </div>
    );
  }

  const overallVariant = detail.overallMatch ? 'success' : 'error';

  return (
    <div ref={glowRef} className="surface interactive cursor-glow-wrapper mt-4 p-0" style={{ borderColor: 'var(--border-color)', backgroundColor: 'var(--bg-surface)' }}>
      <div 
        role="button"
        tabIndex={0}
        className="flex gap-4 p-3 items-center cursor-pointer select-none"
        onClick={() => setExpanded(!expanded)}
        onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') setExpanded(!expanded); }}
        style={{ borderBottom: expanded ? '1px solid var(--border-color)' : 'none' }}
      >
        {expanded ? <ChevronDown size={18} /> : <ChevronRight size={18} />}
        
        <Badge variant={overallVariant}>{detail.overallMatch ? 'Matched' : 'Mismatch'}</Badge>
        
        <span className="font-monospace text-sm flex-1 truncate" style={{ color: 'var(--text-secondary)' }}>
          ID: {detail.requestId}
        </span>
        
        <div className="flex gap-3 items-center text-xs" style={{ color: 'var(--text-tertiary)' }}>
          <span className="flex items-center gap-1" title="Status Match">
            Status: {detail.statusCodeMatch ? <Check size={14} className="text-success" style={{ color: 'var(--accent-success)' }}/> : <X size={14} className="text-error" style={{ color: 'var(--accent-error)' }}/>}
          </span>
          <span className="flex items-center gap-1" title="Body Match">
            Body: {detail.responseBodyMatch ? <Check size={14} className="text-success" style={{ color: 'var(--accent-success)' }}/> : <X size={14} className="text-error" style={{ color: 'var(--accent-error)' }}/>}
          </span>
          {detail.executionTimeMs != null && (
            <span className="flex items-center gap-1"><Activity size={12} /> {detail.executionTimeMs}ms</span>
          )}
        </div>
      </div>
      
      {expanded && (
        <div className="p-4 grid grid-cols-1 md:grid-cols-2 gap-6" style={{ backgroundColor: 'var(--bg-base)' }}>
          {/* Original */}
          <div>
            <h5 className="text-sm m-0 mb-3 flex items-center gap-2" style={{ color: 'var(--text-secondary)' }}>
              Original Response
              <Badge variant="neutral">{detail.originalStatusCode || 'N/A'}</Badge>
            </h5>
            <pre className="code-viewer">
              {detail.originalResponseBody || '(Empty)'}
            </pre>
          </div>
          
          {/* Replay */}
          <div>
            <h5 className="text-sm m-0 mb-3 flex items-center gap-2" style={{ color: 'var(--text-secondary)' }}>
              Replay Response
              <Badge variant={detail.statusCodeMatch ? 'neutral' : 'error'}>{detail.replayStatusCode || 'N/A'}</Badge>
            </h5>
            <pre className="code-viewer" style={{ borderColor: detail.responseBodyMatch ? 'var(--border-color)' : 'var(--accent-error)' }}>
              {detail.replayResponseBody || '(Empty)'}
            </pre>
          </div>
        </div>
      )}
    </div>
  );
}
