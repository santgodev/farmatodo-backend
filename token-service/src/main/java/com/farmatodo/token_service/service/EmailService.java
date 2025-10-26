package com.farmatodo.token_service.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Value("${email.enabled:false}")
    private boolean emailEnabled;

    @Value("${email.from:noreply@farmatodo.com}")
    private String fromEmail;

    /**
     * Send payment rejection notification email to client
     * This is a mock implementation - in production, integrate with actual email service
     */
    public void sendPaymentRejectionEmail(String clientEmail, Long orderId, Integer attempts) {
        if (!emailEnabled) {
            logger.info("Email notification disabled. Would have sent rejection email to: {} for order: {} after {} attempts",
                    clientEmail, orderId, attempts);
            return;
        }

        // Mock email sending
        logger.info("Sending payment rejection email:");
        logger.info("From: {}", fromEmail);
        logger.info("To: {}", clientEmail);
        logger.info("Subject: Payment Rejected for Order #{}", orderId);
        logger.info("Body: Your payment for order #{} was rejected after {} attempts. Please contact customer support.",
                orderId, attempts);

        // In production, integrate with email service like SendGrid, AWS SES, etc.
    }
}
