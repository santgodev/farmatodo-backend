package com.farmatodo.token_service.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class MdcFilter implements Filter {

    private static final String TRANSACTION_ID = "transactionId";
    private static final String TRANSACTION_ID_HEADER = "X-Transaction-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;

            // Check if transaction ID is provided in request header (from upstream service)
            String transactionId = httpRequest.getHeader(TRANSACTION_ID_HEADER);

            // If not provided, generate a new one
            if (transactionId == null || transactionId.trim().isEmpty()) {
                transactionId = UUID.randomUUID().toString();
            }

            MDC.put(TRANSACTION_ID, transactionId);

            // Add transaction ID to response header for tracking
            if (response instanceof jakarta.servlet.http.HttpServletResponse) {
                ((jakarta.servlet.http.HttpServletResponse) response)
                        .setHeader(TRANSACTION_ID_HEADER, transactionId);
            }

            chain.doFilter(request, response);
        } finally {
            // Clear MDC after request processing
            MDC.clear();
        }
    }
}
