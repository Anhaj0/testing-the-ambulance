import React from 'react';
import { NavLink } from 'react-router-dom';
import { useDispatchWorkflow } from './DispatchWorkflow.jsx';
import { callPath } from '../../utils/workflow.js';

const GROUPS = [
  {
    label: 'Live Dispatch',
    links: [
      { to: '/triage', label: 'Triage' },
      { to: '/resource-allocation', label: 'Resource Allocation' },
      { to: '/routing', label: 'Routing' },
      { to: '/network-detection', label: 'Network Detection' },
    ],
  },
  {
    label: 'Command & Planning',
    links: [
      { to: '/scheduling', label: 'Scheduling' },
    ],
  },
];

// Top-level nav shared by every module page. The brand doubles as the link
// back to the dashboard (the "/" route) — the standard place a logo goes.
export default function NavBar() {
  const { activeCall } = useDispatchWorkflow();
  return (
    <nav className="top-nav">
      <NavLink to="/" end className="top-nav-brand">Ambulance Dispatch</NavLink>
      {GROUPS.map((group) => (
        <div className="top-nav-group" key={group.label}>
          <span className="top-nav-group-label">{group.label}</span>
          <div className="top-nav-links">
            {group.links.map((link) => (
              <NavLink
                key={link.to}
                to={['/triage', '/resource-allocation'].includes(link.to) ? callPath(link.to, activeCall?.status === 'DISPATCHED' ? null : activeCall?.id) : link.to}
                className={({ isActive }) => `top-nav-link${isActive ? ' active' : ''}`}
              >
                {link.label}
              </NavLink>
            ))}
          </div>
        </div>
      ))}
    </nav>
  );
}
