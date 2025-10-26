package com.farmatodo.cart_service.controller;

import com.farmatodo.cart_service.dto.AddItemRequestDTO;
import com.farmatodo.cart_service.dto.CartResponseDTO;
import com.farmatodo.cart_service.dto.UpdateItemQuantityRequestDTO;
import com.farmatodo.cart_service.service.CartService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/carts")
@RequiredArgsConstructor
public class CartController {

    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    private final CartService cartService;

    @GetMapping("/{userId}")
    public ResponseEntity<CartResponseDTO> getCart(@PathVariable Long userId) {
        logger.info("Get cart endpoint called for userId: {}, transaction: {}", userId, MDC.get("transactionId"));

        CartResponseDTO response = cartService.getOrCreateCart(userId);
        logger.info("Cart retrieved for userId: {}", userId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{userId}/items")
    public ResponseEntity<CartResponseDTO> addItemToCart(
            @PathVariable Long userId,
            @RequestBody AddItemRequestDTO request) {
        logger.info("Add item to cart endpoint called for userId: {}, transaction: {}",
                userId, MDC.get("transactionId"));

        CartResponseDTO response = cartService.addItemToCart(userId, request);
        logger.info("Item added to cart for userId: {}", userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{userId}/items/{productId}")
    public ResponseEntity<CartResponseDTO> updateItemQuantity(
            @PathVariable Long userId,
            @PathVariable Long productId,
            @RequestBody UpdateItemQuantityRequestDTO request) {
        logger.info("Update item quantity endpoint called for userId: {}, productId: {}, transaction: {}",
                userId, productId, MDC.get("transactionId"));

        CartResponseDTO response = cartService.updateItemQuantity(userId, productId, request);
        logger.info("Item quantity updated for userId: {}, productId: {}", userId, productId);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}/items/{productId}")
    public ResponseEntity<CartResponseDTO> removeItemFromCart(
            @PathVariable Long userId,
            @PathVariable Long productId) {
        logger.info("Remove item from cart endpoint called for userId: {}, productId: {}, transaction: {}",
                userId, productId, MDC.get("transactionId"));

        CartResponseDTO response = cartService.removeItemFromCart(userId, productId);
        logger.info("Item removed from cart for userId: {}, productId: {}", userId, productId);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Map<String, String>> clearCart(@PathVariable Long userId) {
        logger.info("Clear cart endpoint called for userId: {}, transaction: {}", userId, MDC.get("transactionId"));

        cartService.clearCart(userId);
        logger.info("Cart cleared for userId: {}", userId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Cart cleared successfully");
        response.put("userId", userId.toString());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{userId}/checkout")
    public ResponseEntity<CartResponseDTO> checkoutCart(@PathVariable Long userId) {
        logger.info("Checkout cart endpoint called for userId: {}, transaction: {}",
                userId, MDC.get("transactionId"));

        CartResponseDTO response = cartService.checkoutCart(userId);
        logger.info("Cart checked out for userId: {}, cartId: {}", userId, response.getId());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        logger.debug("Health check endpoint called");

        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "cart-service");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        logger.debug("Info endpoint called");

        Map<String, Object> response = new HashMap<>();
        response.put("service", "cart-service");
        response.put("version", "1.0.0");
        response.put("description", "Cart management service for Farmatodo");

        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("GET /carts/{userId}", "Get or create cart for user");
        endpoints.put("POST /carts/{userId}/items", "Add item to cart");
        endpoints.put("PUT /carts/{userId}/items/{productId}", "Update item quantity");
        endpoints.put("DELETE /carts/{userId}/items/{productId}", "Remove item from cart");
        endpoints.put("DELETE /carts/{userId}", "Clear entire cart");
        endpoints.put("POST /carts/{userId}/checkout", "Checkout cart (prepare for payment)");
        endpoints.put("GET /carts/health", "Health check");
        endpoints.put("GET /carts/info", "Service information");
        endpoints.put("GET /carts/ping", "Simple ping endpoint");

        response.put("endpoints", endpoints);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }
}
