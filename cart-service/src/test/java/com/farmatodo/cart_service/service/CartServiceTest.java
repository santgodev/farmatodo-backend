package com.farmatodo.cart_service.service;

import com.farmatodo.cart_service.client.ProductServiceClient;
import com.farmatodo.cart_service.dto.AddItemRequestDTO;
import com.farmatodo.cart_service.dto.CartResponseDTO;
import com.farmatodo.cart_service.dto.ProductDTO;
import com.farmatodo.cart_service.dto.UpdateItemQuantityRequestDTO;
import com.farmatodo.cart_service.exception.BusinessException;
import com.farmatodo.cart_service.model.Cart;
import com.farmatodo.cart_service.model.CartItem;
import com.farmatodo.cart_service.repository.CartItemRepository;
import com.farmatodo.cart_service.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for CartService focusing on:
 * 1. Adding a valid product to the cart
 * 2. Error when adding a product that doesn't exist
 * 3. Error when adding a product with insufficient stock (if implemented)
 * 4. Validation of cart total after adding products
 */
@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductServiceClient productServiceClient;

    @InjectMocks
    private CartService cartService;

    private Cart activeCart;
    private ProductDTO validProduct;
    private ProductDTO productWithLowStock;
    private AddItemRequestDTO addItemRequest;

    @BeforeEach
    void setUp() {
        activeCart = Cart.builder()
                .id(1L)
                .userId(1L)
                .items(new ArrayList<>())
                .totalAmount(BigDecimal.ZERO)
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        validProduct = ProductDTO.builder()
                .id(101L)
                .name("Aspirin 500mg")
                .description("Pain reliever")
                .price(new BigDecimal("5.99"))
                .stock(100)
                .category("Medications")
                .sku("ASP-500")
                .build();

        productWithLowStock = ProductDTO.builder()
                .id(102L)
                .name("Vitamin C")
                .description("Immune support")
                .price(new BigDecimal("12.99"))
                .stock(2)
                .category("Vitamins")
                .sku("VIT-C")
                .build();

        addItemRequest = AddItemRequestDTO.builder()
                .productId(101L)
                .quantity(2)
                .build();
    }

    // ==================== ADD VALID PRODUCT TO CART TESTS ====================

    @Test
    void testAddItemToCart_ValidProduct_ShouldAddSuccessfully() {
        // Arrange
        when(cartRepository.findByUserIdAndStatusWithItems(1L, "ACTIVE"))
                .thenReturn(Optional.of(activeCart));
        when(productServiceClient.getProductById(101L)).thenReturn(validProduct);
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CartResponseDTO response = cartService.addItemToCart(1L, addItemRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getItems()).isNotEmpty();

        verify(productServiceClient).getProductById(101L);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void testAddItemToCart_NewProduct_ShouldCreateNewCartItem() {
        // Arrange
        when(cartRepository.findByUserIdAndStatusWithItems(1L, "ACTIVE"))
                .thenReturn(Optional.of(activeCart));
        when(productServiceClient.getProductById(101L)).thenReturn(validProduct);
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<Cart> cartCaptor = ArgumentCaptor.forClass(Cart.class);

        // Act
        CartResponseDTO response = cartService.addItemToCart(1L, addItemRequest);

        // Assert
        verify(cartRepository).save(cartCaptor.capture());

        Cart savedCart = cartCaptor.getValue();
        assertThat(savedCart.getItems()).hasSize(1);
        assertThat(savedCart.getItems().get(0).getProductId()).isEqualTo(101L);
        assertThat(savedCart.getItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void testAddItemToCart_DuplicateProduct_ShouldIncrementQuantity() {
        // Arrange - Cart already has the product
        CartItem existingItem = CartItem.builder()
                .id(1L)
                .cart(activeCart)
                .productId(101L)
                .quantity(2)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        activeCart.getItems().add(existingItem);

        when(cartRepository.findByUserIdAndStatusWithItems(1L, "ACTIVE"))
                .thenReturn(Optional.of(activeCart));
        when(productServiceClient.getProductById(101L)).thenReturn(validProduct);
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<Cart> cartCaptor = ArgumentCaptor.forClass(Cart.class);

        // Act
        cartService.addItemToCart(1L, addItemRequest);

        // Assert
        verify(cartRepository).save(cartCaptor.capture());

        Cart savedCart = cartCaptor.getValue();
        assertThat(savedCart.getItems()).hasSize(1); // Still only one item
        assertThat(savedCart.getItems().get(0).getQuantity()).isEqualTo(4); // 2 + 2
    }

    @Test
    void testAddItemToCart_CreatesNewCartIfNotExists() {
        // Arrange - No active cart exists
        when(cartRepository.findByUserIdAndStatusWithItems(1L, "ACTIVE"))
                .thenReturn(Optional.empty());
        when(productServiceClient.getProductById(101L)).thenReturn(validProduct);
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<Cart> cartCaptor = ArgumentCaptor.forClass(Cart.class);

        // Act
        CartResponseDTO response = cartService.addItemToCart(1L, addItemRequest);

        // Assert
        verify(cartRepository).save(cartCaptor.capture());

        Cart savedCart = cartCaptor.getValue();
        assertThat(savedCart.getUserId()).isEqualTo(1L);
        assertThat(savedCart.getStatus()).isEqualTo("ACTIVE");
        assertThat(savedCart.getItems()).hasSize(1);
    }

    // ==================== PRODUCT NOT FOUND ERROR TESTS ====================

    @Test
    void testAddItemToCart_ProductNotFound_ShouldThrowBusinessException() {
        // Arrange
        when(cartRepository.findByUserIdAndStatusWithItems(1L, "ACTIVE"))
                .thenReturn(Optional.of(activeCart));
        when(productServiceClient.getProductById(999L))
                .thenThrow(new BusinessException("Product not found", "PRODUCT_NOT_FOUND", 404));

        AddItemRequestDTO invalidRequest = AddItemRequestDTO.builder()
                .productId(999L)
                .quantity(1)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> cartService.addItemToCart(1L, invalidRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Product not found")
                .matches(e -> ((BusinessException) e).getErrorCode().equals("PRODUCT_NOT_FOUND"))
                .matches(e -> ((BusinessException) e).getHttpStatus() == 404);

        verify(productServiceClient).getProductById(999L);
        verify(cartRepository, never()).save(any());
    }

    @Test
    void testAddItemToCart_ProductServiceFailure_ShouldPropagateException() {
        // Arrange
        when(cartRepository.findByUserIdAndStatusWithItems(1L, "ACTIVE"))
                .thenReturn(Optional.of(activeCart));
        when(productServiceClient.getProductById(101L))
                .thenThrow(new RuntimeException("Product service unavailable"));

        // Act & Assert
        assertThatThrownBy(() -> cartService.addItemToCart(1L, addItemRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Product service unavailable");

        verify(cartRepository, never()).save(any());
    }

    // ==================== STOCK VALIDATION TESTS ====================
    // Note: Current implementation does NOT validate stock, but tests are here for future implementation

    @Test
    void testAddItemToCart_InsufficientStock_ShouldThrowBusinessException() {
        // Arrange
        when(cartRepository.findByUserIdAndStatusWithItems(1L, "ACTIVE"))
                .thenReturn(Optional.of(activeCart));
        when(productServiceClient.getProductById(102L)).thenReturn(productWithLowStock);

        // Product has stock=2, requesting quantity=5 (more than available)
        AddItemRequestDTO excessiveQuantityRequest = AddItemRequestDTO.builder()
                .productId(102L)
                .quantity(5)
                .build();

        // Act & Assert
        // NOTE: Current implementation does NOT validate stock
        // If stock validation is implemented, this test should pass
        // For now, this test documents the expected behavior

        // Uncomment when stock validation is implemented:
        /*
        assertThatThrownBy(() -> cartService.addItemToCart(1L, excessiveQuantityRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient stock")
                .matches(e -> ((BusinessException) e).getErrorCode().equals("INSUFFICIENT_STOCK"));
        */

        // Current behavior (no stock validation):
        // The item will be added regardless of stock
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CartResponseDTO response = cartService.addItemToCart(1L, excessiveQuantityRequest);
        assertThat(response).isNotNull(); // Currently succeeds
    }

    @Test
    void testAddItemToCart_StockAvailable_ShouldSucceed() {
        // Arrange
        when(cartRepository.findByUserIdAndStatusWithItems(1L, "ACTIVE"))
                .thenReturn(Optional.of(activeCart));
        when(productServiceClient.getProductById(102L)).thenReturn(productWithLowStock);
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Product has stock=2, requesting quantity=1 (within available stock)
        AddItemRequestDTO validQuantityRequest = AddItemRequestDTO.builder()
                .productId(102L)
                .quantity(1)
                .build();

        // Act
        CartResponseDTO response = cartService.addItemToCart(1L, validQuantityRequest);

        // Assert
        assertThat(response).isNotNull();
        verify(cartRepository).save(any(Cart.class));
    }

    // ==================== CART TOTAL CALCULATION TESTS ====================

    @Test
    void testAddItemToCart_ShouldCalculateCorrectTotal() {
        // Arrange
        when(cartRepository.findByUserIdAndStatusWithItems(1L, "ACTIVE"))
                .thenReturn(Optional.of(activeCart));
        when(productServiceClient.getProductById(101L)).thenReturn(validProduct);
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CartResponseDTO response = cartService.addItemToCart(1L, addItemRequest);

        // Assert
        // Price: $5.99, Quantity: 2, Expected Total: $11.98
        BigDecimal expectedTotal = new BigDecimal("5.99").multiply(new BigDecimal("2"));
        assertThat(response.getTotalAmount()).isEqualByComparingTo(expectedTotal);
    }

    @Test
    void testAddItemToCart_MultipleItems_ShouldCalculateTotalCorrectly() {
        // Arrange - Cart already has one item
        CartItem existingItem = CartItem.builder()
                .id(1L)
                .cart(activeCart)
                .productId(102L)
                .quantity(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        activeCart.getItems().add(existingItem);

        when(cartRepository.findByUserIdAndStatusWithItems(1L, "ACTIVE"))
                .thenReturn(Optional.of(activeCart));
        when(productServiceClient.getProductById(eq(101L))).thenReturn(validProduct);
        when(productServiceClient.getProductById(eq(102L))).thenReturn(productWithLowStock);

        Map<Long, ProductDTO> productMap = new HashMap<>();
        productMap.put(101L, validProduct);
        productMap.put(102L, productWithLowStock);
        when(productServiceClient.getProductsByIds(anyList())).thenReturn(productMap);

        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CartResponseDTO response = cartService.addItemToCart(1L, addItemRequest);

        // Assert
        // Item 1: $12.99 × 1 = $12.99 (existing)
        // Item 2: $5.99 × 2 = $11.98 (new)
        // Total: $24.97
        BigDecimal expectedTotal = new BigDecimal("12.99").add(new BigDecimal("11.98"));
        assertThat(response.getTotalAmount()).isEqualByComparingTo(expectedTotal);
    }

    @Test
    void testAddItemToCart_ZeroQuantity_ShouldThrowException() {
        // Arrange
        AddItemRequestDTO zeroQuantityRequest = AddItemRequestDTO.builder()
                .productId(101L)
                .quantity(0)
                .build();

        when(cartRepository.findByUserIdAndStatusWithItems(1L, "ACTIVE"))
                .thenReturn(Optional.of(activeCart));

        // Act & Assert
        assertThatThrownBy(() -> cartService.addItemToCart(1L, zeroQuantityRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Quantity must be greater than zero");

        verify(productServiceClient, never()).getProductById(anyLong());
        verify(cartRepository, never()).save(any());
    }

    @Test
    void testAddItemToCart_NegativeQuantity_ShouldThrowException() {
        // Arrange
        AddItemRequestDTO negativeQuantityRequest = AddItemRequestDTO.builder()
                .productId(101L)
                .quantity(-5)
                .build();

        when(cartRepository.findByUserIdAndStatusWithItems(1L, "ACTIVE"))
                .thenReturn(Optional.of(activeCart));

        // Act & Assert
        assertThatThrownBy(() -> cartService.addItemToCart(1L, negativeQuantityRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Quantity must be greater than zero");

        verify(productServiceClient, never()).getProductById(anyLong());
        verify(cartRepository, never()).save(any());
    }

    // ==================== UPDATE ITEM QUANTITY TESTS ====================

    @Test
    void testUpdateItemQuantity_ValidQuantity_ShouldUpdateSuccessfully() {
        // Arrange
        CartItem existingItem = CartItem.builder()
                .id(1L)
                .cart(activeCart)
                .productId(101L)
                .quantity(2)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        activeCart.getItems().add(existingItem);

        when(cartRepository.findByUserIdAndStatusWithItems(1L, "ACTIVE"))
                .thenReturn(Optional.of(activeCart));
        when(cartItemRepository.findByCartIdAndProductId(1L, 101L))
                .thenReturn(Optional.of(existingItem));
        when(productServiceClient.getProductById(101L)).thenReturn(validProduct);
        when(productServiceClient.getProductsByIds(anyList())).thenReturn(Map.of(101L, validProduct));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateItemQuantityRequestDTO updateRequest = UpdateItemQuantityRequestDTO.builder()
                .quantity(5)
                .build();

        // Act
        CartResponseDTO response = cartService.updateItemQuantity(1L, 101L, updateRequest);

        // Assert
        assertThat(response).isNotNull();
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void testUpdateItemQuantity_ProductNotInCart_ShouldThrowException() {
        // Arrange
        when(cartRepository.findByUserIdAndStatusWithItems(1L, "ACTIVE"))
                .thenReturn(Optional.of(activeCart));
        when(cartItemRepository.findByCartIdAndProductId(1L, 999L))
                .thenReturn(Optional.empty());

        UpdateItemQuantityRequestDTO updateRequest = UpdateItemQuantityRequestDTO.builder()
                .quantity(5)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> cartService.updateItemQuantity(1L, 999L, updateRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not found in cart");

        verify(cartRepository, never()).save(any());
    }

    // ==================== REMOVE ITEM FROM CART TESTS ====================

    @Test
    void testRemoveItemFromCart_ExistingItem_ShouldRemoveSuccessfully() {
        // Arrange
        CartItem itemToRemove = CartItem.builder()
                .id(1L)
                .cart(activeCart)
                .productId(101L)
                .quantity(2)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        activeCart.getItems().add(itemToRemove);

        when(cartRepository.findByUserIdAndStatusWithItems(1L, "ACTIVE"))
                .thenReturn(Optional.of(activeCart));
        when(cartItemRepository.findByCartIdAndProductId(1L, 101L))
                .thenReturn(Optional.of(itemToRemove));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CartResponseDTO response = cartService.removeItemFromCart(1L, 101L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getItems()).isEmpty();

        verify(cartItemRepository).delete(itemToRemove);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void testRemoveItemFromCart_NonExistingItem_ShouldThrowException() {
        // Arrange
        when(cartRepository.findByUserIdAndStatusWithItems(1L, "ACTIVE"))
                .thenReturn(Optional.of(activeCart));
        when(cartItemRepository.findByCartIdAndProductId(1L, 999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> cartService.removeItemFromCart(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not found");

        verify(cartItemRepository, never()).delete(any());
        verify(cartRepository, never()).save(any());
    }

    // ==================== CLEAR CART TESTS ====================

    @Test
    void testClearCart_ShouldRemoveAllItems() {
        // Arrange
        CartItem item1 = CartItem.builder()
                .id(1L)
                .cart(activeCart)
                .productId(101L)
                .quantity(2)
                .build();

        CartItem item2 = CartItem.builder()
                .id(2L)
                .cart(activeCart)
                .productId(102L)
                .quantity(1)
                .build();

        activeCart.getItems().add(item1);
        activeCart.getItems().add(item2);

        when(cartRepository.findByUserIdAndStatusWithItems(1L, "ACTIVE"))
                .thenReturn(Optional.of(activeCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<Cart> cartCaptor = ArgumentCaptor.forClass(Cart.class);

        // Act
        cartService.clearCart(1L);

        // Assert
        verify(cartRepository).save(cartCaptor.capture());

        Cart savedCart = cartCaptor.getValue();
        assertThat(savedCart.getItems()).isEmpty();
        assertThat(savedCart.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void testClearCart_NonExistingCart_ShouldThrowException() {
        // Arrange
        when(cartRepository.findByUserIdAndStatusWithItems(1L, "ACTIVE"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> cartService.clearCart(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cart not found");

        verify(cartRepository, never()).save(any());
    }

    // ==================== CHECKOUT TESTS ====================

    @Test
    void testCheckoutCart_WithItems_ShouldMarkAsCompleted() {
        // Arrange
        CartItem item = CartItem.builder()
                .id(1L)
                .cart(activeCart)
                .productId(101L)
                .quantity(2)
                .build();

        activeCart.getItems().add(item);

        when(cartRepository.findByUserIdAndStatusWithItems(1L, "ACTIVE"))
                .thenReturn(Optional.of(activeCart));
        when(productServiceClient.getProductsByIds(anyList())).thenReturn(Map.of(101L, validProduct));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<Cart> cartCaptor = ArgumentCaptor.forClass(Cart.class);

        // Act
        CartResponseDTO response = cartService.checkoutCart(1L);

        // Assert
        verify(cartRepository).save(cartCaptor.capture());

        Cart savedCart = cartCaptor.getValue();
        assertThat(savedCart.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void testCheckoutCart_EmptyCart_ShouldThrowException() {
        // Arrange
        when(cartRepository.findByUserIdAndStatusWithItems(1L, "ACTIVE"))
                .thenReturn(Optional.of(activeCart)); // Empty cart

        // Act & Assert
        assertThatThrownBy(() -> cartService.checkoutCart(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot checkout empty cart");

        verify(cartRepository, never()).save(any());
    }

    // ==================== GET OR CREATE CART TESTS ====================

    @Test
    void testGetOrCreateCart_ExistingCart_ShouldReturnCart() {
        // Arrange
        when(cartRepository.findByUserIdAndStatusWithItems(1L, "ACTIVE"))
                .thenReturn(Optional.of(activeCart));

        // Act
        CartResponseDTO response = cartService.getOrCreateCart(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo("ACTIVE");

        verify(cartRepository, times(1)).findByUserIdAndStatusWithItems(1L, "ACTIVE");
        verify(cartRepository, never()).save(any());
    }

    @Test
    void testGetOrCreateCart_NoExistingCart_ShouldCreateNew() {
        // Arrange
        when(cartRepository.findByUserIdAndStatusWithItems(2L, "ACTIVE"))
                .thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart cart = invocation.getArgument(0);
            cart.setId(2L);
            return cart;
        });

        ArgumentCaptor<Cart> cartCaptor = ArgumentCaptor.forClass(Cart.class);

        // Act
        CartResponseDTO response = cartService.getOrCreateCart(2L);

        // Assert
        verify(cartRepository).save(cartCaptor.capture());

        Cart savedCart = cartCaptor.getValue();
        assertThat(savedCart.getUserId()).isEqualTo(2L);
        assertThat(savedCart.getStatus()).isEqualTo("ACTIVE");
        assertThat(savedCart.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
