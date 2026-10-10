import React from 'react';
import { formatLabel } from '../../utils/formatLabel.js';

const formatEquipment = (equipment = []) =>
  equipment.length === 0 ? 'No special equipment required' : equipment.map((item) => item.replace(/_/g, ' ')).join(', ');

export default function EmergencyList({ emergencies, selectedId, onSelect }) {
  if (!emergencies.length) {
    return (
      <div className="resource-panel empty-panel">
        <h3>Pending emergencies</h3>
        <p>No emergencies are currently queued for allocation.</p>
      </div>
    );
  }

  return (
    <div className="resource-panel">
      <div className="panel-heading">
        <h3>Pending emergencies</h3>
        <span>{emergencies.length} active</span>
      </div>

      <div className="emergency-list">
        {emergencies.map((emergency) => {
          const patient = emergency.patient || {};
          const isSelected = emergency.id === selectedId;

          return (
            <button
              key={emergency.id}
              type="button"
              className={`emergency-item ${isSelected ? 'selected' : ''}`}
              onClick={() => onSelect(emergency.id)}
            >
              <div className="emergency-topline">
                <span className="emergency-id">#{emergency.id}</span>
                <span className={`urgency-pill ${String(patient.urgencyLevel || emergency.status).toLowerCase()}`}>
                  {formatLabel(patient.urgencyLevel || emergency.status)}
                </span>
              </div>

              <h4>{patient.name || 'Unknown patient'}</h4>

              <div className="emergency-meta">
                <span>Location: {emergency.locationNode}</span>
                <span>Status: {formatLabel(emergency.status)}</span>
              </div>

              <div className="equipment-row">
                <strong>Required equipment:</strong>
                <span>{formatEquipment(emergency.requiredEquipment)}</span>
              </div>
            </button>
          );
        })}
      </div>
    </div>
  );
}
