import React from 'react';
import EmptyState from '../common/EmptyState.jsx';
import { TOKEN_COLORS } from '../../styles/tokenColors.js';

export default function CoverageCurveChart({ data }) {
  if (!data || data.length === 0) {
    return <EmptyState>Loading coverage curve…</EmptyState>;
  }

  const width = 268;
  const height = 120;
  const padding = { top: 10, right: 10, bottom: 22, left: 30 };
  const plotW = width - padding.left - padding.right;
  const plotH = height - padding.top - padding.bottom;

  const minX = data[0].thresholdMinutes;
  const maxX = data[data.length - 1].thresholdMinutes;
  const xScale = (x) => padding.left + ((x - minX) / (maxX - minX || 1)) * plotW;
  const yScale = (y) => padding.top + plotH - (y / 100) * plotH;

  const pathD = data
      .map((d, i) => `${i === 0 ? 'M' : 'L'} ${xScale(d.thresholdMinutes)} ${yScale(d.coveragePercentage)}`)
      .join(' ');

  const areaD = `${pathD} L ${xScale(maxX)} ${padding.top + plotH} L ${xScale(minX)} ${padding.top + plotH} Z`;

  const gridLines = [0, 25, 50, 75, 100];

  return (
      <div>
        <svg width={width} height={height} viewBox={`0 0 ${width} ${height}`}>
          {gridLines.map((g) => (
              <line
                  key={g}
                  x1={padding.left}
                  x2={width - padding.right}
                  y1={yScale(g)}
                  y2={yScale(g)}
                  stroke={TOKEN_COLORS.line}
                  strokeWidth="1"
              />
          ))}

          <path d={areaD} fill="rgba(79, 70, 229, 0.12)" stroke="none" />
          <path d={pathD} fill="none" stroke={TOKEN_COLORS.info} strokeWidth="2" strokeLinejoin="round" strokeLinecap="round" />

          {data.map((d) => (
              <circle
                  key={d.thresholdMinutes}
                  cx={xScale(d.thresholdMinutes)}
                  cy={yScale(d.coveragePercentage)}
                  r="2.5"
                  fill={TOKEN_COLORS.info}
              />
          ))}

          <text x={padding.left} y={height - 6} fontSize="9" fill={TOKEN_COLORS.muted} fontFamily="JetBrains Mono, monospace">
            {minX}min
          </text>
          <text x={width - padding.right} y={height - 6} fontSize="9" fill={TOKEN_COLORS.muted} textAnchor="end" fontFamily="JetBrains Mono, monospace">
            {maxX}min
          </text>
          <text x={padding.left - 6} y={yScale(0) + 3} fontSize="9" fill={TOKEN_COLORS.muted} textAnchor="end" fontFamily="JetBrains Mono, monospace">
            0%
          </text>
          <text x={padding.left - 6} y={yScale(100) + 3} fontSize="9" fill={TOKEN_COLORS.muted} textAnchor="end" fontFamily="JetBrains Mono, monospace">
            100%
          </text>
        </svg>
        <div style={{ fontSize: 11, color: 'var(--muted)', marginTop: 4 }}>
          Coverage % vs. response-time threshold (minutes)
        </div>
      </div>
  );
}