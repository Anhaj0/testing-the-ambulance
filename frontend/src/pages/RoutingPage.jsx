import React, { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { callPath } from '../utils/workflow.js';
import { useDispatchWorkflow } from '../components/common/DispatchWorkflow.jsx';
import { findRoute } from '../api/routing.api.js';
import { fetchNodes } from '../api/networkDetection.api.js';
import RouteMap from '../components/routing/RouteMap.jsx';
import 'leaflet/dist/leaflet.css';
import '../styles/routing.css';

export default function RoutingPage() {
  const [searchParams] = useSearchParams();
  const linkedStart = searchParams.get('startNode');
  const linkedDestination = searchParams.get('destinationNode');
  const linkedCallId = searchParams.get('callId');
  const linkedVehicle = searchParams.get('vehicle');
  const { lastDispatch } = useDispatchWorkflow();
  const completedLinkedCall = linkedCallId && lastDispatch?.callId === Number(linkedCallId);
  const [nodes, setNodes] = useState([]);
  const [nodesLoading, setNodesLoading] = useState(true);
  const [nodesError, setNodesError] = useState(null);

  const [startLocationId, setStartLocationId] = useState('');
  const [destinationLocationId, setDestinationLocationId] = useState('');
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  // Location picker is fed by the same /api/v1/network/graph/nodes endpoint
  // the Network Detection map already uses — no separate "list locations"
  // endpoint was ever needed.
  useEffect(() => {
    let cancelled = false;
    fetchNodes()
      .then((data) => {
        if (cancelled) return;
        setNodes([...data].sort((a, b) => a.name.localeCompare(b.name)));
        setNodesError(null);
      })
      .catch((err) => {
        if (!cancelled) setNodesError(err.message);
      })
      .finally(() => {
        if (!cancelled) setNodesLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const sameLocationSelected =
    startLocationId !== '' && startLocationId === destinationLocationId;
  const picking = nodesLoading || Boolean(nodesError);

  // Cross-page navigation carries real node names; resolve them against the
  // graph before requesting directions rather than guessing numeric IDs.
  useEffect(() => {
    if (nodesLoading || nodesError) return;
    let cancelled = false;
    setLoading(false);
    setResult(null);
    setError(null);
    if (!linkedStart && !linkedDestination) return;
    const start = nodes.find((node) => node.name === linkedStart);
    const destination = nodes.find((node) => node.name === linkedDestination);
    setStartLocationId(start ? String(start.id) : '');
    setDestinationLocationId(destination ? String(destination.id) : '');
    if ((linkedStart && !start) || (linkedDestination && !destination)) {
      setError('A linked location is no longer on the road network. Choose locations below.');
      return;
    }
    if (!start || !destination || start.id === destination.id) return;
    setLoading(true);
    findRoute(start.id, destination.id).then((data) => {
      if (cancelled) return;
      if (!data.drivingRoute?.coordinates?.length) throw new Error('Driving directions are unavailable. Please try again.');
      setResult(data);
    }).catch((err) => { if (!cancelled) setError(err.message); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [nodes, nodesLoading, nodesError, linkedStart, linkedDestination]);

  const showingLinkedRoute = linkedVehicle
    && nodes.find((node) => String(node.id) === startLocationId)?.name === linkedStart
    && nodes.find((node) => String(node.id) === destinationLocationId)?.name === linkedDestination;

  async function handleSubmit(e) {
    e.preventDefault();
    setError(null);
    setResult(null);

    if (!startLocationId || !destinationLocationId) {
      setError('Please select both a start and destination location.');
      return;
    }
    if (sameLocationSelected) {
      setError('Start and destination must be different locations.');
      return;
    }

    setLoading(true);
    try {
      const data = await findRoute(
        Number(startLocationId),
        Number(destinationLocationId)
      );
      if (!data.drivingRoute?.coordinates?.length) {
        throw new Error('Driving directions are unavailable. Please try again.');
      }
      setResult(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="routing-page">
      <div className="routing-panel">
        <h1>Route optimization</h1>
        {showingLinkedRoute && <p className="empty-note">Driving route for {linkedVehicle} · Call #{linkedCallId}</p>}
        {linkedCallId && <div className="workflow-actions">
          <Link to={completedLinkedCall ? '/resource-allocation' : callPath('/resource-allocation', linkedCallId)}>
            {completedLinkedCall ? '← Next emergency' : '← Return to ambulance matching'}
          </Link>
        </div>}

        <form onSubmit={handleSubmit} className="routing-form">
          <label>
            Start location
            <select
              value={startLocationId}
              onChange={(e) => { setStartLocationId(e.target.value); setResult(null); setError(null); }}
              disabled={picking || loading}
            >
              <option value="">{nodesLoading ? 'Loading locations…' : 'Select a location'}</option>
              {nodes.map((n) => (
                <option key={n.id} value={n.id}>{n.name}</option>
              ))}
            </select>
          </label>

          <label>
            Destination location
            <select
              value={destinationLocationId}
              onChange={(e) => { setDestinationLocationId(e.target.value); setResult(null); setError(null); }}
              disabled={picking || loading}
            >
              <option value="">{nodesLoading ? 'Loading locations…' : 'Select a location'}</option>
              {nodes.map((n) => (
                <option key={n.id} value={n.id}>{n.name}</option>
              ))}
            </select>
          </label>

          <button type="submit" disabled={loading || picking}>
            {loading ? 'Finding route…' : 'Find route'}
          </button>
        </form>

        {nodesError && (
          <div className="routing-error">
            <span className="dot err" />
            Could not load locations: {nodesError}
          </div>
        )}

        {loading && (
          <div className="status-row">
            <span className="dot" />
            Calculating shortest path…
          </div>
        )}

        {error && (
          <div className="routing-error">
            <span className="dot err" />
            {error}
          </div>
        )}

        {result && (
          <div className="routing-result">
            <h2>Driving route found</h2>
            <p>
              {result.drivingRoute.distanceKm.toFixed(2)} km &middot;{' '}
              {result.drivingRoute.durationMinutes.toFixed(1)} min
            </p>
            <p className="routing-estimate-note">Estimated driving time · no live traffic</p>
            <h3>Via selected locations ({result.algorithm})</h3>

            <ol className="routing-node-list">
              {result.route.map((node) => (
                <li key={node.id}>
                  {node.name}
                  <span className="routing-node-coords">
                    {node.latitude.toFixed(4)}, {node.longitude.toFixed(4)}
                  </span>
                </li>
              ))}
            </ol>
            <div className="workflow-actions">
              <Link to={`/network-detection?${new URLSearchParams({ nodeId: String(result.route[result.route.length - 1].id) })}`}>Check coverage at destination →</Link>
            </div>
          </div>
        )}
      </div>

      <RouteMap allNodes={nodes} route={result?.route ?? []} drivingRoute={result?.drivingRoute} />
    </div>
  );
}
