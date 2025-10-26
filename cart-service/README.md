# Cart Service

Shopping cart management microservice for the Farmatodo e-commerce platform.

## Overview

The Cart Service manages user shopping carts with full CRUD operations for cart items. It provides functionality to add, update, remove items, and prepare carts for checkout/payment processing.

## Features

- **User-based cart management** - One active cart per user
- **Item management** - Add, update, remove products
- **Automatic calculations** - Subtotals and total cart value
- **Clear cart** - Remove all items at once
- **Checkout preparation** - Mark cart as COMPLETED for payment service
- **API Key security** - Protected endpoints
- **Request tracing** - MDC transaction IDs for all requests
- **PostgreSQL persistence** - Dedicated cartdb database

## Technology Stack

- Java 17
- Spring Boot 3.5.6
- Spring Data JPA
- PostgreSQL 16
- Lombok
- Maven

## Database Schema

### Cart Table
```sql
CREATE TABLE carts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_user_status (user_id, status)
);
```

### Cart Items Table
```sql
CREATE TABLE cart_items (
    id BIGSERIAL PRIMARY KEY,
    cart_id BIGINT NOT NULL REFERENCES carts(id),
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    quantity INTEGER NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_cart_id (cart_id),
    INDEX idx_product_id (product_id)
);
```

## API Endpoints

### Public Endpoints (No API Key Required)

**Health Check**
```bash
GET /carts/health
```

**Service Info**
```bash
GET /carts/info
```

### Protected Endpoints (API Key Required)

All protected endpoints require the `Authorization` header:
```
Authorization: ApiKey cart-service-api-key-change-in-production
```

**Get or Create Cart**
```bash
GET /carts/{userId}

Response:
{
  "id": 1,
  "userId": 1,
  "items": [],
  "totalAmount": 0.00,
  "status": "ACTIVE",
  "itemCount": 0,
  "createdAt": "2025-01-01T10:00:00",
  "updatedAt": "2025-01-01T10:00:00"
}
```

**Add Item to Cart**
```bash
POST /carts/{userId}/items
Content-Type: application/json

{
  "productId": 1,
  "productName": "Product A",
  "unitPrice": 10.50,
  "quantity": 2
}

Response: CartResponseDTO with updated cart
```

**Update Item Quantity**
```bash
PUT /carts/{userId}/items/{productId}
Content-Type: application/json

{
  "quantity": 5
}

Response: CartResponseDTO with updated cart
```

**Remove Item from Cart**
```bash
DELETE /carts/{userId}/items/{productId}

Response: CartResponseDTO with updated cart
```

**Clear Cart**
```bash
DELETE /carts/{userId}

Response:
{
  "message": "Cart cleared successfully",
  "userId": "1"
}
```

**Checkout Cart**
```bash
POST /carts/{userId}/checkout

Response: CartResponseDTO with status "COMPLETED"
```

## Configuration

### application.yml

```yaml
spring:
  application:
    name: cart-service
  datasource:
    url: jdbc:postgresql://localhost:5434/cartdb
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: update

server:
  port: 8084

api:
  key: cart-service-api-key-change-in-production
```

## Running Locally

### Prerequisites
- Java 17+
- PostgreSQL 16
- Maven 3.9+

### Steps

1. **Start PostgreSQL**
```bash
# Create database
createdb cartdb
```

2. **Build the project**
```bash
cd cart-service
./mvnw clean package
```

3. **Run the application**
```bash
./mvnw spring-boot:run
```

4. **Test the service**
```bash
# Health check
curl http://localhost:8084/carts/health

# Get cart for user 1
curl -H "Authorization: ApiKey cart-service-api-key-change-in-production" \
  http://localhost:8084/carts/1
```

## Running with Docker

```bash
# From project root
docker-compose up cart-service
```

## Integration with Other Services

### Preparing for Payment Service

The cart service is designed to integrate with a future payment microservice:

1. User adds items to cart
2. User updates quantities as needed
3. User calls `POST /carts/{userId}/checkout`
4. Cart status changes to `COMPLETED`
5. Payment service can:
   - Fetch cart details by cartId
   - Process payment
   - Mark cart as PAID or create new ACTIVE cart

### Recommended Payment Service Integration

```java
// Payment service calls cart service
GET /carts/{userId} (with status=COMPLETED)

// Process payment...

// After successful payment:
// Option 1: Mark cart with custom status (requires cart-service update)
// Option 2: Cart service creates new ACTIVE cart for user
// Option 3: Payment service stores reference to completed cart
```

## Error Handling

All errors follow a standard format:

```json
{
  "errorCode": "CART_NOT_FOUND",
  "message": "Cart not found",
  "timestamp": "2025-01-01T10:00:00",
  "transactionId": "abc-123-def"
}
```

### Common Error Codes

- `CART_NOT_FOUND` - Cart doesn't exist for user
- `ITEM_NOT_FOUND` - Product not in cart
- `INVALID_QUANTITY` - Quantity must be > 0
- `INVALID_PRICE` - Price must be > 0
- `MISSING_PRODUCT_ID` - Product ID required
- `EMPTY_CART` - Cannot checkout empty cart
- `UNAUTHORIZED` - Invalid or missing API key
- `INTERNAL_ERROR` - Unexpected server error

## Testing

```bash
# Run all tests
./mvnw test

# Run specific test
./mvnw test -Dtest=CartServiceApplicationTests
```

## Logging

All requests include transaction IDs for tracing:

```
2025-01-01 10:00:00 [http-nio-8084-exec-1] [abc-123-def] INFO  c.f.cart_service.controller.CartController - Add item to cart endpoint called for userId: 1
```

## Security

- **API Key Authentication** - All protected endpoints require valid API key
- **MDC Tracing** - Unique transaction ID per request
- **Input Validation** - All inputs validated before processing

## Performance Considerations

- **Lazy loading** - Cart items loaded on demand
- **Indexed queries** - Optimized for user + status lookups
- **Automatic calculations** - Subtotals and totals computed on updates
- **Transactional operations** - ACID guarantees for cart modifications

## Future Enhancements

- [ ] Cart expiration (auto-clear old carts)
- [ ] Cart merge (for anonymous -> authenticated users)
- [ ] Cart sharing/wishlist features
- [ ] Inventory validation integration
- [ ] Discount/coupon support
- [ ] Save for later functionality
