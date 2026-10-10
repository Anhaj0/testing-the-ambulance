package com.ambulance.dispatch_system.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadata for the auto-generated OpenAPI document (springdoc-openapi reads the actual
 * {@code @RequestMapping}/{@code @GetMapping}/etc. annotations on every controller to build
 * the spec, so this document is always in sync with what the backend actually exposes -
 * unlike a hand-written API reference, it cannot drift when a controller is refactored).
 *
 * <p>Once the app is running:
 * <ul>
 *   <li>Browsable UI: http://localhost:8080/swagger-ui.html</li>
 *   <li>Raw JSON spec: http://localhost:8080/v3/api-docs</li>
 * </ul>
 * Both are served directly by the backend, not through the frontend's Vite proxy - open them
 * against port 8080 regardless of whether the frontend dev server is running.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI dispatchSystemOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ambulance Dispatch System API")
                        .description("REST endpoints for network detection, triage, resource "
                                + "allocation, routing, and staff/schedule optimization. "
                                + "Generated from the controllers - treat this as the source "
                                + "of truth over any README or frontend api/*.api.js comments.")
                        .version("v1"));
    }
}
