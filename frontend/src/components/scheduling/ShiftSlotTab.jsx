import React, { useEffect, useState, useCallback } from 'react';
import { fetchShiftSlots, createShiftSlot, updateShiftSlot, deleteShiftSlot } from '../../api/scheduling.api.js';
import EmptyState from '../common/EmptyState.jsx';
import StatusBadge from '../common/StatusBadge.jsx';
import { formatLabel } from '../../utils/formatLabel.js';

const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
const CERTIFICATIONS = ['BASIC_LIFE_SUPPORT', 'ADVANCED_LIFE_SUPPORT', 'ECG_CERTIFIED', 'ICU_TRAINED'];
const EMPTY_FORM = {
  dayOfWeek: 'MONDAY',
  startTime: '08:00',
  endTime: '16:00',
  requiredCertification: '',
  requiredStaffCount: 1,
};

export default function ShiftSlotTab() {
  const [slots, setSlots] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [formOpen, setFormOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setSlots(await fetchShiftSlots());
      setError(null);
    } catch (err) {
      console.error(err);
      setError('Could not load shift slots.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  function openCreateForm() {
    setEditingId(null);
    setForm(EMPTY_FORM);
    setFormOpen(true);
  }

  function openEditForm(slot) {
    setEditingId(slot.id);
    setForm({
      dayOfWeek: slot.dayOfWeek,
      startTime: slot.startTime,
      endTime: slot.endTime,
      requiredCertification: slot.requiredCertification ?? '',
      requiredStaffCount: slot.requiredStaffCount,
    });
    setFormOpen(true);
  }

  function closeForm() {
    setFormOpen(false);
    setEditingId(null);
    setForm(EMPTY_FORM);
  }

  async function handleSubmit(e) {
    e.preventDefault();
    const payload = {
      ...form,
      requiredCertification: form.requiredCertification === '' ? null : form.requiredCertification,
    };
    setSaving(true);
    try {
      if (editingId) {
        await updateShiftSlot(editingId, payload);
      } else {
        await createShiftSlot(payload);
      }
      closeForm();
      await load();
      setError(null);
    } catch (err) {
      console.error(err);
      setError(err.message || 'Could not save shift slot.');
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(id) {
    if (!window.confirm('Remove this shift slot from the weekly template?')) return;
    setDeletingId(id);
    try {
      await deleteShiftSlot(id);
      await load();
      setError(null);
    } catch (err) {
      console.error(err);
      setError(err.message || 'Could not delete shift slot.');
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <div className="shift-slot-tab">
      <div className="tab-header">
        <div className="tab-header-text">
          <h2>Shift template</h2>
          <p className="tab-subtitle">Set the weekly coverage pattern the scheduler fills each week.</p>
        </div>
        <button type="button" className="btn-primary" onClick={openCreateForm}>+ Add shift slot</button>
      </div>

      {error && <div className="form-error">{error}</div>}

      {formOpen && (
        <form className="entity-form" onSubmit={handleSubmit}>
          <fieldset disabled={saving} className="fieldset-plain">
            <h3 className="entity-form-title">{editingId ? 'Edit shift slot' : 'Add shift slot'}</h3>

            <div className="form-row">
              <label>
                Day
                <select value={form.dayOfWeek} onChange={(e) => setForm((f) => ({ ...f, dayOfWeek: e.target.value }))}>
                  {DAYS.map((d) => <option key={d} value={d}>{formatLabel(d)}</option>)}
                </select>
              </label>
              <label>
                Start Time
                <input type="time" required value={form.startTime}
                  onChange={(e) => setForm((f) => ({ ...f, startTime: e.target.value }))} />
              </label>
              <label>
                End Time
                <input type="time" required value={form.endTime}
                  onChange={(e) => setForm((f) => ({ ...f, endTime: e.target.value }))} />
              </label>
              <label>
                Staff Needed
                <input type="number" min="1" required value={form.requiredStaffCount}
                  onChange={(e) => setForm((f) => ({ ...f, requiredStaffCount: parseInt(e.target.value, 10) || 1 }))} />
              </label>
              <label>
                Required Certification
                <select value={form.requiredCertification}
                  onChange={(e) => setForm((f) => ({ ...f, requiredCertification: e.target.value }))}>
                  <option value="">Any (no requirement)</option>
                  {CERTIFICATIONS.map((c) => <option key={c} value={c}>{formatLabel(c)}</option>)}
                </select>
              </label>
            </div>

            <div className="form-actions">
              <button type="submit" className="btn-primary">
                {saving ? 'Saving…' : editingId ? 'Save changes' : 'Add shift slot'}
              </button>
              <button type="button" className="btn-secondary" onClick={closeForm}>Cancel</button>
            </div>
          </fieldset>
        </form>
      )}

      {loading ? (
        <EmptyState>Loading shift slots…</EmptyState>
      ) : slots.length === 0 ? (
        <EmptyState>No shift slots defined yet — this is the weekly coverage template the scheduler fills.</EmptyState>
      ) : (
        <div className="entity-list">
          {slots.map((slot) => (
            <div className="entity-row" key={slot.id}>
              <div className="entity-row-main">
                <span className="entity-row-name">{formatLabel(slot.dayOfWeek)}</span>
                <StatusBadge>{slot.startTime}–{slot.endTime}</StatusBadge>
                <StatusBadge tone="muted">{slot.requiredStaffCount} needed</StatusBadge>
                {slot.requiredCertification && (
                  <StatusBadge tone="muted">{formatLabel(slot.requiredCertification)}</StatusBadge>
                )}
              </div>
              <div className="entity-row-actions">
                <button type="button" className="btn-link" disabled={deletingId === slot.id}
                  onClick={() => openEditForm(slot)}>Edit</button>
                <button type="button" className="btn-link btn-danger" disabled={deletingId === slot.id}
                  onClick={() => handleDelete(slot.id)}>
                  {deletingId === slot.id ? 'Deleting…' : 'Delete'}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}