import React from 'react';

export default function FitnessScorecard({ result, algorithmName, executionTimeMillis, generationsRun }) {
  if (!result) return null;

  return (
    <div className="fitness-scorecard">
      <div className="fitness-scorecard-header">
        <span className="fitness-scorecard-title">{algorithmName}</span>
        <span className="fitness-scorecard-meta">{executionTimeMillis}ms · {generationsRun} gen</span>
      </div>
      <div className="stat-grid">
        <div className={`stat-card${result.understaffedViolations > 0 ? ' danger' : ' ok'}`}>
          <div className="num">{result.understaffedViolations}</div>
          <div className="lbl">Understaffed</div>
        </div>
        <div className={`stat-card${result.overtimeHours > 0 ? ' warning' : ' ok'}`}>
          <div className="num">{result.overtimeHours.toFixed(1)}h</div>
          <div className="lbl">Overtime</div>
        </div>
        <div className={`stat-card${result.restViolations > 0 ? ' danger' : ' ok'}`}>
          <div className="num">{result.restViolations}</div>
          <div className="lbl">Rest Violations</div>
        </div>
        <div className="stat-card">
          <div className="num">{result.fairnessStdDevHours.toFixed(1)}h</div>
          <div className="lbl">Fairness (σ)</div>
        </div>
      </div>
      <div className="fitness-scorecard-total">
        <span>Fitness score</span>
        <span className="fitness-scorecard-score">{result.fitness.toFixed(1)}</span>
      </div>
    </div>
  );
}