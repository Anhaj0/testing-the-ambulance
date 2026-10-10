// Only identifiers and road-node names go in local navigation URLs.
export function callPath(path, callId) {
  return callId == null ? path : `${path}?${new URLSearchParams({ callId: String(callId) })}`;
}

export function routePath(emergency, ambulance) {
  if (!emergency?.locationNode || !ambulance?.currentLocationNode
      || ambulance.currentLocationNode === 'Unknown node') return null;
  return `/routing?${new URLSearchParams({
    callId: String(emergency.id),
    startNode: ambulance.currentLocationNode,
    destinationNode: emergency.locationNode,
    vehicle: ambulance.vehicleNumber,
  })}`;
}
