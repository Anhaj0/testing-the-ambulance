import React from 'react';

// Generic "nothing to show yet" / loading / status message block. Replaces
// the scattered ad hoc <div className="empty-note"> instances across every
// tab with one shared component, so every module renders this consistently.
export default function EmptyState({ children }) {
  return <div className="empty-note">{children}</div>;
}