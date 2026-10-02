const API_BASE = '/api'; // Using relative path, Vite proxy will forward to backend

export async function fetchApi(endpoint, options = {}) {
  const url = `${API_BASE}${endpoint}`;
  
  const defaultOptions = {
    headers: {
      'Content-Type': 'application/json',
    },
  };
  
  const mergedOptions = {
    ...defaultOptions,
    ...options,
    headers: {
      ...defaultOptions.headers,
      ...options.headers,
    },
  };

  try {
    const response = await fetch(url, mergedOptions);
    
    // Attempt to parse JSON even on error, to get error details
    let data;
    try {
      data = await response.json();
    } catch (e) {
      console.error(e);
      data = null;
    }

    if (!response.ok) {
      throw {
        status: response.status,
        message: data?.message || response.statusText || 'An error occurred',
        data
      };
    }

    return data;
  } catch (error) {
    if (error.status) throw error;
    throw {
      status: 0,
      message: error.message || 'Network error',
    };
  }
}

// Service methods for specific endpoints can be added here
export const IncidentService = {
  getIncidents: () => fetchApi('/incidents'),
  getIncident: (id) => fetchApi(`/incidents/${id}`),
  create: (data) => fetchApi('/incidents', { method: 'POST', body: JSON.stringify(data) }),
  getContext: (id) => fetchApi(`/incidents/${id}/context`),
  saveContext: (id, data) => fetchApi(`/incidents/${id}/context`, { method: 'POST', body: JSON.stringify(data) }),
  getRequests: (id) => fetchApi(`/incidents/${id}/requests`),
  captureRequest: (id, data) => fetchApi(`/incidents/${id}/requests`, { method: 'POST', body: JSON.stringify(data) }),
  replayRequest: (incidentId, requestId) => fetchApi(`/incidents/${incidentId}/requests/${requestId}/replay`, { method: 'POST' }),
  getRequestReplays: (incidentId, requestId) => fetchApi(`/incidents/${incidentId}/requests/${requestId}/replays`),
  replayIncident: (incidentId) => fetchApi(`/incidents/${incidentId}/replay`, { method: 'POST' }),
  getReplaySessions: (incidentId) => fetchApi(`/incidents/${incidentId}/replay-sessions`),
  getReplaySession: (incidentId, sessionId) => fetchApi(`/incidents/${incidentId}/replay-sessions/${sessionId}`),
  getSessionReplays: (incidentId, sessionId) => fetchApi(`/incidents/${incidentId}/replay-sessions/${sessionId}/replays`),
  getRequestComparison: (incidentId, requestId) => fetchApi(`/incidents/${incidentId}/requests/${requestId}/compare`),
  getSessionComparison: (incidentId, sessionId) => fetchApi(`/incidents/${incidentId}/replay-sessions/${sessionId}/compare`),
  getSessionComparisonDetails: (incidentId, sessionId) => fetchApi(`/incidents/${incidentId}/replay-sessions/${sessionId}/comparison-details`),
  getSessionSummary: (incidentId, sessionId) => fetchApi(`/incidents/${incidentId}/replay-sessions/${sessionId}/summary`),
  getIncidentEvents: (incidentId) => fetchApi(`/incidents/${incidentId}/events`),
};

export const ReplayService = {
  getSessions: (incidentId) => fetchApi(`/incidents/${incidentId}/replay/sessions`),
  getSessionSummary: (incidentId, sessionId) => fetchApi(`/incidents/${incidentId}/replay/sessions/${sessionId}/summary`),
  startBatchReplay: (incidentId) => fetchApi(`/incidents/${incidentId}/replay`, { method: 'POST' })
};
