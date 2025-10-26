package com.farmatodo.token_service.service;

import com.farmatodo.token_service.dto.CardRequestDTO;
import com.farmatodo.token_service.dto.TokenResponseDTO;
import com.farmatodo.token_service.exception.BusinessException;
import com.farmatodo.token_service.model.TokenizedCard;
import com.farmatodo.token_service.repository.TokenRepository;
import com.farmatodo.token_service.util.CardValidator;
import com.farmatodo.token_service.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {

    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);

    private final TokenRepository tokenRepository;
    private final CardValidator cardValidator;
    private final EncryptionUtil encryptionUtil;

    @Value("${token.rejectionProbability:0.3}")
    private double defaultRejectionProbability;

    @Transactional
    public TokenResponseDTO tokenize(CardRequestDTO request) {
        String transactionId = MDC.get("transactionId");
        logger.info("Starting tokenization process for transaction: {}", transactionId);

        // Validate all card fields
        cardValidator.validateCardNumber(request.getCardNumber());
        cardValidator.validateCvv(request.getCvv());
        cardValidator.validateExpiry(request.getExpiry());
        cardValidator.validateCardholderName(request.getCardholderName());

        logger.debug("Card validation passed for transaction: {}", transactionId);

        // Determine which rejection probability to use
        double rejectionProbability = request.getRejectionProbability() != null
                ? request.getRejectionProbability()
                : defaultRejectionProbability;

        // Validate rejection probability range
        if (rejectionProbability < 0.0 || rejectionProbability > 1.0) {
            logger.warn("Invalid rejection probability: {} for transaction: {}", rejectionProbability, transactionId);
            throw new BusinessException(
                    "Rejection probability must be between 0.0 and 1.0",
                    "INVALID_REJECTION_PROBABILITY",
                    400
            );
        }

        logger.info("Using rejection probability: {} for transaction: {}", rejectionProbability, transactionId);

        // Check rejection probability
        if (shouldReject(rejectionProbability)) {
            logger.warn("Token rejected due to rejection probability for transaction: {}", transactionId);
            throw new BusinessException(
                    "Token generation was rejected",
                    "TOKEN_REJECTED",
                    422
            );
        }

        // Clean card number
        String cleanCardNumber = cardValidator.cleanCardNumber(request.getCardNumber());

        // Get last 4 digits
        String last4 = cleanCardNumber.substring(cleanCardNumber.length() - 4);

        // Create card data string to encrypt
        String cardData = String.format("%s|%s|%s|%s",
                cleanCardNumber,
                request.getCvv(),
                request.getExpiry(),
                request.getCardholderName()
        );

        // Encrypt sensitive data
        String encryptedData = encryptionUtil.encrypt(cardData);

        // Generate UUID token
        String token = UUID.randomUUID().toString();

        // Create and save tokenized card
        TokenizedCard tokenizedCard = TokenizedCard.builder()
                .token(token)
                .last4(last4)
                .cardHashOrCipher(encryptedData)
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();

        tokenRepository.save(tokenizedCard);

        logger.info("Tokenization successful for transaction: {}, token: {}", transactionId, token);

        // Return response
        return TokenResponseDTO.builder()
                .token(token)
                .last4(last4)
                .status("ACTIVE")
                .createdAt(tokenizedCard.getCreatedAt())
                .build();
    }

    /**
     * Determines if the token should be rejected based on provided probability
     * @param probability Rejection probability (0.0 to 1.0)
     * @return true if token should be rejected, false otherwise
     */
    private boolean shouldReject(double probability) {
        if (probability <= 0.0) {
            return false;
        }
        if (probability >= 1.0) {
            return true;
        }
        return Math.random() < probability;
    }
}
