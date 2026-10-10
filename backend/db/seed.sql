-- Ambulance Dispatch System — Supabase/Postgres seed script (Colombo only)
-- ============================================================================
-- Inserts a sample dataset across every application table, all locations
-- within Colombo. Safe to run on empty tables; run db/cleanup.sql first if
-- the tables already have data (explicit ids start at 1).
--
-- Run with: psql "<connection string>" -f db/seed.sql
-- or paste into the Supabase SQL editor.
-- ============================================================================

BEGIN;

-- ---------------------------------------------------------------------------
-- road_node — vertices of the routing graph
-- ---------------------------------------------------------------------------
INSERT INTO road_node (id, name, latitude, longitude) VALUES
    (1,  'City General Hospital Junction', 6.9271, 79.8612),
    (2,  'Central Bus Stand',              6.9319, 79.8478),
    (3,  'Riverside Market',               6.9147, 79.8730),
    (4,  'Northgate Interchange',          6.9450, 79.8600),
    (5,  'Fire Station Circle',            6.9200, 79.8550),
    (6,  'Borella Junction',               6.9147, 79.8779),
    (7,  'Kollupitiya Junction',           6.9101, 79.8481),
    (8,  'Dematagoda Flyover',             6.9377, 79.8759),
    (9,  'Wellawatte Junction',            6.8778, 79.8600),
    (10, 'Maradana Railway Circle',        6.9280, 79.8640);

-- ---------------------------------------------------------------------------
-- road_edge — directed, weighted edges between road_node
-- ---------------------------------------------------------------------------
INSERT INTO road_edge (id, from_node_id, to_node_id, distance_km, travel_time_minutes, blocked) VALUES
    (1,  1, 2,  3.2, 7.5,  false),
    (2,  2, 3,  4.8, 11.0, false),
    (3,  3, 1,  5.1, 12.5, false),
    (4,  2, 4,  6.0, 14.0, false),
    (5,  4, 5,  2.7, 6.0,  false),
    (6,  5, 1,  3.9, 9.0,  true), -- blocked road, excluded by the routing module
    (7,  6, 1,  2.5, 8.0,  false),
    (8,  7, 1,  1.8, 6.0,  false),
    (9,  8, 6,  3.0, 9.0,  false),
    (10, 9, 7,  4.2, 10.5, false),
    (11, 10, 2, 1.5, 5.0,  false);

-- ---------------------------------------------------------------------------
-- hospital
-- ---------------------------------------------------------------------------
INSERT INTO hospital (id, name, location, available_beds) VALUES
    (1, 'City General Hospital',           'Downtown',      12),
    (2, 'St. Mary Medical Center',         'Riverside',     5),
    (3, 'Northgate Regional Hospital',     'Northgate',     20),
    (4, 'National Hospital of Sri Lanka',  'Colombo 10',    30),
    (5, 'Colombo South Teaching Hospital', 'Kalubowila',    22),
    (6, 'Lady Ridgeway Hospital',          'Colombo 8',     10);

-- ---------------------------------------------------------------------------
-- ambulance + its equipment set
-- ---------------------------------------------------------------------------
INSERT INTO ambulance (id, vehicle_number, current_location_node, status) VALUES
    (1, 'AMB-001', 'City General Hospital Junction', 'AVAILABLE'),
    (2, 'AMB-002', 'Central Bus Stand',               'DISPATCHED'),
    (3, 'AMB-003', 'Northgate Interchange',           'OUT_OF_SERVICE'),
    (4, 'AMB-004', 'Borella Junction',                'AVAILABLE'),
    (5, 'AMB-005', 'Kollupitiya Junction',            'DISPATCHED'),
    (6, 'AMB-006', 'Maradana Railway Circle',         'AVAILABLE');

INSERT INTO ambulance_equipment (ambulance_id, equipment) VALUES
    (1, 'DEFIBRILLATOR'),
    (1, 'OXYGEN_SUPPLY'),
    (2, 'ECG_MONITOR'),
    (2, 'OXYGEN_SUPPLY'),
    (3, 'VENTILATOR'),
    (3, 'ICU_EQUIPMENT'),
    (4, 'DEFIBRILLATOR'),
    (4, 'ECG_MONITOR'),
    (5, 'OXYGEN_SUPPLY'),
    (5, 'VENTILATOR'),
    (6, 'ICU_EQUIPMENT'),
    (6, 'OXYGEN_SUPPLY');

-- ---------------------------------------------------------------------------
-- patient + required equipment set
-- ---------------------------------------------------------------------------
INSERT INTO patient (id, name, age, condition, urgency_level, contact_number) VALUES
    (1, 'Nimal Perera',          54, 'Chest pain and shortness of breath',    'CRITICAL', '0771234567'),
    (2, 'Kamala Silva',          29, 'Fractured arm after a fall',            'MEDIUM',   '0772345678'),
    (3, 'Ruwan Fernando',        41, 'High fever and persistent cough',       'LOW',      '0773456789'),
    (4, 'Sandun Wickramasinghe', 63, 'Suspected stroke, slurred speech',      'CRITICAL', '0714567890'),
    (5, 'Dilani Rajapaksha',     34, 'Severe abdominal pain, pregnant',       'HIGH',     '0765678901'),
    (6, 'Mohamed Rizwan',        19, 'Road traffic accident, leg trauma',     'CRITICAL', '0756789012'),
    (7, 'Tharshini Sivakumar',   47, 'Diabetic emergency, low consciousness', 'HIGH',     '0777890123');

INSERT INTO patient_required_equipment (patient_id, required_equipment) VALUES
    (1, 'DEFIBRILLATOR'),
    (1, 'OXYGEN_SUPPLY'),
    (2, 'OXYGEN_SUPPLY'),
    (4, 'DEFIBRILLATOR'),
    (4, 'ECG_MONITOR'),
    (5, 'OXYGEN_SUPPLY'),
    (6, 'VENTILATOR'),
    (7, 'ICU_EQUIPMENT');

-- ---------------------------------------------------------------------------
-- staff + certifications set
-- ---------------------------------------------------------------------------
INSERT INTO staff (id, name, role, max_weekly_hours) VALUES
    (1, 'Dr. Anushka Jayasuriya',   'DOCTOR',    40),
    (2, 'Priyantha Kumara',         'PARAMEDIC', 45),
    (3, 'Chamara Bandara',          'DRIVER',    40),
    (4, 'Dr. Nadeesha Gunawardena', 'DOCTOR',    40),
    (5, 'Sunil Ratnayake',          'PARAMEDIC', 42),
    (6, 'Fathima Nazreen',          'DRIVER',    40),
    (7, 'Dr. Kasun Abeysekara',     'DOCTOR',    38);

INSERT INTO staff_certifications (staff_id, certifications) VALUES
    (1, 'ADVANCED_LIFE_SUPPORT'),
    (1, 'ICU_TRAINED'),
    (2, 'BASIC_LIFE_SUPPORT'),
    (2, 'ECG_CERTIFIED'),
    (3, 'BASIC_LIFE_SUPPORT'),
    (4, 'ADVANCED_LIFE_SUPPORT'),
    (4, 'ECG_CERTIFIED'),
    (5, 'BASIC_LIFE_SUPPORT'),
    (5, 'ICU_TRAINED'),
    (6, 'BASIC_LIFE_SUPPORT'),
    (7, 'ADVANCED_LIFE_SUPPORT'),
    (7, 'ICU_TRAINED');

-- ---------------------------------------------------------------------------
-- shift_slot — weekly roster template
-- ---------------------------------------------------------------------------
INSERT INTO shift_slot (id, day_of_week, start_time, end_time, required_certification, required_staff_count) VALUES
    (1, 'MONDAY',    '08:00:00', '16:00:00', 'ECG_CERTIFIED',          1),
    (2, 'MONDAY',    '16:00:00', '23:59:59', NULL,                     2),
    (3, 'TUESDAY',   '08:00:00', '16:00:00', 'ICU_TRAINED',            1),
    (4, 'WEDNESDAY', '00:00:00', '08:00:00', 'ADVANCED_LIFE_SUPPORT',  1),
    (5, 'THURSDAY',  '08:00:00', '16:00:00', NULL,                     2),
    (6, 'FRIDAY',    '16:00:00', '23:59:59', 'ICU_TRAINED',            1);

-- ---------------------------------------------------------------------------
-- shift — concrete staff -> shift_slot assignments for a given week
-- ---------------------------------------------------------------------------
INSERT INTO shift (id, shift_slot_id, staff_id, week_starting) VALUES
    (1, 1, 2, '2026-09-07'),
    (2, 2, 3, '2026-09-07'),
    (3, 3, 1, '2026-09-08'),
    (4, 4, 4, '2026-09-07'),
    (5, 5, 5, '2026-09-07'),
    (6, 6, 7, '2026-09-08');

-- ---------------------------------------------------------------------------
-- call + its required equipment set
-- ---------------------------------------------------------------------------
INSERT INTO call (id, patient_id, location_node, received_at, status, assigned_ambulance_id, destination_hospital_id) VALUES
    (1, 1, 'City General Hospital Junction', now(), 'DISPATCHED', 1,    1),
    (2, 2, 'Central Bus Stand',              now(), 'RECEIVED',   NULL, NULL),
    (3, 4, 'Borella Junction',               now(), 'DISPATCHED', 4,    4),
    (4, 5, 'Kollupitiya Junction',           now(), 'EN_ROUTE',   5,    1),
    (5, 6, 'Wellawatte Junction',            now(), 'RECEIVED',   NULL, NULL),
    (6, 7, 'Maradana Railway Circle',        now(), 'DISPATCHED', 6,    6);

INSERT INTO call_required_equipment (call_id, required_equipment) VALUES
    (1, 'DEFIBRILLATOR'),
    (3, 'DEFIBRILLATOR'),
    (3, 'ECG_MONITOR'),
    (4, 'OXYGEN_SUPPLY'),
    (6, 'ICU_EQUIPMENT');

-- ---------------------------------------------------------------------------
-- triage_assessments + symptoms list
-- ---------------------------------------------------------------------------
INSERT INTO triage_assessments
    (id, patient_id, breathing, pulse_rate, avpu, oxygen_saturation, systolic_bp,
     pain_score, temperature, age, hazard_present, assigned_category,
     tie_breaker_score, created_at, resolved, severity_rank) VALUES
    ('70a530c0-dab1-4996-8177-7a4ebe1eba89', 1, true,  118, 'ALERT',  91, 100, 8, 37.8, 54, false, 'RED',    92.5, now(), false, 5),
    ('794935e4-2ca7-47af-8519-4ff5f134661c', 3, true,  88,  'ALERT',  97, 118, 3, 38.9, 41, false, 'YELLOW', 41.0, now(), false, 3),
    ('b1f3c2a0-1a2b-4c3d-9e4f-5a6b7c8d9e0f', 4, true,  102, 'VOICE',  93, 160, 6, 37.1, 63, false, 'RED',    88.0, now(), false, 5),
    ('c2e4d3b1-2b3c-5d4e-0f5a-6b7c8d9e0f1a', 5, true,  96,  'ALERT',  95, 110, 7, 37.5, 34, false, 'ORANGE', 65.5, now(), false, 4),
    ('d3f5e4c2-3c4d-6e5f-1a6b-7c8d9e0f1a2b', 6, true,  130, 'ALERT',  90, 90,  9, 36.9, 19, true,  'RED',    95.0, now(), false, 5),
    ('e4a6f5d3-4d5e-7f6a-2b7c-8d9e0f1a2b3c', 7, false, 110, 'PAIN',   88, 105, 4, 38.2, 47, false, 'ORANGE', 70.0, now(), false, 4);

INSERT INTO triage_assessment_symptoms (assessment_id, symptom, symptom_order) VALUES
    ('70a530c0-dab1-4996-8177-7a4ebe1eba89', 'Chest pain',          0),
    ('70a530c0-dab1-4996-8177-7a4ebe1eba89', 'Shortness of breath', 1),
    ('794935e4-2ca7-47af-8519-4ff5f134661c', 'Fever',               0),
    ('794935e4-2ca7-47af-8519-4ff5f134661c', 'Cough',               1),
    ('b1f3c2a0-1a2b-4c3d-9e4f-5a6b7c8d9e0f', 'Slurred speech',      0),
    ('b1f3c2a0-1a2b-4c3d-9e4f-5a6b7c8d9e0f', 'Facial drooping',     1),
    ('c2e4d3b1-2b3c-5d4e-0f5a-6b7c8d9e0f1a', 'Abdominal pain',      0),
    ('d3f5e4c2-3c4d-6e5f-1a6b-7c8d9e0f1a2b', 'Leg deformity',       0),
    ('d3f5e4c2-3c4d-6e5f-1a6b-7c8d9e0f1a2b', 'Heavy bleeding',      1),
    ('e4a6f5d3-4d5e-7f6a-2b7c-8d9e0f1a2b3c', 'Confusion',           0),
    ('e4a6f5d3-4d5e-7f6a-2b7c-8d9e0f1a2b3c', 'Sweating',            1);

-- ---------------------------------------------------------------------------
-- Re-sync identity sequences so the next app-generated insert doesn't
-- collide with the explicit ids used above.
-- ---------------------------------------------------------------------------
SELECT setval(pg_get_serial_sequence('road_node', 'id'), (SELECT MAX(id) FROM road_node));
SELECT setval(pg_get_serial_sequence('road_edge', 'id'), (SELECT MAX(id) FROM road_edge));
SELECT setval(pg_get_serial_sequence('hospital', 'id'), (SELECT MAX(id) FROM hospital));
SELECT setval(pg_get_serial_sequence('ambulance', 'id'), (SELECT MAX(id) FROM ambulance));
SELECT setval(pg_get_serial_sequence('patient', 'id'), (SELECT MAX(id) FROM patient));
SELECT setval(pg_get_serial_sequence('staff', 'id'), (SELECT MAX(id) FROM staff));
SELECT setval(pg_get_serial_sequence('shift_slot', 'id'), (SELECT MAX(id) FROM shift_slot));
SELECT setval(pg_get_serial_sequence('shift', 'id'), (SELECT MAX(id) FROM shift));
SELECT setval(pg_get_serial_sequence('call', 'id'), (SELECT MAX(id) FROM call));

COMMIT;
