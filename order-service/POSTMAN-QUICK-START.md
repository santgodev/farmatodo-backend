# Postman Quick Start Guide - Order Service

## 🔐 Security Notice

**All endpoints except `/orders/ping` require API Key authentication.**

## Quick Setup

### 1. Import Collection

Import `Order-Service.postman_collection.json` into Postman.

### 2. API Key Configuration

The collection includes the API Key in variables:

- **Variable**: `apiKey`
- **Default Value**: `order-service-secure-api-key-change-in-production`

**The API Key is already configured in all requests!** You don't need to add it manually.

### 3. Test Health Check (No Auth Required)

**GET** `http://localhost:8085/orders/ping`

```bash
curl http://localhost:8085/orders/ping
```

**Response**: `pong`

## Creating an Order

### Prerequisites

1. **Client created** (client-service)
2. **Items in cart** (cart-service)
3. **Valid token** (token-service)

### Request

**POST** `http://localhost:8085/orders`

**Headers**:
```
Authorization: ApiKey order-service-secure-api-key-change-in-production
Content-Type: application/json
```

**Body**:
```json
{
  "clientId": 1,
  "token": "tok_abc123",
  "email": "your-email@gmail.com"
}
```

### cURL Example

```bash
curl -X POST http://localhost:8085/orders \
  -H "Authorization: ApiKey order-service-secure-api-key-change-in-production" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": 1,
    "token": "tok_abc123",
    "email": "your-email@gmail.com"
  }'
```

### Expected Response (201 Created)

```json
{
  "orderId": 1,
  "clientId": 1,
  "token": "tok_abc123",
  "items": [
    {
      "productId": 1,
      "productName": "Product A",
      "unitPrice": 10.50,
      "quantity": 2,
      "subtotal": 21.00
    }
  ],
  "totalAmount": 21.00,
  "status": "APPROVED",
  "transactionId": "uuid-here",
  "rejectionReason": null,
  "paymentAttempts": 1,
  "createdAt": "2025-10-27T10:30:00",
  "updatedAt": "2025-10-27T10:30:05"
}
```

**✉️ Email Sent**: Check your inbox for order confirmation!

## Error Responses

### Missing API Key

**Request without Authorization header**:
```bash
curl -X POST http://localhost:8085/orders \
  -H "Content-Type: application/json" \
  -d '{"clientId": 1, "token": "tok_abc", "email": "test@example.com"}'
```

**Response** (401):
```json
{
  "error": "UNAUTHORIZED",
  "message": "Missing or invalid API Key. Use 'Authorization: ApiKey <your-key>'",
  "status": 401
}
```

### Invalid API Key

**Request with wrong key**:
```bash
curl -X POST http://localhost:8085/orders \
  -H "Authorization: ApiKey wrong-key" \
  -H "Content-Type: application/json" \
  -d '{"clientId": 1, "token": "tok_abc", "email": "test@example.com"}'
```

**Response** (401):
```json
{
  "error": "UNAUTHORIZED",
  "message": "Invalid API Key",
  "status": 401
}
```

## Complete Testing Workflow

### Step 1: Create Client

**POST** `http://localhost:8081/api/clients`

```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "phone": "1234567890"
}
```

### Step 2: Add Items to Cart

**POST** `http://localhost:8084/carts/1/items`

**Headers**:
```
Authorization: ApiKey cart-service-api-key-change-in-production
Content-Type: application/json
```

**Body**:
```json
{
  "productId": 1,
  "productName": "Aspirin 500mg",
  "unitPrice": 5.99,
  "quantity": 3
}
```

### Step 3: Generate Token

**POST** `http://localhost:8082/api/tokens/tokenize`

```json
{
  "cardNumber": "4111111111111111",
  "expiryDate": "12/25",
  "cvv": "123"
}
```

**Response**:
```json
{
  "token": "tok_a1b2c3d4e5f6g7h8",
  "maskedCard": "****1111",
  "expiresAt": "2025-10-27T11:00:00",
  "status": "ACTIVE"
}
```

### Step 4: Create Order

**POST** `http://localhost:8085/orders`

**Headers**:
```
Authorization: ApiKey order-service-secure-api-key-change-in-production
Content-Type: application/json
```

**Body**:
```json
{
  "clientId": 1,
  "token": "tok_a1b2c3d4e5f6g7h8",
  "email": "your-email@gmail.com"
}
```

**Result**:
- ✅ Order created
- ✅ Payment processed
- ✅ Email sent
- ✅ Cart cleared (if approved)

## Postman Variables

| Variable | Default Value | Description |
|----------|--------------|-------------|
| `baseUrl` | `http://localhost:8085` | Order service URL |
| `clientId` | `1` | Test client ID |
| `token` | `tok_1234567890abcdef` | Sample token |
| `email` | `customer@example.com` | Test email |
| `apiKey` | `order-service-secure-api-key-change-in-production` | API Key for authentication |

**To modify variables**:
1. Click collection name
2. Go to "Variables" tab
3. Update "Current Value"
4. Save

## Testing Scenarios

### ✅ Scenario 1: Successful Order

- Payment approved (70% probability)
- Email: Order confirmation
- Cart: Cleared

### ❌ Scenario 2: Failed Payment

- Payment rejected (30% probability)
- Email: Failure notification
- Cart: NOT cleared
- Retries: 3 attempts

### 🔒 Scenario 3: Missing API Key

- Request rejected immediately
- Error: 401 Unauthorized
- Message: "Missing or invalid API Key"

### 🔒 Scenario 4: Invalid API Key

- Request rejected immediately
- Error: 401 Unauthorized
- Message: "Invalid API Key"

## Troubleshooting

### "Missing or invalid API Key"

**Solution**:
1. Check Postman collection has `apiKey` variable set
2. Verify header format: `Authorization: ApiKey <key>`
3. Ensure request inherits collection auth or has header

### "Invalid API Key"

**Solution**:
1. Verify API Key matches value in `application.yml`
2. Check for typos or extra spaces
3. Restart order-service if configuration changed

### "Cart is empty"

**Solution**: Add items to cart before creating order (Step 2)

### Email not received

**Solution**:
1. Check spam folder
2. Verify email address in request
3. Check application logs for email errors

## Security Best Practices

✅ **API Key Protection**:
- Never share API Key publicly
- Use environment-specific keys
- Rotate keys regularly
- Store in secure location (not in code)

✅ **Production Configuration**:
- Change default API Key
- Use HTTPS only
- Implement rate limiting
- Monitor authentication failures

## Summary

🔑 **Authentication Required**: All endpoints except `/orders/ping`

📋 **Header Format**: `Authorization: ApiKey <your-key>`

🔐 **Default API Key**: `order-service-secure-api-key-change-in-production`

✉️ **Email Notifications**: Sent automatically after payment

📚 **Full Documentation**: See `API-KEY-SECURITY-GUIDE.md`

---

**Ready to test?** Import the Postman collection and start creating orders!
