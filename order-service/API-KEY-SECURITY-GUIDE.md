# API Key Security Guide - Order Service

## Overview

The order-service now requires API Key authentication for all endpoints **except** the health check endpoint (`/orders/ping`). This ensures that only authorized clients can create and retrieve orders.

## Security Implementation

### Protected Endpoints

All endpoints require API Key authentication:

- `POST /orders` - Create new order
- `GET /orders/{id}` - Get order by ID
- Any future endpoints under `/orders/*`

### Public Endpoints

These endpoints do NOT require authentication:

- `GET /orders/ping` - Health check endpoint

## API Key Configuration

### Default API Key

The default API Key is configured in `application.yml`:

```yaml
api:
  key: order-service-secure-api-key-change-in-production
```

**⚠️ IMPORTANT**: Change this key in production environments!

### Production Configuration

For production, use environment variables:

```yaml
api:
  key: ${ORDER_SERVICE_API_KEY:order-service-secure-api-key-change-in-production}
```

Set the environment variable:

```bash
# Windows
set ORDER_SERVICE_API_KEY=your-secure-production-key-here

# Linux/Mac
export ORDER_SERVICE_API_KEY=your-secure-production-key-here
```

## Using the API

### Authentication Header Format

All protected endpoints require the `Authorization` header with the format:

```
Authorization: ApiKey <your-api-key>
```

**Example**:
```
Authorization: ApiKey order-service-secure-api-key-change-in-production
```

### cURL Examples

#### Create Order (with API Key)

```bash
curl -X POST http://localhost:8085/orders \
  -H "Authorization: ApiKey order-service-secure-api-key-change-in-production" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": 1,
    "token": "tok_abc123",
    "email": "customer@example.com"
  }'
```

#### Get Order (with API Key)

```bash
curl -X GET http://localhost:8085/orders/1 \
  -H "Authorization: ApiKey order-service-secure-api-key-change-in-production"
```

#### Health Check (no API Key required)

```bash
curl -X GET http://localhost:8085/orders/ping
# Returns: pong
```

## Postman Usage

### Import Collection

The `Order-Service.postman_collection.json` file includes API Key configuration.

### Collection Variables

The collection includes an `apiKey` variable:

| Variable | Value |
|----------|-------|
| `apiKey` | `order-service-secure-api-key-change-in-production` |

### Using API Key in Postman

**Option 1: Collection-level Authentication (Recommended)**

1. Import the collection
2. Right-click collection → "Edit"
3. Go to "Authorization" tab
4. Type is already set to "API Key"
5. The variable `{{apiKey}}` is used automatically

**Option 2: Request-level Authentication**

Each request already includes the header:
```
Authorization: ApiKey {{apiKey}}
```

The `{{apiKey}}` variable is replaced with the actual key from collection variables.

### Changing the API Key

1. Click on collection name
2. Go to "Variables" tab
3. Update `apiKey` current value
4. Save

## Error Responses

### Missing API Key

**Request without Authorization header**:
```bash
curl -X POST http://localhost:8085/orders \
  -H "Content-Type: application/json" \
  -d '{"clientId": 1, "token": "tok_abc", "email": "test@example.com"}'
```

**Response** (401 Unauthorized):
```json
{
  "error": "UNAUTHORIZED",
  "message": "Missing or invalid API Key. Use 'Authorization: ApiKey <your-key>'",
  "status": 401
}
```

### Invalid API Key Format

**Request with wrong format**:
```bash
curl -X POST http://localhost:8085/orders \
  -H "Authorization: Bearer wrong-format" \
  -H "Content-Type: application/json" \
  -d '{"clientId": 1}'
```

**Response** (401 Unauthorized):
```json
{
  "error": "UNAUTHORIZED",
  "message": "Missing or invalid API Key. Use 'Authorization: ApiKey <your-key>'",
  "status": 401
}
```

### Wrong API Key

**Request with incorrect key**:
```bash
curl -X POST http://localhost:8085/orders \
  -H "Authorization: ApiKey wrong-key-here" \
  -H "Content-Type: application/json" \
  -d '{"clientId": 1}'
```

**Response** (401 Unauthorized):
```json
{
  "error": "UNAUTHORIZED",
  "message": "Invalid API Key",
  "status": 401
}
```

## Security Best Practices

### 1. Key Rotation

Rotate API keys regularly (every 90 days recommended):

1. Generate new key
2. Update configuration
3. Notify API consumers
4. Restart service
5. Monitor for old key usage
6. Revoke old key after grace period

### 2. Key Storage

**Never commit keys to version control**:

- ❌ Hardcode in application.yml
- ✅ Use environment variables
- ✅ Use secrets management (Vault, AWS Secrets Manager, GCP Secret Manager)

**Example .gitignore**:
```
application-production.yml
.env
*.key
```

### 3. Key Complexity

Generate strong API keys:

```bash
# Generate random 32-character key
openssl rand -hex 32

# Or use UUID
uuidgen
```

### 4. HTTPS Only

Always use HTTPS in production to prevent key interception:

```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
```

### 5. Rate Limiting

Implement rate limiting to prevent abuse:

```xml
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
</dependency>
```

### 6. IP Whitelisting

For additional security, whitelist known IPs:

```java
@Component
public class IpWhitelistFilter implements Filter {
    private static final Set<String> ALLOWED_IPS = Set.of(
        "192.168.1.100",
        "10.0.0.50"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        String remoteAddr = request.getRemoteAddr();
        if (!ALLOWED_IPS.contains(remoteAddr)) {
            // Reject request
        }
        chain.doFilter(request, response);
    }
}
```

## Monitoring and Logging

### Security Logs

The API Key filter logs authentication attempts:

**Successful authentication**:
```
DEBUG c.f.o.filter.ApiKeyAuthFilter - API Key validated successfully for path: /orders
```

**Failed authentication**:
```
WARN  c.f.o.filter.ApiKeyAuthFilter - Invalid API Key provided for path: /orders from IP: 192.168.1.100
```

### Monitoring Recommendations

1. **Track failed authentication attempts**:
   - Alert on high failure rate
   - Block IPs with repeated failures

2. **Monitor API usage**:
   - Track requests per API key (if using multiple keys)
   - Identify unusual patterns

3. **Log all access attempts**:
   - Include timestamp, IP, endpoint, result
   - Store in centralized logging system

## Multiple API Keys (Future Enhancement)

For production systems with multiple clients, consider implementing:

### Database-backed API Keys

```sql
CREATE TABLE api_keys (
    id BIGSERIAL PRIMARY KEY,
    key_value VARCHAR(255) UNIQUE NOT NULL,
    client_name VARCHAR(100),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP,
    expires_at TIMESTAMP,
    last_used_at TIMESTAMP
);
```

### Key Management Service

```java
@Service
public class ApiKeyService {
    public boolean validateApiKey(String key) {
        // Check database
        // Update last_used_at
        // Check expiration
        return apiKeyRepository.existsByKeyValueAndIsActiveTrue(key);
    }
}
```

## Testing Security

### Test Case 1: Access Without API Key

```bash
# Should return 401 Unauthorized
curl -X POST http://localhost:8085/orders \
  -H "Content-Type: application/json" \
  -d '{"clientId": 1, "token": "tok_test", "email": "test@example.com"}'
```

**Expected**: 401 error with message about missing API Key

### Test Case 2: Access With Valid API Key

```bash
# Should return 201 Created (or appropriate response)
curl -X POST http://localhost:8085/orders \
  -H "Authorization: ApiKey order-service-secure-api-key-change-in-production" \
  -H "Content-Type: application/json" \
  -d '{"clientId": 1, "token": "tok_test", "email": "test@example.com"}'
```

**Expected**: Order created successfully

### Test Case 3: Health Check Without API Key

```bash
# Should return "pong" without authentication
curl -X GET http://localhost:8085/orders/ping
```

**Expected**: "pong" response (200 OK)

### Test Case 4: Invalid API Key

```bash
# Should return 401 Unauthorized
curl -X POST http://localhost:8085/orders \
  -H "Authorization: ApiKey wrong-key" \
  -H "Content-Type: application/json" \
  -d '{"clientId": 1, "token": "tok_test", "email": "test@example.com"}'
```

**Expected**: 401 error with message "Invalid API Key"

## Integration with Other Services

### Service-to-Service Communication

If other services need to call order-service, they must include the API Key:

**Example from cart-service**:
```java
@Service
public class OrderServiceClient {
    @Value("${order.service.api.key}")
    private String apiKey;

    public OrderResponse createOrder(OrderRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "ApiKey " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<OrderRequest> entity = new HttpEntity<>(request, headers);
        return restTemplate.postForObject(orderServiceUrl, entity, OrderResponse.class);
    }
}
```

### API Gateway Configuration

If using an API Gateway (api-gateway service), configure it to:

1. Validate API Key at gateway level
2. Forward key to backend services
3. Or replace with internal service token

## Troubleshooting

### Issue: "Missing or invalid API Key" Error

**Cause**: Authorization header not sent or wrong format

**Solution**:
1. Check Authorization header is present
2. Verify format: `Authorization: ApiKey <key>`
3. Ensure "ApiKey" has capital K
4. Check for extra spaces

### Issue: "Invalid API Key" Error

**Cause**: API Key doesn't match configured value

**Solution**:
1. Verify key in application.yml matches request
2. Check for trailing spaces in configuration
3. Ensure environment variable is set correctly
4. Restart service after configuration change

### Issue: Health Check Returns 401

**Cause**: API Key filter incorrectly applied to /orders/ping

**Solution**:
- This shouldn't happen - health check is excluded
- Check FilterConfig.java configuration
- Verify endpoint path is exactly `/orders/ping`

### Issue: Request Hangs or Timeout

**Cause**: Network or service issue, not authentication

**Solution**:
1. Check service is running: `curl http://localhost:8085/orders/ping`
2. Verify port 8085 is not blocked by firewall
3. Check service logs for errors

## Summary

✅ **Security Features Implemented**:
- API Key authentication on all endpoints except health check
- Custom filter for key validation
- Clear error messages for authentication failures
- Logging of authentication attempts

✅ **Configuration**:
- API Key defined in application.yml
- Environment variable support for production
- Postman collection includes API Key setup

✅ **Usage**:
- Header format: `Authorization: ApiKey <key>`
- Default key: `order-service-secure-api-key-change-in-production`
- Health check (`/orders/ping`) remains public

⚠️ **Production Checklist**:
- [ ] Generate strong, unique API Key
- [ ] Store key in secrets management system
- [ ] Use environment variables, not hardcoded values
- [ ] Enable HTTPS/TLS
- [ ] Implement rate limiting
- [ ] Set up monitoring and alerting
- [ ] Document key rotation process
- [ ] Train team on security best practices

---

**Security Contact**: For security issues, contact the development team immediately.

**Last Updated**: 2025-10-27
