-- ============================================================================
-- Ambulance Dispatch System — Supabase/Postgres cleanup script
-- ============================================================================
-- Wipes ALL rows from every application table (including @ElementCollection
-- join tables) and resets identity sequences back to 1, so the next insert
-- (from this seed script or from the running app) starts clean at id = 1.
--
-- DESTRUCTIVE — this deletes every row in these tables. Run only against a
-- dev/test database, and take a backup/snapshot first if you're unsure.
--
-- Run with: psql "<connection string>" -f db/cleanup.sql
-- or paste into the Supabase SQL editor.
-- ============================================================================

TRUNCATE TABLE
    triage_assessment_symptoms,
    triage_assessments,
    call_required_equipment,
    call,
    shift,
    shift_slot,
    staff_certifications,
    staff,
    patient_required_equipment,
    patient,
    ambulance_equipment,
    ambulance,
    road_edge,
    road_node,
    hospital
RESTART IDENTITY CASCADE;
