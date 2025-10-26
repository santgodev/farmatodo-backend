package com.farmatodo.order_service.client;

import com.farmatodo.order_service.dto.ProductDTO;
import com.farmatodo.order_service.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceClient.class);

    private final RestTemplate restTemplate;

    @Value("${services.product.url}")
    private String productServiceUrl;

    @Value("${services.product.apiKey}")
    private String apiKey;

    public ProductDTO getProductById(Long productId) {
        try {
            String url = productServiceUrl + "/products/" + productId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "ApiKey " + apiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            logger.info("Fetching product data from product-service for productId: {}", productId);

            ResponseEntity<ProductDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    ProductDTO.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                logger.info("Successfully fetched product data for productId: {}", productId);
                return response.getBody();
            } else {
                throw new BusinessException(
                        "Failed to fetch product data",
                        "PRODUCT_FETCH_FAILED",
                        500
                );
            }
        } catch (Exception e) {
            logger.error("Error fetching product data for productId: {}", productId, e);
            throw new BusinessException(
                    "Product service unavailable or product not found",
                    "PRODUCT_SERVICE_ERROR",
                    500
            );
        }
    }

    public List<ProductDTO> getProductsByIds(List<Long> productIds) {
        try {
            String url = productServiceUrl + "/products/by-ids";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "ApiKey " + apiKey);
            headers.set("Content-Type", "application/json");
            HttpEntity<List<Long>> entity = new HttpEntity<>(productIds, headers);

            logger.info("Fetching products data from product-service for {} productIds", productIds.size());

            ResponseEntity<List<ProductDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<List<ProductDTO>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                logger.info("Successfully fetched {} products data", response.getBody().size());
                return response.getBody();
            } else {
                throw new BusinessException(
                        "Failed to fetch products data",
                        "PRODUCTS_FETCH_FAILED",
                        500
                );
            }
        } catch (Exception e) {
            logger.error("Error fetching products data for {} IDs", productIds.size(), e);
            throw new BusinessException(
                    "Product service unavailable",
                    "PRODUCT_SERVICE_ERROR",
                    500
            );
        }
    }

    public List<ProductDTO> getAllProducts() {
        try {
            String url = productServiceUrl + "/products/all";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "ApiKey " + apiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            logger.info("Fetching all products from product-service");

            ResponseEntity<List<ProductDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<ProductDTO>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                logger.info("Successfully fetched {} products", response.getBody().size());
                return response.getBody();
            } else {
                throw new BusinessException(
                        "Failed to fetch all products",
                        "PRODUCTS_FETCH_FAILED",
                        500
                );
            }
        } catch (Exception e) {
            logger.error("Error fetching all products", e);
            throw new BusinessException(
                    "Product service unavailable",
                    "PRODUCT_SERVICE_ERROR",
                    500
            );
        }
    }
}
