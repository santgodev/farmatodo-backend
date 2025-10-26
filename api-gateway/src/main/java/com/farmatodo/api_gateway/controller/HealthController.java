package com.farmatodo.api_gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/gateway")
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "api-gateway");
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("message", "API Gateway is running successfully");
        return response;
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "api-gateway");
        response.put("version", "1.0.0");
        response.put("description", "Farmatodo API Gateway - Entry point for all microservices");
        response.put("endpoints", new String[]{
            "GET /api/gateway/health - Health check",
            "GET /api/gateway/info - Service information",
            "GET /api/gateway/ping - Simple ping endpoint"
        });
        return response;
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}
