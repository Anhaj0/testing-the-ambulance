# Ambulance Dispatch System — Frontend

React + Vite single-page app for the Ambulance Dispatch System. It talks to a
Spring Boot backend over `/api/v1/...`. The landing page is a cross-module
dispatch overview; from there it covers five modules: live network coverage,
routing, triage, resource allocation, and staff scheduling.

## Tech stack

- [React 18](https://react.dev/) with [React Router](https://reactrouter.com/) for client-side routing
- [Vite](https://vitejs.dev/) for dev server and builds
- [Leaflet](https://leafletjs.com/) / [react-leaflet](https://react-leaflet.js.org/) for the coverage and routing maps
- Plain CSS with a shared token layer (`src/styles/variables.css`) — no CSS framework or CSS-in-JS

## Prerequisites

- Node.js 20+
- The [backend](../backend/) running locally on `http://localhost:8080` (see its own README) — the app calls relative `/api/...` paths and expects a backend to answer them

## Getting started

```bash
npm install
npm run dev
```

This starts Vite on `http://localhost:5173` and proxies any request to
`/api/...` through to `http://localhost:8080` (see `vite.config.js`), so the
frontend code never hardcodes a backend URL. For production, configure your
web server to serve the built files and forward `/api` to the backend; this
repository does not automatically install the frontend build into Spring Boot.

## Scripts

| Command           | Does                                                        |
|--------------------|--------------------------------------------------------------|
| `npm run dev`      | Start the Vite dev server with the `/api` proxy               |
| `npm run build`    | Type-check-free production build, output to `dist/`           |
| `npm run preview`  | Serve the last `dist/` build locally, for a quick sanity check |

## Project structure

```text
src
│   App.jsx              — route table
│   main.jsx              — entry point, loads styles/global.css + variables.css
│
├── api                   — one file per module, thin wrappers around client.js
│       client.js         — shared fetch wrapper (JSON parsing, error shape)
│       networkDetection.api.js
│       resourceAllocation.api.js
│       routing.api.js
│       scheduling.api.js
│       triage.api.js
│
├── components
│   ├── common             — shared across modules (and the dashboard)
│   │       EmptyState.jsx
│   │       NavBar.jsx
│   │       ResourceCard.jsx
│   │       StatusBadge.jsx
│   ├── network-detection
│   ├── resource-allocation
│   ├── routing
│   ├── scheduling
│   └── triage
│
├── pages                  — one top-level page per route
│       DashboardPage.jsx
│       NetworkDetectionPage.jsx
│       ResourceAllocationPage.jsx
│       RoutingPage.jsx
│       SchedulingPage.jsx
│       TriagePage.jsx
│
├── styles
│       global.css         — reset, app shell, nav, shared small components
│       variables.css      — design tokens: colour, radius, spacing (loaded once, global)
│       tokenColors.js     — JS mirror of the colour tokens, for Leaflet/SVG that can't read CSS vars
│       dashboard.css
│       network-detection.css
│       resource-allocation.css
│       routing.css
│       scheduling.css
│       triage.css
│
└── utils
        formatLabel.js     — turns a raw backend enum ("AVAILABLE") into a label ("Available")
```

## Modules

| Route                  | Page                       | What it does |
|-------------------------|-----------------------------|---------------|
| `/`                      | Dashboard                    | Cross-module overview: active emergencies, fleet availability, blind spots, and triage queue at a glance, with quick links into every module |
| `/network-detection`     | Network Detection            | Live map of fleet coverage, blind spots, and the coverage curve |
| `/routing`               | Routing                      | Shortest-path lookup between two locations, plotted on a map |
| `/triage`                | Triage                       | MTS-based patient assessment and the prioritized dispatch queue |
| `/resource-allocation`   | Resource Allocation           | Matches pending emergencies to the best available ambulance |
| `/scheduling`            | Scheduling                    | Staff roster, shift templates, and schedule generation/comparison |

The nav bar mirrors this grouping: **Live Dispatch** (Network Detection,
Routing, Triage) and **Command & Planning** (Resource Allocation,
Scheduling). The "Ambulance Dispatch" brand link always returns to `/`.

## Connected dispatch workflow

The existing pages share an in-memory call selection. Start with a pending call
on the overview or Triage page, assess its actual patient, continue to ambulance
matching, and preview a candidate's road route without re-entering locations.
After dispatch, the route link uses the vehicle returned by the backend and
keeps the completed call in context until another emergency is selected.
Routing can open coverage at its destination; coverage and scheduling link back
to dispatch. Scheduling remains staff planning, with no automatic driver assignment.

Call links use `callId`; route links carry road-node names and a vehicle number.
Invalid or completed pending-call links show a message rather than silently
selecting someone else. Patient names and clinical data are not stored in browser
storage. The backend adds patient identity to call and triage responses; existing
algorithms, persistence models, and dispatch decisions are unchanged.

## Maps and driving routes

Maps use OpenStreetMap tiles without an API key. The Routing page draws the
backend's `drivingRoute.coordinates` road geometry through the A* waypoints,
with markers snapped to the roads. Distance and time come from this driving
route; time is an estimate without live traffic. Internet access is required
for map tiles and the backend's OSRM lookup. If driving directions cannot be
loaded, the page shows an error instead of drawing straight lines between nodes.

## Conventions

- **Design tokens**: colours (`--bg`, `--panel`, `--text`, `--accent-*`) and
  a border-radius/spacing scale (`--radius-sm/md/lg/pill`, `--space-1..6`)
  live in `src/styles/variables.css`. New styles should reference these
  instead of hardcoding hex values or arbitrary `border-radius`/spacing.
- **Copy casing**: headings and buttons use sentence case ("Route
  optimization", "Save changes"). Nav labels, tabs, and eyebrows are the
  exception — those act as navigation chrome, not body copy.
- **Enum display**: never render a raw backend enum (`AVAILABLE`) straight
  into the UI — format it with `formatLabel()` from `src/utils/formatLabel.js`.
- **Map/chart colours**: Leaflet layers and inline SVG charts can't read CSS
  custom properties, so they import `TOKEN_COLORS` from
  `src/styles/tokenColors.js` instead of hardcoding their own copy of an
  accent colour.
- **Shared components/CSS**: a component used by more than one page (e.g.
  `ResourceCard`) belongs in `components/common`, and its CSS belongs in
  `global.css` — not in one page's own stylesheet. Each page only imports
  its own `styles/<module>.css`, so a class defined there is invisible to
  every other page; relying on another page happening to load first is a
  real bug, not a shortcut (this has already happened once, with
  `FitnessScorecard` depending on Network Detection's `.stat-card`).
