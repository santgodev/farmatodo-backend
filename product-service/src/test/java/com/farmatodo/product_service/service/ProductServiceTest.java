package com.farmatodo.product_service.service;

import com.farmatodo.product_service.dto.ProductDTO;
import com.farmatodo.product_service.dto.ProductRequestDTO;
import com.farmatodo.product_service.dto.ProductSearchResponseDTO;
import com.farmatodo.product_service.event.SearchEvent;
import com.farmatodo.product_service.event.SearchEventPublisher;
import com.farmatodo.product_service.model.Product;
import com.farmatodo.product_service.repository.ProductRepository;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for ProductService focusing on:
 * 1. Successful product search by name, description, or category
 * 2. Stock filtering with configurable limit
 * 3. Asynchronous logging of search queries
 * 4. Handling of empty or null queries
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SearchEventPublisher searchEventPublisher;

    @InjectMocks
    private ProductService productService;

    private Product productWithHighStock;
    private Product productWithLowStock;
    private Product productInactive;

    @BeforeEach
    void setUp() {
        // Configure minStock via reflection since we can't use @Value in tests
        setMinStock(10);

        productWithHighStock = Product.builder()
                .id(1L)
                .name("Aspirin 500mg")
                .description("Pain reliever and fever reducer")
                .price(new BigDecimal("5.99"))
                .stock(100)
                .category("Medications")
                .sku("ASP-500")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        productWithLowStock = Product.builder()
                .id(2L)
                .name("Vitamin C")
                .description("Immune system support")
                .price(new BigDecimal("12.99"))
                .stock(5)
                .category("Vitamins")
                .sku("VIT-C-100")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        productInactive = Product.builder()
                .id(3L)
                .name("Discontinued Medicine")
                .description("No longer available")
                .price(new BigDecimal("9.99"))
                .stock(50)
                .category("Medications")
                .sku("DISC-001")
                .status("INACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // Helper method to set minStock field via reflection
    private void setMinStock(int minStock) {
        try {
            var field = ProductService.class.getDeclaredField("minStock");
            field.setAccessible(true);
            field.set(productService, minStock);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set minStock", e);
        }
    }

    // ==================== SEARCH BY NAME TESTS ====================

    @Test
    void testSearchProducts_ByName_ShouldReturnMatchingProducts() {
        // Arrange
        String query = "aspirin";
        String userIdentifier = "192.168.1.1";
        List<Product> products = Arrays.asList(productWithHighStock);

        when(productRepository.searchProducts(eq(query), eq(10))).thenReturn(products);

        MDC.put("transactionId", "test-txn-123");

        // Act
        ProductSearchResponseDTO response = productService.searchProducts(query, userIdentifier);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getQuery()).isEqualTo(query);
        assertThat(response.getTotalResults()).isEqualTo(1);
        assertThat(response.getProducts()).hasSize(1);
        assertThat(response.getProducts().get(0).getName()).isEqualTo("Aspirin 500mg");

        verify(productRepository).searchProducts(query, 10);

        MDC.clear();
    }

    @Test
    void testSearchProducts_ByDescription_ShouldReturnMatchingProducts() {
        // Arrange
        String query = "pain reliever";
        String userIdentifier = "10.0.0.1";
        List<Product> products = Arrays.asList(productWithHighStock);

        when(productRepository.searchProducts(eq(query), eq(10))).thenReturn(products);

        // Act
        ProductSearchResponseDTO response = productService.searchProducts(query, userIdentifier);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getQuery()).isEqualTo(query);
        assertThat(response.getTotalResults()).isEqualTo(1);
        assertThat(response.getProducts()).hasSize(1);
        assertThat(response.getProducts().get(0).getDescription()).contains("Pain reliever");

        verify(productRepository).searchProducts(query, 10);
    }

    @Test
    void testSearchProducts_ByCategory_ShouldReturnMatchingProducts() {
        // Arrange
        String query = "vitamins";
        String userIdentifier = "127.0.0.1";
        List<Product> products = Arrays.asList(productWithLowStock);

        when(productRepository.searchProducts(eq(query), eq(10))).thenReturn(products);

        // Act
        ProductSearchResponseDTO response = productService.searchProducts(query, userIdentifier);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getQuery()).isEqualTo(query);
        assertThat(response.getTotalResults()).isEqualTo(1);
        assertThat(response.getProducts()).hasSize(1);
        assertThat(response.getProducts().get(0).getCategory()).isEqualTo("Vitamins");

        verify(productRepository).searchProducts(query, 10);
    }

    @Test
    void testSearchProducts_MultipleMatches_ShouldReturnAllMatching() {
        // Arrange
        String query = "medicine";
        String userIdentifier = "192.168.1.1";
        List<Product> products = Arrays.asList(productWithHighStock, productWithLowStock);

        when(productRepository.searchProducts(eq(query), eq(10))).thenReturn(products);

        // Act
        ProductSearchResponseDTO response = productService.searchProducts(query, userIdentifier);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getQuery()).isEqualTo(query);
        assertThat(response.getTotalResults()).isEqualTo(2);
        assertThat(response.getProducts()).hasSize(2);

        verify(productRepository).searchProducts(query, 10);
    }

    // ==================== STOCK FILTERING TESTS ====================

    @Test
    void testSearchProducts_OnlyReturnsProductsWithStockGreaterThanConfigurableLimit() {
        // Arrange
        String query = "test";
        String userIdentifier = "192.168.1.1";

        // Repository should only return products with stock > minStock (10)
        // productWithHighStock has stock=100, productWithLowStock has stock=5
        List<Product> productsAboveMinStock = Arrays.asList(productWithHighStock);

        when(productRepository.searchProducts(eq(query), eq(10))).thenReturn(productsAboveMinStock);

        // Act
        ProductSearchResponseDTO response = productService.searchProducts(query, userIdentifier);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getTotalResults()).isEqualTo(1);
        assertThat(response.getProducts()).hasSize(1);
        assertThat(response.getProducts().get(0).getStock()).isGreaterThan(10);

        // Verify repository was called with correct minStock parameter
        verify(productRepository).searchProducts(query, 10);
    }

    @Test
    void testSearchProducts_WithDifferentMinStockConfiguration_FiltersCorrectly() {
        // Arrange - Change minStock to 50
        setMinStock(50);

        String query = "test";
        String userIdentifier = "192.168.1.1";

        // Only products with stock > 50 should be returned
        // productWithHighStock has stock=100 (should be returned)
        // productWithLowStock has stock=5 (should NOT be returned)
        List<Product> productsAboveMinStock = Arrays.asList(productWithHighStock);

        when(productRepository.searchProducts(eq(query), eq(50))).thenReturn(productsAboveMinStock);

        // Act
        ProductSearchResponseDTO response = productService.searchProducts(query, userIdentifier);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getTotalResults()).isEqualTo(1);
        assertThat(response.getProducts()).hasSize(1);
        assertThat(response.getProducts().get(0).getStock()).isGreaterThan(50);

        // Verify repository was called with minStock=50
        verify(productRepository).searchProducts(query, 50);
    }

    @Test
    void testSearchProducts_WithMinStockZero_ReturnsAllActiveProducts() {
        // Arrange - Set minStock to 0 (default configuration)
        setMinStock(0);

        String query = "test";
        String userIdentifier = "192.168.1.1";

        // All products with stock > 0 should be returned
        List<Product> allProducts = Arrays.asList(productWithHighStock, productWithLowStock);

        when(productRepository.searchProducts(eq(query), eq(0))).thenReturn(allProducts);

        // Act
        ProductSearchResponseDTO response = productService.searchProducts(query, userIdentifier);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getTotalResults()).isEqualTo(2);
        assertThat(response.getProducts()).hasSize(2);

        verify(productRepository).searchProducts(query, 0);
    }

    // ==================== LOW STOCK FUNCTIONALITY TESTS ====================

    @Test
    void testGetProductsWithLowStock_ShouldReturnProductsBelowThreshold() {
        // Arrange
        int maxStock = 10;
        List<Product> lowStockProducts = Arrays.asList(productWithLowStock);

        when(productRepository.findProductsWithLowStock(maxStock)).thenReturn(lowStockProducts);

        // Act
        ProductSearchResponseDTO response = productService.getProductsWithLowStock(maxStock);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getQuery()).isEqualTo("stock < 10");
        assertThat(response.getTotalResults()).isEqualTo(1);
        assertThat(response.getProducts()).hasSize(1);
        assertThat(response.getProducts().get(0).getStock()).isLessThan(maxStock);

        verify(productRepository).findProductsWithLowStock(maxStock);
    }

    @Test
    void testGetProductsWithLowStock_NoProductsBelowThreshold_ShouldReturnEmptyList() {
        // Arrange
        int maxStock = 1;

        when(productRepository.findProductsWithLowStock(maxStock)).thenReturn(Collections.emptyList());

        // Act
        ProductSearchResponseDTO response = productService.getProductsWithLowStock(maxStock);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getQuery()).isEqualTo("stock < 1");
        assertThat(response.getTotalResults()).isEqualTo(0);
        assertThat(response.getProducts()).isEmpty();

        verify(productRepository).findProductsWithLowStock(maxStock);
    }

    // ==================== ASYNC SEARCH LOGGING TESTS ====================

    @Test
    void testSearchProducts_ShouldPublishSearchEventAsynchronously() {
        // Arrange
        String query = "aspirin";
        String userIdentifier = "192.168.1.1";
        String transactionId = "test-txn-456";

        List<Product> products = Arrays.asList(productWithHighStock);

        when(productRepository.searchProducts(eq(query), eq(10))).thenReturn(products);

        MDC.put("transactionId", transactionId);

        ArgumentCaptor<SearchEvent> eventCaptor = ArgumentCaptor.forClass(SearchEvent.class);

        // Act
        ProductSearchResponseDTO response = productService.searchProducts(query, userIdentifier);

        // Assert - Verify the event was published
        verify(searchEventPublisher, times(1)).publishEvent(eventCaptor.capture());

        SearchEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent).isNotNull();
        assertThat(capturedEvent.getSearchTerm()).isEqualTo(query);
        assertThat(capturedEvent.getResultsCount()).isEqualTo(1);
        assertThat(capturedEvent.getUserIdentifier()).isEqualTo(userIdentifier);
        assertThat(capturedEvent.getTransactionId()).isEqualTo(transactionId);
        assertThat(capturedEvent.getSearchedAt()).isNotNull();

        // Verify response is returned immediately (non-blocking)
        assertThat(response).isNotNull();
        assertThat(response.getTotalResults()).isEqualTo(1);

        MDC.clear();
    }

    @Test
    void testSearchProducts_AsyncLoggingFailure_ShouldNotAffectResponse() {
        // Arrange
        String query = "test";
        String userIdentifier = "192.168.1.1";
        List<Product> products = Arrays.asList(productWithHighStock);

        when(productRepository.searchProducts(eq(query), eq(10))).thenReturn(products);

        // Simulate async logging failure
        doThrow(new RuntimeException("Async logging failed"))
                .when(searchEventPublisher).publishEvent(any(SearchEvent.class));

        // Act & Assert - Should not throw exception
        ProductSearchResponseDTO response = productService.searchProducts(query, userIdentifier);

        // Response should still be successful
        assertThat(response).isNotNull();
        assertThat(response.getTotalResults()).isEqualTo(1);

        // Verify publish was attempted
        verify(searchEventPublisher).publishEvent(any(SearchEvent.class));
    }

    @Test
    void testSearchProducts_VerifySearchEventContainsCorrectData() {
        // Arrange
        String query = "medications";
        String userIdentifier = "10.0.0.50";
        String transactionId = "txn-789";

        List<Product> products = Arrays.asList(productWithHighStock, productWithLowStock);

        when(productRepository.searchProducts(eq(query), eq(10))).thenReturn(products);

        MDC.put("transactionId", transactionId);

        ArgumentCaptor<SearchEvent> eventCaptor = ArgumentCaptor.forClass(SearchEvent.class);

        // Act
        productService.searchProducts(query, userIdentifier);

        // Assert
        verify(searchEventPublisher).publishEvent(eventCaptor.capture());

        SearchEvent event = eventCaptor.getValue();
        assertThat(event.getSearchTerm()).isEqualTo(query);
        assertThat(event.getResultsCount()).isEqualTo(2);
        assertThat(event.getUserIdentifier()).isEqualTo(userIdentifier);
        assertThat(event.getTransactionId()).isEqualTo(transactionId);

        MDC.clear();
    }

    @Test
    void testSearchProducts_WithNoResults_ShouldStillPublishSearchEvent() {
        // Arrange
        String query = "nonexistent";
        String userIdentifier = "192.168.1.1";

        when(productRepository.searchProducts(eq(query), eq(10))).thenReturn(Collections.emptyList());

        ArgumentCaptor<SearchEvent> eventCaptor = ArgumentCaptor.forClass(SearchEvent.class);

        // Act
        ProductSearchResponseDTO response = productService.searchProducts(query, userIdentifier);

        // Assert
        assertThat(response.getTotalResults()).isEqualTo(0);

        // Verify event was published even for zero results
        verify(searchEventPublisher).publishEvent(eventCaptor.capture());

        SearchEvent event = eventCaptor.getValue();
        assertThat(event.getSearchTerm()).isEqualTo(query);
        assertThat(event.getResultsCount()).isEqualTo(0);
    }

    // ==================== EMPTY AND NULL QUERY HANDLING TESTS ====================

    @Test
    void testSearchProducts_WithEmptyQuery_ShouldReturnAllProducts() {
        // Arrange
        String emptyQuery = "";
        String userIdentifier = "192.168.1.1";
        List<Product> allProducts = Arrays.asList(productWithHighStock, productWithLowStock);

        when(productRepository.searchProducts(eq(emptyQuery), eq(10))).thenReturn(allProducts);

        // Act
        ProductSearchResponseDTO response = productService.searchProducts(emptyQuery, userIdentifier);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getQuery()).isEqualTo(emptyQuery);
        assertThat(response.getTotalResults()).isEqualTo(2);
        assertThat(response.getProducts()).hasSize(2);

        verify(productRepository).searchProducts(emptyQuery, 10);
        verify(searchEventPublisher).publishEvent(any(SearchEvent.class));
    }

    @Test
    void testSearchProducts_WithNullQuery_ShouldHandleGracefully() {
        // Arrange
        String nullQuery = null;
        String userIdentifier = "192.168.1.1";

        // Simulate repository handling null query
        when(productRepository.searchProducts(isNull(), eq(10))).thenReturn(Collections.emptyList());

        // Act
        ProductSearchResponseDTO response = productService.searchProducts(nullQuery, userIdentifier);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getQuery()).isNull();
        assertThat(response.getTotalResults()).isEqualTo(0);

        verify(productRepository).searchProducts(null, 10);
    }

    @Test
    void testSearchProducts_WithWhitespaceQuery_ShouldTrimAndSearch() {
        // Arrange
        String whitespaceQuery = "  aspirin  ";
        String userIdentifier = "192.168.1.1";
        List<Product> products = Arrays.asList(productWithHighStock);

        // Note: The service should pass the query as-is to repository
        // Repository handles trimming via SQL LIKE
        when(productRepository.searchProducts(eq(whitespaceQuery), eq(10))).thenReturn(products);

        // Act
        ProductSearchResponseDTO response = productService.searchProducts(whitespaceQuery, userIdentifier);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getTotalResults()).isEqualTo(1);

        verify(productRepository).searchProducts(whitespaceQuery, 10);
    }

    // ==================== CREATE PRODUCT TESTS ====================

    @Test
    void testCreateProduct_ValidData_ShouldReturnCreatedProduct() {
        // Arrange
        ProductRequestDTO request = ProductRequestDTO.builder()
                .name("New Medicine")
                .description("Test medicine")
                .price(new BigDecimal("15.99"))
                .stock(50)
                .category("Medications")
                .sku("MED-001")
                .build();

        Product savedProduct = Product.builder()
                .id(10L)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .category(request.getCategory())
                .sku(request.getSku())
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        // Act
        ProductDTO result = productService.createProduct(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getName()).isEqualTo("New Medicine");
        assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("15.99"));

        verify(productRepository).save(any(Product.class));
    }

    @Test
    void testCreateProduct_WithNullName_ShouldThrowException() {
        // Arrange
        ProductRequestDTO request = ProductRequestDTO.builder()
                .name(null)
                .description("Test")
                .price(new BigDecimal("10.00"))
                .stock(10)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product name is required");

        verify(productRepository, never()).save(any());
    }

    @Test
    void testCreateProduct_WithNegativePrice_ShouldThrowException() {
        // Arrange
        ProductRequestDTO request = ProductRequestDTO.builder()
                .name("Test Product")
                .description("Test")
                .price(new BigDecimal("-10.00"))
                .stock(10)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product price must be positive");

        verify(productRepository, never()).save(any());
    }

    @Test
    void testCreateProduct_WithNegativeStock_ShouldThrowException() {
        // Arrange
        ProductRequestDTO request = ProductRequestDTO.builder()
                .name("Test Product")
                .description("Test")
                .price(new BigDecimal("10.00"))
                .stock(-5)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product stock cannot be negative");

        verify(productRepository, never()).save(any());
    }

    // ==================== DTO MAPPING TESTS ====================

    @Test
    void testSearchProducts_CorrectlyMapsProductEntityToDTO() {
        // Arrange
        String query = "aspirin";
        String userIdentifier = "192.168.1.1";
        List<Product> products = Arrays.asList(productWithHighStock);

        when(productRepository.searchProducts(eq(query), eq(10))).thenReturn(products);

        // Act
        ProductSearchResponseDTO response = productService.searchProducts(query, userIdentifier);

        // Assert
        assertThat(response.getProducts()).hasSize(1);

        ProductDTO dto = response.getProducts().get(0);
        assertThat(dto.getId()).isEqualTo(productWithHighStock.getId());
        assertThat(dto.getName()).isEqualTo(productWithHighStock.getName());
        assertThat(dto.getDescription()).isEqualTo(productWithHighStock.getDescription());
        assertThat(dto.getPrice()).isEqualByComparingTo(productWithHighStock.getPrice());
        assertThat(dto.getStock()).isEqualTo(productWithHighStock.getStock());
        assertThat(dto.getCategory()).isEqualTo(productWithHighStock.getCategory());
        assertThat(dto.getSku()).isEqualTo(productWithHighStock.getSku());
    }
}
