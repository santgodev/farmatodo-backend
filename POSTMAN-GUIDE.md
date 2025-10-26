# Farmatodo Postman Collection Guide

Complete guide to testing the Farmatodo microservices using Postman.

## 📦 Files Included

1. **Farmatodo-Postman-Collection.json** - Complete API collection with all endpoints
2. **Farmatodo-Postman-Environment.json** - Environment variables for local testing

## 🚀 Quick Start

### Step 1: Import Collection

1. Open Postman
2. Click **Import** button (top left)
3. Select **Farmatodo-Postman-Collection.json**
4. Click **Import**

### Step 2: Import Environment

1. Click **Environments** (left sidebar)
2. Click **Import** button
3. Select **Farmatodo-Postman-Environment.json**
4. Click **Import**

### Step 3: Activate Environment

1. Click the environment dropdown (top right)
2. Select **Farmatodo - Local**
3. You're ready to test!

## 🏗️ Start the Services

Before testing, make sure all services are running:

```bash
# Option 1: Using Docker Compose (Recommended)
docker-compose up --build

# Option 2: Run services individually (Windows)
cd client-service && mvnw.cmd spring-boot:run
cd token-service && mvnw.cmd spring-boot:run
cd product-service && mvnw.cmd spring-boot:run
cd cart-service && mvnw.cmd spring-boot:run
cd order-service && mvnw.cmd spring-boot:run
cd api-gateway && mvnw.cmd spring-boot:run
```

Wait until all services are healthy before making requests.

### Verify Services are Running

```bash
# Check all services
curl http://localhost:8080/api/gateway/health  # API Gateway
curl http://localhost:8081/api/clients/health  # Client Service
curl http://localhost:8082/ping                # Token Service
curl http://localhost:8083/products/ping       # Product Service
curl http://localhost:8084/carts/health        # Cart Service
curl http://localhost:8085/orders/ping         # Order Service
```

## 📋 Collection Structure

The collection is organized into 7 main folders:

### 1. **API Gateway**
- Health Check
- Service Info

### 2. **Client Service** (Customer Management)
- Health Check
- Service Info
- Create Client ✨ *Auto-saves client_id*
- Get Client by ID
- Get Client by Email
- Get All Clients
- Update Client
- Delete Client (Soft Delete)

### 3. **Token Service** (Card Tokenization & Payment)
- Health Check
- Service Info
- Ping
- Tokenize Card ✨ *Auto-saves payment_token*

### 4. **Product Service** (Catalog & Search)
- Health Check
- Service Info
- Search Products ✨ *Auto-saves product details*
- Get Low Stock Products

### 5. **Cart Service** (Shopping Cart)
- Health Check
- Service Info
- Get or Create Cart ✨ *Auto-saves cart_id*
- Add Item to Cart
- Add Another Item
- Update Item Quantity
- Remove Item from Cart
- Clear Cart
- Checkout Cart

### 6. **Order Service** (Order & Payment Management) 🆕
- Ping
- Create Order ✨ *Auto-saves order_id, processes payment with retry*
- Get Order by ID

### 7. **Complete E2E Flow** (Full User Journey) 🌟
An automated 8-step sequence demonstrating the complete platform:
1. **Create Client** - Register new customer
2. **Search Products** - Find products in catalog
3. **Add Products to Cart** - Add first product
4. **Add Second Product** - Add another item
5. **Review Cart** - Check cart contents
6. **Tokenize Payment Card** - Secure card tokenization
7. **Create Order and Process Payment** - Complete order with automatic retry
8. **Get Final Order Details** - Verify order status

## 🔑 Environment Variables

The environment includes these pre-configured variables:

### Service URLs
| Variable | Value | Description |
|----------|-------|-------------|
| `gateway_url` | http://localhost:8080 | API Gateway URL |
| `client_service_url` | http://localhost:8081 | Client Service URL |
| `token_service_url` | http://localhost:8082 | Token Service URL |
| `product_service_url` | http://localhost:8083 | Product Service URL |
| `cart_service_url` | http://localhost:8084 | Cart Service URL |
| `order_service_url` | http://localhost:8085 | Order Service URL 🆕 |

### API Keys
| Variable | Value | Description |
|----------|-------|-------------|
| `client_api_key` | client-service-api-key-change-in-production | Client Service API Key |
| `token_api_key` | your-secret-api-key-change-in-production | Token Service API Key |
| `product_api_key` | product-service-api-key-change-in-production | Product Service API Key |
| `cart_api_key` | cart-service-api-key-change-in-production | Cart Service API Key |

### Auto-Captured Variables
These are automatically saved by request scripts:
| Variable | Description |
|----------|-------------|
| `client_id` | Created client ID |
| `client_email` | Client email address |
| `client_name` | Client full name |
| `payment_token` | Tokenized payment token (secure) |
| `card_last4` | Last 4 digits of card |
| `product_id` | First found product ID |
| `product_name` | Product name |
| `product_price` | Product price |
| `cart_id` | Active cart ID |
| `cart_total` | Cart total amount |
| `order_id` | Created order ID 🆕 |
| `order_status` | Order status (APPROVED/REJECTED) 🆕 |
| `order_transaction_id` | Transaction tracking ID 🆕 |

## 🧪 Testing Workflows

### Workflow 1: Basic Health Checks

Run all health check endpoints to verify services are running:

1. API Gateway → Health Check
2. Client Service → Health Check
3. Token Service → Ping
4. Product Service → Health Check
5. Cart Service → Health Check
6. Order Service → Ping 🆕

All should return `200 OK` with `"pong"` or `"status": "UP"`.

### Workflow 2: Create and Manage Client

1. **Client Service → Create Client**
   - Creates a new customer
   - Returns client with ID
   - **Auto-saves** `client_id`, `client_email`, `client_name`

   ```json
   POST /api/clients
   {
     "name": "Juan Perez",
     "email": "juan.perez@example.com",
     "phone": "+573001234567",
     "address": "Calle 123 #45-67, Bogotá",
     "documentType": "DNI",
     "documentNumber": "1234567890"
   }
   ```

2. **Client Service → Get Client by ID**
   - Uses saved `{{client_id}}`
   - Verify client details

3. **Client Service → Update Client**
   - Modify client information
   - Check updated fields

### Workflow 3: Shopping Cart Flow

1. **Cart Service → Get or Create Cart**
   - Uses `{{client_id}}` from environment
   - Creates empty cart if doesn't exist
   - **Auto-saves** `cart_id`, `cart_total`

2. **Cart Service → Add Item to Cart**
   - Uses saved `{{product_id}}` or defaults
   - Adds first product
   - Check `totalAmount` and `itemCount`

3. **Cart Service → Add Another Item**
   - Adds second product
   - Cart automatically calculates new total

4. **Cart Service → Update Item Quantity**
   - Change quantity using `{{product_id}}`
   - Total recalculated automatically

5. **Cart Service → Get or Create Cart**
   - Review final cart state
   - Verify all items and totals

6. **Cart Service → Checkout Cart**
   - Marks cart as COMPLETED
   - Ready for payment processing

### Workflow 4: Payment Tokenization

1. **Token Service → Tokenize Card**
   - Use test card: `4532015112830366`
   - CVV: `123`
   - Expiry: `12/25`
   - Returns secure token
   - **Auto-saves** `payment_token`, `card_last4`
   - Only last 4 digits visible in response

   ```json
   POST /api/tokens/tokenize
   {
     "cardNumber": "4532015112830366",
     "cvv": "123",
     "expiry": "12/25",
     "cardholderName": "Juan Perez"
   }
   ```

### Workflow 5: Product Search

1. **Product Service → Search Products**
   - Search by name or description
   - Returns matching products with stock
   - **Auto-saves** first product details

2. **Product Service → Get Low Stock Products**
   - Find products needing restock
   - Useful for inventory management

### Workflow 6: Order & Payment Processing 🆕

1. **Order Service → Create Order**
   - Uses saved `{{client_id}}` and `{{payment_token}}`
   - Automatically:
     - Fetches client info from client-service
     - Creates order in PENDING state
     - Processes payment via token-service
     - Retries up to 3 times if payment rejected
     - Updates order to APPROVED or REJECTED
   - **Auto-saves** `order_id`, `order_status`, `order_transaction_id`

   ```json
   POST /orders
   {
     "clientId": {{client_id}},
     "token": "{{payment_token}}",
     "products": [
       {
         "productId": {{product_id}},
         "productName": "{{product_name}}",
         "unitPrice": {{product_price}},
         "quantity": 2
       }
     ]
   }
   ```

2. **Order Service → Get Order by ID**
   - Uses saved `{{order_id}}`
   - View complete order details
   - Check payment status and attempts
   - See transaction tracking ID

### Workflow 7: Complete E2E Flow (Automated) 🌟

**The crown jewel of the collection!** Run the entire user journey automatically:

1. **Open the "Complete E2E Flow" folder**
2. **Right-click on folder** → Select "Run folder"
3. **Click "Run Farmatodo..."**
4. **Watch the magic happen:**
   - ✅ Step 1: Client created
   - ✅ Step 2: Products found
   - ✅ Step 3: Product added to cart
   - ✅ Step 4: Second product added
   - ✅ Step 5: Cart reviewed
   - ✅ Step 6: Card tokenized
   - ✅ Step 7: Order created & payment processed
   - ✅ Step 8: Final order details retrieved

5. **Check the Console** for detailed logs and visual feedback
6. **Review Test Results** - All tests should pass ✓

**Features:**
- Automatic variable capture between steps
- Built-in test assertions
- Beautiful console logging with emojis
- Shows payment retry attempts
- Displays final order summary

## 📊 Response Examples

### Successful Client Creation
```json
{
  "id": 1,
  "name": "Juan Perez",
  "email": "juan.perez@example.com",
  "phone": "+573001234567",
  "address": "Calle 123 #45-67, Bogotá",
  "documentType": "DNI",
  "documentNumber": "1234567890",
  "status": "ACTIVE",
  "createdAt": "2025-01-24T10:00:00",
  "updatedAt": "2025-01-24T10:00:00"
}
```

### Cart with Items
```json
{
  "cartId": 1,
  "userId": 1,
  "items": [
    {
      "productId": 1,
      "productName": "Aspirin 500mg",
      "unitPrice": 15.50,
      "quantity": 2,
      "subtotal": 31.00
    }
  ],
  "totalAmount": 31.00,
  "status": "ACTIVE",
  "createdAt": "2025-01-24T10:00:00",
  "updatedAt": "2025-01-24T10:05:00"
}
```

### Tokenization Response
```json
{
  "token": "abc123def-456-789-ghi-jkl012mno345",
  "last4": "0366",
  "status": "ACTIVE",
  "createdAt": "2025-01-24T10:10:00"
}
```

### Order Response (APPROVED) 🆕
```json
{
  "orderId": 1,
  "clientId": 1,
  "token": "abc123def-456-789-ghi-jkl012mno345",
  "items": [
    {
      "productId": 1,
      "productName": "Aspirin 500mg",
      "unitPrice": 15.50,
      "quantity": 2,
      "subtotal": 31.00
    }
  ],
  "totalAmount": 31.00,
  "status": "APPROVED",
  "transactionId": "xyz-789-abc-123-def-456",
  "rejectionReason": null,
  "paymentAttempts": 1,
  "createdAt": "2025-01-24T10:15:00",
  "updatedAt": "2025-01-24T10:15:05"
}
```

### Order Response (REJECTED) 🆕
```json
{
  "orderId": 2,
  "clientId": 1,
  "token": "pqr678stu-901-234-vwx-yz012abc345",
  "items": [
    {
      "productId": 2,
      "productName": "Vitamin C",
      "unitPrice": 25.00,
      "quantity": 1,
      "subtotal": 25.00
    }
  ],
  "totalAmount": 25.00,
  "status": "REJECTED",
  "transactionId": "mno-456-pqr-789-stu-012",
  "rejectionReason": "Payment rejected on attempt 3",
  "paymentAttempts": 3,
  "createdAt": "2025-01-24T10:20:00",
  "updatedAt": "2025-01-24T10:20:10"
}
```

### Error Response
```json
{
  "errorCode": "ORDER_NOT_FOUND",
  "message": "Order not found",
  "timestamp": "2025-01-24T10:15:00",
  "transactionId": "abc-123-def-456"
}
```

## 🔍 Common Test Scenarios

### Test 1: Add Same Product Twice
1. Add item with productId=1, quantity=2
2. Add item with productId=1, quantity=3
3. **Expected**: Quantity becomes 5, not duplicate items

### Test 2: Update to Zero Quantity
1. Update item quantity to 0
2. **Expected**: Error "Quantity must be greater than 0"

### Test 3: Remove Non-Existent Item
1. Try to remove productId=999
2. **Expected**: Error "Item not found in cart"

### Test 4: Checkout Empty Cart
1. Clear cart
2. Try to checkout
3. **Expected**: Error "Cannot checkout empty cart"

### Test 5: Invalid API Key
1. Change Authorization header to wrong key
2. Make any protected request
3. **Expected**: 401 Unauthorized

### Test 6: Tokenize Invalid Card
1. Use card number: `1234567890123456`
2. **Expected**: Error "Invalid card number" (Luhn validation fails)

### Test 7: Payment Retry Mechanism 🆕
1. Create order with valid data
2. Observe payment attempts in response
3. **Expected**:
   - If approved: `paymentAttempts: 1` or `2` or `3`, `status: APPROVED`
   - If rejected: `paymentAttempts: 3`, `status: REJECTED`
4. Check logs for retry details

### Test 8: Order Without Token 🆕
1. Create order without payment token
2. **Expected**: Error "Payment token required"

### Test 9: Order With Invalid Client 🆕
1. Create order with clientId=99999
2. **Expected**: Error "Client service unavailable or client not found"

### Test 10: Transaction ID Tracking 🆕
1. Create order and note `transactionId`
2. Get order by ID
3. **Expected**: Same `transactionId` in both responses
4. Check centralized logs for complete transaction history

## 💡 Tips & Tricks

### Auto-Run Complete Flow
Use Postman Runner for automated testing:
1. Click "Complete E2E Flow" folder
2. Click "Run" button
3. Select all 8 requests
4. Set delay: 1000ms between requests
5. Click "Run Farmatodo..."
6. Watch the complete flow execute!

### Monitor Variables
View all environment variables:
1. Click the "eye" icon (top right)
2. See all saved values
3. Edit if needed
4. Reset by clearing values

### View Transaction Logs
After running E2E flow:
1. Check Postman Console (bottom panel)
2. See detailed logs with emojis
3. View request/response for each step
4. Track transaction IDs across services

### Test Payment Retry
To see payment retries in action:
1. Set `PAYMENT_REJECTIONPROBABILITY=0.5` in docker-compose.yml
2. Restart token-service
3. Create multiple orders
4. Observe different `paymentAttempts` counts

### Save Test Results
1. Run folder with Postman Runner
2. Click "Export Results"
3. Save as JSON for reporting

## 🎯 Advanced Testing

### Test Suite: Happy Path
Run these in sequence for full happy path:
1. Create Client
2. Search Products
3. Add 3 different products to cart
4. Review cart
5. Tokenize card
6. Create order
7. Verify order status = APPROVED

### Test Suite: Edge Cases
Test error handling:
1. Create order with invalid clientId
2. Create order with invalid token
3. Tokenize card with invalid CVV
4. Add item to non-existent cart
5. Checkout empty cart

### Test Suite: Performance
Test with multiple clients:
1. Create 10 clients (change email each time)
2. Create cart for each
3. Add items concurrently
4. Measure response times

### Test Suite: Transaction Tracking
Verify centralized logging:
1. Create order and save transaction ID
2. Query order-service logs (database)
3. Query token-service logs (database)
4. Verify same transaction ID in all logs

## 🐛 Troubleshooting

### Issue: Connection Refused
**Solution**: Make sure services are running
```bash
docker-compose ps
# or check individual service logs
docker-compose logs order-service
docker-compose logs token-service
```

### Issue: 401 Unauthorized
**Solution**: Check API key in environment variables matches service configuration
```bash
# In docker-compose.yml, verify:
API_KEY: client-service-api-key-change-in-production
```

### Issue: 404 Not Found
**Solution**: Verify URL path and service port
- Client Service: :8081/api/clients
- Token Service: :8082/api/tokens
- Product Service: :8083/products
- Cart Service: :8084/carts
- Order Service: :8085/orders

### Issue: Order Service Returns "Client not found"
**Solution**:
1. Make sure client-service is running
2. Verify client ID exists
3. Check service URLs in order-service configuration

### Issue: Payment Always Rejected
**Solution**:
1. Check `PAYMENT_REJECTIONPROBABILITY` in docker-compose.yml
2. Set to `0.0` for always approve
3. Set to `1.0` for always reject (testing)
4. Set to `0.3` for 30% rejection (realistic)

### Issue: Database Connection Error
**Solution**: Ensure PostgreSQL containers are healthy
```bash
docker-compose ps
# Check database status
docker-compose logs order-db
# Restart if needed
docker-compose restart order-db
```

### Issue: Variables Not Saving
**Solution**:
1. Make sure environment is selected (top right)
2. Check "Tests" tab in request
3. Verify script is saving to `pm.environment.set()`
4. Try manually setting in environment

### Issue: E2E Flow Fails Midway
**Solution**:
1. Run each step individually to identify issue
2. Check console for error messages
3. Verify all services are healthy
4. Reset environment variables and try again

## 📊 Test Card Numbers

Use these Luhn-valid test cards:

| Card Type | Number | CVV | Expiry |
|-----------|--------|-----|--------|
| Visa | 4532015112830366 | 123 | 12/25 |
| Visa | 4556737586899855 | 456 | 06/26 |
| MasterCard | 5425233430109903 | 789 | 09/27 |
| MasterCard | 2221000010000015 | 321 | 03/28 |

All cards are test/demo cards for development only.

## 📝 Notes

- All protected endpoints require `Authorization: ApiKey <key>` header
- Transaction IDs are automatically generated (UUID) and tracked across all services
- Cart operations are user-scoped (userId in URL path)
- Order service communicates with client-service, token-service, and product-service
- Payment retry logic is configurable (default: 3 attempts)
- Centralized logging tracks all events with transaction IDs
- All monetary values use 2 decimal places (BigDecimal)
- Email notifications are mocked (logs only, no actual emails sent)

## 🔗 Related Documentation

- [CLAUDE.md](CLAUDE.md) - Complete project documentation
- [README.md](README.md) - Project overview
- [docker-compose.yml](docker-compose.yml) - Service configuration

## 🚦 Quick Reference

### Service Ports
- API Gateway: **8080**
- Client Service: **8081**
- Token Service: **8082**
- Product Service: **8083**
- Cart Service: **8084**
- Order Service: **8085** 🆕

### Database Ports
- Client DB: **5432**
- Token DB: **5433**
- Product DB: **5434**
- Cart DB: **5435**
- Order DB: **5436** 🆕

### Payment Configuration
- Rejection Probability: **0.3** (30%)
- Retry Count: **3** attempts
- Email Enabled: **false** (mock only)

## 🎓 Learning Path

### For Beginners
1. Start with health checks (Workflow 1)
2. Create a client (Workflow 2)
3. Search products (Workflow 5)
4. Run "Complete E2E Flow" and observe

### For Intermediate Users
1. Build shopping cart (Workflow 3)
2. Tokenize payment (Workflow 4)
3. Create order manually (Workflow 6)
4. Test edge cases (Test Suite: Edge Cases)

### For Advanced Users
1. Create custom test suites
2. Implement performance testing
3. Query centralized logs
4. Modify payment rejection probability
5. Build CI/CD integration

## 🚀 Next Steps

1. ✅ Import both JSON files into Postman
2. ✅ Start all services with `docker-compose up --build`
3. ✅ Run health checks to verify all services
4. ✅ Try the "Complete E2E Flow"
5. ✅ Experiment with individual endpoints
6. ✅ Build your own test scenarios
7. ✅ Check centralized logs for transaction tracking
8. ✅ Test payment retry mechanism
9. ✅ Create custom workflows

## 🎉 Success Criteria

You'll know everything is working when:
- ✓ All 6 services respond to health checks
- ✓ You can create a client and receive an ID
- ✓ Products are searchable and returned
- ✓ Cart operations work smoothly
- ✓ Card tokenization returns a secure token
- ✓ Orders are created and payment processed
- ✓ Complete E2E Flow runs without errors
- ✓ Console shows beautiful logs with status updates
- ✓ All tests pass (green checkmarks)

**Happy Testing! 🎉🚀**

---

*Last Updated: 2025-01-24*
*Version: 2.0 (with Order Service)*
*Microservices: 6 | Endpoints: 40+ | Test Flows: 7+*
