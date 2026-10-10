import React from 'react';

export default function Legend() {
  return (
    <div className="legend">
      <div className="legend-item"><span className="swatch node"></span> Road node</div>
      <div className="legend-item"><span className="swatch blind"></span> Blind spot (uncovered)</div>
      <div className="legend-item"><span className="swatch amb-avail"></span> Ambulance — available</div>
      <div className="legend-item"><span className="swatch amb-busy"></span> Ambulance — busy/other</div>
      <div className="legend-item"><span className="line-swatch"></span> Road segment</div>
      <div className="legend-item"><span className="line-swatch blocked"></span> Blocked road</div>
    </div>
  );
}
