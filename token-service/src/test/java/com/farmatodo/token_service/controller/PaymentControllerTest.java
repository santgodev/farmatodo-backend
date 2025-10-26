package com.farmatodo.token_service.controller;

import com.farmatodo.token_service.dto.PaymentRequestDTO;
import com.farmatodo.token_service.dto.PaymentResponseDTO;
import com.farmatodo.token_service.exception.BusinessException;
import com.farmatodo.token_service.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@TestPropertySource(properties = {
        "api.key=test-api-key-12345",
        "payment.rejectionProbability=0.3",
        "payment.retryCount=3"
})
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    private PaymentRequestDTO validPaymentRequest;
    private PaymentResponseDTO approvedPaymentResponse;
    private PaymentResponseDTO rejectedPaymentResponse;

    @BeforeEach
    void setUp() {
        validPaymentRequest = PaymentRequestDTO.builder()
                .token("test-token-uuid")
                .amount(new BigDecimal("100.00"))
                .orderId(1L)
                .clientId(1L)
                .build();

        approvedPaymentResponse = PaymentResponseDTO.builder()
                .approved(true)
                .message("Payment approved")
                .token("test-token-uuid")
                .attempts(1)
                .build();

        rejectedPaymentResponse = PaymentResponseDTO.builder()
                .approved(false)
                .message("Payment rejected on attempt 3")
                .token("test-token-uuid")
                .attempts(3)
                .build();
    }

    // ==================== PING ENDPOINT TESTS ====================

    @Test
    void testPing_ShouldReturnPong() throws Exception {
        mockMvc.perform(get("/api/tokens/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("pong"));
    }

    // ==================== PAYMENT ENDPOINT - HAPPY PATH TESTS ====================

    @Test
    void testProcessPayment_ApprovedOnFirstAttempt_ShouldReturnOk() throws Exception {
        // Arrange
        when(paymentService.processPayment(any(PaymentRequestDTO.class), anyString()))
                .thenReturn(approvedPaymentResponse);

        // Act & Assert
        mockMvc.perform(post("/api/tokens/payment")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .header("X-Client-Email", "customer@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPaymentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approved").value(true))
                .andExpect(jsonPath("$.message").value("Payment approved"))
                .andExpect(jsonPath("$.token").value("test-token-uuid"))
                .andExpect(jsonPath("$.attempts").value(1));

        verify(paymentService, times(1)).processPayment(any(PaymentRequestDTO.class), eq("customer@example.com"));
    }

    @Test
    void testProcessPayment_ApprovedAfterRetries_ShouldReturnOk() throws Exception {
        // Arrange
        PaymentResponseDTO retriedResponse = PaymentResponseDTO.builder()
                .approved(true)
                .message("Payment approved")
                .token("test-token-uuid")
                .attempts(2)  // Approved on 2nd attempt
                .build();

        when(paymentService.processPayment(any(PaymentRequestDTO.class), anyString()))
                .thenReturn(retriedResponse);

        // Act & Assert
        mockMvc.perform(post("/api/tokens/payment")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .header("X-Client-Email", "customer@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPaymentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approved").value(true))
                .andExpect(jsonPath("$.attempts").value(2));

        verify(paymentService, times(1)).processPayment(any(PaymentRequestDTO.class), anyString());
    }

    @Test
    void testProcessPayment_WithDefaultEmail_ShouldUseDefaultCustomerEmail() throws Exception {
        // Arrange
        when(paymentService.processPayment(any(PaymentRequestDTO.class), anyString()))
                .thenReturn(approvedPaymentResponse);

        // Act & Assert - No X-Client-Email header provided
        mockMvc.perform(post("/api/tokens/payment")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPaymentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approved").value(true));

        verify(paymentService, times(1)).processPayment(
                any(PaymentRequestDTO.class),
                eq("customer@example.com")  // Default email
        );
    }

    // ==================== PAYMENT ENDPOINT - REJECTION TESTS ====================

    @Test
    void testProcessPayment_RejectedAfterAllAttempts_ShouldReturnPaymentRequired() throws Exception {
        // Arrange
        when(paymentService.processPayment(any(PaymentRequestDTO.class), anyString()))
                .thenReturn(rejectedPaymentResponse);

        // Act & Assert
        mockMvc.perform(post("/api/tokens/payment")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .header("X-Client-Email", "customer@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPaymentRequest)))
                .andExpect(status().isPaymentRequired())  // HTTP 402
                .andExpect(jsonPath("$.approved").value(false))
                .andExpect(jsonPath("$.message").value("Payment rejected on attempt 3"))
                .andExpect(jsonPath("$.attempts").value(3));

        verify(paymentService, times(1)).processPayment(any(PaymentRequestDTO.class), anyString());
    }

    // ==================== PAYMENT ENDPOINT - API KEY AUTHENTICATION TESTS ====================

    @Test
    void testProcessPayment_MissingApiKey_ShouldReturnUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/tokens/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPaymentRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Missing Authorization header"));

        verify(paymentService, never()).processPayment(any(), anyString());
    }

    @Test
    void testProcessPayment_InvalidApiKey_ShouldReturnUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/tokens/payment")
                        .header("Authorization", "ApiKey wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPaymentRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid API key"));

        verify(paymentService, never()).processPayment(any(), anyString());
    }

    // ==================== PAYMENT ENDPOINT - VALIDATION TESTS ====================

    @Test
    void testProcessPayment_InvalidToken_ShouldReturnNotFound() throws Exception {
        // Arrange
        when(paymentService.processPayment(any(PaymentRequestDTO.class), anyString()))
                .thenThrow(new BusinessException("Invalid token", "INVALID_TOKEN", 404));

        // Act & Assert
        mockMvc.perform(post("/api/tokens/payment")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .header("X-Client-Email", "customer@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPaymentRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"))
                .andExpect(jsonPath("$.message").value("Invalid token"));

        verify(paymentService, times(1)).processPayment(any(PaymentRequestDTO.class), anyString());
    }

    @Test
    void testProcessPayment_TokenDecryptError_ShouldReturnBadRequest() throws Exception {
        // Arrange
        when(paymentService.processPayment(any(PaymentRequestDTO.class), anyString()))
                .thenThrow(new BusinessException("Invalid token data", "TOKEN_DECRYPT_ERROR", 400));

        // Act & Assert
        mockMvc.perform(post("/api/tokens/payment")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .header("X-Client-Email", "customer@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPaymentRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("TOKEN_DECRYPT_ERROR"))
                .andExpect(jsonPath("$.message").value("Invalid token data"));

        verify(paymentService, times(1)).processPayment(any(PaymentRequestDTO.class), anyString());
    }

    @Test
    void testProcessPayment_MissingToken_ShouldProcessRequest() throws Exception {
        // Arrange
        PaymentRequestDTO invalidRequest = PaymentRequestDTO.builder()
                .token(null)  // Missing token
                .amount(new BigDecimal("100.00"))
                .orderId(1L)
                .clientId(1L)
                .build();

        when(paymentService.processPayment(any(PaymentRequestDTO.class), anyString()))
                .thenThrow(new BusinessException("Token is required", "INVALID_TOKEN", 400));

        // Act & Assert
        mockMvc.perform(post("/api/tokens/payment")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testProcessPayment_MissingAmount_ShouldProcessRequest() throws Exception {
        // Arrange
        PaymentRequestDTO invalidRequest = PaymentRequestDTO.builder()
                .token("test-token-uuid")
                .amount(null)  // Missing amount
                .orderId(1L)
                .clientId(1L)
                .build();

        when(paymentService.processPayment(any(PaymentRequestDTO.class), anyString()))
                .thenReturn(approvedPaymentResponse);

        // Act & Assert
        mockMvc.perform(post("/api/tokens/payment")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void testProcessPayment_MissingOrderId_ShouldProcessRequest() throws Exception {
        // Arrange
        PaymentRequestDTO invalidRequest = PaymentRequestDTO.builder()
                .token("test-token-uuid")
                .amount(new BigDecimal("100.00"))
                .orderId(null)  // Missing orderId
                .clientId(1L)
                .build();

        when(paymentService.processPayment(any(PaymentRequestDTO.class), anyString()))
                .thenReturn(approvedPaymentResponse);

        // Act & Assert
        mockMvc.perform(post("/api/tokens/payment")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isOk());
    }

    // ==================== PAYMENT ENDPOINT - CUSTOM EMAIL HEADER TESTS ====================

    @Test
    void testProcessPayment_WithCustomEmail_ShouldUseCustomEmail() throws Exception {
        // Arrange
        when(paymentService.processPayment(any(PaymentRequestDTO.class), anyString()))
                .thenReturn(approvedPaymentResponse);

        // Act & Assert
        mockMvc.perform(post("/api/tokens/payment")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .header("X-Client-Email", "john.doe@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPaymentRequest)))
                .andExpect(status().isOk());

        verify(paymentService, times(1)).processPayment(
                any(PaymentRequestDTO.class),
                eq("john.doe@example.com")
        );
    }

    @Test
    void testProcessPayment_WithEmptyEmail_ShouldUseDefaultEmail() throws Exception {
        // Arrange
        when(paymentService.processPayment(any(PaymentRequestDTO.class), anyString()))
                .thenReturn(approvedPaymentResponse);

        // Act & Assert
        mockMvc.perform(post("/api/tokens/payment")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .header("X-Client-Email", "")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPaymentRequest)))
                .andExpect(status().isOk());

        verify(paymentService, times(1)).processPayment(
                any(PaymentRequestDTO.class),
                eq("customer@example.com")  // Falls back to default
        );
    }
}
