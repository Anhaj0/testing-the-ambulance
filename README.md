# Ambulance Dispatch System

The complete application: a React frontend, Spring Boot API, and PostgreSQL
schema/sample data. The existing appearance is preserved, with connected triage,
ambulance matching, dispatch, road routing, coverage, and scheduling pages.

## Contents

- [`frontend/`](frontend/): React + Vite UI and both Leaflet maps.
- [`backend/`](backend/): Java API, all five algorithm modules, and tests.
- [`backend/db/seed.sql`](backend/db/seed.sql): sample Colombo dataset.
- [`backend/docs/`](backend/docs/): module explanations and project reports.

## Run locally

Install Java 17+, Node.js 20+, and PostgreSQL. Maven is provided by the wrapper.

1. Create an empty PostgreSQL database named `ambulance_dispatch`, using pgAdmin
   or `createdb -U YOUR_DATABASE_USER ambulance_dispatch`.
2. Copy `backend/src/main/resources/application-local.properties.example` to
   `backend/src/main/resources/application-local.properties`. Replace the database
   user and password with your own. The local file is excluded from Git.
3. Start the backend from `backend/`:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

   On macOS/Linux, use `./mvnw spring-boot:run`. The API listens on port 8080 and
   creates the database tables on its first start.
4. After the tables exist, stop the backend and load the sample dataset once into
   the empty tables, using pgAdmin's Query Tool or this command from `backend/`:

   ```powershell
   psql -h localhost -U YOUR_DATABASE_USER -d ambulance_dispatch -f db/seed.sql
   ```

   Restart the backend afterward so its in-memory triage queue loads the records.
   The seed file is for empty tables and should not be rerun over existing data.
5. In a second terminal, start the frontend from `frontend/`:

   ```powershell
   npm ci
   npm run dev
   ```

Open [the app](http://localhost:5173). Vite forwards `/api` requests to the backend
on port 8080. API documentation is at
[Swagger UI](http://localhost:8080/swagger-ui.html).

## Connected workflow

Select a pending emergency, assess its patient in Triage, open ambulance matching,
and dispatch a suitable ambulance. Continue to Routing with that ambulance's
location and the emergency destination already selected. The active call is
carried between pages; coverage and scheduling include links back to that case.
Scheduling supports staff planning and does not automatically assign a driver to
a dispatch. Clinical assessment values must be entered by the operator.

Maps use OpenStreetMap tiles without an API key. Road geometry comes from the
public OSRM car service, so map tiles and driving routes require internet access.
Routes follow roads; failed lookups and ferry routes show an error rather than a
straight line. The A* graph selects the waypoints; its travel estimates and
blocked edges are separate from OSRM's road geometry. Road estimates do not
include live traffic. See the component READMEs for more detail.

## Validation

Build the frontend from `frontend/` with `npm run build`. Run the relevant backend
checks from `backend/`:

```powershell
.\mvnw.cmd "-Dtest=DispatchServiceTest,DispatchControllerTest,TriageControllerIntegrationTest,AStarAlgorithmTest,DrivingRouteServiceTest" test
```

The source snapshot passed the frontend build and these 30 backend tests, plus a
local browser check of the connected dispatch workflow.

## Source snapshot

This repository combines the following source snapshots with the local map,
CORS, road routing, and workflow integration fixes:

- [Backend source](https://github.com/starlightaris/Ambulance-Dispatch-System),
  commit `a457c295bd50e3a4acb795e83a976f34bf4a252f`.
- [Frontend source](https://github.com/starlightaris/Ambulance-Dispatch-System-Frontend),
  commit `1851e64c986a426f68094415fc36b4f166495b54`.

Machine-specific credentials, the running database, installed dependencies, and
generated build files are excluded. Each developer creates their own local
database and configuration using the instructions above.
