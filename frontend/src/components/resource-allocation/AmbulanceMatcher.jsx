import React from 'react';
import { Link } from 'react-router-dom';
import { routePath } from '../../utils/workflow.js';
import { formatLabel } from '../../utils/formatLabel.js';

const formatEquipment = (equipment = []) =>
  equipment.length === 0 ? 'No equipment listed' : equipment.map((item) => item.replace(/_/g, ' ')).join(', ');

export default function AmbulanceMatcher({ emergency, matches, isLoading, error, onDispatch, isDispatching, summaryText }) {
  if (!emergency) {
    return (
      <div className="resource-panel empty-panel">
        <h3>Allocation logic</h3>
        <p>Select an emergency to review matching ambulances.</p>
      </div>
    );
  }

  const required = emergency.requiredEquipment || [];

  if (isLoading) {
    return (
      <div className="resource-panel">
        <div className="panel-heading">
          <h3>Ambulance match</h3>
        </div>
        <p>Asking the dispatch scheduler for the current ranking…</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="resource-panel">
        <div className="panel-heading">
          <h3>Ambulance match</h3>
        </div>
        <div className="no-match-box">
          <p>{error}</p>
        </div>
      </div>
    );
  }

  if (!matches.length) {
    return (
      <div className="resource-panel">
        <div className="panel-heading">
          <h3>Ambulance match</h3>
        </div>
        <div className="no-match-box">
          <p>No available ambulance covers all required equipment for this emergency.</p>
          <ul>
            {required.map((item) => (
              <li key={item}>{item.replace(/_/g, ' ')}</li>
            ))}
          </ul>
        </div>
      </div>
    );
  }

  const bestMatch = matches[0];

  return (
    <div className="resource-panel">
      <div className="panel-heading match-heading">
        <h3>Ambulance match</h3>
        <button type="button" className="dispatch-button" onClick={() => onDispatch()} disabled={isDispatching}>
          {isDispatching ? 'Dispatching…' : 'Dispatch best match'}
        </button>
      </div>

      <div className="selection-summary">
        <div>
          <span className="summary-label">Selected emergency</span>
          <strong>{emergency.patient?.name || 'Unknown patient'}</strong>
        </div>
        <div>
          <span className="summary-label">Why this ambulance</span>
          <strong>{summaryText || 'Equipment match and fastest route.'}</strong>
        </div>
      </div>

      {/*
        These numbers (travelMinutes, extraEquipmentCount, score) come straight from
        GET /api/v1/calls/{id}/candidates - the same greedy-scheduler + shortest-path
        computation that POST /dispatch actually commits to. Nothing here is estimated
        client-side.
      */}
      {matches.map((match, index) => (
        <div key={match.ambulanceId} className={`match-card ${index === 0 ? 'winner' : ''}`}>
          <div className="match-header-row">
            <div>
              <span className="match-badge">#{index + 1}</span>
              <h4>{match.vehicleNumber}</h4>
            </div>
            <div className="score-box">score {match.score.toFixed(1)}</div>
          </div>

          <div className="match-grid">
            <div>
              <span className="mini-label">Current location</span>
              <strong>{match.currentLocationNode}</strong>
            </div>
            <div>
              <span className="mini-label">Travel time</span>
              <strong>{match.travelMinutes.toFixed(1)} min</strong>
            </div>
            <div>
              <span className="mini-label">Status</span>
              <strong>{formatLabel(match.status)}</strong>
            </div>
          </div>

          <div className="coverage-block">
            <span className="mini-label">Required equipment</span>
            <p>{formatEquipment(required)}</p>
          </div>

          <div className="coverage-block">
            <span className="mini-label">Ambulance equipment</span>
            <p>{formatEquipment(match.equipment)}</p>
          </div>

          <div className="match-reason">
            <span className="mini-label">Decision logic</span>
            <p>
              Covers every required piece of equipment ({formatEquipment(required)}) and carries{' '}
              {match.extraEquipmentCount} extra resource(s). Real shortest-path travel time from{' '}
              {match.currentLocationNode} to {emergency.locationNode} is {match.travelMinutes.toFixed(1)} minutes.
            </p>
          </div>
          {routePath(emergency, match) && <div className="workflow-actions">
            <Link to={routePath(emergency, match)}>Preview driving route for {match.vehicleNumber} →</Link>
          </div>}
        </div>
      ))}

      {bestMatch ? (
        <div className="winning-callout">
          <strong>Best dispatch decision:</strong> {bestMatch.vehicleNumber} is selected because it covers every
          required clinical resource and has the lowest fitness score ({bestMatch.score.toFixed(1)}) for the
          emergency at {emergency.locationNode}. This is the same ranking the backend will act on if dispatched now -
          it can only change if the fleet changes in the meantime.
        </div>
      ) : null}
    </div>
  );
}
