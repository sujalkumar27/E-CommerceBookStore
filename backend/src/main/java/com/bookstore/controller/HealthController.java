package com.bookstore.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * ============================================================
 * HealthController — Simple "Is the server alive?" Check
 * ============================================================
 *
 * WHAT THIS DOES:
 * Provides a single public endpoint:  GET /actuator/health
 * Returns: { "status": "UP" }
 *
 * WHY WE NEED IT:
 * - Docker Compose uses it to know when the backend is ready
 *   before starting the frontend ("depends_on with healthcheck")
 * - In production, monitoring tools ping this to alert if the
 *   server goes down
 *
 * This endpoint is public — no login required (configured in SecurityConfig).
 */
@RestController
@RequestMapping("/actuator")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
