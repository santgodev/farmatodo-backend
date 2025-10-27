package com.farmatodo.order_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendOrderConfirmationEmail(String toEmail, Long orderId, String clientName,
                                          BigDecimal totalAmount, String paymentStatus) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Farmatodo - Order Confirmation #" + orderId);

            String emailBody = buildOrderConfirmationBody(orderId, clientName, totalAmount, paymentStatus);
            message.setText(emailBody);

            mailSender.send(message);
            log.info("Order confirmation email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send order confirmation email to: {}. Error: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendPaymentFailureEmail(String toEmail, Long orderId, String clientName,
                                       BigDecimal totalAmount, int retryAttempts) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Farmatodo - Payment Failed for Order #" + orderId);

            String emailBody = buildPaymentFailureBody(orderId, clientName, totalAmount, retryAttempts);
            message.setText(emailBody);

            mailSender.send(message);
            log.info("Payment failure email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send payment failure email to: {}. Error: {}", toEmail, e.getMessage());
        }
    }

    private String buildOrderConfirmationBody(Long orderId, String clientName,
                                             BigDecimal totalAmount, String paymentStatus) {
        return String.format("""
            Dear %s,

            Thank you for your order at Farmatodo!

            Order Details:
            - Order ID: #%d
            - Total Amount: $%.2f
            - Payment Status: %s

            %s

            Best regards,
            Farmatodo Team
            """,
            clientName,
            orderId,
            totalAmount,
            paymentStatus,
            paymentStatus.equals("APPROVED")
                ? "Your order has been processed successfully and will be shipped soon."
                : "Your payment is being processed. You will receive an update shortly."
        );
    }

    private String buildPaymentFailureBody(Long orderId, String clientName,
                                          BigDecimal totalAmount, int retryAttempts) {
        return String.format("""
            Dear %s,

            We're sorry, but we were unable to process your payment for Order #%d.

            Order Details:
            - Order ID: #%d
            - Total Amount: $%.2f
            - Retry Attempts: %d

            Please review your payment information and try again, or contact our support team for assistance.

            You can try placing your order again or contact us at support@farmatodo.com

            Best regards,
            Farmatodo Team
            """,
            clientName,
            orderId,
            orderId,
            totalAmount,
            retryAttempts
        );
    }
}
