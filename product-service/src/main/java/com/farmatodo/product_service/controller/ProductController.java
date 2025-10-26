package com.farmatodo.product_service.controller;

import com.farmatodo.product_service.dto.ProductDTO;
import com.farmatodo.product_service.dto.ProductRequestDTO;
import com.farmatodo.product_service.dto.ProductSearchResponseDTO;
import com.farmatodo.product_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    /**
     * Search products by name or description
     *
     * GET /products?query=aspirin
     *
     * @param query Search term to find in product name or description
     * @param request HTTP request to extract user identifier (IP address)
     * @return List of products matching the search criteria with stock > minStock
     */
    @GetMapping
    public ResponseEntity<ProductSearchResponseDTO> searchProducts(
            @RequestParam(required = false, defaultValue = "") String query,
            HttpServletRequest request) {

        logger.info("Product search endpoint called with query: '{}', transaction: {}",
                query, MDC.get("transactionId"));

        // Extract user identifier (could be email from auth, for now use IP)
        String userIdentifier = extractUserIdentifier(request);

        // Search products (search logging happens asynchronously)
        ProductSearchResponseDTO response = productService.searchProducts(query, userIdentifier);

        logger.info("Product search completed - Query: '{}', Results: {}, transaction: {}",
                query, response.getTotalResults(), MDC.get("transactionId"));

        return ResponseEntity.ok(response);
    }

    /**
     * Get all active products with stock less than the specified threshold
     *
     * GET /products/low-stock?maxStock=10
     *
     * @param maxStock Maximum stock level (products with stock < maxStock will be returned)
     * @return List of products with low stock, ordered by stock ascending
     */
    @GetMapping("/low-stock")
    public ResponseEntity<ProductSearchResponseDTO> getProductsWithLowStock(
            @RequestParam(required = true) int maxStock) {

        logger.info("Low stock endpoint called with maxStock: {}, transaction: {}",
                maxStock, MDC.get("transactionId"));

        ProductSearchResponseDTO response = productService.getProductsWithLowStock(maxStock);

        logger.info("Low stock query completed - maxStock: {}, Results: {}, transaction: {}",
                maxStock, response.getTotalResults(), MDC.get("transactionId"));

        return ResponseEntity.ok(response);
    }



    /**
     * Create a new product
     *
     * POST /products
     */
    @PostMapping
    public ResponseEntity<ProductDTO> createProduct(@RequestBody ProductRequestDTO request) {
        logger.info("Create product endpoint called, transaction: {}", MDC.get("transactionId"));

        ProductDTO response = productService.createProduct(request);

        logger.info("Product created with ID: {}, transaction: {}", response.getId(), MDC.get("transactionId"));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Simple ping endpoint
     *
     * GET /products/ping
     */
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }

    /**
     * Get product by ID
     *
     * GET /products/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
        logger.info("Get product by ID endpoint called for ID: {}, transaction: {}", id, MDC.get("transactionId"));

        ProductDTO response = productService.getProductById(id);

        logger.info("Product retrieved: {}, transaction: {}", response.getName(), MDC.get("transactionId"));

        return ResponseEntity.ok(response);
    }

    /**
     * Get all products
     *
     * GET /products/all
     */
    @GetMapping("/all")
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        logger.info("Get all products endpoint called, transaction: {}", MDC.get("transactionId"));

        List<ProductDTO> response = productService.getAllProducts();

        logger.info("{} products retrieved, transaction: {}", response.size(), MDC.get("transactionId"));

        return ResponseEntity.ok(response);
    }

    /**
     * Get products by IDs (for order service)
     *
     * POST /products/by-ids
     */
    @PostMapping("/by-ids")
    public ResponseEntity<List<ProductDTO>> getProductsByIds(@RequestBody List<Long> ids) {
        logger.info("Get products by IDs endpoint called with {} IDs, transaction: {}",
                ids.size(), MDC.get("transactionId"));

        List<ProductDTO> response = productService.getProductsByIds(ids);

        logger.info("{} products retrieved for {} IDs, transaction: {}",
                response.size(), ids.size(), MDC.get("transactionId"));

        return ResponseEntity.ok(response);
    }

    /**
     * Extracts user identifier from request
     * Priority: X-Forwarded-For header > Remote address
     */
    private String extractUserIdentifier(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
