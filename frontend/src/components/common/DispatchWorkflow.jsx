import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { callPath } from '../../utils/workflow.js';

const WorkflowContext = createContext(null);

export function DispatchWorkflowProvider({ children }) {
  const [activeCall, setActiveCall] = useState(null);
  const [lastDispatch, setLastDispatch] = useState(null);
  const selectCall = useCallback((call) => setActiveCall(call), []);
  const clearCall = useCallback(() => setActiveCall(null), []);
  const value = useMemo(() => ({ activeCall, selectCall, clearCall, lastDispatch, setLastDispatch }),
    [activeCall, selectCall, clearCall, lastDispatch]);
  return <WorkflowContext.Provider value={value}>{children}</WorkflowContext.Provider>;
}

export function useDispatchWorkflow() {
  return useContext(WorkflowContext);
}

export function DispatchContextBar() {
  const { activeCall, clearCall, lastDispatch } = useDispatchWorkflow();
  const navigate = useNavigate();
  if (!activeCall && !lastDispatch) return null;
  return (
    <aside className="workflow-bar" aria-label="Current dispatch context">
      {activeCall && <>
        <span><strong>Call #{activeCall.id}</strong> · {activeCall.patient.name} · {activeCall.locationNode}{activeCall.status === 'DISPATCHED' ? ' · Dispatched' : ''}</span>
        {activeCall.status === 'DISPATCHED' ? <>
          <Link to="/resource-allocation">Next emergency</Link>
          <Link to="/network-detection">Review coverage</Link>
        </> : <>
          <Link to={callPath('/triage', activeCall.id)}>Assess patient</Link>
          <Link to={callPath('/resource-allocation', activeCall.id)}>Match ambulance</Link>
        </>}
        <button type="button" onClick={() => { clearCall(); navigate('/'); }}>Clear selection</button>
      </>}
      {lastDispatch?.routeUrl && <Link to={lastDispatch.routeUrl}>Call #{lastDispatch.callId} · Dispatched {lastDispatch.vehicleNumber} · View route</Link>}
    </aside>
  );
}
