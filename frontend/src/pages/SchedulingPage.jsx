import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useDispatchWorkflow } from '../components/common/DispatchWorkflow.jsx';
import { callPath } from '../utils/workflow.js';
import ScheduleTabs from '../components/scheduling/ScheduleTabs.jsx';
import StaffTab from '../components/scheduling/StaffTab.jsx';
import ShiftSlotTab from '../components/scheduling/ShiftSlotTab.jsx';
import RunCompareTab from '../components/scheduling/RunCompareTab.jsx';
import RosterTab from '../components/scheduling/RosterTab.jsx';
import '../styles/scheduling.css';

export default function SchedulingPage() {
  const { activeCall } = useDispatchWorkflow();
  const [activeTab, setActiveTab] = useState('roster');

  return (
    <div className="schedule-page">
      <div className="workflow-actions schedule-workflow-actions">
        <span>Staffing support</span>
        <Link to={callPath('/resource-allocation', activeCall?.id)}>Return to ambulance matching →</Link>
        <Link to="/network-detection">View fleet coverage →</Link>
      </div>
      <ScheduleTabs activeTab={activeTab} onTabChange={setActiveTab} />
      <div className="schedule-tab-content">
        {activeTab === 'roster' && <RosterTab />}
        {activeTab === 'run-compare' && <RunCompareTab />}
        {activeTab === 'staff' && <StaffTab />}
        {activeTab === 'shift-template' && <ShiftSlotTab />}
      </div>
    </div>
  );
}
