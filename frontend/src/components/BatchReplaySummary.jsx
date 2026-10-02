import React from 'react';
import { Card, CardBody, CardHeader, CardTitle } from './ui/Card';
import { Badge } from './ui/Badge';
import { Button } from './ui/Button';
import { CheckCircle, XCircle, ArrowRight } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export function BatchReplaySummary({ incidentId, summary }) {
  const navigate = useNavigate();
  
  if (!summary) return null;

  const isSuccess = summary.status === 'COMPLETED';

  return (
    <Card className="mt-8 border" style={{ borderColor: isSuccess ? 'var(--accent-success)' : 'var(--accent-error)' }}>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          {isSuccess ? <CheckCircle size={20} className="text-success" style={{ color: 'var(--accent-success)' }} /> : <XCircle size={20} className="text-error" style={{ color: 'var(--accent-error)' }} />}
          Batch Replay Summary
        </CardTitle>
        <Badge variant={isSuccess ? 'success' : 'error'}>{summary.status}</Badge>
      </CardHeader>
      
      <CardBody>
        <div className="grid grid-cols-3 gap-4 mb-6">
          <div className="p-4 rounded border" style={{ borderColor: 'var(--border-color)', background: 'var(--bg-base)' }}>
            <div className="text-xs mb-1" style={{ color: 'var(--text-secondary)' }}>Total Requests</div>
            <div className="text-xl font-bold">{summary.totalRequests}</div>
          </div>
          <div className="p-4 rounded border" style={{ borderColor: 'var(--border-color)', background: 'var(--bg-base)' }}>
            <div className="text-xs mb-1" style={{ color: 'var(--text-secondary)' }}>Successful Replays</div>
            <div className="text-xl font-bold text-success" style={{ color: 'var(--accent-success)' }}>{summary.successfulReplays}</div>
          </div>
          <div className="p-4 rounded border" style={{ borderColor: 'var(--border-color)', background: 'var(--bg-base)' }}>
            <div className="text-xs mb-1" style={{ color: 'var(--text-secondary)' }}>Failed Replays</div>
            <div className="text-xl font-bold text-error" style={{ color: 'var(--accent-error)' }}>{summary.failedReplays}</div>
          </div>
        </div>

        <div className="flex-between mb-2">
          <div className="text-sm" style={{ color: 'var(--text-secondary)' }}>Session ID: <span style={{ fontFamily: 'monospace' }}>{summary.sessionId}</span></div>
          {incidentId && summary.sessionId && (
            <Button size="sm" variant="ghost" icon={ArrowRight} onClick={() => navigate(`/incidents/${incidentId}/replay-sessions/${summary.sessionId}`)}>
              View Session
            </Button>
          )}
        </div>
        
        <h4 className="text-md mb-3 mt-4">Request Results</h4>
        <div className="flex-col gap-2">
          {summary.results && summary.results.map((res, idx) => (
            <div key={idx} className="flex-between p-3 border rounded text-sm" style={{ borderColor: 'var(--border-color)', backgroundColor: 'var(--bg-base)' }}>
              <div className="flex gap-4 items-center">
                <Badge variant={res.status === 'SUCCESS' ? 'success' : 'error'}>{res.status}</Badge>
                <span style={{ fontFamily: 'monospace' }}>ID: {res.requestId.substring(0,8)}...</span>
              </div>
              <div className="flex gap-4 items-center">
                {res.statusCode && <span className="font-semibold">{res.statusCode}</span>}
                {res.failureReason && <span style={{ color: 'var(--accent-error)' }}>{res.failureReason}</span>}
              </div>
            </div>
          ))}
        </div>
      </CardBody>
    </Card>
  );
}
