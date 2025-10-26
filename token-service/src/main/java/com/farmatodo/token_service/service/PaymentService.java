package com.farmatodo.token_service.service;

import com.farmatodo.token_service.dto.PaymentRequestDTO;
import com.farmatodo.token_service.dto.PaymentResponseDTO;
import com.farmatodo.token_service.exception.BusinessException;
import com.farmatodo.token_service.model.TokenizedCard;
import com.farmatodo.token_service.repository.TokenRepository;
import com.farmatodo.token_service.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private final TokenRepository tokenRepository;
    private final EncryptionUtil encryptionUtil;
    private final LogService logService;
    private final EmailService emailService;

    @Value("${payment.rejectionProbability:0.3}")
    private double rejectionProbability;

    @Value("${payment.retryCount:3}")
    private int maxRetryCount;

    /**
     * Process payment with retry logic
     */
    public PaymentResponseDTO processPayment(PaymentRequestDTO request, String clientEmail) {
        String transactionId = MDC.get("transactionId");
        logger.info("Starting payment processing for orderId: {}, transactionId: {}", request.getOrderId(), transactionId);

        logService.logInfo("Payment process started",
                String.format("OrderId: %d, Token: %s, Amount: %s", request.getOrderId(), request.getToken(), request.getAmount()));

        // Validate token exists
        TokenizedCard tokenizedCard = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> {
                    logService.logError("Token not found", "Token: " + request.getToken());
                    return new BusinessException("Invalid token", "INVALID_TOKEN", 404);
                });

        // Verify card data can be decrypted
        try {
            String cardData = encryptionUtil.decrypt(tokenizedCard.getCardHashOrCipher());
            logger.debug("Token validated and card data decrypted successfully");
        } catch (Exception e) {
            logService.logError("Failed to decrypt card data", "Token: " + request.getToken());
            throw new BusinessException("Invalid token data", "TOKEN_DECRYPT_ERROR", 400);
        }

        // Attempt payment with retries
        int attempts = 0;
        boolean approved = false;
        String message = "";

        while (attempts < maxRetryCount && !approved) {
            attempts++;
            logger.info("Payment attempt {} of {} for orderId: {}", attempts, maxRetryCount, request.getOrderId());

            logService.logInfo("Payment attempt " + attempts,
                    String.format("OrderId: %d, MaxRetries: %d", request.getOrderId(), maxRetryCount));

            // Simulate payment processing
            if (shouldRejectPayment()) {
                message = String.format("Payment rejected on attempt %d", attempts);
                logger.warn("Payment rejected for orderId: {} on attempt {}", request.getOrderId(), attempts);

                logService.logWarn("Payment attempt rejected",
                        String.format("OrderId: %d, Attempt: %d/%d", request.getOrderId(), attempts, maxRetryCount));

                if (attempts < maxRetryCount) {
                    // Wait a bit before retry (in production, use exponential backoff)
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            } else {
                approved = true;
                message = "Payment approved";
                logger.info("Payment approved for orderId: {} on attempt {}", request.getOrderId(), attempts);

                logService.logInfo("Payment approved",
                        String.format("OrderId: %d, Attempt: %d/%d", request.getOrderId(), attempts, maxRetryCount));
            }
        }

        // If all attempts failed, send email notification
        if (!approved) {
            logger.error("Payment failed for orderId: {} after {} attempts", request.getOrderId(), attempts);

            logService.logError("Payment failed after all attempts",
                    String.format("OrderId: %d, TotalAttempts: %d, ClientEmail: %s",
                            request.getOrderId(), attempts, clientEmail));

            emailService.sendPaymentRejectionEmail(clientEmail, request.getOrderId(), attempts);
        }

        return PaymentResponseDTO.builder()
                .approved(approved)
                .message(message)
                .token(request.getToken())
                .attempts(attempts)
                .build();
    }

    /**
     * Determines if the payment should be rejected based on configured probability
     */
    private boolean shouldRejectPayment() {
        if (rejectionProbability <= 0.0) {
            return false;
        }
        if (rejectionProbability >= 1.0) {
            return true;
        }
        return Math.random() < rejectionProbability;
    }
}
