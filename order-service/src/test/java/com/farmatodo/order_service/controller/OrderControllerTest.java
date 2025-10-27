package com.farmatodo.order_service.controller;

import com.farmatodo.order_service.dto.CreateOrderRequestDTO;
import com.farmatodo.order_service.dto.OrderItemDTO;
import com.farmatodo.order_service.dto.OrderItemResponseDTO;
import com.farmatodo.order_service.dto.OrderResponseDTO;
import com.farmatodo.order_service.exception.BusinessException;
import com.farmatodo.order_service.service.OrderService;
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
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@TestPropertySource(properties = {
        "api.key=test-api-key-12345"
})
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    private CreateOrderRequestDTO validOrderRequest;
    private OrderResponseDTO orderResponse;

    @BeforeEach
    void setUp() {
        validOrderRequest = CreateOrderRequestDTO.builder()
                .clientId(1L)
                .token("test-token-uuid")
                .build();

        OrderItemResponseDTO item1 = OrderItemResponseDTO.builder()
                .productId(101L)
                .productName("Aspirin 500mg")
                .unitPrice(new BigDecimal("5.99"))
                .quantity(2)
                .subtotal(new BigDecimal("11.98"))
                .build();

        OrderItemResponseDTO item2 = OrderItemResponseDTO.builder()
                .productId(102L)
                .productName("Vitamin C")
                .unitPrice(new BigDecimal("12.99"))
                .quantity(1)
                .subtotal(new BigDecimal("12.99"))
                .build();

        orderResponse = OrderResponseDTO.builder()
                .orderId(1L)
                .clientId(1L)
                .token("test-token-uuid")
                .status("APPROVED")
                .paymentAttempts(1)
                .totalAmount(new BigDecimal("24.97"))
                .items(Arrays.asList(item1, item2))
                .transactionId("test-transaction-id")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ==================== PING ENDPOINT TESTS ====================

    @Test
    void testPing_ShouldReturnPong() throws Exception {
        mockMvc.perform(get("/orders/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("pong"));
    }

    // ==================== CREATE ORDER - HAPPY PATH TESTS ====================

    @Test
    void testCreateOrder_ValidRequest_ShouldReturnCreatedOrder() throws Exception {
        // Arrange
        when(orderService.createOrder(any(CreateOrderRequestDTO.class))).thenReturn(orderResponse);

        // Act & Assert
        mockMvc.perform(post("/orders")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.clientId").value(1))
                .andExpect(jsonPath("$.token").value("test-token-uuid"))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.paymentAttempts").value(1))
                .andExpect(jsonPath("$.totalAmount").value(24.97))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.transactionId").exists())
                .andExpect(jsonPath("$.createdAt").exists());

        verify(orderService, times(1)).createOrder(any(CreateOrderRequestDTO.class));
    }

    @Test
    void testCreateOrder_PaymentApprovedOnSecondAttempt_ShouldReturnOrder() throws Exception {
        // Arrange
        OrderResponseDTO retriedOrder = OrderResponseDTO.builder()
                .orderId(1L)
                .clientId(1L)
                .token("test-token-uuid")
                .status("APPROVED")
                .paymentAttempts(2)  // Approved on 2nd attempt
                .totalAmount(new BigDecimal("24.97"))
                .items(orderResponse.getItems())
                .transactionId("test-transaction-id")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(orderService.createOrder(any(CreateOrderRequestDTO.class))).thenReturn(retriedOrder);

        // Act & Assert
        mockMvc.perform(post("/orders")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.paymentAttempts").value(2));
    }

    // ==================== CREATE ORDER - PAYMENT REJECTION TESTS ====================

    @Test
    void testCreateOrder_PaymentRejected_ShouldReturnRejectedOrder() throws Exception {
        // Arrange
        OrderResponseDTO rejectedOrder = OrderResponseDTO.builder()
                .orderId(1L)
                .clientId(1L)
                .token("test-token-uuid")
                .status("REJECTED")
                .rejectionReason("Payment rejected on attempt 3")
                .paymentAttempts(3)  // All attempts failed
                .totalAmount(new BigDecimal("24.97"))
                .items(orderResponse.getItems())
                .transactionId("test-transaction-id")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(orderService.createOrder(any(CreateOrderRequestDTO.class))).thenReturn(rejectedOrder);

        // Act & Assert
        mockMvc.perform(post("/orders")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validOrderRequest)))
                .andExpect(status().isCreated())  // Order is created even if payment fails
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Payment rejected on attempt 3"))
                .andExpect(jsonPath("$.paymentAttempts").value(3));
    }

    // ==================== CREATE ORDER - VALIDATION TESTS ====================

    @Test
    void testCreateOrder_EmptyCart_ShouldReturnBadRequest() throws Exception {
        // Arrange
        when(orderService.createOrder(any(CreateOrderRequestDTO.class)))
                .thenThrow(new BusinessException(
                        "Cart is empty. Cannot create order.",
                        "CART_EMPTY",
                        400
                ));

        // Act & Assert
        mockMvc.perform(post("/orders")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validOrderRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("CART_EMPTY"))
                .andExpect(jsonPath("$.message").value("Cart is empty. Cannot create order."));
    }

    @Test
    void testCreateOrder_InvalidToken_ShouldReturnNotFound() throws Exception {
        // Arrange
        when(orderService.createOrder(any(CreateOrderRequestDTO.class)))
                .thenThrow(new BusinessException(
                        "Invalid token",
                        "INVALID_TOKEN",
                        404
                ));

        // Act & Assert
        mockMvc.perform(post("/orders")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validOrderRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"));
    }

    @Test
    void testCreateOrder_ClientNotFound_ShouldReturnNotFound() throws Exception {
        // Arrange
        when(orderService.createOrder(any(CreateOrderRequestDTO.class)))
                .thenThrow(new BusinessException(
                        "Client not found",
                        "CLIENT_NOT_FOUND",
                        404
                ));

        // Act & Assert
        mockMvc.perform(post("/orders")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validOrderRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CLIENT_NOT_FOUND"));
    }

    @Test
    void testCreateOrder_MissingUserId_ShouldProcessRequest() throws Exception {
        // Arrange
        CreateOrderRequestDTO invalidRequest = CreateOrderRequestDTO.builder()
                .clientId(null)  // Missing clientId
                .token("test-token-uuid")
                .build();

        when(orderService.createOrder(any(CreateOrderRequestDTO.class)))
                .thenThrow(new BusinessException(
                        "User ID is required",
                        "INVALID_REQUEST",
                        400
                ));

        // Act & Assert
        mockMvc.perform(post("/orders")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateOrder_MissingToken_ShouldProcessRequest() throws Exception {
        // Arrange
        CreateOrderRequestDTO invalidRequest = CreateOrderRequestDTO.builder()
                .clientId(1L)
                .token(null)  // Missing token
                .build();

        when(orderService.createOrder(any(CreateOrderRequestDTO.class)))
                .thenThrow(new BusinessException(
                        "Token is required",
                        "INVALID_REQUEST",
                        400
                ));

        // Act & Assert
        mockMvc.perform(post("/orders")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    // ==================== GET ORDER BY ID TESTS ====================

    @Test
    void testGetOrder_ExistingOrder_ShouldReturnOrder() throws Exception {
        // Arrange
        when(orderService.getOrderById(1L)).thenReturn(orderResponse);

        // Act & Assert
        mockMvc.perform(get("/orders/1")
                        .header("Authorization", "ApiKey test-api-key-12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.clientId").value(1))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.totalAmount").value(24.97));

        verify(orderService, times(1)).getOrderById(1L);
    }

    @Test
    void testGetOrder_NonExistingOrder_ShouldReturnNotFound() throws Exception {
        // Arrange
        when(orderService.getOrderById(999L))
                .thenThrow(new BusinessException(
                        "Order not found with id: 999",
                        "ORDER_NOT_FOUND",
                        404
                ));

        // Act & Assert
        mockMvc.perform(get("/orders/999")
                        .header("Authorization", "ApiKey test-api-key-12345"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ORDER_NOT_FOUND"));
    }

    @Test
    void testGetOrder_PendingOrder_ShouldReturnPendingStatus() throws Exception {
        // Arrange
        OrderResponseDTO pendingOrder = OrderResponseDTO.builder()
                .orderId(1L)
                .clientId(1L)
                .token("test-token-uuid")
                .status("PENDING")
                .rejectionReason(null)
                .paymentAttempts(0)
                .totalAmount(new BigDecimal("24.97"))
                .items(orderResponse.getItems())
                .transactionId("test-transaction-id")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(orderService.getOrderById(1L)).thenReturn(pendingOrder);

        // Act & Assert
        mockMvc.perform(get("/orders/1")
                        .header("Authorization", "ApiKey test-api-key-12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.paymentAttempts").value(0));
    }

    // ==================== API KEY AUTHENTICATION TESTS ====================

    @Test
    void testCreateOrder_MissingApiKey_ShouldReturnUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validOrderRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verify(orderService, never()).createOrder(any());
    }

    @Test
    void testCreateOrder_InvalidApiKey_ShouldReturnUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/orders")
                        .header("Authorization", "ApiKey wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validOrderRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verify(orderService, never()).createOrder(any());
    }

    @Test
    void testGetOrder_MissingApiKey_ShouldReturnUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/orders/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verify(orderService, never()).getOrderById(anyLong());
    }

    // ==================== TRANSACTION ID TRACKING TESTS ====================

    @Test
    void testCreateOrder_ShouldIncludeTransactionId() throws Exception {
        // Arrange
        when(orderService.createOrder(any(CreateOrderRequestDTO.class))).thenReturn(orderResponse);

        // Act & Assert
        mockMvc.perform(post("/orders")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").exists())
                .andExpect(jsonPath("$.transactionId").value("test-transaction-id"));
    }

    // ==================== ORDER ITEMS TESTS ====================

    @Test
    void testCreateOrder_WithMultipleItems_ShouldReturnAllItems() throws Exception {
        // Arrange
        when(orderService.createOrder(any(CreateOrderRequestDTO.class))).thenReturn(orderResponse);

        // Act & Assert
        mockMvc.perform(post("/orders")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].productName").value("Aspirin 500mg"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].unitPrice").value(5.99))
                .andExpect(jsonPath("$.items[0].subtotal").value(11.98))
                .andExpect(jsonPath("$.items[1].productName").value("Vitamin C"))
                .andExpect(jsonPath("$.items[1].quantity").value(1))
                .andExpect(jsonPath("$.items[1].unitPrice").value(12.99))
                .andExpect(jsonPath("$.items[1].subtotal").value(12.99));
    }
}
