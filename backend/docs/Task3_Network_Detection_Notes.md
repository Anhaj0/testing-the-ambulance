# Task 3 — Network Detection (Coverage) Module

## Overview

This module identifies "blind spots" — road nodes an ambulance cannot
reach quickly from any currently available base — and reports how
fleet coverage changes as the acceptable travel-time threshold changes.
It also serves the raw road-network graph and live ambulance positions
so a frontend can render it on a map.

## Algorithm

**Multi-source Dijkstra** (`DijkstraBlindSpotOptimizer`) computes, in one
traversal, the minimum travel time from *any* available ambulance base to
*every* node in the road network:

- Every node whose name is in `baseNodeNames` (the current location of
  each `AVAILABLE` ambulance) is seeded into the priority queue at
  distance `0` — this is what makes it "multi-source": the algorithm
  effectively starts from every base simultaneously rather than running
  one single-source Dijkstra per base and taking the minimum.
- If no ambulance is currently available, the first node in the list is
  used as a fallback source so the traversal still produces a result
  instead of leaving every node undefined.
- Blocked `RoadEdge`s (`blocked = true`) are excluded when building the
  adjacency list, so a blocked road is never used to reach a node
  through it.
- **Complexity:** standard binary-heap Dijkstra, O((V + E) log V) for V
  nodes and E edges.
- **Threshold-independence:** the expensive part — the Dijkstra pass
  itself — is separated (`computeMinTravelTimes`) from applying a
  threshold (`node.time > thresholdMinutes` ⇒ blind spot). A caller that
  needs several thresholds (see Coverage below) runs the traversal
  **once** and filters the same result repeatedly, instead of re-fetching
  the whole road network and re-running Dijkstra per threshold.

## Data Model

Uses the shared `common` entities:

- **RoadNode** — `id`, `name`, `latitude`, `longitude`.
- **RoadEdge** — directed, weighted connection between two `RoadNode`s:
  `travelTimeMinutes`, `distanceKm`, `blocked`.
- **Ambulance** — only `currentLocationNode` and `status` matter here;
  an ambulance's node name is a "base" for this module's purposes only
  while its status is `AVAILABLE`.

`NetworkReachability` is this module's own in-memory result type: every
node paired with its minimum travel time from the nearest base
(`Map<Long, Double>`), plus helpers (`blindSpots(threshold)`,
`blindSpotCount(threshold)`) to query it against any threshold without
re-running Dijkstra.

## API

All endpoints are under `/api/v1/network`.

| Endpoint | Description |
|---|---|
| `GET /blind-spots?thresholdMinutes=10.0` | Nodes with no route to a base, or whose best route exceeds the threshold (default 10.0). |
| `GET /coverage?thresholds=5,10,15,...` | Coverage percentage at each threshold in the comma-separated list (default `5,10,15,20,25,30,35,40`), built from a single Dijkstra pass. |
| `GET /graph/nodes` | All `RoadNode`s, for map rendering. |
| `GET /graph/edges` | All `RoadEdge`s, for map rendering. |
| `GET /graph/ambulances` | Current ambulance positions as map markers. |

**Response (`GET /blind-spots`)** — raw `RoadNode` entities:
```json
[
  { "id": 12, "name": "Kesbewa Junction", "latitude": 6.7972, "longitude": 79.9392 }
]
```

**Response (`CoverageStatsDto[]`, from `GET /coverage`):**
```json
[
  { "thresholdMinutes": 5.0, "totalNodes": 40, "blindSpotCount": 18, "coveragePercentage": 55.0 },
  { "thresholdMinutes": 10.0, "totalNodes": 40, "blindSpotCount": 6, "coveragePercentage": 85.0 }
]
```
This is the coverage-vs-threshold curve: as the threshold relaxes,
`blindSpotCount` falls and `coveragePercentage` rises toward 100%.

**Response (`RoadNodeDto` / `RoadEdgeDto` / `AmbulanceMarkerDto`, from
`/graph/*`)** — thin map-friendly views, not the raw JPA entities:
```json
{ "id": 3, "name": "Kollupitiya", "latitude": 6.9147, "longitude": 79.8483 }
```

## Frontend

- `src/api/networkDetection.api.js` — `getJson` wrappers for
  `/graph/nodes`, `/graph/edges`, `/graph/ambulances`, `/blind-spots`,
  and `/coverage`.
- `src/pages/NetworkDetectionPage.jsx` — assembles the module's
  component set:
  - `MapView.jsx` — renders nodes/edges/ambulance markers, highlighting
    blind spots.
  - `Sidebar.jsx`, `Legend.jsx` — map chrome.
  - `ThresholdSlider.jsx` — drives the `thresholdMinutes` query param.
  - `BlindSpotList.jsx` — tabular view of the current blind-spot nodes.
  - `CoverageGauge.jsx` — coverage percentage at the current threshold.
  - `CoverageCurveChart.jsx` — the full coverage-vs-threshold curve from
    `/coverage`.
  - `StatsGrid.jsx` — summary tiles (total nodes, blind-spot count, etc).

## Known Limitations / Future Work

- Only `AVAILABLE` ambulances count as bases; a `DISPATCHED` ambulance
  contributes no coverage even if it happens to be near a blind spot,
  which may understate real-world coverage during busy periods.
- Directed edges mean coverage is computed for reachability *from* a
  base *to* every node; the reverse (can an ambulance get back to base
  from that node) is never checked.
- No caching across requests: `/blind-spots` and `/coverage` each
  re-fetch the full node/edge list and re-run Dijkstra from scratch on
  every call, so a highly interactive threshold slider on the frontend
  means one full traversal per drag step rather than a debounced or
  server-cached one.
