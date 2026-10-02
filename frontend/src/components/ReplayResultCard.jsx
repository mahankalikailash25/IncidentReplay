import React, { useState } from 'react';
import { Badge } from './ui/Badge';
import { ChevronDown, ChevronRight, Activity, Clock } from 'lucide-react';
import { useCursorGlow } from '../hooks/useCursorGlow';

export function ReplayResultCard({ result }) {
  const [expanded, setExpanded] = useState(false);
  const glowRef = useCursorGlow();

  // Consider it successful if statusCode is present (even 4xx/5xx).
  // Timeouts or complete failures usually don't have a status code.
  const hasStatusCode = result.statusCode != null && result.statusCode !== 0;
  
  let statusVariant = 'neutral';
  if (hasStatusCode) {
    if (result.statusCode >= 200 && result.statusCode < 300) statusVariant = 'success';
    else if (result.statusCode >= 400 && result.statusCode < 500) statusVariant = 'warning';
    else if (result.statusCode >= 500) statusVariant = 'error';
  } else {
    statusVariant = 'error';
  }

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
        
        <span className="font-semibold text-sm">Replay Result</span>
        
        {hasStatusCode ? (
          <Badge variant={statusVariant}>{result.statusCode}</Badge>
        ) : (
          <Badge variant="error">Failed</Badge>
        )}
        
        <span className="flex-1 truncate text-sm" style={{ color: 'var(--text-secondary)' }}>
          {result.targetUrl}
        </span>
        
        <div className="flex gap-3 text-xs" style={{ color: 'var(--text-tertiary)' }}>
          <span className="flex items-center gap-1"><Activity size={12} /> {result.executionTimeMs}ms</span>
          <span className="flex items-center gap-1"><Clock size={12} /> {new Date(result.replayedAt).toLocaleTimeString()}</span>
        </div>
      </div>
      
      {expanded && (
        <div className="p-4" style={{ backgroundColor: 'var(--bg-base)' }}>
          {!hasStatusCode && result.responseBody && (
            <div className="mb-4">
              <h5 className="text-sm m-0 mb-2" style={{ color: 'var(--text-secondary)' }}>Failure Reason</h5>
              <div className="code-viewer border-error text-error">
                {result.responseBody}
              </div>
            </div>
          )}

          {hasStatusCode && (
            <div>
              <h5 className="text-sm m-0 mb-2" style={{ color: 'var(--text-secondary)' }}>Response Body</h5>
              <pre className="code-viewer">
                {result.responseBody || '(Empty response)'}
              </pre>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
