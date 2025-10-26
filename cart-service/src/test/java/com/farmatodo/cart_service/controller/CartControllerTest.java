package com.farmatodo.cart_service.controller;

import com.farmatodo.cart_service.dto.AddItemRequestDTO;
import com.farmatodo.cart_service.dto.CartItemDTO;
import com.farmatodo.cart_service.dto.CartResponseDTO;
import com.farmatodo.cart_service.dto.UpdateItemQuantityRequestDTO;
import com.farmatodo.cart_service.service.CartService;
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
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartController.class)
@TestPropertySource(properties = {
        "api.key=cart-service-api-key-change-in-production"
})
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartService cartService;

    private CartResponseDTO emptyCart;
    private CartResponseDTO cartWithItems;
    private AddItemRequestDTO addItemRequest;
    private UpdateItemQuantityRequestDTO updateQuantityRequest;

    @BeforeEach
    void setUp() {
        // Empty cart
        emptyCart = CartResponseDTO.builder()
                .id(1L)
                .userId(1L)
                .status("ACTIVE")
                .items(Collections.emptyList())
                .totalAmount(BigDecimal.ZERO)
                .itemCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Cart with items
        CartItemDTO item1 = CartItemDTO.builder()
                .id(1L)
                .productId(101L)
                .productName("Aspirin 500mg")
                .unitPrice(new BigDecimal("5.99"))
                .quantity(2)
                .subtotal(new BigDecimal("11.98"))
                .build();

        CartItemDTO item2 = CartItemDTO.builder()
                .id(2L)
                .productId(102L)
                .productName("Vitamin C")
                .unitPrice(new BigDecimal("12.99"))
                .quantity(1)
                .subtotal(new BigDecimal("12.99"))
                .build();

        cartWithItems = CartResponseDTO.builder()
                .id(1L)
                .userId(1L)
                .status("ACTIVE")
                .items(Arrays.asList(item1, item2))
                .totalAmount(new BigDecimal("24.97"))
                .itemCount(2)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        addItemRequest = AddItemRequestDTO.builder()
                .productId(101L)
                .quantity(2)
                .build();

        updateQuantityRequest = UpdateItemQuantityRequestDTO.builder()
                .quantity(3)
                .build();
    }

    // ==================== PING ENDPOINT TESTS ====================

    @Test
    void testPing_ShouldReturnPong() throws Exception {
        mockMvc.perform(get("/carts/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("pong"));
    }

    // ==================== HEALTH ENDPOINT TESTS ====================

    @Test
    void testHealth_ShouldReturnHealthStatus() throws Exception {
        mockMvc.perform(get("/carts/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("cart-service"));
    }

    // ==================== INFO ENDPOINT TESTS ====================

    @Test
    void testInfo_ShouldReturnServiceInfo() throws Exception {
        mockMvc.perform(get("/carts/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("cart-service"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.description").exists())
                .andExpect(jsonPath("$.endpoints").exists());
    }

    // ==================== GET CART TESTS ====================

    @Test
    void testGetCart_ExistingCart_ShouldReturnCart() throws Exception {
        // Arrange
        when(cartService.getOrCreateCart(1L)).thenReturn(cartWithItems);

        // Act & Assert
        mockMvc.perform(get("/carts/1")
                        .header("Authorization", "ApiKey cart-service-api-key-change-in-production"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.totalAmount").value(24.97));

        verify(cartService, times(1)).getOrCreateCart(1L);
    }

    @Test
    void testGetCart_NewCart_ShouldCreateAndReturnEmptyCart() throws Exception {
        // Arrange
        when(cartService.getOrCreateCart(2L)).thenReturn(emptyCart);

        // Act & Assert
        mockMvc.perform(get("/carts/2")
                        .header("Authorization", "ApiKey cart-service-api-key-change-in-production"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalAmount").value(0));

        verify(cartService, times(1)).getOrCreateCart(2L);
    }

    @Test
    void testGetCart_MissingApiKey_ShouldReturnUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/carts/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verify(cartService, never()).getOrCreateCart(anyLong());
    }

    // ==================== ADD ITEM TO CART TESTS ====================

    @Test
    void testAddItemToCart_ValidItem_ShouldReturnUpdatedCart() throws Exception {
        // Arrange
        when(cartService.addItemToCart(eq(1L), any(AddItemRequestDTO.class))).thenReturn(cartWithItems);

        // Act & Assert
        mockMvc.perform(post("/carts/1/items")
                        .header("Authorization", "ApiKey cart-service-api-key-change-in-production")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addItemRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.totalAmount").value(24.97));

        verify(cartService, times(1)).addItemToCart(eq(1L), any(AddItemRequestDTO.class));
    }

    @Test
    void testAddItemToCart_DuplicateProduct_ShouldUpdateQuantity() throws Exception {
        // Arrange - Adding same product should update quantity
        CartItemDTO updatedItem = CartItemDTO.builder()
                .id(1L)
                .productId(101L)
                .productName("Aspirin 500mg")
                .unitPrice(new BigDecimal("5.99"))
                .quantity(4)  // Updated quantity
                .subtotal(new BigDecimal("23.96"))
                .build();

        CartResponseDTO updatedCart = CartResponseDTO.builder()
                .id(1L)
                .userId(1L)
                .status("ACTIVE")
                .items(Arrays.asList(updatedItem))
                .totalAmount(new BigDecimal("23.96"))
                .itemCount(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(cartService.addItemToCart(eq(1L), any(AddItemRequestDTO.class))).thenReturn(updatedCart);

        // Act & Assert
        mockMvc.perform(post("/carts/1/items")
                        .header("Authorization", "ApiKey cart-service-api-key-change-in-production")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addItemRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].quantity").value(4))
                .andExpect(jsonPath("$.totalAmount").value(23.96));
    }

    @Test
    void testAddItemToCart_MissingApiKey_ShouldReturnUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/carts/1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addItemRequest)))
                .andExpect(status().isUnauthorized());

        verify(cartService, never()).addItemToCart(anyLong(), any());
    }

    // ==================== UPDATE ITEM QUANTITY TESTS ====================

    @Test
    void testUpdateItemQuantity_ValidQuantity_ShouldReturnUpdatedCart() throws Exception {
        // Arrange
        when(cartService.updateItemQuantity(eq(1L), eq(101L), any(UpdateItemQuantityRequestDTO.class)))
                .thenReturn(cartWithItems);

        // Act & Assert
        mockMvc.perform(put("/carts/1/items/101")
                        .header("Authorization", "ApiKey cart-service-api-key-change-in-production")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateQuantityRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.items").isArray());

        verify(cartService, times(1)).updateItemQuantity(eq(1L), eq(101L), any(UpdateItemQuantityRequestDTO.class));
    }

    @Test
    void testUpdateItemQuantity_ProductNotInCart_ShouldReturnError() throws Exception {
        // Arrange
        when(cartService.updateItemQuantity(eq(1L), eq(999L), any(UpdateItemQuantityRequestDTO.class)))
                .thenThrow(new RuntimeException("Product not found in cart"));

        // Act & Assert
        mockMvc.perform(put("/carts/1/items/999")
                        .header("Authorization", "ApiKey cart-service-api-key-change-in-production")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateQuantityRequest)))
                .andExpect(status().isInternalServerError());
    }

    // ==================== REMOVE ITEM FROM CART TESTS ====================

    @Test
    void testRemoveItemFromCart_ExistingItem_ShouldReturnUpdatedCart() throws Exception {
        // Arrange - Cart after removing one item
        CartItemDTO remainingItem = CartItemDTO.builder()
                .id(2L)
                .productId(102L)
                .productName("Vitamin C")
                .unitPrice(new BigDecimal("12.99"))
                .quantity(1)
                .subtotal(new BigDecimal("12.99"))
                .build();

        CartResponseDTO updatedCart = CartResponseDTO.builder()
                .id(1L)
                .userId(1L)
                .status("ACTIVE")
                .items(Arrays.asList(remainingItem))
                .totalAmount(new BigDecimal("12.99"))
                .itemCount(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(cartService.removeItemFromCart(1L, 101L)).thenReturn(updatedCart);

        // Act & Assert
        mockMvc.perform(delete("/carts/1/items/101")
                        .header("Authorization", "ApiKey cart-service-api-key-change-in-production"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.totalAmount").value(12.99));

        verify(cartService, times(1)).removeItemFromCart(1L, 101L);
    }

    @Test
    void testRemoveItemFromCart_NonExistingItem_ShouldReturnError() throws Exception {
        // Arrange
        when(cartService.removeItemFromCart(1L, 999L))
                .thenThrow(new RuntimeException("Product not found in cart"));

        // Act & Assert
        mockMvc.perform(delete("/carts/1/items/999")
                        .header("Authorization", "ApiKey cart-service-api-key-change-in-production"))
                .andExpect(status().isInternalServerError());
    }

    // ==================== CLEAR CART TESTS ====================

    @Test
    void testClearCart_ExistingCart_ShouldReturnSuccessMessage() throws Exception {
        // Arrange
        doNothing().when(cartService).clearCart(1L);

        // Act & Assert
        mockMvc.perform(delete("/carts/1")
                        .header("Authorization", "ApiKey cart-service-api-key-change-in-production"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cart cleared successfully"))
                .andExpect(jsonPath("$.userId").value("1"));

        verify(cartService, times(1)).clearCart(1L);
    }

    @Test
    void testClearCart_NonExistingCart_ShouldReturnError() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Cart not found"))
                .when(cartService).clearCart(999L);

        // Act & Assert
        mockMvc.perform(delete("/carts/999")
                        .header("Authorization", "ApiKey cart-service-api-key-change-in-production"))
                .andExpect(status().isInternalServerError());
    }

    // ==================== CHECKOUT CART TESTS ====================

    @Test
    void testCheckoutCart_ValidCart_ShouldMarkAsCompleted() throws Exception {
        // Arrange - Cart with COMPLETED status after checkout
        CartResponseDTO completedCart = CartResponseDTO.builder()
                .id(1L)
                .userId(1L)
                .status("COMPLETED")
                .items(cartWithItems.getItems())
                .totalAmount(new BigDecimal("24.97"))
                .itemCount(2)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(cartService.checkoutCart(1L)).thenReturn(completedCart);

        // Act & Assert
        mockMvc.perform(post("/carts/1/checkout")
                        .header("Authorization", "ApiKey cart-service-api-key-change-in-production"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.totalAmount").value(24.97));

        verify(cartService, times(1)).checkoutCart(1L);
    }

    @Test
    void testCheckoutCart_EmptyCart_ShouldReturnError() throws Exception {
        // Arrange
        when(cartService.checkoutCart(1L))
                .thenThrow(new RuntimeException("Cannot checkout empty cart"));

        // Act & Assert
        mockMvc.perform(post("/carts/1/checkout")
                        .header("Authorization", "ApiKey cart-service-api-key-change-in-production"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testCheckoutCart_AlreadyCompleted_ShouldReturnError() throws Exception {
        // Arrange
        when(cartService.checkoutCart(1L))
                .thenThrow(new RuntimeException("Cart already completed"));

        // Act & Assert
        mockMvc.perform(post("/carts/1/checkout")
                        .header("Authorization", "ApiKey cart-service-api-key-change-in-production"))
                .andExpect(status().isInternalServerError());
    }

    // ==================== API KEY AUTHORIZATION TESTS ====================

    @Test
    void testAddItemToCart_InvalidApiKey_ShouldReturnUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/carts/1/items")
                        .header("Authorization", "ApiKey wrong-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addItemRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verify(cartService, never()).addItemToCart(anyLong(), any());
    }

    @Test
    void testUpdateItemQuantity_MissingApiKey_ShouldReturnUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/carts/1/items/101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateQuantityRequest)))
                .andExpect(status().isUnauthorized());

        verify(cartService, never()).updateItemQuantity(anyLong(), anyLong(), any());
    }
}
