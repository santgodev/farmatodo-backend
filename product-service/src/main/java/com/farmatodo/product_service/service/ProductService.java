package com.farmatodo.product_service.service;

import com.farmatodo.product_service.dto.ProductDTO;
import com.farmatodo.product_service.dto.ProductRequestDTO;
import com.farmatodo.product_service.dto.ProductSearchResponseDTO;
import com.farmatodo.product_service.event.SearchEvent;
import com.farmatodo.product_service.event.SearchEventPublisher;
import com.farmatodo.product_service.model.Product;
import com.farmatodo.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final SearchEventPublisher searchEventPublisher;

    @Value("${product.minStock:0}")
    private int minStock;

    @Transactional(readOnly = true)
    public ProductSearchResponseDTO searchProducts(String query, String userIdentifier) {
        String transactionId = MDC.get("transactionId");
        logger.info("Searching products with query: '{}', minStock: {}, transaction: {}",
                query, minStock, transactionId);

        // Validate query
        if (query == null || query.trim().isEmpty()) {
            query = "";
        }

        // Search products in database
        List<Product> products = productRepository.searchProducts(query.trim(), minStock);

        logger.debug("Found {} products for query: '{}', transaction: {}",
                products.size(), query, transactionId);

        // Convert to DTOs
        List<ProductDTO> productDTOs = products.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        // Publish search event asynchronously (non-blocking)
        publishSearchEventAsync(query, products.size(), userIdentifier, transactionId);

        // Return response immediately (search logging happens in background)
        return ProductSearchResponseDTO.builder()
                .query(query.trim())
                .totalResults(products.size())
                .products(productDTOs)
                .build();
    }

    /**
     * Publishes search event asynchronously
     * This method returns immediately - actual logging happens in background thread
     */
    private void publishSearchEventAsync(String query, int resultsCount, String userIdentifier, String transactionId) {
        try {
            SearchEvent event = SearchEvent.builder()
                    .searchTerm(query.trim())
                    .resultsCount(resultsCount)
                    .userIdentifier(userIdentifier)
                    .transactionId(transactionId)
                    .searchedAt(LocalDateTime.now())
                    .build();

            searchEventPublisher.publishSearchEvent(event);

            logger.debug("Search event published for transaction: {}", transactionId);
        } catch (Exception e) {
            // Don't fail the request if event publishing fails
            logger.error("Failed to publish search event for transaction: {}. Error: {}",
                    transactionId, e.getMessage());
        }
    }

    /**
     * Get all active products with stock less than the specified threshold
     * @param maxStock Maximum stock level (products with stock < maxStock)
     * @return Response with list of low stock products
     */
    @Transactional(readOnly = true)
    public ProductSearchResponseDTO getProductsWithLowStock(int maxStock) {
        String transactionId = MDC.get("transactionId");
        logger.info("Finding products with stock < {}, transaction: {}", maxStock, transactionId);

        // Validate maxStock
        if (maxStock < 0) {
            logger.warn("Invalid maxStock value: {}. Using 0 instead.", maxStock);
            maxStock = 0;
        }

        // Query products with low stock
        List<Product> products = productRepository.findProductsWithLowStock(maxStock);

        logger.info("Found {} products with stock < {}, transaction: {}",
                products.size(), maxStock, transactionId);

        // Convert to DTOs
        List<ProductDTO> productDTOs = products.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        // Return response
        return ProductSearchResponseDTO.builder()
                .query("stock < " + maxStock)
                .totalResults(products.size())
                .products(productDTOs)
                .build();
    }



    @Transactional
    public ProductDTO createProduct(ProductRequestDTO request) {
        String transactionId = MDC.get("transactionId");
        logger.info("Creating product: {}, transaction: {}", request.getName(), transactionId);

        // Validate request
        validateProductRequest(request);

        // Create product entity
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .category(request.getCategory())
                .sku(request.getSku())
                .status("ACTIVE")
                .build();

        // Save to database
        product = productRepository.save(product);

        logger.info("Product created successfully with ID: {}, transaction: {}", product.getId(), transactionId);

        return mapToDTO(product);
    }

    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        String transactionId = MDC.get("transactionId");
        logger.info("Getting product by ID: {}, transaction: {}", id, transactionId);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Product not found with ID: {}, transaction: {}", id, transactionId);
                    return new RuntimeException("Product not found with ID: " + id);
                });

        return mapToDTO(product);
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProducts() {
        String transactionId = MDC.get("transactionId");
        logger.info("Getting all products, transaction: {}", transactionId);

        List<Product> products = productRepository.findAll();

        logger.info("Found {} products, transaction: {}", products.size(), transactionId);

        return products.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByIds(List<Long> ids) {
        String transactionId = MDC.get("transactionId");
        logger.info("Getting products by IDs: {}, transaction: {}", ids, transactionId);

        List<Product> products = productRepository.findAllById(ids);

        logger.info("Found {} products for {} IDs, transaction: {}", products.size(), ids.size(), transactionId);

        return products.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private void validateProductRequest(ProductRequestDTO request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (request.getPrice() == null || request.getPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Product price must be greater than 0");
        }
        if (request.getStock() == null || request.getStock() < 0) {
            throw new IllegalArgumentException("Product stock cannot be negative");
        }
    }

    private ProductDTO mapToDTO(Product product) {
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .category(product.getCategory())
                .sku(product.getSku())
                .build();
    }
}
