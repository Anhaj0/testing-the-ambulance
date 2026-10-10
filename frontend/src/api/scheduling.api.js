import { getJson, postJson, putJson, deleteJson } from './client.js';

const STAFF_BASE = '/api/v1/optimization/staff';
const SHIFT_SLOTS_BASE = '/api/v1/optimization/shift-slots';
const SCHEDULE_BASE = '/api/v1/optimization/schedules';

// --- Staff ---
export function fetchStaff() {
  return getJson(STAFF_BASE);
}
export function fetchStaffMember(id) {
  return getJson(`${STAFF_BASE}/${id}`);
}
export function createStaff(staff) {
  return postJson(STAFF_BASE, staff);
}
export function updateStaff(id, staff) {
  return putJson(`${STAFF_BASE}/${id}`, staff);
}
export function deleteStaff(id) {
  return deleteJson(`${STAFF_BASE}/${id}`);
}

// --- Shift Slots (recurring weekly coverage template) ---
export function fetchShiftSlots() {
  return getJson(SHIFT_SLOTS_BASE);
}
export function fetchShiftSlot(id) {
  return getJson(`${SHIFT_SLOTS_BASE}/${id}`);
}
export function createShiftSlot(slot) {
  return postJson(SHIFT_SLOTS_BASE, slot);
}
export function updateShiftSlot(id, slot) {
  return putJson(`${SHIFT_SLOTS_BASE}/${id}`, slot);
}
export function deleteShiftSlot(id) {
  return deleteJson(`${SHIFT_SLOTS_BASE}/${id}`);
}

// --- Scheduling runs ---

// request: { weekStarting, algorithm?, randomSeed?, gaParameters?, fitnessWeights?, persist? }
export function runSchedule(request) {
  return postJson(`${SCHEDULE_BASE}/runs`, request);
}

// Same request shape as runSchedule, minus algorithm (always runs both). Never persists.
export function compareSchedule(request) {
  return postJson(`${SCHEDULE_BASE}/comparisons`, request);
}

// weekStarting: 'YYYY-MM-DD' string (must be a Monday)
export function fetchRoster(weekStarting) {
  return getJson(`${SCHEDULE_BASE}?weekStarting=${weekStarting}`);
}

export function fetchScheduleDefaults() {
  return getJson(`${SCHEDULE_BASE}/defaults`);
}