package com.farmatodo.token_service.util;

import com.farmatodo.token_service.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class CardValidator {

    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("^[0-9]{13,19}$");
    private static final Pattern CVV_PATTERN = Pattern.compile("^[0-9]{3,4}$");
    private static final Pattern EXPIRY_PATTERN = Pattern.compile("^(0[1-9]|1[0-2])/([0-9]{2})$");

    /**
     * Validates card number using Luhn algorithm and basic format checks
     */
    public void validateCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.trim().isEmpty()) {
            throw new BusinessException("Card number is required", "INVALID_CARD_NUMBER", 400);
        }

        // Remove any spaces or dashes
        String cleanCardNumber = cardNumber.replaceAll("[\\s-]", "");

        // Check format (13-19 digits)
        if (!CARD_NUMBER_PATTERN.matcher(cleanCardNumber).matches()) {
            throw new BusinessException(
                    "Card number must be 13-19 digits",
                    "INVALID_CARD_NUMBER",
                    400
            );
        }

        // Luhn algorithm validation
        if (!isValidLuhn(cleanCardNumber)) {
            throw new BusinessException(
                    "Card number failed Luhn validation",
                    "INVALID_CARD_NUMBER",
                    400
            );
        }
    }

    /**
     * Validates CVV format
     */
    public void validateCvv(String cvv) {
        if (cvv == null || !CVV_PATTERN.matcher(cvv).matches()) {
            throw new BusinessException(
                    "CVV must be 3-4 digits",
                    "INVALID_CVV",
                    400
            );
        }
    }

    /**
     * Validates expiry format (MM/YY)
     */
    public void validateExpiry(String expiry) {
        if (expiry == null || !EXPIRY_PATTERN.matcher(expiry).matches()) {
            throw new BusinessException(
                    "Expiry must be in format MM/YY",
                    "INVALID_EXPIRY",
                    400
            );
        }
    }

    /**
     * Validates cardholder name
     */
    public void validateCardholderName(String cardholderName) {
        if (cardholderName == null || cardholderName.trim().isEmpty()) {
            throw new BusinessException(
                    "Cardholder name is required",
                    "INVALID_CARDHOLDER_NAME",
                    400
            );
        }

        if (cardholderName.trim().length() < 2) {
            throw new BusinessException(
                    "Cardholder name must be at least 2 characters",
                    "INVALID_CARDHOLDER_NAME",
                    400
            );
        }
    }

    /**
     * Luhn algorithm implementation
     */
    private boolean isValidLuhn(String cardNumber) {
        int sum = 0;
        boolean alternate = false;

        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));

            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            alternate = !alternate;
        }

        return sum % 10 == 0;
    }

    /**
     * Returns cleaned card number (digits only)
     */
    public String cleanCardNumber(String cardNumber) {
        return cardNumber.replaceAll("[\\s-]", "");
    }
}
