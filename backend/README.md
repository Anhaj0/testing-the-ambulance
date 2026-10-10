# Ambulance Dispatch System

An intelligent decision-support system for emergency ambulance dispatch, built as a Spring Boot
REST API. Each functional domain is backed by its own algorithm, chosen and justified for the
problem it solves — from real-time routing to fleet scheduling.

## Modules & Algorithms

| # | Domain | Main Algorithm | What it does |
|---|--------|-----------------|---------------|
| 1 | Routing | A\* Search | Shortest travel-time path between two points on the road network |
| 2 | Resource Allocation | Greedy Selection | Assigns the nearest suitable ambulance to an emergency call |
| 3 | Network Detection | Multi-Source Dijkstra | Flags road nodes ("blind spots") outside a travel-time threshold from any ambulance base |
| 4 | Triage | MTS Decision Tree + Priority Queue | Manchester Triage System categorisation, ranked by a binary max-heap |
| 5 | Optimization | Genetic Algorithm | Builds a weekly staff roster; a Greedy baseline is used for evaluation only |

See [docs/Chapter8_Report.md](docs/Chapter8_Report.md) for the Triage module's complexity analysis
and benchmark results.

## Tech Stack

- **Java 17**, **Spring Boot 4.1** (Web, Data JPA, Validation)
- **PostgreSQL** (runtime), **H2** (tests)
- **Maven** (via the included `mvnw` wrapper)
- **JUnit 5** + **Mockito** for testing, **JMH** for micro-benchmarks

## Getting Started

### Prerequisites
- JDK 17+
- A running PostgreSQL instance

### Setup
1. Clone the repository.
2. Create `src/main/resources/application-local.properties` with your database credentials:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/<your-db>
   spring.datasource.username=<your-username>
   spring.datasource.password=<your-password>
   ```
   The app runs with the `local` profile active by default (see `application.properties`).
3. Build and run:
   ```bash
   ./mvnw spring-boot:run
   ```
   The API is served at `http://localhost:8080`. Interactive API docs (Swagger UI) are at
   `http://localhost:8080/swagger-ui.html`, with the raw OpenAPI spec at `/v3/api-docs`.

### Running Tests
```bash
./mvnw test
```

## API Overview

All endpoints are versioned under `/api/v1`. This table is a snapshot for orientation — it can
drift as controllers change. The generated OpenAPI spec (`/v3/api-docs`, browsable at
`/swagger-ui.html`) is the source of truth; check it directly against a running instance before
building against any endpoint listed here.

| Module | Endpoint | Description |
|--------|----------|--------------|
| Routing | `POST /api/v1/routes` | Find the shortest route between two locations |
| Resource Allocation | `POST /api/v1/calls/{id}/dispatch` | Dispatch the best available ambulance to a call |
| Network Detection | `GET /api/v1/network/blind-spots` | List nodes beyond a travel-time threshold from any base |
| Network Detection | `GET /api/v1/network/coverage` | Coverage percentage across a range of thresholds |
| Network Detection | `GET /api/v1/network/graph/{nodes\|edges\|ambulances}` | Raw graph data for visualization |
| Triage | `POST /api/v1/triage/assessments` | Submit a patient assessment and get its queue position |
| Triage | `GET /api/v1/triage/assessments/queue` | View the current priority queue |
| Triage | `PUT /api/v1/triage/assessments/{id}/resolve` | Mark a triage case resolved |
| Optimization | `POST /api/v1/optimization/schedules/runs` | Generate and persist a weekly roster (Genetic Algorithm) |
| Optimization | `POST /api/v1/optimization/schedules/comparisons` | Run GA vs. Greedy side by side (evaluation only) |
| Optimization | `GET /api/v1/optimization/schedules` | Fetch the persisted roster for a given week |
| Optimization | `GET /api/v1/optimization/schedules/defaults` | Default GA parameters and fitness weights |
| Optimization | `/api/v1/optimization/staff`, `/api/v1/optimization/shift-slots` | CRUD for staff and shift-slot templates |

### Driving geometry

`POST /api/v1/routes` keeps the A* route and graph totals, and adds `drivingRoute`
with road-following GeoJSON coordinates (`[longitude, latitude]`), snapped
waypoints, distance in kilometres, and estimated driving time in minutes.
The frontend displays these driving metrics. They do not include live traffic.

The default geometry provider is the public OSRM car service operated by
[FOSSGIS](https://routing.openstreetmap.de/about.html). Internet access is required;
no API key is needed. Requests are cached and paced at one per second. Configure
`app.routing.osrm-base-url` to use another compatible OSRM car server.
Ferry routes and failed lookups return an error, with no straight-line fallback.
Project blocked edges affect A* waypoint selection; they are not sent to OSRM as
street closures. The graph totals remain separate from the road geometry totals.

Local frontend origins on both `localhost` and `127.0.0.1` are allowed by default.
Override `app.cors.allowed-origin-patterns` for other environments.

## Project Structure

```
com.ambulance.dispatch_system
├── common/                 shared entities, repositories, exceptions
├── routing/                Task 1 — A* routing
├── resource_allocation/    Task 2 — ambulance dispatch
├── network_detection/      Task 3 — multi-source Dijkstra blind-spot analysis
├── triage/                 Task 4 — MTS triage engine
├── optimization/           Task 5 — GA/Greedy staff scheduling
└── DispatchSystemApplication.java
```

Each domain module keeps `controller/`, `service/`, and `exception/` (domain-specific error types
extending `common.exception.BaseException`) at a minimum, plus whatever else its own algorithm
and data shapes need (e.g. routing's `algorithm/` and `dto/`, optimization's
`ga/`/`greedy/`/`fitness/`/`model/`).

## Contributing

- Branch naming: `feature/taskNo/submodule` (e.g. `feature/task1/routing`).
- All changes to `main` go through a Pull Request — no force pushes.
