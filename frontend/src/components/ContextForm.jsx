import React, { useState, useEffect } from 'react';
import { Button } from './ui/Button';
import { IncidentService } from '../lib/api';

export function ContextForm({ incidentId, existingContext, onSuccess, onCancel }) {
  const [formData, setFormData] = useState({
    affectedService: '',
    environment: '',
    serviceVersion: '',
    errorMessage: '',
    stackTrace: '',
    metadata: ''
  });
  
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (existingContext) {
      setFormData({
        affectedService: existingContext.affectedService || '',
        environment: existingContext.environment || '',
        serviceVersion: existingContext.serviceVersion || '',
        errorMessage: existingContext.errorMessage || '',
        stackTrace: existingContext.stackTrace || '',
        metadata: existingContext.metadata ? JSON.stringify(existingContext.metadata, null, 2) : ''
      });
    }
  }, [existingContext]);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    
    let parsedMetadata = null;
    if (formData.metadata.trim()) {
      try {
        parsedMetadata = JSON.parse(formData.metadata);
      } catch (e) {
        console.error(e);
        setError('Metadata must be valid JSON');
        setLoading(false);
        return;
      }
    }
    
    const payload = {
      ...formData,
      metadata: parsedMetadata
    };

    try {
      await IncidentService.saveContext(incidentId, payload);
      onSuccess();
    } catch (err) {
      setError(err.message || 'Failed to save context');
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="surface" style={{ padding: '1.5rem', marginBottom: '1.5rem' }}>
      <h3 className="text-lg m-0 mb-4">{existingContext ? 'Update Context' : 'Capture Context'}</h3>
      
      {error && (
        <div className="p-3 mb-4 rounded border badge-error">
          {error}
        </div>
      )}

      <div className="grid grid-cols-2 gap-4">
        <div className="form-group">
          <label className="form-label" htmlFor="affectedService">Affected Service *</label>
          <input id="affectedService" name="affectedService" className="form-input" value={formData.affectedService} onChange={handleChange} required />
        </div>
        <div className="form-group">
          <label className="form-label" htmlFor="environment">Environment *</label>
          <input id="environment" name="environment" className="form-input" value={formData.environment} onChange={handleChange} required />
        </div>
        <div className="form-group">
          <label className="form-label" htmlFor="serviceVersion">Service Version *</label>
          <input id="serviceVersion" name="serviceVersion" className="form-input" value={formData.serviceVersion} onChange={handleChange} required />
        </div>
        <div className="form-group">
          <label className="form-label" htmlFor="errorMessage">Error Message *</label>
          <input id="errorMessage" name="errorMessage" className="form-input" value={formData.errorMessage} onChange={handleChange} required />
        </div>
      </div>
      
      <div className="form-group mt-4">
        <label className="form-label" htmlFor="stackTrace">Stack Trace (Optional)</label>
        <textarea id="stackTrace" name="stackTrace" className="form-textarea" style={{ fontFamily: 'monospace' }} value={formData.stackTrace} onChange={handleChange} />
      </div>

      <div className="form-group mt-4">
        <label className="form-label" htmlFor="metadata">Metadata JSON (Optional)</label>
        <textarea id="metadata" name="metadata" className="form-textarea" style={{ fontFamily: 'monospace' }} value={formData.metadata} onChange={handleChange} placeholder="{}" />
      </div>

      <div className="flex gap-4 mt-6" style={{ justifyContent: 'flex-end', display: 'flex' }}>
        <Button type="button" variant="ghost" onClick={onCancel} disabled={loading}>
          Cancel
        </Button>
        <Button type="submit" variant="primary" loading={loading}>
          {existingContext ? 'Update Context' : 'Save Context'}
        </Button>
      </div>
    </form>
  );
}
