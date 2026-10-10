import { categoryDetails } from './categoryDetails.js';
import { explainCategory, explainScore } from './decisionRules.js';

export default function TriageResult({ result, assessment }) {
  if (!result) {
    return <section className="triage-result triage-result-empty"><strong>Decision engine ready</strong><span>Submit an assessment to calculate its MTS category and queue position.</span></section>;
  }

  const details = categoryDetails[result.category];
  const decision = assessment ? explainCategory(assessment) : null;
  const score = assessment ? explainScore(assessment) : null;

  return (
    <>
      <section className="triage-result" style={{ '--triage-category': details?.color ?? '#2563eb' }} aria-live="polite">
        <div><span>Latest decision</span><strong>{result.category}</strong><small>{details?.label}</small></div>
        <div><span>Queue position</span><strong>#{result.queuePosition}</strong></div>
        <div><span>Priority score</span><strong>{Number(result.score).toFixed(1)}</strong></div>
        <div><span>Target response</span><strong>{details?.responseTime}</strong></div>
      </section>

      {decision && score && (
        <section className="triage-why" style={{ '--triage-category': details?.color ?? '#2563eb' }}>
          <div className="triage-why-block">
            <h3>Why {result.category}</h3>
            <p className="triage-why-hint">The MTS decision tree checks RED first, then ORANGE, YELLOW, GREEN — the first category with a matching rule wins.</p>
            <ul>
              {decision.reasons.map((reason) => <li key={reason}>{reason}</li>)}
            </ul>
          </div>

          <div className="triage-why-block">
            <h3>Priority score breakdown</h3>
            <p className="triage-why-hint">Breaks ties between patients in the same category — higher score is seen sooner.</p>
            <ul className="triage-score-breakdown">
              {score.breakdown.map((item) => (
                <li key={item.label}>
                  <span>{item.label}</span>
                  <strong>+{item.value.toFixed(1)}</strong>
                </li>
              ))}
              <li className="triage-score-total">
                <span>Total</span>
                <strong>{score.total.toFixed(1)}</strong>
              </li>
            </ul>
          </div>
        </section>
      )}
    </>
  );
}
