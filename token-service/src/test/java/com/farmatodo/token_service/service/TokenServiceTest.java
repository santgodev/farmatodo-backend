package com.farmatodo.token_service.service;

import com.farmatodo.token_service.dto.CardRequestDTO;
import com.farmatodo.token_service.dto.TokenResponseDTO;
import com.farmatodo.token_service.exception.BusinessException;
import com.farmatodo.token_service.model.TokenizedCard;
import com.farmatodo.token_service.repository.TokenRepository;
import com.farmatodo.token_service.util.CardValidator;
import com.farmatodo.token_service.util.EncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private CardValidator cardValidator;

    @Mock
    private EncryptionUtil encryptionUtil;

    @InjectMocks
    private TokenService tokenService;

    private CardRequestDTO validCardRequest;

    @BeforeEach
    void setUp() {
        MDC.put("transactionId", "test-transaction-id");

        validCardRequest = new CardRequestDTO(
                "4111111111111111",
                "123",
                "12/28",
                "John Doe",
                null  // No custom rejection probability, use default
        );

        // Set default rejection probability to 0 for most tests
        ReflectionTestUtils.setField(tokenService, "defaultRejectionProbability", 0.0);
    }

    @Test
    void testTokenize_HappyPath_ShouldReturnTokenResponse() {
        // Arrange
        doNothing().when(cardValidator).validateCardNumber(anyString());
        doNothing().when(cardValidator).validateCvv(anyString());
        doNothing().when(cardValidator).validateExpiry(anyString());
        doNothing().when(cardValidator).validateCardholderName(anyString());
        when(cardValidator.cleanCardNumber(anyString())).thenReturn("4111111111111111");
        when(encryptionUtil.encrypt(anyString())).thenReturn("encrypted-data");

        TokenizedCard savedCard = TokenizedCard.builder()
                .id(1L)
                .token("test-token")
                .last4("1111")
                .cardHashOrCipher("encrypted-data")
                .status("ACTIVE")
                .build();

        when(tokenRepository.save(any(TokenizedCard.class))).thenReturn(savedCard);

        // Act
        TokenResponseDTO response = tokenService.tokenize(validCardRequest);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("1111", response.getLast4());
        assertEquals("ACTIVE", response.getStatus());
        assertNotNull(response.getCreatedAt());

        verify(cardValidator).validateCardNumber("4111111111111111");
        verify(cardValidator).validateCvv("123");
        verify(cardValidator).validateExpiry("12/28");
        verify(cardValidator).validateCardholderName("John Doe");
        verify(tokenRepository).save(any(TokenizedCard.class));
    }

    @Test
    void testTokenize_WithRejectionProbability_ShouldThrowTokenRejected() {
        // Arrange
        // Set rejection probability to 1.0 (100%) in the request
        validCardRequest.setRejectionProbability(1.0);

        doNothing().when(cardValidator).validateCardNumber(anyString());
        doNothing().when(cardValidator).validateCvv(anyString());
        doNothing().when(cardValidator).validateExpiry(anyString());
        doNothing().when(cardValidator).validateCardholderName(anyString());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            tokenService.tokenize(validCardRequest);
        });

        assertEquals("TOKEN_REJECTED", exception.getErrorCode());
        assertEquals(422, exception.getHttpStatus());
        assertEquals("Token generation was rejected", exception.getMessage());

        verify(tokenRepository, never()).save(any());
    }

    @Test
    void testTokenize_InvalidCardNumber_ShouldThrowException() {
        // Arrange
        doThrow(new BusinessException("Card number failed Luhn validation", "INVALID_CARD_NUMBER", 400))
                .when(cardValidator).validateCardNumber(anyString());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            tokenService.tokenize(validCardRequest);
        });

        assertEquals("INVALID_CARD_NUMBER", exception.getErrorCode());
        assertEquals(400, exception.getHttpStatus());

        verify(tokenRepository, never()).save(any());
    }

    @Test
    void testTokenize_InvalidCvv_ShouldThrowException() {
        // Arrange
        doNothing().when(cardValidator).validateCardNumber(anyString());
        doThrow(new BusinessException("CVV must be 3-4 digits", "INVALID_CVV", 400))
                .when(cardValidator).validateCvv(anyString());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            tokenService.tokenize(validCardRequest);
        });

        assertEquals("INVALID_CVV", exception.getErrorCode());
        assertEquals(400, exception.getHttpStatus());

        verify(tokenRepository, never()).save(any());
    }

    @Test
    void testTokenize_InvalidExpiry_ShouldThrowException() {
        // Arrange
        doNothing().when(cardValidator).validateCardNumber(anyString());
        doNothing().when(cardValidator).validateCvv(anyString());
        doThrow(new BusinessException("Expiry must be in format MM/YY", "INVALID_EXPIRY", 400))
                .when(cardValidator).validateExpiry(anyString());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            tokenService.tokenize(validCardRequest);
        });

        assertEquals("INVALID_EXPIRY", exception.getErrorCode());
        assertEquals(400, exception.getHttpStatus());

        verify(tokenRepository, never()).save(any());
    }

    @Test
    void testTokenize_InvalidCardholderName_ShouldThrowException() {
        // Arrange
        doNothing().when(cardValidator).validateCardNumber(anyString());
        doNothing().when(cardValidator).validateCvv(anyString());
        doNothing().when(cardValidator).validateExpiry(anyString());
        doThrow(new BusinessException("Cardholder name is required", "INVALID_CARDHOLDER_NAME", 400))
                .when(cardValidator).validateCardholderName(anyString());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            tokenService.tokenize(validCardRequest);
        });

        assertEquals("INVALID_CARDHOLDER_NAME", exception.getErrorCode());
        assertEquals(400, exception.getHttpStatus());

        verify(tokenRepository, never()).save(any());
    }

    @Test
    void testTokenize_ShouldExtractLast4Digits() {
        // Arrange
        doNothing().when(cardValidator).validateCardNumber(anyString());
        doNothing().when(cardValidator).validateCvv(anyString());
        doNothing().when(cardValidator).validateExpiry(anyString());
        doNothing().when(cardValidator).validateCardholderName(anyString());
        when(cardValidator.cleanCardNumber(anyString())).thenReturn("4111111111111111");
        when(encryptionUtil.encrypt(anyString())).thenReturn("encrypted-data");

        TokenizedCard savedCard = TokenizedCard.builder()
                .id(1L)
                .token("test-token")
                .last4("1111")
                .cardHashOrCipher("encrypted-data")
                .status("ACTIVE")
                .build();

        when(tokenRepository.save(any(TokenizedCard.class))).thenReturn(savedCard);

        // Act
        TokenResponseDTO response = tokenService.tokenize(validCardRequest);

        // Assert
        assertEquals("1111", response.getLast4());
    }

    @Test
    void testTokenize_ShouldEncryptCardData() {
        // Arrange
        doNothing().when(cardValidator).validateCardNumber(anyString());
        doNothing().when(cardValidator).validateCvv(anyString());
        doNothing().when(cardValidator).validateExpiry(anyString());
        doNothing().when(cardValidator).validateCardholderName(anyString());
        when(cardValidator.cleanCardNumber(anyString())).thenReturn("4111111111111111");
        when(encryptionUtil.encrypt(anyString())).thenReturn("encrypted-data");

        TokenizedCard savedCard = TokenizedCard.builder()
                .id(1L)
                .token("test-token")
                .last4("1111")
                .cardHashOrCipher("encrypted-data")
                .status("ACTIVE")
                .build();

        when(tokenRepository.save(any(TokenizedCard.class))).thenReturn(savedCard);

        // Act
        tokenService.tokenize(validCardRequest);

        // Assert
        verify(encryptionUtil).encrypt(anyString());
    }
}
