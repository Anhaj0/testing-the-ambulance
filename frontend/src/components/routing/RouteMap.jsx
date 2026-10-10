import React, { useEffect, useMemo } from 'react';
import { MapContainer, TileLayer, CircleMarker, Polyline, Popup, useMap } from 'react-leaflet';
import { TOKEN_COLORS } from '../../styles/tokenColors.js';

// Re-fits the viewport whenever the set of points to show changes — the full
// node list on first load, then just the route once one's been found.
function FitBounds({ positions }) {
  const map = useMap();

  useEffect(() => {
    if (positions.length) {
      map.fitBounds(positions, { padding: [48, 48], maxZoom: 15, animate: false });
    }
  }, [positions, map]);

  return null;
}

/**
 * Plots the road network and, once a route has been found, highlights the
 * driving geometry through the A* waypoints — start (green),
 * intermediate hops (indigo), destination (red) — instead of leaving the
 * result as a plain coordinate list with no spatial context.
 */
export default function RouteMap({ allNodes, route, drivingRoute }) {
  // GeoJSON uses longitude/latitude; Leaflet expects latitude/longitude.
  // Never substitute direct node-to-node lines for unavailable driving geometry.
  const routeLatLngs = useMemo(
    () => (drivingRoute?.coordinates ?? []).map(([longitude, latitude]) => [latitude, longitude]),
    [drivingRoute]
  );
  const snappedWaypoints = (drivingRoute?.waypoints ?? []).map(
    ([longitude, latitude]) => [latitude, longitude]
  );
  const hasRoute = route.length > 0 && routeLatLngs.length > 0;
  const start = hasRoute ? route[0] : null;
  const destination = hasRoute ? route[route.length - 1] : null;
  const waypoints = hasRoute ? route.slice(1, -1) : [];
  const fitTarget = useMemo(
    () => routeLatLngs.length ? routeLatLngs : allNodes.map((node) => [node.latitude, node.longitude]),
    [routeLatLngs, allNodes]
  );

  return (
    <div className="routing-map-area">
      <MapContainer center={[0, 0]} zoom={2} style={{ width: '100%', height: '100%' }}>
        {/* Use the same key-free OpenStreetMap tiles as Network Detection. */}
        <TileLayer
          url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
          attribution="&copy; <a href='https://www.openstreetmap.org/copyright'>OpenStreetMap</a> contributors | Routing: <a href='https://routing.openstreetmap.de/about.html'>OSRM / FOSSGIS</a> | <a href='https://www.openstreetmap.org/fixthemap'>Fix the map</a>"
          maxZoom={19}
        />
        <FitBounds positions={fitTarget} />

        {allNodes.map((n) => (
          <CircleMarker
            key={n.id}
            center={[n.latitude, n.longitude]}
            radius={3}
            pathOptions={{ color: TOKEN_COLORS.lineLight, weight: 1, fillColor: TOKEN_COLORS.line, fillOpacity: 0.6 }}
          >
            <Popup>{n.name}</Popup>
          </CircleMarker>
        ))}

        {hasRoute && (
          <Polyline positions={routeLatLngs} pathOptions={{ color: TOKEN_COLORS.info, weight: 4, opacity: 0.9 }} />
        )}

        {waypoints.map((n, i) => (
          <CircleMarker
            key={n.id}
            center={snappedWaypoints[i + 1]}
            radius={5}
            pathOptions={{ color: TOKEN_COLORS.info, weight: 2, fillColor: '#6366f1', fillOpacity: 0.9 }}
          >
            <Popup>{i + 2}. {n.name}</Popup>
          </CircleMarker>
        ))}

        {start && (
          <CircleMarker
            center={snappedWaypoints[0]}
            radius={8}
            pathOptions={{ color: TOKEN_COLORS.ok, weight: 2, fillColor: TOKEN_COLORS.ok, fillOpacity: 0.9 }}
          >
            <Popup>Start: {start.name}</Popup>
          </CircleMarker>
        )}

        {destination && (
          <CircleMarker
            center={snappedWaypoints[snappedWaypoints.length - 1]}
            radius={8}
            pathOptions={{ color: TOKEN_COLORS.danger, weight: 2, fillColor: TOKEN_COLORS.danger, fillOpacity: 0.9 }}
          >
            <Popup>Destination: {destination.name}</Popup>
          </CircleMarker>
        )}
      </MapContainer>

      {!hasRoute && (
        <div className="routing-map-badge">Select a start and destination, then Find route</div>
      )}
    </div>
  );
}
