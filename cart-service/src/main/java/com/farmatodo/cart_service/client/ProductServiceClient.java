package com.farmatodo.cart_service.client;

import com.farmatodo.cart_service.dto.ProductDTO;
import com.farmatodo.cart_service.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceClient.class);

    private final RestTemplate restTemplate;

    @Value("${services.product.url}")
    private String productServiceUrl;

    @Value("${services.product.apiKey}")
    private String productServiceApiKey;

    /**
     * Get product details by product ID
     */
    public ProductDTO getProductById(Long productId) {
        String transactionId = MDC.get("transactionId");
        logger.info("Fetching product details for productId: {} - transaction: {}", productId, transactionId);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "ApiKey " + productServiceApiKey);
            headers.set("X-Transaction-Id", transactionId);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = productServiceUrl + "/products/" + productId;

            ResponseEntity<ProductDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    ProductDTO.class
            );

            logger.debug("Product details fetched successfully for productId: {}", productId);
            return response.getBody();

        } catch (Exception e) {
            logger.error("Failed to fetch product details for productId: {} - Error: {}", productId, e.getMessage());
            throw new BusinessException("Product not found or unavailable", "PRODUCT_NOT_FOUND", 404);
        }
    }

    /**
     * Get multiple products by their IDs
     */
    public Map<Long, ProductDTO> getProductsByIds(List<Long> productIds) {
        String transactionId = MDC.get("transactionId");
        logger.info("Fetching {} products - transaction: {}", productIds.size(), transactionId);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "ApiKey " + productServiceApiKey);
            headers.set("X-Transaction-Id", transactionId);
            headers.set("Content-Type", "application/json");

            HttpEntity<List<Long>> entity = new HttpEntity<>(productIds, headers);

            String url = productServiceUrl + "/products/by-ids";

            ResponseEntity<List<ProductDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<List<ProductDTO>>() {}
            );

            List<ProductDTO> products = response.getBody();
            if (products == null) {
                logger.warn("No products returned from product service");
                return Map.of();
            }

            // Convert list to map for easy lookup
            Map<Long, ProductDTO> productMap = products.stream()
                    .collect(Collectors.toMap(ProductDTO::getId, product -> product));

            logger.debug("Fetched {} products successfully", productMap.size());
            return productMap;

        } catch (Exception e) {
            logger.error("Failed to fetch products - Error: {}", e.getMessage());
            throw new BusinessException("Failed to fetch product details", "PRODUCTS_FETCH_FAILED", 500);
        }
    }
}
