import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { fetchPendingEmergencies } from '../api/resourceAllocation.api.js';
import { useDispatchWorkflow } from '../components/common/DispatchWorkflow.jsx';
import { callPath } from '../utils/workflow.js';
import { evaluateTriage, fetchActiveQueue, resolveAssessment } from '../api/triage.api.js';
import TriageForm from '../components/triage/TriageForm.jsx';
import TriageQueue from '../components/triage/TriageQueue.jsx';
import TriageResult from '../components/triage/TriageResult.jsx';
import '../styles/triage.css';

export default function TriagePage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const { activeCall, selectCall, clearCall } = useDispatchWorkflow();
  const [pendingCalls, setPendingCalls] = useState([]);
  const [callsLoading, setCallsLoading] = useState(true);
  const [callsError, setCallsError] = useState('');
  const requestedId = searchParams.get('callId');
  const selectedCall = pendingCalls.find((call) => call.id === Number(requestedId ?? activeCall?.id));
  const [activeQueue, setActiveQueue] = useState([]);
  const [latestResult, setLatestResult] = useState(null);
  const [latestAssessment, setLatestAssessment] = useState(null);
  const [isConnected, setIsConnected] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [resolvingId, setResolvingId] = useState(null);
  const [errorMessage, setErrorMessage] = useState('');
  const [lastUpdated, setLastUpdated] = useState('');
  const latestResultCall = pendingCalls.find((call) => call.patient.id === latestAssessment?.patientId);
  const evaluationVersion = useRef(0);

  useEffect(() => {
    evaluationVersion.current += 1;
    setLatestResult(null);
    setLatestAssessment(null);
  }, [selectedCall?.patient.id]);

  useEffect(() => {
    let cancelled = false;
    setCallsLoading(true);
    fetchPendingEmergencies().then((calls) => {
      if (cancelled) return;
      setPendingCalls(calls);
      setCallsError('');
      const selected = calls.find((call) => call.id === Number(requestedId ?? activeCall?.id));
      if (selected) selectCall(selected);
      else if (requestedId) clearCall();
    }).catch((error) => {
      if (!cancelled) setCallsError(error.message);
    }).finally(() => { if (!cancelled) setCallsLoading(false); });
    return () => { cancelled = true; };
  }, [requestedId, selectCall, clearCall]);

  // showLoader doubles as "this call is user-visible" (initial mount, the
  // Retry button, the Refresh now button all pass the default true) vs. the
  // silent 15s background poll and the post-evaluate/post-resolve refresh
  // (both pass false). Only a user-visible call clears errorMessage on
  // success — otherwise a background poll succeeding a few seconds after an
  // unrelated evaluate/resolve failure would silently wipe that error off
  // the screen before the user ever saw it.
  const loadQueue = useCallback(async ({ showLoader = true } = {}) => {
    if (showLoader) setIsLoading(true);
    try {
      setActiveQueue(await fetchActiveQueue());
      setIsConnected(true);
      if (showLoader) setErrorMessage('');
      setLastUpdated(new Intl.DateTimeFormat('en', { hour: '2-digit', minute: '2-digit' }).format(new Date()));
    } catch (error) {
      setIsConnected(false);
      setErrorMessage(error.message);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadQueue();
    const refreshInterval = window.setInterval(() => loadQueue({ showLoader: false }), 15000);
    return () => window.clearInterval(refreshInterval);
  }, [loadQueue]);

  async function handleEvaluate(assessment) {
    const version = ++evaluationVersion.current;
    setIsSubmitting(true);
    setErrorMessage('');
    try {
      const result = await evaluateTriage(assessment);
      if (version === evaluationVersion.current) {
        setLatestResult(result);
        setLatestAssessment(assessment);
      }
      await loadQueue({ showLoader: false });
    } catch (error) {
      if (version === evaluationVersion.current) setErrorMessage(error.message);
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleResolve(assessment) {
    if (!window.confirm(`Mark queue position #${assessment.queuePosition} as resolved?`)) return;
    setResolvingId(assessment.id);
    setErrorMessage('');
    try {
      await resolveAssessment(assessment.id);
      await loadQueue({ showLoader: false });
    } catch (error) {
      setErrorMessage(error.message);
    } finally {
      setResolvingId(null);
    }
  }

  return (
    <main className="triage-page">
      <header className="triage-header">
        <div><span>Live dispatch · triage</span><h1>Triage command</h1></div>
        <p className={isConnected ? 'triage-online' : ''}>● {isConnected ? 'Decision engine online' : 'Backend unavailable'}</p>
      </header>
      <div className="workflow-picker">
        <label>Emergency call
          <select aria-label="Emergency call" disabled={callsLoading || isSubmitting} value={selectedCall?.id ?? ''}
            onChange={(event) => {
              const selected = pendingCalls.find((call) => call.id === Number(event.target.value));
              if (selected) selectCall(selected); else clearCall();
              setSearchParams(selected ? { callId: String(selected.id) } : {}, { replace: true });
              setLatestResult(null);
              setLatestAssessment(null);
            }}>
            <option value="">{callsLoading ? 'Loading emergency calls…' : 'Enter a patient ID manually'}</option>
            {pendingCalls.map((call) => <option key={call.id} value={call.id}>Call #{call.id} · {call.patient.name} · {call.locationNode}</option>)}
          </select>
        </label>
        {selectedCall && <Link to={callPath('/resource-allocation', selectedCall.id)}>Continue to ambulance matching →</Link>}
        {callsError && <p role="alert">Could not load emergency calls: {callsError}</p>}
        {!callsLoading && !callsError && requestedId && !selectedCall && <p role="alert">This call is no longer pending. Choose another call or <Link to="/triage">enter a patient ID manually</Link>.</p>}
      </div>
      {errorMessage && <div className="triage-error" role="alert"><span>{errorMessage}</span><button type="button" onClick={() => loadQueue()}>Retry</button><button type="button" aria-label="Dismiss" onClick={() => setErrorMessage('')}>×</button></div>}
      <div className="triage-dashboard">
        <section>
          <TriageForm key={selectedCall?.patient.id ?? 'manual'} patient={selectedCall?.patient}
            isSubmitting={isSubmitting} disabled={Boolean(requestedId) && !selectedCall} onSubmit={handleEvaluate} />
          <TriageResult result={latestResult} assessment={latestAssessment} />
          {latestResult && (latestResultCall ? <div className="workflow-actions">
            <Link to={callPath('/resource-allocation', latestResultCall.id)}>Review ambulance matching →</Link>
          </div> : <p className="empty-note">Assessment saved. No pending emergency call is linked to this patient.</p>)}
        </section>
        <TriageQueue activeQueue={activeQueue} pendingCalls={pendingCalls} isLoading={isLoading} isConnected={isConnected} resolvingId={resolvingId} lastUpdated={lastUpdated} onRefresh={() => loadQueue()} onResolve={handleResolve} />
      </div>
    </main>
  );
}
