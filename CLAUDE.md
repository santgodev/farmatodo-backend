# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot microservices project named "Farmatodo" consisting of six independent services:

1. **api-gateway** - API Gateway service using Spring Cloud Gateway (WebMVC)
2. **client-service** - Client management service with JPA/PostgreSQL persistence
3. **token-service** - Token management and payment processing service with PostgreSQL backend
4. **product-service** - Product catalog and search service with async analytics
5. **cart-service** - Shopping cart service with user-based cart management
6. **order-service** - Order and payment management service with retry logic and centralized logging

All services are built with:
- Java 17
- Spring Boot 3.5.6
- Maven as the build tool
- Lombok for boilerplate reduction

## Architecture

**Microservices Pattern**: Each service is independently deployable and located in its own directory at the repository root.

**Database**: Services `client-service`, `token-service`, `product-service`, `cart-service`, and `order-service` use PostgreSQL. Each service has its own isolated database. Connection details should be configured in `application.yml` of each service.

**Gateway**: The `api-gateway` service uses Spring Cloud Gateway (WebMVC variant) to route requests to backend services.

**Package Structure**: Each service follows the standard Maven structure with base package `com.farmatodo.<service-name>`.

## Testing with Postman

A complete Postman collection is available for testing all services:

- **Farmatodo-Postman-Collection.json** - All API endpoints
- **Farmatodo-Postman-Environment.json** - Environment variables
- **POSTMAN-GUIDE.md** - Complete testing guide

Import both files into Postman and follow the [POSTMAN-GUIDE.md](POSTMAN-GUIDE.md) for detailed instructions.

## Common Commands

### Building a Service

Navigate to the specific service directory and run:

```bash
cd <service-name>
./mvnw clean package
```

On Windows:
```bash
cd <service-name>
mvnw.cmd clean package
```

### Running a Service

From within the service directory:

```bash
./mvnw spring-boot:run
```

On Windows:
```bash
mvnw.cmd spring-boot:run
```

### Running Tests

From within the service directory:

```bash
./mvnw test
```

On Windows:
```bash
mvnw.cmd test
```

### Running a Single Test

```bash
./mvnw test -Dtest=ClassName#methodName
```

### Building All Services

From the repository root:

```bash
cd api-gateway && ./mvnw clean package && cd ../client-service && ./mvnw clean package && cd ../token-service && ./mvnw clean package && cd ../product-service && ./mvnw clean package && cd ../cart-service && ./mvnw clean package && cd ../order-service && ./mvnw clean package && cd ..
```

## Service Dependencies

**api-gateway**:
- spring-cloud-starter-gateway-server-webmvc
- Spring Cloud version: 2025.0.0

**client-service**:
- spring-boot-starter-data-jpa
- spring-boot-starter-web
- postgresql (runtime)
- lombok

**token-service**:
- spring-boot-starter-web
- postgresql (runtime)
- lombok

**product-service**:
- spring-boot-starter-data-jpa
- spring-boot-starter-web
- postgresql (runtime)
- lombok

**cart-service**:
- spring-boot-starter-data-jpa
- spring-boot-starter-web
- postgresql (runtime)
- lombok

**order-service**:
- spring-boot-starter-data-jpa
- spring-boot-starter-web
- postgresql (runtime)
- lombok

## Docker

### Running with Docker Compose

From the repository root:

```bash
# Build and start all services
docker-compose up --build

# Run in detached mode
docker-compose up -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

### Service URLs (Docker)
- API Gateway: http://localhost:8080
- Client Service: http://localhost:8081
- Token Service: http://localhost:8082
- Product Service: http://localhost:8083
- Cart Service: http://localhost:8084
- Order Service: http://localhost:8085
- Client Database: localhost:5432
- Token Database: localhost:5433
- Product Database: localhost:5434
- Cart Database: localhost:5435
- Order Database: localhost:5436

## API Endpoints

### API Gateway (Port 8080)
```
GET /api/gateway/health - Health check
GET /api/gateway/info - Service information
```

### Client Service (Port 8081)
```
GET /api/clients/health - Health check with database connectivity status
GET /api/clients/info - Service information
```

### Token Service (Port 8082)
```
GET /api/tokens/health - Health check with database connectivity status
GET /api/tokens/info - Service information
POST /api/tokens/tokenize - Tokenize a credit card
POST /api/tokens/payment - Process payment with retry logic
```

### Product Service (Port 8083)
```
GET /products/health - Health check
GET /products/info - Service information
GET /products?query=<search> - Search products by name/description
GET /products/low-stock?maxStock=<threshold> - Get low-stock products
```

### Cart Service (Port 8084)
```
GET /carts/health - Health check
GET /carts/info - Service information
GET /carts/{userId} - Get or create cart for user
POST /carts/{userId}/items - Add item to cart
PUT /carts/{userId}/items/{productId} - Update item quantity
DELETE /carts/{userId}/items/{productId} - Remove item from cart
DELETE /carts/{userId} - Clear entire cart
POST /carts/{userId}/checkout - Checkout cart (prepare for payment)
```

### Order Service (Port 8085)
```
GET /orders/ping - Health check returning "pong"
POST /orders - Create a new order and process payment
GET /orders/{id} - Retrieve order details and payment status
```

The Order Service orchestrates the following:
1. Fetches client information from client-service
2. Creates order in database with PENDING status
3. Processes payment via token-service with automatic retry logic
4. Updates order status based on payment result (APPROVED/REJECTED)
5. Logs all events to centralized log database with transaction tracking

**Payment Configuration:**
- `payment.rejectionProbability`: Probability of payment rejection (0.0 to 1.0, default: 0.3)
- `payment.retryCount`: Number of retry attempts if payment is rejected (default: 3)
- If all payment attempts fail, an email notification is sent to the client (mock implementation)

**Centralized Logging:**
- All microservices log events to a centralized `log_entries` table
- Each request has a unique `transactionId` (UUID) that propagates across services
- Transaction IDs are passed via `X-Transaction-Id` HTTP header
- Logs include: transactionId, timestamp, serviceName, eventType, message, additionalData

### Testing the Services

After starting the services with Docker Compose or manually, you can test them:

```bash
# Test API Gateway
curl http://localhost:8080/api/gateway/health
curl http://localhost:8080/api/gateway/info

# Test Client Service
curl http://localhost:8081/api/clients/health
curl http://localhost:8081/api/clients/info

# Test Token Service
curl http://localhost:8082/api/tokens/health
curl http://localhost:8082/api/tokens/info

# Test Product Service
curl http://localhost:8083/products/health
curl http://localhost:8083/products/info

# Test Cart Service
curl http://localhost:8084/carts/health
curl http://localhost:8084/carts/info

# Test Cart Operations (requires API key)
curl -H "Authorization: ApiKey cart-service-api-key-change-in-production" \
  http://localhost:8084/carts/1

curl -X POST -H "Authorization: ApiKey cart-service-api-key-change-in-production" \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"productName":"Product A","unitPrice":10.50,"quantity":2}' \
  http://localhost:8084/carts/1/items

# Test Order Service
curl http://localhost:8085/orders/ping

# Create an order
curl -X POST -H "Content-Type: application/json" \
  -d '{
    "clientId": 1,
    "token": "your-token-here",
    "products": [
      {"productId": 1, "productName": "Product A", "unitPrice": 10.50, "quantity": 2},
      {"productId": 2, "productName": "Product B", "unitPrice": 5.00, "quantity": 1}
    ]
  }' \
  http://localhost:8085/orders

# Get order by ID
curl http://localhost:8085/orders/1
```

## Development Notes

**Lombok Configuration**: All backend services are configured with Lombok annotation processing. The Maven compiler plugin is configured to recognize Lombok annotations during compilation.

**PostgreSQL Setup**: Before running services, ensure PostgreSQL is running and connection properties (URL, username, password) are properly configured in the respective `application.yml` files.

**Service Ports**:
- API Gateway: 8080
- Client Service: 8081
- Token Service: 8082
- Product Service: 8083
- Cart Service: 8084
- Order Service: 8085

**Database per Service**: Each service has its own PostgreSQL database (clientdb, tokendb, productdb, cartdb, orderdb) following microservices best practices. Services cannot directly JOIN tables across databases and must communicate via REST APIs.

**Cart Service Features**:
- User-based cart management (one active cart per user)
- Add/update/remove items from cart
- Automatic subtotal and total calculation
- Clear cart functionality
- Checkout operation marks cart as COMPLETED (ready for payment service integration)

**Order Service Features**:
- Complete order and payment orchestration
- Inter-service communication with client-service, token-service, and product-service
- Automatic payment retry logic (configurable retry count)
- Email notification on payment failure (mock implementation)
- Centralized logging with transaction tracking across all services
- Transaction ID propagation via X-Transaction-Id HTTP header
- Order status tracking (PENDING, PROCESSING, APPROVED, REJECTED)
