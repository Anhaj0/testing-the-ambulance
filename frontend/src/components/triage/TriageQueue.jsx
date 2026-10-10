import { categoryDetails, categoryNames } from './categoryDetails.js';
import { Link } from 'react-router-dom';
import { callPath } from '../../utils/workflow.js';

function formatTimestamp(timestamp) {
  if (!timestamp) return 'Time unavailable';
  return new Intl.DateTimeFormat('en', { hour: '2-digit', minute: '2-digit', second: '2-digit' }).format(new Date(timestamp));
}

export default function TriageQueue({ activeQueue, pendingCalls = [], isLoading, isConnected, resolvingId, lastUpdated, onRefresh, onResolve }) {
  const categoryCounts = categoryNames.reduce((counts, category) => ({ ...counts, [category]: activeQueue.filter((assessment) => assessment.category === category).length }), {});

  return (
    <section className="triage-queue" aria-busy={isLoading}>
      <div className="triage-queue-heading">
        <div><span>02</span><h2>Active dispatch queue</h2><p>Ordered by a binary max-heap — MTS category severity, then priority score, then earliest arrival, each only breaking a tie in the one before it.</p></div>
        <div className="triage-queue-actions"><small className={isConnected ? 'is-live' : ''}>{isConnected ? '● Live · 15s refresh' : '● Updates paused'}</small><button type="button" onClick={onRefresh} disabled={isLoading}>{isLoading ? 'Refreshing…' : 'Refresh now'}</button></div>
      </div>
      <div className="triage-summary">
        <div className="triage-total"><span>Waiting</span><strong>{activeQueue.length}</strong><small>{lastUpdated ? `Updated ${lastUpdated}` : 'Awaiting connection'}</small></div>
        <div className="triage-counts">{categoryNames.map((category) => <div key={category}><i style={{ backgroundColor: categoryDetails[category].color }} />{category}<strong>{categoryCounts[category]}</strong></div>)}</div>
      </div>
      <div className="triage-list">
        {isLoading && activeQueue.length === 0 && <p className="triage-empty">Loading active assessments…</p>}
        {!isLoading && activeQueue.length === 0 && <p className="triage-empty">No patients are currently waiting.</p>}
        {activeQueue.map((assessment) => {
          const details = categoryDetails[assessment.category];
          const isResolving = resolvingId === assessment.id;
          const pendingCall = pendingCalls.find((call) => call.patient.id === assessment.patientId);
          return <article className="triage-row" key={assessment.id} style={{ '--triage-category': details?.color ?? '#2563eb' }}>
            <b>{String(assessment.queuePosition).padStart(2, '0')}</b><div><strong>{assessment.patientName ?? `Patient #${assessment.patientId ?? '—'}`}</strong><small>{assessment.category} · {details?.label}</small>
              {pendingCall && <Link to={callPath('/resource-allocation', pendingCall.id)}>Match ambulance →</Link>}
            </div>
            <div><span>Score</span><strong>{Number(assessment.score).toFixed(1)}</strong></div><div><span>Assessed</span><strong>{formatTimestamp(assessment.timestamp)}</strong></div>
            <button type="button" disabled={!assessment.id || isResolving} onClick={() => onResolve(assessment)}>{isResolving ? 'Resolving…' : 'Mark resolved'}</button>
          </article>;
        })}
      </div>
    </section>
  );
}
