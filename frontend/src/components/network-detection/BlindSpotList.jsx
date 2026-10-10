import React from 'react';
import EmptyState from '../common/EmptyState.jsx';

export default function BlindSpotList({ blindSpots, onSelect }) {
  if (!blindSpots.length) {
    return <EmptyState>No blind spots at this threshold — full coverage.</EmptyState>;
  }

  return (
    <div className="blindspot-list">
      {blindSpots.map((n) => (
        <div key={n.id} className="blindspot-row" onClick={() => onSelect(n)}>
          <span className="name">{n.name}</span>
          <span className="coord">{n.latitude.toFixed(4)}, {n.longitude.toFixed(4)}</span>
        </div>
      ))}
    </div>
  );
}
