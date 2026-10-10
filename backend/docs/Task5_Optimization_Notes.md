# Task 5 — Staff Scheduling Optimization Module

## Overview

This module builds a weekly staff roster: given a recurring weekly coverage
template (`ShiftSlot`s, each wanting some number of qualified staff) and a
pool of `Staff`, it assigns a staff member to every seat that needs filling.
Two algorithms solve the same problem so they can be compared directly:

- **Genetic Algorithm (GA)** — the primary solver; its result is what
  actually gets persisted as the week's roster.
- **Greedy** — a fast, single-pass baseline used only to generate
  comparison data (solution quality, constraint violations, execution
  time) for the evaluation chapter. It is never persisted.

## Algorithm

**Genetic Algorithm.** Shift scheduling under staffing, overtime, rest and
fairness constraints is an NP-hard constraint-satisfaction problem, so an
exact method (e.g. ILP) becomes computationally infeasible as the roster
grows — a GA was chosen as an approximation method that scales instead.

- Each **chromosome** (`RosterChromosome`) is a full weekly roster: one
  gene per seat, aligned index-for-index with `SchedulingProblem`'s
  expanded slot list (a `ShiftSlot` wanting N staff appears N times, one
  entry per seat).
- **Initialization** — every gene in every chromosome starts as a
  uniformly-random staff member.
- **Selection** — tournament selection (sample `tournamentSize`
  chromosomes at random, keep the fittest).
- **Crossover** — single-point: genes before a random cut point come from
  parent A, the rest from parent B, applied with probability
  `crossoverRate` (otherwise the child is an unmodified copy of parent A).
- **Mutation** — per-gene, independently reassigns the gene to a random
  staff member with probability `mutationRate`.
- **Elitism** — the fittest `elitismCount` chromosomes are carried into
  the next generation unchanged, so the best solution found never
  regresses.
- **Stopping** — runs until `maxGenerations`, or earlier once the best
  fitness has improved by less than `convergenceThreshold` over the last
  `convergenceWindow` generations.
- **Complexity** — one generation costs O(P·n log n) to evaluate (P
  chromosomes, each an O(n log n) fitness pass over n seats) plus O(P·n)
  to produce the next generation, so a full run is O(G·P·n log n) over G
  generations.

**Greedy baseline.** Fills each seat once, in order, with the qualified
staff member who has worked the fewest hours so far and still has
capacity under their `maxWeeklyHours`; if no qualified staff member has
capacity left, it falls back to the least-loaded qualified staff member
anyway (recording an overtime violation) rather than leaving the seat
empty. There is no backtracking, so a locally "fair" run of choices can
still leave a later seat understaffed even when a better full assignment
exists. Complexity: O(n·m) — a linear scan of the m staff members for
each of the n seats.

## Fitness Function

`RosterFitnessEvaluator` scores a chromosome as a single scalar
(`fitness = -totalPenalty`, so higher is better — the GA maximises this),
built from four weighted penalty terms (`FitnessWeights`):

| Violation | How it's counted | Default weight |
|---|---|---|
| Understaffed | seats left empty or filled by a staff member missing the required certification | `understaffedPenalty` = 100 per seat |
| Overtime | hours worked beyond a staff member's `maxWeeklyHours`, summed across staff | `overtimePenaltyPerHour` = 10 per hour |
| Insufficient rest | back-to-back shifts (per staff member) closer together than `minRestHours` (default 8h) | `restViolationPenalty` = 50 per violation |
| Unfair distribution | population standard deviation of hours worked across the whole staff pool | `fairnessWeight` = 1.0 |

`FitnessResult` reports each term separately alongside the combined
`totalPenalty`/`fitness`, so the evaluation chapter can break down *why*
one run scored worse than another, not just by how much.

Default GA parameters (`GAParameters.defaults()`): population 50, max
generations 200, crossover rate 0.8, mutation rate 0.05, elitism 2,
tournament size 5, convergence threshold 0.001 over a 20-generation
window. Both `GAParameters` and `FitnessWeights` can be overridden
per-request (see API below) without touching code, so they can be varied
experimentally.

## Data Model

- **Staff** — a schedulable person: `name`, `certifications`,
  `maxWeeklyHours`.
- **ShiftSlot** — a recurring weekly coverage requirement: `dayOfWeek`,
  `startTime`, `endTime`, `requiredStaffCount`, an optional
  `requiredCertification`. Shifts that cross midnight are handled (end
  time not after start time means the shift ends the next day).
- **Shift** — one persisted assignment: a `ShiftSlot` + `Staff` +
  `weekStarting`, produced by writing out the GA's winning chromosome.
- **SchedulingProblem** — the immutable, in-memory unit both algorithms
  are scored against: the expanded per-seat slot list, the staff pool,
  and the target week (`weekStarting`, must be a Monday).

## API

All endpoints are under `/api/v1/optimization`.

| Endpoint | Description |
|---|---|
| `POST /schedules/runs` | Runs the GA and, unless `persist: false`, saves the resulting roster. Rejects any `algorithm` other than the default (GA) — use `/comparisons` for Greedy. |
| `POST /schedules/comparisons` | Runs the GA and the Greedy baseline against the same problem, side by side. Never persists. |
| `GET /schedules?weekStarting=YYYY-MM-DD` | The persisted roster for a given week. |
| `GET /schedules/defaults` | The default `GAParameters` and `FitnessWeights`, so callers know what's tunable. |
| `/staff`, `/shift-slots` | Standard CRUD for the staff pool and the shift-slot template. |

**Request body (`ScheduleRunRequest`, used by both `/runs` and
`/comparisons`):**
```json
{
  "weekStarting": "2026-09-07",
  "randomSeed": 42,
  "gaParameters": { "populationSize": 100 },
  "fitnessWeights": { "overtimePenaltyPerHour": 15.0 },
  "persist": true
}
```
`randomSeed` seeds the GA's `Random` for a reproducible run (omit for a
fresh random run each time); `gaParameters`/`fitnessWeights` only need to
carry the fields being overridden — everything else falls back to the
documented default.

**Response body (`ScheduleRunResponse`):**
```json
{
  "algorithmName": "Genetic Algorithm",
  "fitnessResult": {
    "understaffedViolations": 0,
    "overtimeHours": 0.0,
    "restViolations": 0,
    "fairnessStdDevHours": 3.2,
    "totalPenalty": 3.2,
    "fitness": -3.2
  },
  "executionTimeMillis": 68,
  "generationsRun": 102,
  "bestFitnessHistory": [-700.0, -412.5, "...", -3.2],
  "assignments": [
    { "id": 1, "staffId": 4, "staffName": "J. Perera", "dayOfWeek": "MONDAY",
      "startTime": "08:00", "endTime": "16:00", "weekStarting": "2026-09-07" }
  ]
}
```
`/comparisons` returns `{ "geneticAlgorithm": ..., "greedy": ... }`, each
shaped like the response above (the Greedy side always has
`generationsRun: 0` and a single-point `bestFitnessHistory`, so it still
plots alongside the GA's convergence curve).

## Benchmark Results

[docs/scheduling-benchmark-results.csv](scheduling-benchmark-results.csv)
holds GA-vs-Greedy runs across increasing problem sizes (seat count `N`,
staff pool size), each row an average over repeated runs: execution time
(mean/median/stddev), memory delta, generations run, final fitness, and
the four violation counts. It shows the expected trade-off — Greedy is
consistently faster (sub-millisecond regardless of `N`) but converges to
a noticeably worse fitness with more constraint violations, while the GA
takes tens of milliseconds and generations but reaches near-zero
violations on the same problem.

## Frontend

- `src/api/scheduling.api.js` — CRUD for staff/shift-slots plus
  `runSchedule`, `compareSchedule`, `fetchRoster`, `fetchScheduleDefaults`.
- `src/pages/SchedulingPage.jsx` — tabbed page (`ScheduleTabs.jsx`)
  switching between:
  - `RosterTab.jsx` — view the persisted roster for a chosen week.
  - `RunCompareTab.jsx` — trigger a run or GA-vs-Greedy comparison;
    renders `FitnessScorecard.jsx` (violation breakdown) and
    `ConvergenceChart.jsx` (best-fitness-per-generation curve).
  - `StaffTab.jsx` / `ShiftSlotTab.jsx` — CRUD for staff and the
    shift-slot template.
- `src/styles/scheduling.css` — module-specific styling, following the
  shared design tokens in `styles/variables.css`.

## Known Limitations / Future Work

- The GA's initial population is fully random with no seeding from the
  Greedy solution or from the previous week's roster, which likely costs
  it convergence speed on larger problems.
- `persistRoster` deletes and re-saves all `Shift` rows for the week
  without wrapping both steps in one transaction — acceptable for this
  coursework's single-writer scenario, but not safe under concurrent
  writers.
- Rest-period and fairness checks only look within the week being
  scheduled; a shift at the very start of a week is not checked against
  the previous week's last shift.
