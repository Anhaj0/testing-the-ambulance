package com.ambulance.dispatch_system.routing.service;

import com.ambulance.dispatch_system.common.entity.RoadNode;
import com.ambulance.dispatch_system.routing.dto.DrivingRoute;
import com.ambulance.dispatch_system.routing.exception.DrivingRouteUnavailableException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/** Supplies driving geometry for the ordered waypoints selected by the project's A*. */
@Service
public class DrivingRouteService {
    private final ObjectMapper mapper;
    private final String baseUrl;
    private final HttpClient client;
    private final Map<String, DrivingRoute> cache = new LinkedHashMap<>(16, 0.75f, true);
    private long nextRequestNanos;

    @Autowired
    public DrivingRouteService(ObjectMapper mapper,
            @Value("${app.routing.osrm-base-url:https://routing.openstreetmap.de/routed-car}") String baseUrl) {
        this(mapper, baseUrl, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    }

    DrivingRouteService(ObjectMapper mapper, String baseUrl, HttpClient client) {
        this.mapper = mapper;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.client = client;
    }

    // Bound the cache and serialize requests to respect the public server's 1 request/sec limit.
    public synchronized DrivingRoute findDrivingRoute(List<RoadNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            throw unavailable("No locations are available for a driving route.");
        }
        if (nodes.size() == 1) {
            List<List<Double>> point = List.of(List.of(nodes.get(0).getLongitude(), nodes.get(0).getLatitude()));
            return new DrivingRoute(point, point, 0, 0);
        }
        String coordinates = nodes.stream()
                .map(node -> node.getLongitude() + "," + node.getLatitude())
                .collect(Collectors.joining(";"));
        DrivingRoute cached = cache.get(coordinates);
        if (cached != null) return cached;

        try {
            long delay = nextRequestNanos - System.nanoTime();
            if (delay > 0) TimeUnit.NANOSECONDS.sleep(delay);
            nextRequestNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);

            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/route/v1/driving/"
                            + coordinates + "?overview=full&geometries=geojson&steps=true"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .header("User-Agent", "AmbulanceDispatchStudentDemo/1.0 (+https://github.com/starlightaris/Ambulance-Dispatch-System)")
                    .GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw unavailable("Driving directions are temporarily unavailable. Please try again.");
            }
            OsrmResponse body = mapper.readValue(response.body(), OsrmResponse.class);
            if (body == null || !"Ok".equals(body.code()) || body.routes() == null || body.routes().isEmpty()) {
                throw unavailable("No driving route was found between these locations.");
            }
            OsrmRoute road = body.routes().get(0);
            if (road == null || road.geometry() == null || !"LineString".equals(road.geometry().type())
                    || road.geometry().coordinates() == null || road.geometry().coordinates().size() < 2
                    || body.waypoints() == null || body.waypoints().size() != nodes.size()
                    || body.waypoints().stream().anyMatch(waypoint -> waypoint == null || waypoint.location() == null)
                    || road.legs() == null || road.legs().size() != nodes.size() - 1
                    || !Double.isFinite(road.distance()) || road.distance() < 0
                    || !Double.isFinite(road.duration()) || road.duration() < 0) {
                throw unavailable("The driving service returned an incomplete route. Please try again.");
            }
            for (OsrmLeg leg : road.legs()) {
                if (leg == null || leg.steps() == null) {
                    throw unavailable("The driving service returned incomplete directions. Please try again.");
                }
                if (leg.steps().stream().anyMatch(step -> step == null || "ferry".equals(step.mode()))) {
                    throw unavailable("A road-only route is unavailable; this journey requires a ferry.");
                }
            }
            DrivingRoute result = new DrivingRoute(
                    validateCoordinates(road.geometry().coordinates()),
                    validateCoordinates(body.waypoints().stream().map(OsrmWaypoint::location).toList()),
                    road.distance() / 1000.0, road.duration() / 60.0);
            cache.put(coordinates, result);
            if (cache.size() > 128) cache.remove(cache.keySet().iterator().next());
            return result;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw unavailable("Driving route lookup was interrupted. Please try again.");
        } catch (IOException | IllegalArgumentException | JacksonException exception) {
            throw unavailable("Could not load driving directions. Please try again.");
        }
    }

    private List<List<Double>> validateCoordinates(List<List<Double>> points) {
        return points.stream().map(point -> {
            if (point == null || point.size() < 2 || point.get(0) == null || point.get(1) == null
                    || !Double.isFinite(point.get(0)) || !Double.isFinite(point.get(1))
                    || Math.abs(point.get(0)) > 180 || Math.abs(point.get(1)) > 90) {
                throw unavailable("The driving service returned invalid map coordinates.");
            }
            return List.of(point.get(0), point.get(1));
        }).toList();
    }

    private DrivingRouteUnavailableException unavailable(String message) {
        return new DrivingRouteUnavailableException(message);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OsrmResponse(String code, List<OsrmRoute> routes, List<OsrmWaypoint> waypoints) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    record OsrmRoute(double distance, double duration, OsrmGeometry geometry, List<OsrmLeg> legs) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    record OsrmGeometry(String type, List<List<Double>> coordinates) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    record OsrmWaypoint(List<Double> location) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    record OsrmLeg(List<OsrmStep> steps) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    record OsrmStep(String mode) {}
}
