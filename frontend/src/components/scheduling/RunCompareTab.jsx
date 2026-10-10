import React, { useEffect, useState } from 'react';
import { runSchedule, compareSchedule, fetchScheduleDefaults, fetchRoster } from '../../api/scheduling.api.js';
import FitnessScorecard from './FitnessScorecard.jsx';
import ConvergenceChart from './ConvergenceChart.jsx';
import EmptyState from '../common/EmptyState.jsx';
import { mondayFromWeekValue, currentWeekValue } from './dateUtils.js';
import { TOKEN_COLORS } from '../../styles/tokenColors.js';

// Friendly display names for what the backend calls algorithms — the raw
// names ("Genetic Algorithm", "Greedy") stay in API payloads and are fine
// in the report, but read as a coursework term rather than a product feature.
const ALGORITHM_LABELS = {
  'Genetic Algorithm': 'Smart Scheduler',
  'Greedy': 'Quick Scheduler',
};
function friendlyName(name) {
  return ALGORITHM_LABELS[name] || name;
}

const GA_FIELDS = [
  ['populationSize', 'Options considered per round'],
  ['maxGenerations', 'Refinement rounds (max)'],
  ['crossoverRate', 'How often good schedules are combined'],
  ['mutationRate', 'Amount of randomness introduced'],
  ['elitismCount', 'Top schedules kept automatically'],
  ['tournamentSize', 'Options compared each pick'],
  ['convergenceThreshold', 'Improvement needed to keep refining'],
  ['convergenceWindow', 'Rounds checked for that improvement'],
];
// GAParametersOverride.java binds these as Integer (the rest are Double) - a
// decimal value here fails Jackson deserialization server-side, so both the
// input step and the parsing below need to treat them differently from the
// Double fields.
const GA_INTEGER_FIELDS = new Set(['populationSize', 'maxGenerations', 'elitismCount', 'tournamentSize', 'convergenceWindow']);

const FITNESS_FIELDS = [
  ['understaffedPenalty', 'Avoid unfilled shifts'],
  ['overtimePenaltyPerHour', 'Avoid overtime'],
  ['restViolationPenalty', 'Protect rest between shifts'],
  ['fairnessWeight', 'Spread hours evenly'],
  ['minRestHours', 'Minimum rest required (hours)'],
];
const emptyOverride = (fields) => Object.fromEntries(fields.map(([f]) => [f, '']));

function cleanOverride(override, integerFields = new Set()) {
  const result = {};
  for (const [key, value] of Object.entries(override)) {
    if (value === '') continue;
    result[key] = integerFields.has(key) ? parseInt(value, 10) : parseFloat(value);
  }
  return Object.keys(result).length > 0 ? result : null;
}

export default function RunCompareTab() {
  const [weekValue, setWeekValue] = useState(currentWeekValue());
  const [mode, setMode] = useState('run');
  const [persist, setPersist] = useState(true);
  const [randomSeed, setRandomSeed] = useState('');
  const [advancedOpen, setAdvancedOpen] = useState(false);
  const [gaOverride, setGaOverride] = useState(emptyOverride(GA_FIELDS));
  const [fitnessOverride, setFitnessOverride] = useState(emptyOverride(FITNESS_FIELDS));
  const [defaults, setDefaults] = useState(null);
  const [existingRosterCount, setExistingRosterCount] = useState(null);
  const [running, setRunning] = useState(false);
  const [error, setError] = useState(null);
  const [result, setResult] = useState(null);

  useEffect(() => {
    fetchScheduleDefaults().then(setDefaults).catch((err) => console.error(err));
  }, []);

  const weekStarting = weekValue ? mondayFromWeekValue(weekValue) : null;

  useEffect(() => {
    if (!weekStarting) return;
    fetchRoster(weekStarting)
      .then((shifts) => setExistingRosterCount(shifts.length))
      .catch((err) => { console.error(err); setExistingRosterCount(null); });
  }, [weekStarting]);

  async function handleSubmit(e) {
    e.preventDefault();
    if (mode === 'run' && persist && existingRosterCount > 0) {
      const ok = window.confirm(
        `This week already has a schedule with ${existingRosterCount} shifts assigned. Generating a new one will replace it. Continue?`
      );
      if (!ok) return;
    }

    setRunning(true);
    setError(null);
    try {
      const request = {
        weekStarting,
        randomSeed: randomSeed === '' ? undefined : parseInt(randomSeed, 10),
        gaParameters: cleanOverride(gaOverride, GA_INTEGER_FIELDS),
        fitnessWeights: cleanOverride(fitnessOverride),
        persist: mode === 'run' ? persist : false,
      };
      const response = mode === 'run' ? await runSchedule(request) : await compareSchedule(request);
      setResult(response);
      if (mode === 'run' && persist) {
        setExistingRosterCount((await fetchRoster(weekStarting)).length);
      }
    } catch (err) {
      console.error(err);
      setError(err.message || 'Something went wrong while generating the schedule.');
    } finally {
      setRunning(false);
    }
  }

  return (
    <div className="run-compare-tab">
      <div className="run-compare-intro">
        <h2>Create this week's schedule</h2>
        <p>Generate a staff schedule automatically, or compare our smart scheduler against a simpler baseline method before you commit to it.</p>
      </div>

      <form className="entity-form run-compare-form" onSubmit={handleSubmit}>
        <fieldset disabled={running} className="fieldset-plain">
          <div className="form-row run-compare-top-row">
            <label className="run-compare-week-field">
              Week
              <input type="week" required value={weekValue} onChange={(e) => setWeekValue(e.target.value)} />
            </label>

            <div className="mode-toggle">
              <button type="button" className={`mode-btn${mode === 'run' ? ' active' : ''}`} onClick={() => setMode('run')}>Generate schedule</button>
              <button type="button" className={`mode-btn${mode === 'compare' ? ' active' : ''}`} onClick={() => setMode('compare')}>Compare methods</button>
            </div>
          </div>

          {mode === 'run' && (
            <label className="checkbox-item run-compare-persist">
              <input type="checkbox" checked={persist} onChange={(e) => setPersist(e.target.checked)} />
              Save as this week's schedule
            </label>
          )}

          <div className="run-compare-status">
            {mode === 'run' && (
              <EmptyState>Creates your schedule using our smart scheduling engine — recommended for everyday use. Switch to Compare methods to see how it stacks up against a simpler approach.</EmptyState>
            )}

            {weekStarting && existingRosterCount !== null && (
              <EmptyState>
                {existingRosterCount > 0
                  ? `This week already has a saved schedule with ${existingRosterCount} shifts assigned.`
                  : 'No schedule has been generated for this week yet.'}
              </EmptyState>
            )}
          </div>

          <div className="run-compare-advanced">
            <button type="button" className="btn-link" onClick={() => setAdvancedOpen((o) => !o)}>
              {advancedOpen ? 'Hide fine-tuning options' : 'Fine-tune before generating (optional)'}
            </button>

            {advancedOpen && (
              <div className="advanced-panel">
                <div className="advanced-panel-section">
                  <div className="run-compare-section-heading">
                    <span className="form-label">How the schedule gets built</span>
                    <p className="run-compare-section-hint">Leave any of these blank to use our recommended settings.</p>
                  </div>
                  <div className="run-compare-settings-grid">
                    {GA_FIELDS.map(([field, label]) => (
                      <label className="run-compare-field" key={field}>
                        <span>{label}</span>
                        <input type="number" step={GA_INTEGER_FIELDS.has(field) ? '1' : 'any'}
                          placeholder={defaults ? String(defaults.gaParameters[field]) : ''}
                          value={gaOverride[field]}
                          onChange={(e) => setGaOverride((o) => ({ ...o, [field]: e.target.value }))} />
                      </label>
                    ))}
                  </div>
                </div>

                <div className="advanced-panel-section">
                  <div className="run-compare-section-heading">
                    <span className="form-label">What matters most</span>
                    <p className="run-compare-section-hint">Leave any of these blank to use our recommended settings.</p>
                  </div>
                  <div className="run-compare-settings-grid">
                    {FITNESS_FIELDS.map(([field, label]) => (
                      <label className="run-compare-field" key={field}>
                        <span>{label}</span>
                        <input type="number" step="any"
                          placeholder={defaults ? String(defaults.fitnessWeights[field]) : ''}
                          value={fitnessOverride[field]}
                          onChange={(e) => setFitnessOverride((o) => ({ ...o, [field]: e.target.value }))} />
                      </label>
                    ))}
                  </div>
                </div>

                <label className="run-compare-field run-compare-seed">
                  <span>Repeat this exact result later (optional)</span>
                  <input type="number" value={randomSeed} onChange={(e) => setRandomSeed(e.target.value)} />
                </label>
              </div>
            )}
          </div>

          {error && <div className="form-error">{error}</div>}

          <div className="form-actions run-compare-actions">
            <button type="submit" className="btn-primary" disabled={!weekStarting}>
              {running ? 'Generating…' : mode === 'run' ? 'Generate schedule' : 'Compare methods'}
            </button>
          </div>
        </fieldset>
      </form>

      {result && mode === 'run' && (
        <section className="run-results">
          <h3 className="run-compare-results-title">Your generated schedule</h3>
          <FitnessScorecard
            result={result.fitnessResult}
            algorithmName={friendlyName(result.algorithmName)}
            executionTimeMillis={result.executionTimeMillis}
            generationsRun={result.generationsRun}
          />
          <div className="run-compare-chart-block">
            <h4 className="run-compare-chart-title">How the schedule improved with each round</h4>
            <ConvergenceChart series={[{ name: friendlyName(result.algorithmName), color: TOKEN_COLORS.info, data: result.bestFitnessHistory }]} />
          </div>
        </section>
      )}

      {result && mode === 'compare' && (
        <section className="compare-results">
          <h3 className="run-compare-results-title">Comparison results</h3>
          <p className="run-compare-section-hint">See how our smart scheduler stacks up against a simpler baseline method.</p>
          <div className="compare-scorecards">
            <FitnessScorecard
              result={result.geneticAlgorithm.fitnessResult}
              algorithmName={friendlyName(result.geneticAlgorithm.algorithmName)}
              executionTimeMillis={result.geneticAlgorithm.executionTimeMillis}
              generationsRun={result.geneticAlgorithm.generationsRun}
            />
            <span className="compare-vs-divider">vs</span>
            <FitnessScorecard
              result={result.greedy.fitnessResult}
              algorithmName={friendlyName(result.greedy.algorithmName)}
              executionTimeMillis={result.greedy.executionTimeMillis}
              generationsRun={result.greedy.generationsRun}
            />
          </div>
          <div className="run-compare-chart-block">
            <h4 className="run-compare-chart-title">How each method improved with each round</h4>
            <ConvergenceChart series={[
              { name: friendlyName(result.geneticAlgorithm.algorithmName), color: TOKEN_COLORS.info, data: result.geneticAlgorithm.bestFitnessHistory },
              { name: friendlyName(result.greedy.algorithmName), color: TOKEN_COLORS.warning, data: result.greedy.bestFitnessHistory },
            ]} />
          </div>
        </section>
      )}
    </div>
  );
}