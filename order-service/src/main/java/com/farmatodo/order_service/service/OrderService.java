package com.farmatodo.order_service.service;

import com.farmatodo.order_service.client.CartServiceClient;
import com.farmatodo.order_service.client.ClientServiceClient;
import com.farmatodo.order_service.client.TokenServiceClient;
import com.farmatodo.order_service.dto.*;
import com.farmatodo.order_service.exception.BusinessException;
import com.farmatodo.order_service.model.Order;
import com.farmatodo.order_service.model.OrderItem;
import com.farmatodo.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ClientServiceClient clientServiceClient;
    private final TokenServiceClient tokenServiceClient;
    private final CartServiceClient cartServiceClient;
    private final LogService logService;

    @Transactional
    public OrderResponseDTO createOrder(CreateOrderRequestDTO request) {
        String transactionId = MDC.get("transactionId");
        logger.info("Creating order - TransactionId: {}, UserId: {}", transactionId, request.getUserId());

        logService.logInfo("Order creation started",
                String.format("UserId: %d, Token: %s",
                        request.getUserId(), request.getToken()));

        // Step 1: Fetch cart from cart-service
        logger.info("Fetching cart for userId: {}", request.getUserId());
        CartDTO cart;
        try {
            cart = cartServiceClient.getCartByUserId(request.getUserId());
            logService.logInfo("Cart data fetched",
                    String.format("UserId: %d, CartId: %d, Items count: %d, TotalAmount: %s",
                            request.getUserId(), cart.getId(), cart.getItemCount(), cart.getTotalAmount()));
        } catch (Exception e) {
            logService.logError("Failed to fetch cart data",
                    String.format("UserId: %d, Error: %s", request.getUserId(), e.getMessage()));
            throw e;
        }

        // Validate cart has items
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            logService.logError("Cart is empty",
                    String.format("UserId: %d, CartId: %d", request.getUserId(), cart.getId()));
            throw new BusinessException(
                    "Cart is empty. Cannot create order.",
                    "CART_EMPTY",
                    400
            );
        }

        // Step 2: Fetch client information
        logger.info("Fetching client information for userId: {}", request.getUserId());
        ClientDTO client;
        try {
            client = clientServiceClient.getClientById(request.getUserId());
            logService.logInfo("Client data fetched",
                    String.format("ClientId: %d, Name: %s, Email: %s",
                            client.getId(), client.getName(), client.getEmail()));
        } catch (Exception e) {
            logService.logError("Failed to fetch client data",
                    String.format("UserId: %d, Error: %s", request.getUserId(), e.getMessage()));
            throw e;
        }

        // Step 3: Create order in PENDING state
        logger.info("Creating order entity");
        Order order = Order.builder()
                .clientId(request.getUserId())
                .token(request.getToken())
                .status("PENDING")
                .transactionId(transactionId)
                .paymentAttempts(0)
                .build();

        // Add items to order from cart
        for (CartItemDTO cartItem : cart.getItems()) {
            OrderItem orderItem = OrderItem.builder()
                    .productId(cartItem.getProductId())
                    .productName(cartItem.getProductName())
                    .unitPrice(cartItem.getUnitPrice())
                    .quantity(cartItem.getQuantity())
                    .build();
            order.addItem(orderItem);
        }

        // Save order
        order = orderRepository.save(order);
        logger.info("Order created with id: {}", order.getId());

        logService.logInfo("Order entity created",
                String.format("OrderId: %d, TotalAmount: %s", order.getId(), order.getTotalAmount()));

        // Step 4: Process payment
        logger.info("Processing payment for order: {}", order.getId());
        order.setStatus("PROCESSING");
        order = orderRepository.save(order);

        PaymentRequestDTO paymentRequest = PaymentRequestDTO.builder()
                .token(request.getToken())
                .amount(order.getTotalAmount())
                .orderId(order.getId())
                .clientId(request.getUserId())
                .build();

        PaymentResponseDTO paymentResponse;
        try {
            // Add client email to request context for potential notification
            paymentResponse = tokenServiceClient.processPayment(paymentRequest);

            order.setPaymentAttempts(paymentResponse.getAttempts());

            if (paymentResponse.isApproved()) {
                order.setStatus("APPROVED");
                logger.info("Payment approved for order: {} after {} attempts",
                        order.getId(), paymentResponse.getAttempts());

                logService.logInfo("Payment approved",
                        String.format("OrderId: %d, Attempts: %d", order.getId(), paymentResponse.getAttempts()));

                // Clear cart after successful payment
                try {
                    logger.info("Clearing cart for userId: {} after successful payment", request.getUserId());
                    cartServiceClient.clearCart(request.getUserId());
                    logService.logInfo("Cart cleared after payment",
                            String.format("UserId: %d, OrderId: %d", request.getUserId(), order.getId()));
                } catch (Exception ex) {
                    logger.warn("Failed to clear cart for userId: {} after payment approval - Error: {}",
                            request.getUserId(), ex.getMessage());
                    logService.logWarn("Cart clearing failed",
                            String.format("UserId: %d, OrderId: %d, Error: %s",
                                    request.getUserId(), order.getId(), ex.getMessage()));
                }
            } else {
                order.setStatus("REJECTED");
                order.setRejectionReason(paymentResponse.getMessage());
                logger.warn("Payment rejected for order: {} after {} attempts",
                        order.getId(), paymentResponse.getAttempts());

                logService.logWarn("Payment rejected",
                        String.format("OrderId: %d, Attempts: %d, Reason: %s",
                                order.getId(), paymentResponse.getAttempts(), paymentResponse.getMessage()));
            }
        } catch (Exception e) {
            order.setStatus("REJECTED");
            order.setRejectionReason("Payment service error: " + e.getMessage());
            logger.error("Payment processing error for order: {}", order.getId(), e);

            logService.logError("Payment processing error",
                    String.format("OrderId: %d, Error: %s", order.getId(), e.getMessage()));
        }

        // Save final order state
        order = orderRepository.save(order);

        logger.info("Order processing completed - OrderId: {}, Status: {}", order.getId(), order.getStatus());
        logService.logInfo("Order processing completed",
                String.format("OrderId: %d, Status: %s, FinalAmount: %s",
                        order.getId(), order.getStatus(), order.getTotalAmount()));

        return convertToDTO(order);
    }

    public OrderResponseDTO getOrderById(Long orderId) {
        String transactionId = MDC.get("transactionId");
        logger.info("Fetching order - TransactionId: {}, OrderId: {}", transactionId, orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    logger.warn("Order not found: {}", orderId);
                    return new BusinessException("Order not found", "ORDER_NOT_FOUND", 404);
                });

        logger.info("Order found: {} with status: {}", orderId, order.getStatus());
        return convertToDTO(order);
    }

    private OrderResponseDTO convertToDTO(Order order) {
        List<OrderItemResponseDTO> items = order.getItems().stream()
                .map(item -> OrderItemResponseDTO.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        return OrderResponseDTO.builder()
                .orderId(order.getId())
                .clientId(order.getClientId())
                .token(order.getToken())
                .items(items)
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .transactionId(order.getTransactionId())
                .rejectionReason(order.getRejectionReason())
                .paymentAttempts(order.getPaymentAttempts())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
