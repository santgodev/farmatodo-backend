package com.farmatodo.cart_service.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class MdcFilter implements Filter {

    private static final String TRANSACTION_ID = "transactionId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        try {
            // Generate a unique transaction ID for this request
            String transactionId = UUID.randomUUID().toString();
            MDC.put(TRANSACTION_ID, transactionId);

            // Add transaction ID to response header for tracking
            if (response instanceof HttpServletResponse) {
                ((HttpServletResponse) response).setHeader("X-Transaction-Id", transactionId);
            }

            chain.doFilter(request, response);
        } finally {
            // Clear MDC after request processing
            MDC.clear();
        }
    }
}
