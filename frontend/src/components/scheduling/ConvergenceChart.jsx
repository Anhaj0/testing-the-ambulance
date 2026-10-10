import React from 'react';
import { TOKEN_COLORS } from '../../styles/tokenColors.js';

const DEFAULT_COLORS = [TOKEN_COLORS.info, TOKEN_COLORS.warning, TOKEN_COLORS.ok, TOKEN_COLORS.danger];

export default function ConvergenceChart({ series }) {
  const plottable = series.filter((s) => s.data && s.data.length > 0);
  const hasCurve = plottable.some((s) => s.data.length > 1);

  if (plottable.length === 0) {
    return <div className="empty-note">No convergence data yet.</div>;
  }
  if (!hasCurve) {
    return <div className="empty-note">Single-pass run(s) — no generations to plot.</div>;
  }

  const width = 460;
  const height = 180;
  const padding = { top: 14, right: 16, bottom: 26, left: 46 };
  const plotW = width - padding.left - padding.right;
  const plotH = height - padding.top - padding.bottom;

  const maxGen = Math.max(...plottable.map((s) => s.data.length - 1), 1);
  const allValues = plottable.flatMap((s) => s.data);
  const minY = Math.min(...allValues);
  const maxY = Math.max(...allValues);
  const yPad = (maxY - minY) * 0.1 || 1;
  const yMin = minY - yPad;
  const yMax = maxY + yPad;

  const xScale = (gen) => padding.left + (gen / maxGen) * plotW;
  const yScale = (val) => padding.top + plotH - ((val - yMin) / (yMax - yMin || 1)) * plotH;

  const gridSteps = 4;
  const gridValues = Array.from({ length: gridSteps + 1 }, (_, i) => yMin + ((yMax - yMin) * i) / gridSteps);

  return (
    <div className="convergence-chart">
      <svg width={width} height={height} viewBox={`0 0 ${width} ${height}`}>
        {gridValues.map((g) => (
          <line key={g} x1={padding.left} x2={width - padding.right} y1={yScale(g)} y2={yScale(g)}
            stroke={TOKEN_COLORS.line} strokeWidth="1" />
        ))}

        {plottable.map((s, i) => {
          const color = s.color || DEFAULT_COLORS[i % DEFAULT_COLORS.length];

          if (s.data.length === 1) {
            const y = yScale(s.data[0]);
            return (
              <g key={s.name}>
                <line x1={padding.left} x2={width - padding.right} y1={y} y2={y}
                  stroke={color} strokeWidth="1.5" strokeDasharray="4,3" opacity="0.8" />
                <text x={width - padding.right} y={y - 5} fontSize="9" fill={color} textAnchor="end"
                  fontFamily="JetBrains Mono, monospace">
                  {s.name}: {s.data[0].toFixed(1)}
                </text>
              </g>
            );
          }

          const pathD = s.data
            .map((v, gen) => `${gen === 0 ? 'M' : 'L'} ${xScale(gen)} ${yScale(v)}`)
            .join(' ');

          return (
            <path key={s.name} d={pathD} fill="none" stroke={color} strokeWidth="2"
              strokeLinejoin="round" strokeLinecap="round" />
          );
        })}

        <text x={padding.left} y={height - 6} fontSize="9" fill={TOKEN_COLORS.muted} fontFamily="JetBrains Mono, monospace">
          gen 0
        </text>
        <text x={width - padding.right} y={height - 6} fontSize="9" fill={TOKEN_COLORS.muted} textAnchor="end"
          fontFamily="JetBrains Mono, monospace">
          gen {maxGen}
        </text>
        <text x={padding.left - 6} y={yScale(yMin) + 3} fontSize="9" fill={TOKEN_COLORS.muted} textAnchor="end"
          fontFamily="JetBrains Mono, monospace">
          {yMin.toFixed(0)}
        </text>
        <text x={padding.left - 6} y={yScale(yMax) + 3} fontSize="9" fill={TOKEN_COLORS.muted} textAnchor="end"
          fontFamily="JetBrains Mono, monospace">
          {yMax.toFixed(0)}
        </text>
      </svg>

      <div className="convergence-legend">
        {plottable.map((s, i) => (
          <span className="convergence-legend-item" key={s.name}>
            <span className="convergence-legend-swatch" style={{ background: s.color || DEFAULT_COLORS[i % DEFAULT_COLORS.length] }} />
            {s.name}
          </span>
        ))}
      </div>
    </div>
  );
}