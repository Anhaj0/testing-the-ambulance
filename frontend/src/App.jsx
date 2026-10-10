import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import NavBar from './components/common/NavBar.jsx';
import { DispatchWorkflowProvider, DispatchContextBar } from './components/common/DispatchWorkflow.jsx';
import DashboardPage from './pages/DashboardPage.jsx';
import NetworkDetectionPage from './pages/NetworkDetectionPage.jsx';
import TriagePage from './pages/TriagePage.jsx';
import SchedulingPage from './pages/SchedulingPage.jsx';
import ResourceAllocationPage from './pages/ResourceAllocationPage.jsx';
import RoutingPage from './pages/RoutingPage.jsx';

export default function App() {
  return (
    <BrowserRouter>
      <DispatchWorkflowProvider>
      <div className="app-shell">
        <NavBar />
        <DispatchContextBar />
        <div className="app-content">
          <Routes>
            <Route path="/" element={<DashboardPage />} />
            <Route path="/network-detection" element={<NetworkDetectionPage />} />
            <Route path="/triage" element={<TriagePage />} />
            <Route path="/scheduling" element={<SchedulingPage />} />
            <Route path="/resource-allocation" element={<ResourceAllocationPage />} />
            <Route path="/routing" element={<RoutingPage />} />
          </Routes>
        </div>
      </div>
      </DispatchWorkflowProvider>
    </BrowserRouter>
  );
}
