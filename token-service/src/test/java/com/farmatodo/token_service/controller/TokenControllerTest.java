package com.farmatodo.token_service.controller;

import com.farmatodo.token_service.dto.CardRequestDTO;
import com.farmatodo.token_service.dto.TokenResponseDTO;
import com.farmatodo.token_service.exception.BusinessException;
import com.farmatodo.token_service.service.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TokenController.class)
@TestPropertySource(properties = {
        "api.key=test-api-key-12345"
})
class TokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TokenService tokenService;

    private CardRequestDTO validCardRequest;
    private TokenResponseDTO tokenResponse;

    @BeforeEach
    void setUp() {
        validCardRequest = new CardRequestDTO(
                "4111111111111111",
                "123",
                "12/28",
                "John Doe",
                null
        );

        tokenResponse = TokenResponseDTO.builder()
                .token("test-token-uuid")
                .last4("1111")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ==================== PING ENDPOINT TESTS ====================

    @Test
    void testPing_ShouldReturnPong() throws Exception {
        mockMvc.perform(get("/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("pong"));
    }

    // ==================== TOKENIZE ENDPOINT - HAPPY PATH TESTS ====================

    @Test
    void testTokenize_ValidCardWithApiKey_ShouldReturnToken() throws Exception {
        // Arrange
        when(tokenService.tokenize(any(CardRequestDTO.class))).thenReturn(tokenResponse);

        // Act & Assert
        mockMvc.perform(post("/tokenize")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCardRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test-token-uuid"))
                .andExpect(jsonPath("$.last4").value("1111"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").exists());

        verify(tokenService, times(1)).tokenize(any(CardRequestDTO.class));
    }

    @Test
    void testTokenize_ValidCardWithCustomRejectionProbability_ShouldReturnToken() throws Exception {
        // Arrange
        CardRequestDTO customRequest = new CardRequestDTO(
                "4111111111111111",
                "123",
                "12/28",
                "John Doe",
                0.5  // 50% rejection probability
        );

        when(tokenService.tokenize(any(CardRequestDTO.class))).thenReturn(tokenResponse);

        // Act & Assert
        mockMvc.perform(post("/tokenize")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.last4").value("1111"));

        verify(tokenService, times(1)).tokenize(any(CardRequestDTO.class));
    }

    // ==================== TOKENIZE ENDPOINT - API KEY AUTHENTICATION TESTS ====================

    @Test
    void testTokenize_MissingApiKey_ShouldReturnUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/tokenize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCardRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Missing Authorization header"));

        verify(tokenService, never()).tokenize(any());
    }

    @Test
    void testTokenize_InvalidApiKeyFormat_ShouldReturnUnauthorized() throws Exception {
        // Act & Assert - Missing "ApiKey " prefix
        mockMvc.perform(post("/tokenize")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCardRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid Authorization header format. Expected: ApiKey <key>"));

        verify(tokenService, never()).tokenize(any());
    }

    @Test
    void testTokenize_WrongApiKey_ShouldReturnUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/tokenize")
                        .header("Authorization", "ApiKey wrong-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCardRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid API key"));

        verify(tokenService, never()).tokenize(any());
    }

    @Test
    void testTokenize_EmptyApiKey_ShouldReturnUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/tokenize")
                        .header("Authorization", "ApiKey ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCardRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verify(tokenService, never()).tokenize(any());
    }

    // ==================== TOKENIZE ENDPOINT - VALIDATION TESTS ====================

    @Test
    void testTokenize_InvalidCardNumber_ShouldReturnBadRequest() throws Exception {
        // Arrange
        when(tokenService.tokenize(any(CardRequestDTO.class)))
                .thenThrow(new BusinessException(
                        "Card number failed Luhn validation",
                        "INVALID_CARD_NUMBER",
                        400
                ));

        // Act & Assert
        mockMvc.perform(post("/tokenize")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCardRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CARD_NUMBER"))
                .andExpect(jsonPath("$.message").value("Card number failed Luhn validation"));

        verify(tokenService, times(1)).tokenize(any(CardRequestDTO.class));
    }

    @Test
    void testTokenize_InvalidCvv_ShouldReturnBadRequest() throws Exception {
        // Arrange
        when(tokenService.tokenize(any(CardRequestDTO.class)))
                .thenThrow(new BusinessException(
                        "CVV must be 3-4 digits",
                        "INVALID_CVV",
                        400
                ));

        // Act & Assert
        mockMvc.perform(post("/tokenize")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCardRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CVV"))
                .andExpect(jsonPath("$.message").value("CVV must be 3-4 digits"));

        verify(tokenService, times(1)).tokenize(any(CardRequestDTO.class));
    }

    @Test
    void testTokenize_InvalidExpiry_ShouldReturnBadRequest() throws Exception {
        // Arrange
        when(tokenService.tokenize(any(CardRequestDTO.class)))
                .thenThrow(new BusinessException(
                        "Expiry must be in format MM/YY",
                        "INVALID_EXPIRY",
                        400
                ));

        // Act & Assert
        mockMvc.perform(post("/tokenize")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCardRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_EXPIRY"))
                .andExpect(jsonPath("$.message").value("Expiry must be in format MM/YY"));

        verify(tokenService, times(1)).tokenize(any(CardRequestDTO.class));
    }

    @Test
    void testTokenize_InvalidCardholderName_ShouldReturnBadRequest() throws Exception {
        // Arrange
        when(tokenService.tokenize(any(CardRequestDTO.class)))
                .thenThrow(new BusinessException(
                        "Cardholder name is required",
                        "INVALID_CARDHOLDER_NAME",
                        400
                ));

        // Act & Assert
        mockMvc.perform(post("/tokenize")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCardRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CARDHOLDER_NAME"))
                .andExpect(jsonPath("$.message").value("Cardholder name is required"));

        verify(tokenService, times(1)).tokenize(any(CardRequestDTO.class));
    }

    // ==================== TOKENIZE ENDPOINT - REJECTION PROBABILITY TESTS ====================

    @Test
    void testTokenize_TokenRejectedDueToProbability_ShouldReturnUnprocessableEntity() throws Exception {
        // Arrange
        when(tokenService.tokenize(any(CardRequestDTO.class)))
                .thenThrow(new BusinessException(
                        "Token generation was rejected",
                        "TOKEN_REJECTED",
                        422
                ));

        // Act & Assert
        mockMvc.perform(post("/tokenize")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCardRequest)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("TOKEN_REJECTED"))
                .andExpect(jsonPath("$.message").value("Token generation was rejected"));

        verify(tokenService, times(1)).tokenize(any(CardRequestDTO.class));
    }

    @Test
    void testTokenize_InvalidRejectionProbability_ShouldReturnBadRequest() throws Exception {
        // Arrange
        CardRequestDTO invalidRequest = new CardRequestDTO(
                "4111111111111111",
                "123",
                "12/28",
                "John Doe",
                1.5  // Invalid: > 1.0
        );

        when(tokenService.tokenize(any(CardRequestDTO.class)))
                .thenThrow(new BusinessException(
                        "Rejection probability must be between 0.0 and 1.0",
                        "INVALID_REJECTION_PROBABILITY",
                        400
                ));

        // Act & Assert
        mockMvc.perform(post("/tokenize")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REJECTION_PROBABILITY"));

        verify(tokenService, times(1)).tokenize(any(CardRequestDTO.class));
    }

    // ==================== TOKENIZE ENDPOINT - MISSING FIELDS TESTS ====================

    @Test
    void testTokenize_MissingCardNumber_ShouldReturnBadRequest() throws Exception {
        // Arrange
        CardRequestDTO invalidRequest = new CardRequestDTO(
                null,  // Missing card number
                "123",
                "12/28",
                "John Doe",
                null
        );

        when(tokenService.tokenize(any(CardRequestDTO.class)))
                .thenThrow(new BusinessException(
                        "Card number is required",
                        "INVALID_CARD_NUMBER",
                        400
                ));

        // Act & Assert
        mockMvc.perform(post("/tokenize")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CARD_NUMBER"));
    }

    @Test
    void testTokenize_MissingCvv_ShouldReturnBadRequest() throws Exception {
        // Arrange
        CardRequestDTO invalidRequest = new CardRequestDTO(
                "4111111111111111",
                null,  // Missing CVV
                "12/28",
                "John Doe",
                null
        );

        when(tokenService.tokenize(any(CardRequestDTO.class)))
                .thenThrow(new BusinessException(
                        "CVV is required",
                        "INVALID_CVV",
                        400
                ));

        // Act & Assert
        mockMvc.perform(post("/tokenize")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CVV"));
    }
}
