import React, { useEffect, useState, useCallback } from 'react';
import { fetchStaff, createStaff, updateStaff, deleteStaff } from '../../api/scheduling.api.js';
import EmptyState from '../common/EmptyState.jsx';
import StatusBadge from '../common/StatusBadge.jsx';
import { formatLabel } from '../../utils/formatLabel.js';

const ROLES = ['DOCTOR', 'PARAMEDIC', 'DRIVER'];
const CERTIFICATIONS = ['BASIC_LIFE_SUPPORT', 'ADVANCED_LIFE_SUPPORT', 'ECG_CERTIFIED', 'ICU_TRAINED'];
const EMPTY_FORM = { name: '', role: 'DOCTOR', certifications: [], maxWeeklyHours: 40 };

export default function StaffTab() {
  const [staff, setStaff] = useState([]);
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
      setStaff(await fetchStaff());
      setError(null);
    } catch (err) {
      console.error(err);
      setError('Could not load staff.');
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

  function openEditForm(member) {
    setEditingId(member.id);
    setForm({
      name: member.name,
      role: member.role,
      certifications: member.certifications ?? [],
      maxWeeklyHours: member.maxWeeklyHours,
    });
    setFormOpen(true);
  }

  function closeForm() {
    setFormOpen(false);
    setEditingId(null);
    setForm(EMPTY_FORM);
  }

  function toggleCertification(cert) {
    setForm((f) => ({
      ...f,
      certifications: f.certifications.includes(cert)
        ? f.certifications.filter((c) => c !== cert)
        : [...f.certifications, cert],
    }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setSaving(true);
    try {
      if (editingId) {
        await updateStaff(editingId, form);
      } else {
        await createStaff(form);
      }
      closeForm();
      await load();
      setError(null);
    } catch (err) {
      console.error(err);
      setError(err.message || 'Could not save staff member.');
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(id) {
    if (!window.confirm('Remove this staff member?')) return;
    setDeletingId(id);
    try {
      await deleteStaff(id);
      await load();
      setError(null);
    } catch (err) {
      console.error(err);
      setError(err.message || 'Could not delete staff member.');
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <div className="staff-tab">
      <div className="tab-header">
        <div className="tab-header-text">
          <h2>Staff</h2>
          <p className="tab-subtitle">Manage who's available to work, their role, and their certifications.</p>
        </div>
        <button type="button" className="btn-primary" onClick={openCreateForm}>+ Add staff</button>
      </div>

      {error && <div className="form-error">{error}</div>}

      {formOpen && (
        <form className="entity-form" onSubmit={handleSubmit}>
          <fieldset disabled={saving} className="fieldset-plain">
            <h3 className="entity-form-title">{editingId ? 'Edit staff member' : 'Add staff member'}</h3>

            <div className="form-row">
              <label>
                Name
                <input type="text" required value={form.name}
                  onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} />
              </label>
              <label>
                Role
                <select value={form.role} onChange={(e) => setForm((f) => ({ ...f, role: e.target.value }))}>
                  {ROLES.map((r) => <option key={r} value={r}>{formatLabel(r)}</option>)}
                </select>
              </label>
              <label>
                Max Weekly Hours
                <input type="number" min="1" required value={form.maxWeeklyHours}
                  onChange={(e) => setForm((f) => ({ ...f, maxWeeklyHours: parseInt(e.target.value, 10) || 0 }))} />
              </label>
            </div>

            <div className="form-row">
              <span className="form-label">Certifications</span>
              <div className="checkbox-group">
                {CERTIFICATIONS.map((cert) => (
                  <label key={cert} className="checkbox-item">
                    <input type="checkbox" checked={form.certifications.includes(cert)}
                      onChange={() => toggleCertification(cert)} />
                    {formatLabel(cert)}
                  </label>
                ))}
              </div>
            </div>

            <div className="form-actions">
              <button type="submit" className="btn-primary">
                {saving ? 'Saving…' : editingId ? 'Save changes' : 'Add staff'}
              </button>
              <button type="button" className="btn-secondary" onClick={closeForm}>Cancel</button>
            </div>
          </fieldset>
        </form>
      )}

      {loading ? (
        <EmptyState>Loading staff…</EmptyState>
      ) : staff.length === 0 ? (
        <EmptyState>No staff added yet.</EmptyState>
      ) : (
        <div className="entity-list">
          {staff.map((member) => (
            <div className="entity-row" key={member.id}>
              <div className="entity-row-main">
                <span className="entity-row-name">{member.name}</span>
                <StatusBadge>{formatLabel(member.role)}</StatusBadge>
                {member.certifications.map((cert) => (
                  <StatusBadge tone="muted" key={cert}>{formatLabel(cert)}</StatusBadge>
                ))}
              </div>
              <div className="entity-row-meta">{member.maxWeeklyHours}h/week max</div>
              <div className="entity-row-actions">
                <button type="button" className="btn-link" disabled={deletingId === member.id}
                  onClick={() => openEditForm(member)}>Edit</button>
                <button type="button" className="btn-link btn-danger" disabled={deletingId === member.id}
                  onClick={() => handleDelete(member.id)}>
                  {deletingId === member.id ? 'Deleting…' : 'Delete'}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}