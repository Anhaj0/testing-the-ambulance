import React, { useEffect, useState } from 'react';
import { fetchRoster, fetchShiftSlots } from '../../api/scheduling.api.js';
import { mondayFromWeekValue, currentWeekValue } from './dateUtils.js';
import EmptyState from '../common/EmptyState.jsx';
import StatusBadge from '../common/StatusBadge.jsx';
import { formatLabel } from '../../utils/formatLabel.js';

const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

function slotKey(slot) {
  return `${slot.dayOfWeek}|${slot.startTime}|${slot.endTime}`;
}

export default function RosterTab() {
  const [weekValue, setWeekValue] = useState(currentWeekValue());
  const [shifts, setShifts] = useState([]);
  const [slots, setSlots] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const weekStarting = weekValue ? mondayFromWeekValue(weekValue) : null;

  useEffect(() => {
    if (!weekStarting) return;
    setLoading(true);
    Promise.all([fetchRoster(weekStarting), fetchShiftSlots()])
      .then(([shiftData, slotData]) => {
        setShifts(shiftData);
        setSlots(slotData);
        setError(null);
      })
      .catch((err) => {
        console.error(err);
        setError(err.message || 'Could not load roster.');
      })
      .finally(() => setLoading(false));
  }, [weekStarting]);

  const shiftsBySlot = new Map();
  for (const shift of shifts) {
    const key = slotKey(shift);
    if (!shiftsBySlot.has(key)) shiftsBySlot.set(key, []);
    shiftsBySlot.get(key).push(shift);
  }

  const slotsByDay = DAYS.map((day) => ({
    day,
    slots: slots.filter((s) => s.dayOfWeek === day).sort((a, b) => a.startTime.localeCompare(b.startTime)),
  }));

  const totalSeats = slots.reduce((sum, s) => sum + s.requiredStaffCount, 0);
  const filledSeats = shifts.length;
  const gapCount = totalSeats - filledSeats;

  return (
    <div className="roster-tab">
      <div className="tab-header">
        <div className="tab-header-text">
          <h2>Roster</h2>
          <p className="tab-subtitle">See who's on shift, day by day.</p>
        </div>
        <label className="week-picker">
          Week
          <input type="week" value={weekValue} onChange={(e) => setWeekValue(e.target.value)} />
        </label>
      </div>

      {weekStarting && slots.length > 0 && (
        <div className="roster-summary">
          <StatusBadge tone={gapCount > 0 ? 'danger' : 'ok'}>{filledSeats} / {totalSeats} seats filled</StatusBadge>
          {gapCount > 0 && <StatusBadge tone="danger">{gapCount} gap{gapCount === 1 ? '' : 's'}</StatusBadge>}
        </div>
      )}

      {error && <div className="form-error">{error}</div>}

      {loading ? (
        <EmptyState>Loading roster…</EmptyState>
      ) : slots.length === 0 ? (
        <EmptyState>No shift slots defined yet — build the weekly template in the Shift Template tab first.</EmptyState>
      ) : (
        <div className="roster-days">
          {slotsByDay.map(({ day, slots: daySlots }) => daySlots.length > 0 && (
            <div className="roster-day" key={day}>
              <h3>{formatLabel(day)}</h3>
              <div className="roster-slot-list">
                {daySlots.map((slot) => {
                  const assigned = shiftsBySlot.get(slotKey(slot)) || [];
                  const gaps = Math.max(slot.requiredStaffCount - assigned.length, 0);
                  return (
                    <div className="roster-slot" key={slot.id}>
                      <div className="roster-slot-header">
                        <StatusBadge>{slot.startTime}–{slot.endTime}</StatusBadge>
                        {slot.requiredCertification && (
                          <StatusBadge tone="muted">{formatLabel(slot.requiredCertification)}</StatusBadge>
                        )}
                      </div>
                      <div className="roster-slot-staff">
                        {assigned.map((shift) => (
                          <span className="roster-staff-chip" key={shift.id ?? shift.staffId}>{shift.staffName}</span>
                        ))}
                        {Array.from({ length: gaps }).map((_, i) => (
                          <span className="roster-staff-chip roster-staff-chip-gap" key={`gap-${i}`}>⚠ Unfilled</span>
                        ))}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}