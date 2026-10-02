import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { IncidentDashboard } from './pages/IncidentDashboard';
import { IncidentDetail } from './pages/IncidentDetail';
import { ReplaySessions } from './pages/ReplaySessions';
import { SessionDetail } from './pages/SessionDetail';
import { IncidentReplayBackground } from './components/ui/IncidentReplayBackground';

function App() {
  return (
    <BrowserRouter>
      <IncidentReplayBackground />
      <Routes>
        <Route path="/" element={<Navigate to="/incidents" replace />} />
        <Route path="/incidents" element={<IncidentDashboard />} />
        <Route path="/incidents/:incidentId" element={<IncidentDetail />} />
        <Route path="/incidents/:incidentId/replay-sessions" element={<ReplaySessions />} />
        <Route path="/incidents/:incidentId/replay-sessions/:sessionId" element={<SessionDetail />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
