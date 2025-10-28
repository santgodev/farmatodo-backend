package com.farmatodo.product_service.controller;

import com.farmatodo.product_service.dto.ProductDTO;
import com.farmatodo.product_service.dto.ProductRequestDTO;
import com.farmatodo.product_service.dto.ProductSearchResponseDTO;
import com.farmatodo.product_service.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
@AutoConfigureMockMvc(addFilters = false)
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
                .stock(5)
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

    // ==================== PING ====================

    @Test
    void testPing_ShouldReturnPong() throws Exception {
        mockMvc.perform(get("/products/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("pong"));
    }

    // ==================== SEARCH ====================

    @Test
    void testSearchProducts_WithQuery_ShouldReturnMatchingProducts() throws Exception {
        when(productService.searchProducts(anyString(), anyString())).thenReturn(searchResponse);

        mockMvc.perform(get("/products")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .param("query", "aspirin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("aspirin"))
                .andExpect(jsonPath("$.totalResults").value(1))
                .andExpect(jsonPath("$.products[0].name").value("Aspirin 500mg"))
                .andExpect(jsonPath("$.products[0].price").value(5.99));

        verify(productService).searchProducts(eq("aspirin"), anyString());
    }

    @Test
    void testSearchProducts_EmptyQuery_ShouldReturnAllProducts() throws Exception {
        ProductSearchResponseDTO allProductsResponse = ProductSearchResponseDTO.builder()
                .query("")
                .totalResults(2)
                .products(Arrays.asList(product1, product2))
                .build();

        when(productService.searchProducts(eq(""), anyString())).thenReturn(allProductsResponse);

        mockMvc.perform(get("/products")
                        .header("Authorization", "ApiKey test-api-key-12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResults").value(2));
    }

    @Test
    void testSearchProducts_NoMatchingProducts_ShouldReturnEmptyList() throws Exception {
        ProductSearchResponseDTO emptyResponse = ProductSearchResponseDTO.builder()
                .query("none")
                .totalResults(0)
                .products(Collections.emptyList())
                .build();

        when(productService.searchProducts(eq("none"), anyString())).thenReturn(emptyResponse);

        mockMvc.perform(get("/products")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .param("query", "none"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResults").value(0))
                .andExpect(jsonPath("$.products").isEmpty());
    }

    // ==================== LOW STOCK ====================

    @Test
    void testGetProductsWithLowStock_ValidThreshold_ShouldReturnLowStockProducts() throws Exception {
        ProductSearchResponseDTO lowStockResponse = ProductSearchResponseDTO.builder()
                .query("stock < 10")
                .totalResults(1)
                .products(List.of(product2))
                .build();

        when(productService.getProductsWithLowStock(10)).thenReturn(lowStockResponse);

        mockMvc.perform(get("/products/low-stock")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .param("maxStock", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResults").value(1))
                .andExpect(jsonPath("$.products[0].name").value("Vitamin C"));
    }

    @Test
    void testGetProductsWithLowStock_MissingParameter_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/products/low-stock")
                        .header("Authorization", "ApiKey test-api-key-12345"))
                .andExpect(status().isBadRequest());
    }

    // ==================== CREATE ====================

    @Test
    void testCreateProduct_ValidData_ShouldReturnCreated() throws Exception {
        ProductDTO createdProduct = ProductDTO.builder()
                .id(3L)
                .name("New Product")
                .price(new BigDecimal("10.00"))
                .stock(50)
                .build();

        when(productService.createProduct(any(ProductRequestDTO.class))).thenReturn(createdProduct);

        mockMvc.perform(post("/products")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validProductRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("New Product"));
    }

    @Test
    void testCreateProduct_InvalidData_ShouldReturnBadRequest() throws Exception {
        when(productService.createProduct(any(ProductRequestDTO.class)))
                .thenThrow(new IllegalArgumentException("Product name is required"));

        mockMvc.perform(post("/products")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validProductRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateProduct_MissingApiKey_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validProductRequest)))
                .andExpect(status().isUnauthorized());
    }

    // ==================== GET BY ID ====================

    @Test
    void testGetProductById_ShouldReturnProduct() throws Exception {
        when(productService.getProductById(1L)).thenReturn(product1);

        mockMvc.perform(get("/products/1")
                        .header("Authorization", "ApiKey test-api-key-12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Aspirin 500mg"));
    }

    // ==================== GET ALL ====================

    @Test
    void testGetAllProducts_ShouldReturnAllProducts() throws Exception {
        when(productService.getAllProducts()).thenReturn(Arrays.asList(product1, product2));

        mockMvc.perform(get("/products/all")
                        .header("Authorization", "ApiKey test-api-key-12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ==================== BY IDS ====================

    @Test
    void testGetProductsByIds_ShouldReturnProducts() throws Exception {
        List<Long> ids = Arrays.asList(1L, 2L);
        when(productService.getProductsByIds(ids)).thenReturn(List.of(product1, product2));

        mockMvc.perform(post("/products/by-ids")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ids)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
