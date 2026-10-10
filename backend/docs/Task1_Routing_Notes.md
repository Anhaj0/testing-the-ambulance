# Task 1 — Intelligent Route Optimization Module



## Overview

This module calculates the fastest route from a patient's location to a
destination hospital, automatically avoiding roads currently marked as
blocked. It is exposed to the rest of the system as a reusable routing
service — other modules (e.g. Resource Allocation) can call it internally
instead of implementing their own pathfinding.

## Algorithm

**A\* (A-Star) search** is used to compute the shortest path between two
`RoadNode`s in the city road-network graph.

A* was selected over plain Dijkstra because it uses a heuristic (straight-
line distance to the destination) to guide the search toward the goal,
reducing the number of nodes explored on a large road network while still
guaranteeing the optimal path, provided the heuristic is admissible.

Blocked roads are excluded from the search automatically — the backend
only loads `RoadEdge`s where `blocked = false` before running the
algorithm, so a blocked road can never appear in a returned route.

## Data Model

- **RoadNode** — a vertex in the graph (an intersection or named
  landmark). Fields: `id`, `name`, `latitude`, `longitude`.
- **RoadEdge** — a weighted connection between two `RoadNode`s, carrying
  `travelTimeMinutes`, `distanceKm`, and a `blocked` flag.

## API

`POST /api/routing/find-route`

**Request body (`RouteRequest`):**
```json
{
  "startLocationId": 1,
  "destinationLocationId": 2,
  "algorithm": "ASTAR"
}
```

**Response body (`RouteResponse`):**
```json
{
  "algorithm": "ASTAR",
  "totalTravelTimeMinutes": 10.0,
  "totalDistanceKm": 5.0,
  "route": [
    { "id": 1, "name": "A", "latitude": 6.9271, "longitude": 79.8612 },
    { "id": 2, "name": "B", "latitude": 6.9275, "longitude": 79.8620 }
  ]
}
```

Note: the `algorithm` field on the request is accepted for future
extensibility, but the current implementation always runs A* regardless
of its value.

## Frontend

- `src/api/routing.api.js` — calls `POST /api/routing/find-route`.
- `src/pages/RoutingPage.jsx` — form for selecting a start/destination
  location, displays the returned route (distance, travel time, and the
  ordered list of nodes along the path).
- `src/styles/routing.css` — module-specific styling, following the
  shared design tokens in `styles/variables.css`.

Tested end-to-end against the live Supabase-backed backend (see commit
history on `main` for the working implementation and styling pass).

## Known Limitations / Future Work

- Locations are currently selected by numeric ID; a dedicated endpoint to
  list all `RoadNode`s by name (for a proper dropdown) has been discussed
  but not yet implemented.
- The `algorithm` field is not yet wired to actually switch between
  multiple pathfinding strategies (e.g. Dijkstra vs A* comparison), which
  would be useful for the complexity/performance evaluation in the
  individual report.