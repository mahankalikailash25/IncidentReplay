import React, { useState } from 'react';
import { Button } from './ui/Button';
import { IncidentService } from '../lib/api';
import { X } from 'lucide-react';

export function CreateIncidentModal({ onClose, onSuccess }) {
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    serviceName: '',
    severity: 'MEDIUM'
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
    
    try {
      await IncidentService.create(formData);
      onSuccess(); // Close and refresh
    } catch (err) {
      setError(err.message || 'Failed to create incident');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="flex-between mb-6">
          <h2 className="text-xl m-0">New Incident</h2>
          <Button variant="ghost" onClick={onClose} icon={X} aria-label="Close" />
        </div>
        
        {error && (
          <div className="p-3 mb-4 rounded border badge-error">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label" htmlFor="title">Title *</label>
            <input 
              id="title"
              name="title" 
              className="form-input" 
              value={formData.title} 
              onChange={handleChange} 
              required 
              placeholder="e.g. Payment Service Failure"
            />
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="description">Description</label>
            <textarea 
              id="description"
              name="description" 
              className="form-textarea" 
              value={formData.description} 
              onChange={handleChange}
              placeholder="Brief description of the issue"
            />
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="serviceName">Service Name *</label>
            <input 
              id="serviceName"
              name="serviceName" 
              className="form-input" 
              value={formData.serviceName} 
              onChange={handleChange} 
              required
              placeholder="e.g. payment-service"
            />
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="severity">Severity *</label>
            <select 
              id="severity"
              name="severity" 
              className="form-select" 
              value={formData.severity} 
              onChange={handleChange}
            >
              <option value="LOW">LOW</option>
              <option value="MEDIUM">MEDIUM</option>
              <option value="HIGH">HIGH</option>
              <option value="CRITICAL">CRITICAL</option>
            </select>
          </div>

          <div className="flex gap-4 mt-6" style={{ justifyContent: 'flex-end', display: 'flex' }}>
            <Button type="button" variant="ghost" onClick={onClose} disabled={loading}>
              Cancel
            </Button>
            <Button type="submit" variant="primary" loading={loading}>
              Create Incident
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
