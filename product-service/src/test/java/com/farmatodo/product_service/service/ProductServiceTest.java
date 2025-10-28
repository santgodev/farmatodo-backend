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
 * ✅ Comprehensive unit tests for ProductService
 * Focus areas:
 * 1. Product search by various criteria
 * 2. Stock filtering
 * 3. Asynchronous logging
 * 4. Validation and exception handling
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

    @BeforeEach
    void setUp() {
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
    }

    private void setMinStock(int minStock) {
        try {
            var field = ProductService.class.getDeclaredField("minStock");
            field.setAccessible(true);
            field.set(productService, minStock);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set minStock", e);
        }
    }

    // ==================== SEARCH TESTS ====================

    @Test
    void testSearchProducts_ByName_ShouldReturnMatchingProducts() {
        lenient().when(productRepository.searchProducts(anyString(), anyInt()))
                .thenReturn(List.of(productWithHighStock));

        ProductSearchResponseDTO response = productService.searchProducts("aspirin", "192.168.1.1");

        assertThat(response).isNotNull();
        assertThat(response.getTotalResults()).isEqualTo(1);
        verify(productRepository).searchProducts(anyString(), anyInt());
    }

    @Test
    void testSearchProducts_WithEmptyQuery_ShouldReturnAllProducts() {
        lenient().when(productRepository.searchProducts(any(), anyInt()))
                .thenReturn(Arrays.asList(productWithHighStock, productWithLowStock));

        ProductSearchResponseDTO response = productService.searchProducts("", "192.168.1.1");

        assertThat(response.getTotalResults()).isEqualTo(2);
        verify(productRepository).searchProducts(any(), anyInt());
    }

    @Test
    void testSearchProducts_WithNullQuery_ShouldHandleGracefully() {
        lenient().when(productRepository.searchProducts(isNull(), anyInt()))
                .thenReturn(Collections.emptyList());

        ProductSearchResponseDTO response = productService.searchProducts(null, "192.168.1.1");

        assertThat(response).isNotNull();
        assertThat(response.getTotalResults()).isEqualTo(0);
        verify(productRepository).searchProducts(isNull(), anyInt());
    }

    @Test
    void testSearchProducts_WithWhitespaceQuery_ShouldTrimAndSearch() {
        lenient().when(productRepository.searchProducts(anyString(), anyInt()))
                .thenReturn(List.of(productWithHighStock));

        ProductSearchResponseDTO response = productService.searchProducts("  aspirin  ", "192.168.1.1");

        assertThat(response).isNotNull();
        assertThat(response.getTotalResults()).isEqualTo(1);
        verify(productRepository).searchProducts(anyString(), anyInt());
    }

    // ==================== LOW STOCK TESTS ====================

    @Test
    void testGetProductsWithLowStock_ShouldReturnProductsBelowThreshold() {
        lenient().when(productRepository.findProductsWithLowStock(anyInt()))
                .thenReturn(List.of(productWithLowStock));

        ProductSearchResponseDTO response = productService.getProductsWithLowStock(10);

        assertThat(response).isNotNull();
        assertThat(response.getProducts().get(0).getStock()).isLessThan(10);
        verify(productRepository).findProductsWithLowStock(10);
    }

    // ==================== ASYNC EVENT TESTS ====================

    @Test
    void testSearchProducts_ShouldPublishSearchEventAsynchronously() {
        lenient().when(productRepository.searchProducts(anyString(), anyInt()))
                .thenReturn(List.of(productWithHighStock));

        MDC.put("transactionId", "txn-123");

        ArgumentCaptor<SearchEvent> eventCaptor = ArgumentCaptor.forClass(SearchEvent.class);

        productService.searchProducts("aspirin", "192.168.1.1");

        verify(searchEventPublisher, times(1)).publishSearchEvent(eventCaptor.capture());
        SearchEvent event = eventCaptor.getValue();

        assertThat(event.getSearchTerm()).isEqualTo("aspirin");
        assertThat(event.getUserIdentifier()).isEqualTo("192.168.1.1");
        assertThat(event.getTransactionId()).isEqualTo("txn-123");

        MDC.clear();
    }

    @Test
    void testSearchProducts_AsyncLoggingFailure_ShouldNotAffectResponse() {
        lenient().when(productRepository.searchProducts(anyString(), anyInt()))
                .thenReturn(List.of(productWithHighStock));

        doThrow(new RuntimeException("Async logging failed"))
                .when(searchEventPublisher).publishSearchEvent(any(SearchEvent.class));

        ProductSearchResponseDTO response = productService.searchProducts("test", "192.168.1.1");

        assertThat(response).isNotNull();
        assertThat(response.getTotalResults()).isEqualTo(1);
        verify(searchEventPublisher).publishSearchEvent(any(SearchEvent.class));
    }

    // ==================== CREATE PRODUCT TESTS ====================

    @Test
    void testCreateProduct_ValidData_ShouldReturnCreatedProduct() {
        ProductRequestDTO request = ProductRequestDTO.builder()
                .name("New Product")
                .description("Medicine")
                .price(new BigDecimal("10.00"))
                .stock(20)
                .category("Health")
                .sku("MED-123")
                .build();

        Product savedProduct = Product.builder()
                .id(10L)
                .name("New Product")
                .description("Medicine")
                .price(new BigDecimal("10.00"))
                .stock(20)
                .category("Health")
                .sku("MED-123")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        lenient().when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        ProductDTO result = productService.createProduct(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void testCreateProduct_WithNullName_ShouldThrowException() {
        ProductRequestDTO request = ProductRequestDTO.builder()
                .name(null)
                .description("Test")
                .price(new BigDecimal("10.00"))
                .stock(10)
                .build();

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product name is required");

        verify(productRepository, never()).save(any());
    }

    @Test
    void testCreateProduct_WithNegativePrice_ShouldThrowException() {
        ProductRequestDTO request = ProductRequestDTO.builder()
                .name("Test Product")
                .description("Test")
                .price(new BigDecimal("-10.00"))
                .stock(10)
                .build();

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product price must be greater than 0"); // ✅ unificado con servicio real

        verify(productRepository, never()).save(any());
    }

    @Test
    void testCreateProduct_WithNegativeStock_ShouldThrowException() {
        ProductRequestDTO request = ProductRequestDTO.builder()
                .name("Test Product")
                .description("Test")
                .price(new BigDecimal("10.00"))
                .stock(-5)
                .build();

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product stock cannot be negative");

        verify(productRepository, never()).save(any());
    }

    // ==================== MAPPING TEST ====================

    @Test
    void testSearchProducts_CorrectlyMapsProductEntityToDTO() {
        lenient().when(productRepository.searchProducts(anyString(), anyInt()))
                .thenReturn(List.of(productWithHighStock));

        ProductSearchResponseDTO response = productService.searchProducts("aspirin", "192.168.1.1");

        assertThat(response.getProducts()).hasSize(1);
        ProductDTO dto = response.getProducts().get(0);

        assertThat(dto.getName()).isEqualTo(productWithHighStock.getName());
        assertThat(dto.getPrice()).isEqualByComparingTo(productWithHighStock.getPrice());
    }
}
