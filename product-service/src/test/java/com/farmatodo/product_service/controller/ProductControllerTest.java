package com.farmatodo.product_service.controller;

import com.farmatodo.product_service.dto.ProductDTO;
import com.farmatodo.product_service.dto.ProductRequestDTO;
import com.farmatodo.product_service.dto.ProductSearchResponseDTO;
import com.farmatodo.product_service.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@TestPropertySource(properties = {
        "api.key=test-api-key-12345",
        "product.minStock=0"
})
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private ProductDTO product1;
    private ProductDTO product2;
    private ProductSearchResponseDTO searchResponse;
    private ProductRequestDTO validProductRequest;

    @BeforeEach
    void setUp() {
        product1 = ProductDTO.builder()
                .id(1L)
                .name("Aspirin 500mg")
                .description("Pain reliever and fever reducer")
                .price(new BigDecimal("5.99"))
                .stock(100)
                .category("Medications")
                .sku("ASP-500")
                .build();

        product2 = ProductDTO.builder()
                .id(2L)
                .name("Vitamin C")
                .description("Immune system support")
                .price(new BigDecimal("12.99"))
                .stock(5)  // Low stock
                .category("Vitamins")
                .sku("VIT-C-100")
                .build();

        searchResponse = ProductSearchResponseDTO.builder()
                .query("aspirin")
                .totalResults(1)
                .products(Arrays.asList(product1))
                .build();

        validProductRequest = ProductRequestDTO.builder()
                .name("New Product")
                .description("Product description")
                .price(new BigDecimal("10.00"))
                .stock(50)
                .category("Test Category")
                .sku("TEST-001")
                .build();
    }

    // ==================== PING ENDPOINT TESTS ====================

    @Test
    void testPing_ShouldReturnPong() throws Exception {
        mockMvc.perform(get("/products/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("pong"));
    }

    // ==================== SEARCH PRODUCTS - HAPPY PATH TESTS ====================

    @Test
    void testSearchProducts_WithQuery_ShouldReturnMatchingProducts() throws Exception {
        // Arrange
        when(productService.searchProducts(anyString(), anyString())).thenReturn(searchResponse);

        // Act & Assert
        mockMvc.perform(get("/products")
                        .param("query", "aspirin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("aspirin"))
                .andExpect(jsonPath("$.totalResults").value(1))
                .andExpect(jsonPath("$.products").isArray())
                .andExpect(jsonPath("$.products[0].name").value("Aspirin 500mg"))
                .andExpect(jsonPath("$.products[0].price").value(5.99))
                .andExpect(jsonPath("$.products[0].stock").value(100));

        verify(productService, times(1)).searchProducts(eq("aspirin"), anyString());
    }

    @Test
    void testSearchProducts_EmptyQuery_ShouldReturnAllProducts() throws Exception {
        // Arrange
        ProductSearchResponseDTO allProductsResponse = ProductSearchResponseDTO.builder()
                .query("")
                .totalResults(2)
                .products(Arrays.asList(product1, product2))
                .build();

        when(productService.searchProducts(eq(""), anyString())).thenReturn(allProductsResponse);

        // Act & Assert
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResults").value(2))
                .andExpect(jsonPath("$.products.length()").value(2));

        verify(productService, times(1)).searchProducts(eq(""), anyString());
    }

    @Test
    void testSearchProducts_NoMatchingProducts_ShouldReturnEmptyList() throws Exception {
        // Arrange
        ProductSearchResponseDTO emptyResponse = ProductSearchResponseDTO.builder()
                .query("nonexistent")
                .totalResults(0)
                .products(Collections.emptyList())
                .build();

        when(productService.searchProducts(eq("nonexistent"), anyString())).thenReturn(emptyResponse);

        // Act & Assert
        mockMvc.perform(get("/products")
                        .param("query", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResults").value(0))
                .andExpect(jsonPath("$.products").isEmpty());
    }

    @Test
    void testSearchProducts_ExtractsUserIdentifierFromXForwardedFor() throws Exception {
        // Arrange
        when(productService.searchProducts(anyString(), eq("192.168.1.1"))).thenReturn(searchResponse);

        // Act & Assert
        mockMvc.perform(get("/products")
                        .param("query", "aspirin")
                        .header("X-Forwarded-For", "192.168.1.1, 10.0.0.1"))
                .andExpect(status().isOk());

        verify(productService, times(1)).searchProducts(anyString(), eq("192.168.1.1"));
    }

    @Test
    void testSearchProducts_UsesRemoteAddrWhenNoXForwardedFor() throws Exception {
        // Arrange
        when(productService.searchProducts(anyString(), anyString())).thenReturn(searchResponse);

        // Act & Assert
        mockMvc.perform(get("/products")
                        .param("query", "aspirin"))
                .andExpect(status().isOk());

        verify(productService, times(1)).searchProducts(anyString(), anyString());
    }

    // ==================== LOW STOCK ENDPOINT TESTS ====================

    @Test
    void testGetProductsWithLowStock_ValidThreshold_ShouldReturnLowStockProducts() throws Exception {
        // Arrange
        ProductSearchResponseDTO lowStockResponse = ProductSearchResponseDTO.builder()
                .query("stock < 10")
                .totalResults(1)
                .products(Arrays.asList(product2))  // Only product2 has low stock
                .build();

        when(productService.getProductsWithLowStock(10)).thenReturn(lowStockResponse);

        // Act & Assert
        mockMvc.perform(get("/products/low-stock")
                        .param("maxStock", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("stock < 10"))
                .andExpect(jsonPath("$.totalResults").value(1))
                .andExpect(jsonPath("$.products[0].name").value("Vitamin C"))
                .andExpect(jsonPath("$.products[0].stock").value(5));

        verify(productService, times(1)).getProductsWithLowStock(10);
    }

    @Test
    void testGetProductsWithLowStock_NoLowStockProducts_ShouldReturnEmptyList() throws Exception {
        // Arrange
        ProductSearchResponseDTO emptyResponse = ProductSearchResponseDTO.builder()
                .query("stock < 1")
                .totalResults(0)
                .products(Collections.emptyList())
                .build();

        when(productService.getProductsWithLowStock(1)).thenReturn(emptyResponse);

        // Act & Assert
        mockMvc.perform(get("/products/low-stock")
                        .param("maxStock", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResults").value(0))
                .andExpect(jsonPath("$.products").isEmpty());
    }

    @Test
    void testGetProductsWithLowStock_MissingParameter_ShouldReturnBadRequest() throws Exception {
        // Act & Assert - maxStock is required
        mockMvc.perform(get("/products/low-stock"))
                .andExpect(status().isBadRequest());

        verify(productService, never()).getProductsWithLowStock(anyInt());
    }

    // ==================== CREATE PRODUCT TESTS ====================

    @Test
    void testCreateProduct_ValidData_ShouldReturnCreated() throws Exception {
        // Arrange
        ProductDTO createdProduct = ProductDTO.builder()
                .id(3L)
                .name("New Product")
                .description("Product description")
                .price(new BigDecimal("10.00"))
                .stock(50)
                .category("Test Category")
                .sku("TEST-001")
                .build();

        when(productService.createProduct(any(ProductRequestDTO.class))).thenReturn(createdProduct);

        // Act & Assert
        mockMvc.perform(post("/products")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validProductRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("New Product"))
                .andExpect(jsonPath("$.price").value(10.00))
                .andExpect(jsonPath("$.stock").value(50));

        verify(productService, times(1)).createProduct(any(ProductRequestDTO.class));
    }

    @Test
    void testCreateProduct_InvalidData_ShouldReturnBadRequest() throws Exception {
        // Arrange
        when(productService.createProduct(any(ProductRequestDTO.class)))
                .thenThrow(new IllegalArgumentException("Product name is required"));

        // Act & Assert
        mockMvc.perform(post("/products")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validProductRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateProduct_MissingApiKey_ShouldReturnUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validProductRequest)))
                .andExpect(status().isUnauthorized());

        verify(productService, never()).createProduct(any());
    }

    // ==================== GET PRODUCT BY ID TESTS ====================

    @Test
    void testGetProductById_ExistingProduct_ShouldReturnProduct() throws Exception {
        // Arrange
        when(productService.getProductById(1L)).thenReturn(product1);

        // Act & Assert
        mockMvc.perform(get("/products/1")
                        .header("Authorization", "ApiKey test-api-key-12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Aspirin 500mg"));

        verify(productService, times(1)).getProductById(1L);
    }

    @Test
    void testGetProductById_NonExistingProduct_ShouldReturnNotFound() throws Exception {
        // Arrange
        when(productService.getProductById(999L))
                .thenThrow(new RuntimeException("Product not found with ID: 999"));

        // Act & Assert
        mockMvc.perform(get("/products/999")
                        .header("Authorization", "ApiKey test-api-key-12345"))
                .andExpect(status().isInternalServerError());
    }

    // ==================== GET ALL PRODUCTS TESTS ====================

    @Test
    void testGetAllProducts_ShouldReturnAllProducts() throws Exception {
        // Arrange
        List<ProductDTO> allProducts = Arrays.asList(product1, product2);
        when(productService.getAllProducts()).thenReturn(allProducts);

        // Act & Assert
        mockMvc.perform(get("/products/all")
                        .header("Authorization", "ApiKey test-api-key-12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Aspirin 500mg"))
                .andExpect(jsonPath("$[1].name").value("Vitamin C"));

        verify(productService, times(1)).getAllProducts();
    }

    @Test
    void testGetAllProducts_EmptyDatabase_ShouldReturnEmptyArray() throws Exception {
        // Arrange
        when(productService.getAllProducts()).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/products/all")
                        .header("Authorization", "ApiKey test-api-key-12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ==================== GET PRODUCTS BY IDS TESTS ====================

    @Test
    void testGetProductsByIds_ValidIds_ShouldReturnProducts() throws Exception {
        // Arrange
        List<Long> ids = Arrays.asList(1L, 2L);
        List<ProductDTO> products = Arrays.asList(product1, product2);
        when(productService.getProductsByIds(ids)).thenReturn(products);

        // Act & Assert
        mockMvc.perform(post("/products/by-ids")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ids)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));

        verify(productService, times(1)).getProductsByIds(ids);
    }

    @Test
    void testGetProductsByIds_EmptyList_ShouldReturnEmptyArray() throws Exception {
        // Arrange
        List<Long> emptyIds = Collections.emptyList();
        when(productService.getProductsByIds(emptyIds)).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(post("/products/by-ids")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyIds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void testGetProductsByIds_SomeIdsNotFound_ShouldReturnFoundProducts() throws Exception {
        // Arrange
        List<Long> ids = Arrays.asList(1L, 999L);  // 999 doesn't exist
        List<ProductDTO> products = Arrays.asList(product1);  // Only product1 found
        when(productService.getProductsByIds(ids)).thenReturn(products);

        // Act & Assert
        mockMvc.perform(post("/products/by-ids")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ids)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1));
    }
}
