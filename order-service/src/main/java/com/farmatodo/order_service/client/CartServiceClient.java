package com.farmatodo.order_service.client;

import com.farmatodo.order_service.dto.CartDTO;
import com.farmatodo.order_service.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class CartServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(CartServiceClient.class);

    private final RestTemplate restTemplate;

    @Value("${services.cartService.url}")
    private String cartServiceUrl;

    @Value("${services.cartService.apiKey}")
    private String cartServiceApiKey;

    public CartDTO getCartByUserId(Long userId) {
        String transactionId = MDC.get("transactionId");
        String url = cartServiceUrl + "/carts/" + userId;

        logger.info("Calling cart-service to get cart for userId: {} - URL: {}, TransactionId: {}",
                userId, url, transactionId);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "ApiKey " + cartServiceApiKey);
            headers.set("X-Transaction-Id", transactionId);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<CartDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    CartDTO.class
            );

            logger.info("Successfully retrieved cart for userId: {}", userId);
            return response.getBody();

        } catch (HttpClientErrorException.NotFound e) {
            logger.error("Cart not found for userId: {}", userId);
            throw new BusinessException(
                    "Cart not found for user: " + userId,
                    "CART_NOT_FOUND",
                    404
            );
        } catch (Exception e) {
            logger.error("Error calling cart-service for userId: {}", userId, e);
            throw new BusinessException(
                    "Failed to fetch cart from cart-service: " + e.getMessage(),
                    "CART_SERVICE_ERROR",
                    500
            );
        }
    }

    public CartDTO checkoutCart(Long userId) {
        String transactionId = MDC.get("transactionId");
        String url = cartServiceUrl + "/carts/" + userId + "/checkout";

        logger.info("Calling cart-service to checkout cart for userId: {} - URL: {}, TransactionId: {}",
                userId, url, transactionId);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "ApiKey " + cartServiceApiKey);
            headers.set("X-Transaction-Id", transactionId);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<CartDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    CartDTO.class
            );

            logger.info("Successfully checked out cart for userId: {}", userId);
            return response.getBody();

        } catch (HttpClientErrorException.NotFound e) {
            logger.error("Cart not found for userId: {}", userId);
            throw new BusinessException(
                    "Cart not found for user: " + userId,
                    "CART_NOT_FOUND",
                    404
            );
        } catch (Exception e) {
            logger.error("Error calling cart-service checkout for userId: {}", userId, e);
            throw new BusinessException(
                    "Failed to checkout cart from cart-service: " + e.getMessage(),
                    "CART_SERVICE_ERROR",
                    500
            );
        }
    }

    public void clearCart(Long userId) {
        String transactionId = MDC.get("transactionId");
        String url = cartServiceUrl + "/carts/" + userId;

        logger.info("Calling cart-service to clear cart for userId: {} - URL: {}, TransactionId: {}",
                userId, url, transactionId);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "ApiKey " + cartServiceApiKey);
            headers.set("X-Transaction-Id", transactionId);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    entity,
                    Void.class
            );

            logger.info("Successfully cleared cart for userId: {}", userId);

        } catch (Exception e) {
            logger.warn("Failed to clear cart for userId: {} - Error: {}", userId, e.getMessage());
            // Don't throw exception, just log warning as this is a cleanup operation
        }
    }
}
