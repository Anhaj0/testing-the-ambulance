import React from 'react';
import { Link } from 'react-router-dom';
import CoverageGauge from './CoverageGauge.jsx';
import ThresholdSlider from './ThresholdSlider.jsx';
import StatsGrid from './StatsGrid.jsx';
import BlindSpotList from './BlindSpotList.jsx';
import CoverageCurveChart from './CoverageCurveChart.jsx';
import Legend from './Legend.jsx';
import StatusBadge from '../common/StatusBadge.jsx';

export default function Sidebar({
  connected,
  threshold,
  onThresholdChange,
  nodeCount,
  edgeCount,
  availableAmbulances,
  blindSpots,
  onSelectBlindSpot,
  coverageCurve
}) {
  return (
    <aside className="sidebar">
      <div className="brand">
  <div className="eyebrow">Live Dispatch Monitoring</div>
  <h1>Coverage console</h1>
  <p>Real-time ambulance coverage across the city road network, powered by shortest-path analysis.</p>
  <StatusBadge tone={connected ? 'ok' : 'muted'}>
    {connected ? 'live — connected to database' : 'connecting to /api/v1/network …'}
  </StatusBadge>
  <div className="workflow-actions">
    <Link to="/resource-allocation">Review dispatch queue →</Link>
    <Link to="/scheduling">Review staff roster →</Link>
  </div>
      </div>

      <div className="section">
        <h2>Fleet coverage</h2>
        <CoverageGauge covered={nodeCount - blindSpots.length} total={nodeCount} />
      </div>

      <div className="section">
        <h2>Threshold</h2>
        <ThresholdSlider value={threshold} onChange={onThresholdChange} />
      </div>

      <div className="section">
        <h2>Coverage curve</h2>
        <CoverageCurveChart data={coverageCurve} />
      </div>

      <div className="section">
        <h2>Live stats</h2>
        <StatsGrid
          nodeCount={nodeCount}
          edgeCount={edgeCount}
          availableAmbulances={availableAmbulances}
          blindSpotCount={blindSpots.length}
        />
      </div>

      <div className="section">
        <h2>Blind spots (nearest station distance &gt; threshold)</h2>
        <BlindSpotList blindSpots={blindSpots} onSelect={onSelectBlindSpot} />
      </div>

      <div className="section last">
        <h2>Legend</h2>
        <Legend />
      </div>
    </aside>
  );
}
