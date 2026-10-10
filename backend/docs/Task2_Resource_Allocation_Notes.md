# Task 2 — Resource Allocation (Dispatch) Module

## Overview

This module matches a pending emergency `Call` to the best available
`Ambulance` in the fleet. It is exposed as both a committing action
(dispatch) and a read-only preview (candidate ranking), so a dispatch
board UI can show a dispatcher what the algorithm would do before they
commit to it. It calls into the [Routing module](Task1_Routing_Notes.md)
internally for real shortest-path travel times rather than a straight-line
estimate.

## Algorithm

**Greedy selection.** For a given call, every `AVAILABLE` ambulance that
carries all the call's required equipment is scored, and the
lowest-scoring (best) one is chosen — a single best-first pick with no
backtracking or lookahead across multiple calls.

- **Score** (`FitnessEvaluator`) = real shortest-path travel time in
  minutes from the ambulance's current node to the call's location
  (via `RouteService`/A*), **plus** a penalty of `5.0` minutes per unit
  of equipment the ambulance carries beyond what the call requires. The
  penalty discourages sending an over-equipped ambulance somewhere a more
  specialized one, held back for a job that actually needs it, would have
  been the better use of the fleet.
- An ambulance that cannot reach the call at all (no recorded location,
  or the routing module finds no path) scores `Double.POSITIVE_INFINITY`
  (`FitnessEvaluator.UNREACHABLE`) and is filtered out rather than
  compared — every unreachable ambulance would otherwise tie.
- **Ranking** (`GreedyScheduler.rankCandidates`) scores every eligible
  ambulance once, then sorts — not the other way around — so a fleet of
  *n* ambulances costs *n* route lookups, not 2*n* from re-running the
  search inside a comparator. All candidates for one dispatch decision
  are scored against a single `RoutingSnapshot` of the road network,
  loaded once up front, instead of each candidate re-fetching the full
  edge list from the database.
- `findBestAmbulance` (used by the actual dispatch) is just
  `rankCandidates(...).findFirst()` — the committed decision is always
  exactly what the candidate-ranking endpoint would have shown.

## Data Model

Uses the shared `common` entities directly rather than module-owned ones:

- **Call** — `locationNode`, `requiredEquipment`, `status`
  (`RECEIVED`/`DISPATCHED`/…), `assignedAmbulance`, linked `Patient`
  (name, `urgencyLevel`).
- **Ambulance** — `vehicleNumber`, `currentLocationNode`, `status`
  (`AVAILABLE`/`DISPATCHED`/…), `equipment` (`Set<MedicalEquipment>`).

## API

All endpoints are under `/api/v1/calls`.

| Endpoint | Description |
|---|---|
| `POST /{id}/dispatch` | Runs the greedy scheduler and, if a suitable ambulance exists, assigns it — sets the call to `DISPATCHED` and the ambulance to `DISPATCHED`. |
| `GET /{id}/candidates` | Ranks every eligible ambulance for the call best-first, exactly as `/dispatch` would decide, without assigning anything. Read-only; safe to poll as the dispatcher reviews a call. |
| `GET /pending` | All calls still awaiting dispatch (`status = RECEIVED`). |
| `GET /ambulances` | The full fleet, regardless of status. |

**Response (`DispatchResultDto`, from `POST /{id}/dispatch`):**
```json
{
  "dispatched": true,
  "callId": 7,
  "ambulanceVehicleNumber": "AMB-003",
  "message": "Ambulance AMB-003 dispatched successfully."
}
```
`dispatched: false` (with `ambulanceVehicleNumber: null`) means no
equipment-eligible, reachable ambulance was available — the call stays
`RECEIVED` and the fleet is left untouched. This is a normal outcome, not
an error.

**Response (`CandidateDto[]`, from `GET /{id}/candidates`), best-first:**
```json
[
  { "ambulanceId": 3, "vehicleNumber": "AMB-003", "travelMinutes": 6.5,
    "extraEquipmentCount": 0, "score": 6.5 },
  { "ambulanceId": 1, "vehicleNumber": "AMB-001", "travelMinutes": 4.0,
    "extraEquipmentCount": 1, "score": 9.0 }
]
```
Note the second candidate is closer (4.0 min) but ranks lower once its
one unit of unneeded equipment adds the 5.0-minute penalty — this is
what makes the `score` column, not raw `travelMinutes`, the actual
dispatch order.

## Frontend

- `src/api/resourceAllocation.api.js` — `fetchPendingEmergencies`,
  `fetchAvailableAmbulances`, `fetchDispatchCandidates`,
  `allocateAmbulance`; field names mirror `CallDto`/`AmbulanceDto`/
  `CandidateDto` exactly.
- `src/pages/ResourceAllocationPage.jsx` — loads pending calls and the
  fleet, re-fetches ranked candidates whenever the selected emergency
  changes, and drives the dispatch action.
  - `EmergencyList.jsx` — the pending-call queue, selectable.
  - `AmbulanceMatcher.jsx` — shows the ranked candidates for the
    selected call and the dispatch button.
  - `ResourceCard.jsx` (shared, `components/common/`) — the summary
    stat tiles (active emergencies, available ambulances, required
    equipment).
- `src/styles/resource-allocation.css` — module-specific styling.

## Known Limitations / Future Work

- Greedy selection is myopic across calls: dispatching call A can send
  away the ambulance that would have been optimal for a call B received
  moments later, with no global reassignment once a call is dispatched.
- The equipment penalty (`5.0` minutes per extra unit) is a fixed
  constant rather than a tunable weight (unlike the Optimization
  module's `FitnessWeights`), so it can't be varied experimentally
  without a code change.
- `/{id}/dispatch` and `/{id}/candidates` both reload the ambulance
  fleet and recompute a fresh `RoutingSnapshot` per call; there's no
  caching across concurrent dispatch decisions in the same instant.
