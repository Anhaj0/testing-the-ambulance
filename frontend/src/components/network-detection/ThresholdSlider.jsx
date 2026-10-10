import React from 'react';

export default function ThresholdSlider({ value, onChange }) {
  return (
    <div className="threshold-control">
      <label>
        Max acceptable response time
        <span className="val">{value.toFixed(1)} min</span>
      </label>
      <input
        type="range"
        min="1"
        max="40"
        step="0.5"
        value={value}
        onChange={(e) => onChange(parseFloat(e.target.value))}
      />
    </div>
  );
}
