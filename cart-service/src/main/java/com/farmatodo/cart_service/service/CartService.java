package com.farmatodo.cart_service.service;

import com.farmatodo.cart_service.client.ProductServiceClient;
import com.farmatodo.cart_service.dto.AddItemRequestDTO;
import com.farmatodo.cart_service.dto.CartItemDTO;
import com.farmatodo.cart_service.dto.CartResponseDTO;
import com.farmatodo.cart_service.dto.ProductDTO;
import com.farmatodo.cart_service.dto.UpdateItemQuantityRequestDTO;
import com.farmatodo.cart_service.exception.BusinessException;
import com.farmatodo.cart_service.model.Cart;
import com.farmatodo.cart_service.model.CartItem;
import com.farmatodo.cart_service.repository.CartItemRepository;
import com.farmatodo.cart_service.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private static final Logger logger = LoggerFactory.getLogger(CartService.class);
    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String COMPLETED_STATUS = "COMPLETED";

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductServiceClient productServiceClient;

    @Transactional
    public CartResponseDTO getOrCreateCart(Long userId) {
        String transactionId = MDC.get("transactionId");
        logger.info("Getting or creating cart for userId: {} - transaction: {}", userId, transactionId);

        Cart cart = cartRepository.findByUserIdAndStatusWithItems(userId, ACTIVE_STATUS)
                .orElseGet(() -> createNewCart(userId));

        logger.debug("Cart retrieved/created with id: {} for userId: {}", cart.getId(), userId);
        return mapToCartResponse(cart);
    }

    @Transactional
    public CartResponseDTO addItemToCart(Long userId, AddItemRequestDTO request) {
        String transactionId = MDC.get("transactionId");
        logger.info("Adding item to cart for userId: {} - transaction: {}", userId, transactionId);

        // Validate request
        validateAddItemRequest(request);

        // Verify product exists by fetching from product service
        ProductDTO product = productServiceClient.getProductById(request.getProductId());

        // Get or create cart
        Cart cart = cartRepository.findByUserIdAndStatusWithItems(userId, ACTIVE_STATUS)
                .orElseGet(() -> createNewCart(userId));

        // Check if item already exists in cart
        CartItem existingItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), request.getProductId())
                .orElse(null);

        if (existingItem != null) {
            // Update quantity
            existingItem.incrementQuantity(request.getQuantity());
            cartItemRepository.save(existingItem);
            logger.debug("Updated quantity for existing item - productId: {}", request.getProductId());
        } else {
            // Create new item (only storing productId and quantity)
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .productId(request.getProductId())
                    .quantity(request.getQuantity())
                    .build();
            cart.addItem(newItem);
            cartItemRepository.save(newItem);
            logger.debug("Added new item to cart - productId: {}", request.getProductId());
        }

        cart = cartRepository.save(cart);

        logger.info("Item added successfully to cart id: {}", cart.getId());
        return mapToCartResponse(cart);
    }

    @Transactional
    public CartResponseDTO updateItemQuantity(Long userId, Long productId, UpdateItemQuantityRequestDTO request) {
        String transactionId = MDC.get("transactionId");
        logger.info("Updating item quantity for userId: {}, productId: {} - transaction: {}",
                userId, productId, transactionId);

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BusinessException("Quantity must be greater than 0", "INVALID_QUANTITY", 400);
        }

        Cart cart = cartRepository.findByUserIdAndStatusWithItems(userId, ACTIVE_STATUS)
                .orElseThrow(() -> new BusinessException("Cart not found", "CART_NOT_FOUND", 404));

        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new BusinessException("Item not found in cart", "ITEM_NOT_FOUND", 404));

        item.updateQuantity(request.getQuantity());
        cartItemRepository.save(item);

        cart = cartRepository.save(cart);

        logger.info("Item quantity updated successfully for productId: {}", productId);
        return mapToCartResponse(cart);
    }

    @Transactional
    public CartResponseDTO removeItemFromCart(Long userId, Long productId) {
        String transactionId = MDC.get("transactionId");
        logger.info("Removing item from cart for userId: {}, productId: {} - transaction: {}",
                userId, productId, transactionId);

        Cart cart = cartRepository.findByUserIdAndStatusWithItems(userId, ACTIVE_STATUS)
                .orElseThrow(() -> new BusinessException("Cart not found", "CART_NOT_FOUND", 404));

        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new BusinessException("Item not found in cart", "ITEM_NOT_FOUND", 404));

        cart.removeItem(item);
        cartItemRepository.delete(item);

        cart = cartRepository.save(cart);

        logger.info("Item removed successfully from cart - productId: {}", productId);
        return mapToCartResponse(cart);
    }

    @Transactional
    public void clearCart(Long userId) {
        String transactionId = MDC.get("transactionId");
        logger.info("Clearing cart for userId: {} - transaction: {}", userId, transactionId);

        Cart cart = cartRepository.findByUserIdAndStatusWithItems(userId, ACTIVE_STATUS)
                .orElseThrow(() -> new BusinessException("Cart not found", "CART_NOT_FOUND", 404));

        cartItemRepository.deleteByCartId(cart.getId());
        cart.clearItems();
        cartRepository.save(cart);

        logger.info("Cart cleared successfully for userId: {}", userId);
    }

    @Transactional
    public CartResponseDTO checkoutCart(Long userId) {
        String transactionId = MDC.get("transactionId");
        logger.info("Checking out cart for userId: {} - transaction: {}", userId, transactionId);

        Cart cart = cartRepository.findByUserIdAndStatusWithItems(userId, ACTIVE_STATUS)
                .orElseThrow(() -> new BusinessException("Cart not found", "CART_NOT_FOUND", 404));

        if (cart.getItems().isEmpty()) {
            throw new BusinessException("Cannot checkout empty cart", "EMPTY_CART", 400);
        }

        // Mark cart as completed - ready for payment service
        cart.setStatus(COMPLETED_STATUS);
        cart = cartRepository.save(cart);

        logger.info("Cart checked out successfully - cart id: {}, total: {}", cart.getId(), cart.getTotalAmount());
        return mapToCartResponse(cart);
    }

    @Transactional(readOnly = true)
    public CartResponseDTO getCart(Long userId) {
        String transactionId = MDC.get("transactionId");
        logger.info("Getting cart for userId: {} - transaction: {}", userId, transactionId);

        Cart cart = cartRepository.findByUserIdAndStatusWithItems(userId, ACTIVE_STATUS)
                .orElseThrow(() -> new BusinessException("Cart not found", "CART_NOT_FOUND", 404));

        return mapToCartResponse(cart);
    }

    private Cart createNewCart(Long userId) {
        Cart cart = Cart.builder()
                .userId(userId)
                .status(ACTIVE_STATUS)
                .totalAmount(BigDecimal.ZERO)
                .build();
        return cartRepository.save(cart);
    }

    private void validateAddItemRequest(AddItemRequestDTO request) {
        if (request.getProductId() == null) {
            throw new BusinessException("Product ID is required", "MISSING_PRODUCT_ID", 400);
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BusinessException("Quantity must be greater than 0", "INVALID_QUANTITY", 400);
        }
    }

    private CartResponseDTO mapToCartResponse(Cart cart) {
        // Fetch all product details from product service
        List<Long> productIds = cart.getItems().stream()
                .map(CartItem::getProductId)
                .collect(Collectors.toList());

        Map<Long, ProductDTO> productMap = productServiceClient.getProductsByIds(productIds);

        // Map cart items with product details and calculate subtotals
        List<CartItemDTO> itemDTOs = cart.getItems().stream()
                .map(item -> mapToCartItemDTO(item, productMap.get(item.getProductId())))
                .collect(Collectors.toList());

        // Calculate total amount
        BigDecimal totalAmount = itemDTOs.stream()
                .map(CartItemDTO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Update cart total in database
        cart.setTotalAmount(totalAmount);
        cartRepository.save(cart);

        return CartResponseDTO.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .items(itemDTOs)
                .totalAmount(totalAmount)
                .status(cart.getStatus())
                .itemCount(cart.getItems().size())
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }

    private CartItemDTO mapToCartItemDTO(CartItem item, ProductDTO product) {
        if (product == null) {
            logger.warn("Product not found for productId: {}. Using placeholder data.", item.getProductId());
            return CartItemDTO.builder()
                    .id(item.getId())
                    .productId(item.getProductId())
                    .productName("Product not available")
                    .unitPrice(BigDecimal.ZERO)
                    .quantity(item.getQuantity())
                    .subtotal(BigDecimal.ZERO)
                    .createdAt(item.getCreatedAt())
                    .updatedAt(item.getUpdatedAt())
                    .build();
        }

        BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));

        return CartItemDTO.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(product.getName())
                .unitPrice(product.getPrice())
                .quantity(item.getQuantity())
                .subtotal(subtotal)
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
