import React from 'react';

const TABS = [
  { id: 'roster', label: 'Roster' },
  { id: 'run-compare', label: 'Run & Compare' },
  { id: 'staff', label: 'Staff' },
  { id: 'shift-template', label: 'Shift Template' },
];

export default function ScheduleTabs({ activeTab, onTabChange }) {
  return (
    <div className="schedule-tabs">
      {TABS.map((tab) => (
        <button
          key={tab.id}
          type="button"
          className={`schedule-tab${activeTab === tab.id ? ' active' : ''}`}
          onClick={() => onTabChange(tab.id)}
        >
          {tab.label}
        </button>
      ))}
    </div>
  );
}