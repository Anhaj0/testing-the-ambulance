import React from 'react';

const TONE_CLASS = {
  default: 'badge',
  muted: 'badge badge-muted',
  ok: 'badge badge-ok',
  danger: 'badge badge-danger',
};

// Small colored label used across Staff/Shift Slot/Roster tabs for roles,
// certifications, time ranges, and fill/gap status.
export default function StatusBadge({ tone = 'default', children }) {
  return <span className={TONE_CLASS[tone] || TONE_CLASS.default}>{children}</span>;
}