import React from 'react';

export default function StatsGrid({ nodeCount, edgeCount, availableAmbulances, blindSpotCount }) {
  return (
    <div className="stat-grid">
      <div className="stat-card">
        <div className="num">{nodeCount}</div>
        <div className="lbl">Road Nodes</div>
      </div>
      <div className="stat-card">
        <div className="num">{edgeCount}</div>
        <div className="lbl">Road Segments</div>
      </div>
      <div className="stat-card ok">
        <div className="num">{availableAmbulances}</div>
        <div className="lbl">Ambulances Available</div>
      </div>
      <div className="stat-card danger">
        <div className="num">{blindSpotCount}</div>
        <div className="lbl">Blind Spots</div>
      </div>
    </div>
  );
}
