package com.farmatodo.order_service.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class ApiKeyAuthFilter implements Filter {

    private final String validApiKey;

    public ApiKeyAuthFilter(String validApiKey) {
        this.validApiKey = validApiKey;
    }

    private static final String API_KEY_HEADER = "Authorization";
    private static final String API_KEY_PREFIX = "ApiKey ";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestPath = httpRequest.getRequestURI();

        // Skip API Key validation for ping endpoint
        if (requestPath.endsWith("/orders/ping")) {
            log.debug("Skipping API Key validation for ping endpoint");
            chain.doFilter(request, response);
            return;
        }

        // Get API Key from Authorization header
        String authHeader = httpRequest.getHeader(API_KEY_HEADER);

        if (authHeader == null || !authHeader.startsWith(API_KEY_PREFIX)) {
            log.warn("Missing or invalid API Key format for path: {}", requestPath);
            sendUnauthorizedResponse(httpResponse, "Missing or invalid API Key. Use 'Authorization: ApiKey <your-key>'");
            return;
        }

        // Extract the API Key
        String providedApiKey = authHeader.substring(API_KEY_PREFIX.length()).trim();

        // Validate API Key
        if (!validApiKey.equals(providedApiKey)) {
            log.warn("Invalid API Key provided for path: {} from IP: {}",
                    requestPath, httpRequest.getRemoteAddr());
            sendUnauthorizedResponse(httpResponse, "Invalid API Key");
            return;
        }

        // API Key is valid, continue with the request
        log.debug("API Key validated successfully for path: {}", requestPath);
        chain.doFilter(request, response);
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String jsonResponse = String.format(
            "{\"error\":\"UNAUTHORIZED\",\"message\":\"%s\",\"status\":401}",
            message
        );

        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}
