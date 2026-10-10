package com.ambulance.dispatch_system.routing.service;

import com.ambulance.dispatch_system.common.entity.RoadNode;
import com.ambulance.dispatch_system.routing.dto.DrivingRoute;
import com.ambulance.dispatch_system.routing.exception.DrivingRouteUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class DrivingRouteServiceTest {
    private HttpClient client;
    private HttpResponse<String> response;
    private DrivingRouteService service;
    private final List<RoadNode> nodes = List.of(node(79.8612, 6.9271), node(79.8478, 6.9319));
    private static final String SUCCESS = """
        {"code":"Ok","routes":[{"distance":1800,"duration":180,
          "geometry":{"type":"LineString","coordinates":[[79.861,6.927],[79.85,6.94],[79.848,6.932]]},
          "legs":[{"steps":[{"mode":"driving"}]}]}],
          "waypoints":[{"location":[79.861,6.927]},{"location":[79.848,6.932]}]}
        """;

    @BeforeEach
    void setup() throws Exception {
        client = mock(HttpClient.class);
        response = mock(HttpResponse.class);
        service = new DrivingRouteService(new ObjectMapper(), "https://routing.example/routed-car", client);
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(SUCCESS);
    }

    @Test
    void returnsRoadBendsAndSnappedEndpointsWithConvertedEstimates() throws Exception {
        DrivingRoute result = service.findDrivingRoute(nodes);
        assertEquals(3, result.coordinates().size());
        assertEquals(List.of(79.85, 6.94), result.coordinates().get(1));
        assertEquals(List.of(79.861, 6.927), result.waypoints().get(0));
        assertEquals(1.8, result.distanceKm(), 0.001);
        assertEquals(3.0, result.durationMinutes(), 0.001);
        ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client).send(request.capture(), any(HttpResponse.BodyHandler.class));
        assertTrue(request.getValue().uri().toString().contains("79.8612,6.9271;79.8478,6.9319"));
        assertTrue(request.getValue().headers().firstValue("User-Agent").orElseThrow().contains("AmbulanceDispatch"));
    }

    @Test
    void cachesRepeatedRoutesInsteadOfCallingPublicServiceAgain() throws Exception {
        assertSame(service.findDrivingRoute(nodes), service.findDrivingRoute(nodes));
        verify(client, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void noRouteNeverFallsBackToStraightLines() {
        when(response.body()).thenReturn("{\"code\":\"NoRoute\"}");
        assertThrows(DrivingRouteUnavailableException.class, () -> service.findDrivingRoute(nodes));
    }

    @Test
    void connectionFailureProducesServiceUnavailable() throws Exception {
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("Connection unavailable"));
        assertThrows(DrivingRouteUnavailableException.class, () -> service.findDrivingRoute(nodes));
    }

    @Test
    void rejectsFerryRoutes() {
        when(response.body()).thenReturn(SUCCESS.replace("\"driving\"", "\"ferry\""));
        assertThrows(DrivingRouteUnavailableException.class, () -> service.findDrivingRoute(nodes));
    }

    @Test
    void rejectsInvalidGeometry() {
        when(response.body()).thenReturn(SUCCESS.replace("[79.85,6.94]", "[999,6.94]"));
        assertThrows(DrivingRouteUnavailableException.class, () -> service.findDrivingRoute(nodes));
    }

    @Test
    void malformedJsonProducesServiceUnavailable() {
        when(response.body()).thenReturn("not json");
        assertThrows(DrivingRouteUnavailableException.class, () -> service.findDrivingRoute(nodes));
    }

    private static RoadNode node(double longitude, double latitude) {
        RoadNode node = new RoadNode();
        node.setLongitude(longitude);
        node.setLatitude(latitude);
        return node;
    }
}
