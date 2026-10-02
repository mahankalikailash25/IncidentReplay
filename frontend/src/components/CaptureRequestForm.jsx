import React, { useState } from 'react';
import { Button } from './ui/Button';
import { IncidentService } from '../lib/api';

export function CaptureRequestForm({ incidentId, onSuccess, onCancel }) {
  const [formData, setFormData] = useState({
    method: 'GET',
    url: '',
    headers: '',
    body: '',
    originalStatusCode: 200,
    originalResponseBody: ''
  });
  
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    
    let parsedHeaders = {};
    if (formData.headers.trim()) {
      try {
        parsedHeaders = JSON.parse(formData.headers);
      } catch (err) {
        console.error(err);
        setError('Headers must be valid JSON');
        setLoading(false);
        return;
      }
    }
    
    const payload = {
      ...formData,
      headers: parsedHeaders,
      originalStatusCode: parseInt(formData.originalStatusCode, 10)
    };

    try {
      await IncidentService.captureRequest(incidentId, payload);
      onSuccess();
    } catch (err) {
      setError(err.message || 'Failed to capture request');
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="surface mt-4" style={{ padding: '1.5rem', marginBottom: '1.5rem' }}>
      <h3 className="text-lg m-0 mb-4">Capture HTTP Request</h3>
      
      {error && (
        <div className="p-3 mb-4 rounded border badge-error">
          {error}
        </div>
      )}

      <div className="grid grid-cols-2 gap-4">
        <div className="form-group">
          <label className="form-label" htmlFor="method">Method *</label>
          <select id="method" name="method" className="form-select" value={formData.method} onChange={handleChange} required>
            <option value="GET">GET</option>
            <option value="POST">POST</option>
            <option value="PUT">PUT</option>
            <option value="DELETE">DELETE</option>
            <option value="PATCH">PATCH</option>
          </select>
        </div>
        <div className="form-group">
          <label className="form-label" htmlFor="url">URL *</label>
          <input id="url" name="url" className="form-input" value={formData.url} onChange={handleChange} required placeholder="http://localhost:8080/api/example" />
        </div>
      </div>
      
      <div className="form-group mt-4">
        <label className="form-label" htmlFor="headers">Headers JSON (Optional)</label>
        <textarea id="headers" name="headers" className="form-textarea" style={{ fontFamily: 'monospace' }} value={formData.headers} onChange={handleChange} placeholder='{"Content-Type": "application/json"}' />
      </div>

      <div className="form-group mt-4">
        <label className="form-label" htmlFor="body">Request Body (Optional)</label>
        <textarea id="body" name="body" className="form-textarea" style={{ fontFamily: 'monospace' }} value={formData.body} onChange={handleChange} />
      </div>

      <div className="grid grid-cols-2 gap-4 mt-4">
        <div className="form-group">
          <label className="form-label" htmlFor="originalStatusCode">Original Status Code</label>
          <input id="originalStatusCode" name="originalStatusCode" type="number" className="form-input" value={formData.originalStatusCode} onChange={handleChange} required />
        </div>
      </div>

      <div className="form-group mt-4">
        <label className="form-label" htmlFor="originalResponseBody">Original Response Body (Optional)</label>
        <textarea id="originalResponseBody" name="originalResponseBody" className="form-textarea" style={{ fontFamily: 'monospace' }} value={formData.originalResponseBody} onChange={handleChange} />
      </div>

      <div className="flex gap-4 mt-6" style={{ justifyContent: 'flex-end', display: 'flex' }}>
        <Button type="button" variant="ghost" onClick={onCancel} disabled={loading}>
          Cancel
        </Button>
        <Button type="submit" variant="primary" loading={loading}>
          Capture Request
        </Button>
      </div>
    </form>
  );
}
