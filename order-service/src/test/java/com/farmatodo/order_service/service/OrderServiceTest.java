package com.farmatodo.order_service.service;

import com.farmatodo.order_service.client.CartServiceClient;
import com.farmatodo.order_service.client.ClientServiceClient;
import com.farmatodo.order_service.client.TokenServiceClient;
import com.farmatodo.order_service.dto.*;
import com.farmatodo.order_service.exception.BusinessException;
import com.farmatodo.order_service.model.Order;
import com.farmatodo.order_service.model.OrderItem;
import com.farmatodo.order_service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for OrderService focusing on:
 * 1. Successful order creation with valid client and payment data
 * 2. Payment rejection when probability threshold is exceeded
 * 3. Correct mapping of order details to DTO
 * 4. Payment retry logic handling
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartServiceClient cartServiceClient;

    @Mock
    private ClientServiceClient clientServiceClient;

    @Mock
    private TokenServiceClient tokenServiceClient;

    @Mock
    private LogService logService;

    @InjectMocks
    private OrderService orderService;

    private CreateOrderRequestDTO validOrderRequest;
    private CartDTO validCart;
    private ClientDTO validClient;
    private PaymentResponseDTO approvedPayment;
    private PaymentResponseDTO rejectedPayment;

    @BeforeEach
    void setUp() {
        validOrderRequest = CreateOrderRequestDTO.builder()
                .clientId(1L)
                .token("test-token-12345")
                .build();

        CartItemDTO cartItem1 = CartItemDTO.builder()
                .id(1L)
                .productId(101L)
                .productName("Aspirin 500mg")
                .unitPrice(new BigDecimal("5.99"))
                .quantity(2)
                .subtotal(new BigDecimal("11.98"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        CartItemDTO cartItem2 = CartItemDTO.builder()
                .id(2L)
                .productId(102L)
                .productName("Vitamin C")
                .unitPrice(new BigDecimal("12.99"))
                .quantity(1)
                .subtotal(new BigDecimal("12.99"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        validCart = CartDTO.builder()
                .id(1L)
                .userId(1L)
                .items(Arrays.asList(cartItem1, cartItem2))
                .totalAmount(new BigDecimal("24.97"))
                .status("ACTIVE")
                .itemCount(2)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        validClient = ClientDTO.builder()
                .id(1L)
                .name("John Doe")
                .email("john.doe@example.com")
                .address("123 Main St, City")
                .phone("+1234567890")
                .build();

        approvedPayment = PaymentResponseDTO.builder()
                .approved(true)
                .message("Payment approved")
                .token("test-token-12345")
                .attempts(1)
                .build();

        rejectedPayment = PaymentResponseDTO.builder()
                .approved(false)
                .message("Payment rejected on attempt 3")
                .token("test-token-12345")
                .attempts(3)
                .build();

        MDC.put("transactionId", "test-txn-123");
    }

    // ==================== SUCCESSFUL ORDER CREATION TESTS ====================

    @Test
    void testCreateOrder_ValidClientAndPaymentData_ShouldCreateOrderSuccessfully() {
        // Arrange
        when(cartServiceClient.getCartByUserId(1L)).thenReturn(validCart);
        when(clientServiceClient.getClientById(1L)).thenReturn(validClient);
        when(tokenServiceClient.processPayment(any(PaymentRequestDTO.class))).thenReturn(approvedPayment);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        // Act
        OrderResponseDTO response = orderService.createOrder(validOrderRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(1L);
        assertThat(response.getClientId()).isEqualTo(1L);
        assertThat(response.getToken()).isEqualTo("test-token-12345");
        assertThat(response.getStatus()).isEqualTo("APPROVED");
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("24.97"));
        assertThat(response.getItems()).hasSize(2);

        verify(cartServiceClient).getCartByUserId(1L);
        verify(clientServiceClient).getClientById(1L);
        verify(tokenServiceClient).processPayment(any(PaymentRequestDTO.class));
        verify(orderRepository, times(3)).save(any(Order.class)); // PENDING -> PROCESSING -> APPROVED
    }

    @Test
    void testCreateOrder_ShouldFetchCartFromCartService() {
        // Arrange
        when(cartServiceClient.getCartByUserId(1L)).thenReturn(validCart);
        when(clientServiceClient.getClientById(1L)).thenReturn(validClient);
        when(tokenServiceClient.processPayment(any(PaymentRequestDTO.class))).thenReturn(approvedPayment);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        // Act
        orderService.createOrder(validOrderRequest);

        // Assert
        verify(cartServiceClient, times(1)).getCartByUserId(1L);
    }

    @Test
    void testCreateOrder_ShouldFetchClientInformation() {
        // Arrange
        when(cartServiceClient.getCartByUserId(1L)).thenReturn(validCart);
        when(clientServiceClient.getClientById(1L)).thenReturn(validClient);
        when(tokenServiceClient.processPayment(any(PaymentRequestDTO.class))).thenReturn(approvedPayment);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        // Act
        orderService.createOrder(validOrderRequest);

        // Assert
        verify(clientServiceClient, times(1)).getClientById(1L);
    }

    @Test
    void testCreateOrder_ShouldProcessPayment() {
        // Arrange
        when(cartServiceClient.getCartByUserId(1L)).thenReturn(validCart);
        when(clientServiceClient.getClientById(1L)).thenReturn(validClient);
        when(tokenServiceClient.processPayment(any(PaymentRequestDTO.class))).thenReturn(approvedPayment);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        ArgumentCaptor<PaymentRequestDTO> paymentCaptor = ArgumentCaptor.forClass(PaymentRequestDTO.class);

        // Act
        orderService.createOrder(validOrderRequest);

        // Assert
        verify(tokenServiceClient, times(1)).processPayment(paymentCaptor.capture());

        PaymentRequestDTO paymentRequest = paymentCaptor.getValue();
        assertThat(paymentRequest.getToken()).isEqualTo("test-token-12345");
        assertThat(paymentRequest.getAmount()).isEqualByComparingTo(new BigDecimal("24.97"));
        assertThat(paymentRequest.getClientId()).isEqualTo(1L);
    }

    @Test
    void testCreateOrder_ShouldClearCartAfterSuccessfulPayment() {
        // Arrange
        when(cartServiceClient.getCartByUserId(1L)).thenReturn(validCart);
        when(clientServiceClient.getClientById(1L)).thenReturn(validClient);
        when(tokenServiceClient.processPayment(any(PaymentRequestDTO.class))).thenReturn(approvedPayment);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        // Act
        orderService.createOrder(validOrderRequest);

        // Assert
        verify(cartServiceClient).clearCart(1L);
    }

    @Test
    void testCreateOrder_ShouldNotClearCartIfPaymentFails() {
        // Arrange
        when(cartServiceClient.getCartByUserId(1L)).thenReturn(validCart);
        when(clientServiceClient.getClientById(1L)).thenReturn(validClient);
        when(tokenServiceClient.processPayment(any(PaymentRequestDTO.class))).thenReturn(rejectedPayment);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        // Act
        orderService.createOrder(validOrderRequest);

        // Assert - Cart should NOT be cleared when payment is rejected
        verify(cartServiceClient, never()).clearCart(anyLong());
    }

    // ==================== PAYMENT REJECTION TESTS ====================

    @Test
    void testCreateOrder_PaymentRejected_ShouldReturnRejectedOrder() {
        // Arrange
        when(cartServiceClient.getCartByUserId(1L)).thenReturn(validCart);
        when(clientServiceClient.getClientById(1L)).thenReturn(validClient);
        when(tokenServiceClient.processPayment(any(PaymentRequestDTO.class))).thenReturn(rejectedPayment);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        // Act
        OrderResponseDTO response = orderService.createOrder(validOrderRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("REJECTED");
        assertThat(response.getRejectionReason()).isEqualTo("Payment rejected on attempt 3");
        assertThat(response.getPaymentAttempts()).isEqualTo(3);

        verify(tokenServiceClient).processPayment(any(PaymentRequestDTO.class));
    }

    @Test
    void testCreateOrder_PaymentRejectionWithRetries_ShouldTrackAttempts() {
        // Arrange
        PaymentResponseDTO rejectedAfterRetries = PaymentResponseDTO.builder()
                .approved(false)
                .message("Payment rejected after 3 attempts")
                .token("test-token-12345")
                .attempts(3)
                .build();

        when(cartServiceClient.getCartByUserId(1L)).thenReturn(validCart);
        when(clientServiceClient.getClientById(1L)).thenReturn(validClient);
        when(tokenServiceClient.processPayment(any(PaymentRequestDTO.class))).thenReturn(rejectedAfterRetries);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

        // Act
        OrderResponseDTO response = orderService.createOrder(validOrderRequest);

        // Assert
        verify(orderRepository, atLeast(2)).save(orderCaptor.capture());

        Order finalOrder = orderCaptor.getAllValues().get(orderCaptor.getAllValues().size() - 1);
        assertThat(finalOrder.getStatus()).isEqualTo("REJECTED");
        assertThat(finalOrder.getPaymentAttempts()).isEqualTo(3);
    }

    @Test
    void testCreateOrder_PaymentProbabilityExceeded_ShouldRejectPayment() {
        // Arrange - Simulating payment rejection due to probability threshold
        PaymentResponseDTO probabilityRejection = PaymentResponseDTO.builder()
                .approved(false)
                .message("Payment rejected due to risk assessment")
                .token("test-token-12345")
                .attempts(1)
                .build();

        when(cartServiceClient.getCartByUserId(1L)).thenReturn(validCart);
        when(clientServiceClient.getClientById(1L)).thenReturn(validClient);
        when(tokenServiceClient.processPayment(any(PaymentRequestDTO.class))).thenReturn(probabilityRejection);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        // Act
        OrderResponseDTO response = orderService.createOrder(validOrderRequest);

        // Assert
        assertThat(response.getStatus()).isEqualTo("REJECTED");
        assertThat(response.getRejectionReason()).contains("rejected");
    }

    // ==================== DTO MAPPING TESTS ====================

    @Test
    void testCreateOrder_CorrectDTOMapping_AllFieldsMapped() {
        // Arrange
        when(cartServiceClient.getCartByUserId(1L)).thenReturn(validCart);
        when(clientServiceClient.getClientById(1L)).thenReturn(validClient);
        when(tokenServiceClient.processPayment(any(PaymentRequestDTO.class))).thenReturn(approvedPayment);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            return order;
        });

        // Act
        OrderResponseDTO response = orderService.createOrder(validOrderRequest);

        // Assert - Verify all DTO fields are correctly mapped
        assertThat(response.getOrderId()).isNotNull();
        assertThat(response.getClientId()).isEqualTo(1L);
        assertThat(response.getToken()).isEqualTo("test-token-12345");
        assertThat(response.getStatus()).isNotNull();
        assertThat(response.getTotalAmount()).isNotNull();
        assertThat(response.getItems()).isNotEmpty();
        assertThat(response.getTransactionId()).isNotNull();
        assertThat(response.getPaymentAttempts()).isNotNull();
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    void testCreateOrder_DTOMapping_OrderItemsCorrectlyMapped() {
        // Arrange
        when(cartServiceClient.getCartByUserId(1L)).thenReturn(validCart);
        when(clientServiceClient.getClientById(1L)).thenReturn(validClient);
        when(tokenServiceClient.processPayment(any(PaymentRequestDTO.class))).thenReturn(approvedPayment);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        // Act
        OrderResponseDTO response = orderService.createOrder(validOrderRequest);

        // Assert
        assertThat(response.getItems()).hasSize(2);

        OrderItemResponseDTO item1 = response.getItems().get(0);
        assertThat(item1.getProductId()).isEqualTo(101L);
        assertThat(item1.getProductName()).isEqualTo("Aspirin 500mg");
        assertThat(item1.getUnitPrice()).isEqualByComparingTo(new BigDecimal("5.99"));
        assertThat(item1.getQuantity()).isEqualTo(2);
        assertThat(item1.getSubtotal()).isEqualByComparingTo(new BigDecimal("11.98"));

        OrderItemResponseDTO item2 = response.getItems().get(1);
        assertThat(item2.getProductId()).isEqualTo(102L);
        assertThat(item2.getProductName()).isEqualTo("Vitamin C");
    }

    @Test
    void testCreateOrder_DTOMapping_TotalAmountCorrectlyCalculated() {
        // Arrange
        when(cartServiceClient.getCartByUserId(1L)).thenReturn(validCart);
        when(clientServiceClient.getClientById(1L)).thenReturn(validClient);
        when(tokenServiceClient.processPayment(any(PaymentRequestDTO.class))).thenReturn(approvedPayment);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        // Act
        OrderResponseDTO response = orderService.createOrder(validOrderRequest);

        // Assert
        BigDecimal expectedTotal = new BigDecimal("11.98").add(new BigDecimal("12.99"));
        assertThat(response.getTotalAmount()).isEqualByComparingTo(expectedTotal);
    }

    // ==================== ERROR HANDLING TESTS ====================

    @Test
    void testCreateOrder_EmptyCart_ShouldThrowBusinessException() {
        // Arrange
        CartDTO emptyCart = CartDTO.builder()
                .id(1L)
                .userId(1L)
                .items(Collections.emptyList())
                .totalAmount(BigDecimal.ZERO)
                .status("ACTIVE")
                .itemCount(0)
                .build();

        when(cartServiceClient.getCartByUserId(1L)).thenReturn(emptyCart);

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(validOrderRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cart is empty")
                .matches(e -> ((BusinessException) e).getErrorCode().equals("CART_EMPTY"))
                .matches(e -> ((BusinessException) e).getHttpStatus() == 400);

        verify(clientServiceClient, never()).getClientById(anyLong());
        verify(tokenServiceClient, never()).processPayment(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testCreateOrder_ClientNotFound_ShouldThrowBusinessException() {
        // Arrange
        when(cartServiceClient.getCartByUserId(1L)).thenReturn(validCart);
        when(clientServiceClient.getClientById(1L))
                .thenThrow(new BusinessException("Client not found", "CLIENT_NOT_FOUND", 404));

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(validOrderRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Client not found");

        verify(tokenServiceClient, never()).processPayment(any());
    }

    @Test
    void testCreateOrder_PaymentServiceFailure_ShouldMarkOrderAsRejected() {
        // Arrange
        when(cartServiceClient.getCartByUserId(1L)).thenReturn(validCart);
        when(clientServiceClient.getClientById(1L)).thenReturn(validClient);
        when(tokenServiceClient.processPayment(any(PaymentRequestDTO.class)))
                .thenThrow(new RuntimeException("Payment service unavailable"));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

        // Act
        OrderResponseDTO response = orderService.createOrder(validOrderRequest);

        // Assert
        assertThat(response.getStatus()).isEqualTo("REJECTED");
        assertThat(response.getRejectionReason()).contains("Payment service unavailable");

        verify(orderRepository, atLeast(2)).save(orderCaptor.capture());
    }

    @Test
    void testCreateOrder_InvalidToken_ShouldThrowBusinessException() {
        // Arrange
        CreateOrderRequestDTO invalidRequest = CreateOrderRequestDTO.builder()
                .clientId(1L)
                .token(null)
                .build();

        when(cartServiceClient.getCartByUserId(1L)).thenReturn(validCart);
        when(clientServiceClient.getClientById(1L)).thenReturn(validClient);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });
        when(tokenServiceClient.processPayment(any(PaymentRequestDTO.class)))
                .thenThrow(new BusinessException("Token is required", "INVALID_TOKEN", 400));

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(invalidRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Token is required");

        verify(tokenServiceClient).processPayment(any());
    }

    // ==================== ORDER STATUS TRANSITION TESTS ====================

    @Test
    void testCreateOrder_OrderStatusTransition_PendingToProcessingToApproved() {
        // Arrange
        when(cartServiceClient.getCartByUserId(1L)).thenReturn(validCart);
        when(clientServiceClient.getClientById(1L)).thenReturn(validClient);
        when(tokenServiceClient.processPayment(any(PaymentRequestDTO.class))).thenReturn(approvedPayment);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

        // Act
        orderService.createOrder(validOrderRequest);

        // Assert
        verify(orderRepository, times(3)).save(orderCaptor.capture());

        // First save: PENDING status
        Order firstSave = orderCaptor.getAllValues().get(0);
        assertThat(firstSave.getStatus()).isEqualTo("PENDING");

        // Second save: PROCESSING status
        Order secondSave = orderCaptor.getAllValues().get(1);
        assertThat(secondSave.getStatus()).isEqualTo("PROCESSING");

        // Third save: APPROVED status
        Order thirdSave = orderCaptor.getAllValues().get(2);
        assertThat(thirdSave.getStatus()).isEqualTo("APPROVED");
    }

    @Test
    void testCreateOrder_OrderStatusTransition_PendingToProcessingToRejected() {
        // Arrange
        when(cartServiceClient.getCartByUserId(1L)).thenReturn(validCart);
        when(clientServiceClient.getClientById(1L)).thenReturn(validClient);
        when(tokenServiceClient.processPayment(any(PaymentRequestDTO.class))).thenReturn(rejectedPayment);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

        // Act
        orderService.createOrder(validOrderRequest);

        // Assert
        verify(orderRepository, times(3)).save(orderCaptor.capture());

        // First save: PENDING status
        Order firstSave = orderCaptor.getAllValues().get(0);
        assertThat(firstSave.getStatus()).isEqualTo("PENDING");

        // Second save: PROCESSING status
        Order secondSave = orderCaptor.getAllValues().get(1);
        assertThat(secondSave.getStatus()).isEqualTo("PROCESSING");

        // Third save: REJECTED status
        Order thirdSave = orderCaptor.getAllValues().get(2);
        assertThat(thirdSave.getStatus()).isEqualTo("REJECTED");
    }

    // ==================== GET ORDER BY ID TESTS ====================

    @Test
    void testGetOrderById_ExistingOrder_ShouldReturnOrder() {
        // Arrange
        Order order = Order.builder()
                .id(1L)
                .clientId(1L)
                .token("test-token")
                .items(new ArrayList<>())
                .totalAmount(new BigDecimal("24.97"))
                .status("APPROVED")
                .transactionId("test-txn-123")
                .paymentAttempts(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // Act
        OrderResponseDTO response = orderService.getOrderById(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo("APPROVED");

        verify(orderRepository).findById(1L);
    }

    @Test
    void testGetOrderById_NonExistingOrder_ShouldThrowBusinessException() {
        // Arrange
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.getOrderById(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Order not found")
                .matches(e -> ((BusinessException) e).getErrorCode().equals("ORDER_NOT_FOUND"));
    }

    // ==================== TRANSACTION TRACKING TESTS ====================

    @Test
    void testCreateOrder_ShouldIncludeTransactionId() {
        // Arrange
        when(cartServiceClient.getCartByUserId(1L)).thenReturn(validCart);
        when(clientServiceClient.getClientById(1L)).thenReturn(validClient);
        when(tokenServiceClient.processPayment(any(PaymentRequestDTO.class))).thenReturn(approvedPayment);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        // Act
        OrderResponseDTO response = orderService.createOrder(validOrderRequest);

        // Assert
        assertThat(response.getTransactionId()).isNotNull();
        assertThat(response.getTransactionId()).isEqualTo("test-txn-123");
    }
}
