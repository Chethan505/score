package com.example.football.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", OffsetDateTime.now(),
                "service", "football-score-app"
        ));
    }

    @GetMapping("/health/liveness")
    public ResponseEntity<Map<String, Object>> liveness() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    @GetMapping("/health/readiness")
    public ResponseEntity<Map<String, Object>> readiness() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}