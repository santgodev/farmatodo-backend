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
    private double defaultRejectionProbability;

    @Value("${payment.retryCount:3}")
    private int maxRetryCount;

    /**
     * Process payment with retry logic
     */
    public PaymentResponseDTO processPayment(PaymentRequestDTO request, String clientEmail) {
        String transactionId = MDC.get("transactionId");
        logger.info("Starting payment processing for orderId: {}, transactionId: {}", request.getOrderId(), transactionId);

        // Determine which rejection probability to use
        double rejectionProbability = request.getRejectionProbability() != null
                ? request.getRejectionProbability()
                : defaultRejectionProbability;

        // Determine which retry count to use
        int retryCount = request.getMaxAttempts() != null
                ? request.getMaxAttempts()
                : maxRetryCount;

        // Validate rejection probability range
        if (rejectionProbability < 0.0 || rejectionProbability > 1.0) {
            logger.warn("Invalid rejection probability: {} for transaction: {}", rejectionProbability, transactionId);
            throw new BusinessException(
                    "Rejection probability must be between 0.0 and 1.0",
                    "INVALID_REJECTION_PROBABILITY",
                    400
            );
        }

        // Validate retry count
        if (retryCount < 1 || retryCount > 10) {
            logger.warn("Invalid retry count: {} for transaction: {}", retryCount, transactionId);
            throw new BusinessException(
                    "Retry count must be between 1 and 10",
                    "INVALID_RETRY_COUNT",
                    400
            );
        }

        logger.info("Using rejection probability: {} and retry count: {} for orderId: {}, transactionId: {}",
                rejectionProbability, retryCount, request.getOrderId(), transactionId);

        logService.logInfo("Payment process started",
                String.format("OrderId: %d, Token: %s, Amount: %s, RejectionProb: %.2f, MaxAttempts: %d",
                        request.getOrderId(), request.getToken(), request.getAmount(), rejectionProbability, retryCount));

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

        while (attempts < retryCount && !approved) {
            attempts++;
            logger.info("Payment attempt {} of {} for orderId: {}", attempts, retryCount, request.getOrderId());

            logService.logInfo("Payment attempt " + attempts,
                    String.format("OrderId: %d, MaxRetries: %d", request.getOrderId(), retryCount));

            // Simulate payment processing
            if (shouldRejectPayment(rejectionProbability)) {
                message = String.format("Payment rejected on attempt %d", attempts);
                logger.warn("Payment rejected for orderId: {} on attempt {}", request.getOrderId(), attempts);

                logService.logWarn("Payment attempt rejected",
                        String.format("OrderId: %d, Attempt: %d/%d", request.getOrderId(), attempts, retryCount));

                if (attempts < retryCount) {
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
                        String.format("OrderId: %d, Attempt: %d/%d", request.getOrderId(), attempts, retryCount));
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
     * Determines if the payment should be rejected based on provided probability
     */
    private boolean shouldRejectPayment(double probability) {
        if (probability <= 0.0) {
            return false;
        }
        if (probability >= 1.0) {
            return true;
        }
        return Math.random() < probability;
    }
}
