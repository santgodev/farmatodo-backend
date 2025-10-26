package com.farmatodo.product_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.farmatodo.product_service.exception.ErrorResponse;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
public class ApiKeyFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String API_KEY_PREFIX = "ApiKey ";

    // Endpoints that don't require API key authentication
    private static final List<String> PUBLIC_ENDPOINTS = Arrays.asList(
            "/api/products/health",
            "/api/products/info",
            "/products/ping"
    );

    @Value("${api.key}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestPath = httpRequest.getRequestURI();

        // Skip authentication for public endpoints
        if (isPublicEndpoint(requestPath)) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = httpRequest.getHeader(AUTHORIZATION_HEADER);

        // Check if Authorization header is present
        if (authHeader == null || authHeader.trim().isEmpty()) {
            logger.warn("Missing Authorization header for transaction: {}", MDC.get("transactionId"));
            sendUnauthorizedResponse(httpResponse, "Missing Authorization header");
            return;
        }

        // Check if header starts with "ApiKey "
        if (!authHeader.startsWith(API_KEY_PREFIX)) {
            logger.warn("Invalid Authorization header format for transaction: {}", MDC.get("transactionId"));
            sendUnauthorizedResponse(httpResponse, "Invalid Authorization header format. Expected: ApiKey <key>");
            return;
        }

        // Extract API key
        String providedApiKey = authHeader.substring(API_KEY_PREFIX.length()).trim();

        // Validate API key
        if (!apiKey.equals(providedApiKey)) {
            logger.warn("Invalid API key for transaction: {}", MDC.get("transactionId"));
            sendUnauthorizedResponse(httpResponse, "Invalid API key");
            return;
        }

        // API key is valid, proceed with the request
        logger.debug("API key validation successful for transaction: {}", MDC.get("transactionId"));
        chain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String path) {
        return PUBLIC_ENDPOINTS.stream().anyMatch(path::endsWith);
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("UNAUTHORIZED")
                .message(message)
                .timestamp(LocalDateTime.now())
                .transactionId(MDC.get("transactionId"))
                .build();

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.findAndRegisterModules(); // Register JavaTimeModule for LocalDateTime
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
