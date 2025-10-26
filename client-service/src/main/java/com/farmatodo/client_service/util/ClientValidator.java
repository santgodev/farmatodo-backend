package com.farmatodo.client_service.util;

import com.farmatodo.client_service.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class ClientValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^[+]?[0-9]{8,15}$"
    );

    /**
     * Validates client name
     */
    public void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException(
                    "Name is required",
                    "INVALID_NAME",
                    400
            );
        }

        if (name.trim().length() < 2) {
            throw new BusinessException(
                    "Name must be at least 2 characters",
                    "INVALID_NAME",
                    400
            );
        }

        if (name.length() > 100) {
            throw new BusinessException(
                    "Name must not exceed 100 characters",
                    "INVALID_NAME",
                    400
            );
        }
    }

    /**
     * Validates email format
     */
    public void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new BusinessException(
                    "Email is required",
                    "INVALID_EMAIL",
                    400
            );
        }

        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new BusinessException(
                    "Email format is invalid",
                    "INVALID_EMAIL",
                    400
            );
        }

        if (email.length() > 100) {
            throw new BusinessException(
                    "Email must not exceed 100 characters",
                    "INVALID_EMAIL",
                    400
            );
        }
    }

    /**
     * Validates phone number format
     */
    public void validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new BusinessException(
                    "Phone is required",
                    "INVALID_PHONE",
                    400
            );
        }

        String cleanPhone = phone.replaceAll("[\\s-]", "");

        if (!PHONE_PATTERN.matcher(cleanPhone).matches()) {
            throw new BusinessException(
                    "Phone must be 8-15 digits, optionally starting with +",
                    "INVALID_PHONE",
                    400
            );
        }
    }

    /**
     * Validates address
     */
    public void validateAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            throw new BusinessException(
                    "Address is required",
                    "INVALID_ADDRESS",
                    400
            );
        }

        if (address.trim().length() < 5) {
            throw new BusinessException(
                    "Address must be at least 5 characters",
                    "INVALID_ADDRESS",
                    400
            );
        }

        if (address.length() > 255) {
            throw new BusinessException(
                    "Address must not exceed 255 characters",
                    "INVALID_ADDRESS",
                    400
            );
        }
    }

    /**
     * Validates document type
     */
    public void validateDocumentType(String documentType) {
        if (documentType == null || documentType.trim().isEmpty()) {
            throw new BusinessException(
                    "Document type is required",
                    "INVALID_DOCUMENT_TYPE",
                    400
            );
        }

        String upperDocType = documentType.trim().toUpperCase();

        if (!upperDocType.matches("^(DNI|PASSPORT|ID|DRIVER_LICENSE|OTHER)$")) {
            throw new BusinessException(
                    "Document type must be one of: DNI, PASSPORT, ID, DRIVER_LICENSE, OTHER",
                    "INVALID_DOCUMENT_TYPE",
                    400
            );
        }
    }

    /**
     * Validates document number
     */
    public void validateDocumentNumber(String documentNumber) {
        if (documentNumber == null || documentNumber.trim().isEmpty()) {
            throw new BusinessException(
                    "Document number is required",
                    "INVALID_DOCUMENT_NUMBER",
                    400
            );
        }

        if (documentNumber.trim().length() < 5) {
            throw new BusinessException(
                    "Document number must be at least 5 characters",
                    "INVALID_DOCUMENT_NUMBER",
                    400
            );
        }

        if (documentNumber.length() > 50) {
            throw new BusinessException(
                    "Document number must not exceed 50 characters",
                    "INVALID_DOCUMENT_NUMBER",
                    400
            );
        }
    }

    /**
     * Returns cleaned phone number (removes spaces and dashes)
     */
    public String cleanPhone(String phone) {
        return phone.replaceAll("[\\s-]", "");
    }

    /**
     * Returns normalized email (lowercase, trimmed)
     */
    public String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    /**
     * Returns normalized document type (uppercase, trimmed)
     */
    public String normalizeDocumentType(String documentType) {
        return documentType.trim().toUpperCase();
    }
}
