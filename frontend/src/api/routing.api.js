import { postJson } from './client.js';

/**
 * Calls POST /api/v1/routes.
 *
 * Backend currently always runs A* internally (RouteServiceImpl ignores
 * the `algorithm` field for now), but we still send it so the request
 * shape matches RouteRequest.java and nothing needs to change here if
 * that gets wired up later.
 *
 * @param {number} startLocationId
 * @param {number} destinationLocationId
 * @param {string} [algorithm='ASTAR']
 * The response includes drivingRoute with road geometry (longitude/latitude),
 * snapped waypoints, distanceKm and durationMinutes. The graph estimates remain
 * separate from these driving estimates.
 */
export function findRoute(startLocationId, destinationLocationId, algorithm = 'ASTAR') {
  return postJson('/api/v1/routes', {
    startLocationId,
    destinationLocationId,
    algorithm,
  });
}
