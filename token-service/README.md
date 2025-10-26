# Token Service

A secure credit card tokenization service built with Spring Boot that validates, encrypts, and stores credit card information, returning secure tokens for future reference.

## Features

- **Card Validation**: Luhn algorithm validation and format checks
- **AES-GCM Encryption**: Military-grade encryption for sensitive card data
- **API Key Authentication**: Secure endpoint access control
- **Request Tracing**: UUID-based transaction IDs with MDC logging
- **Configurable Rejection**: Simulate token rejection for testing
- **Comprehensive Error Handling**: Detailed error responses with transaction tracking
- **PostgreSQL Persistence**: Reliable data storage with JPA
- **Extensive Testing**: Unit tests for all major scenarios

## Table of Contents

- [Quick Start](#quick-start)
- [API Documentation](#api-documentation)
- [Configuration](#configuration)
- [Testing](#testing)
- [Architecture](#architecture)
- [Security](#security)
- [Development](#development)

## Quick Start

### Prerequisites

- Java 17+
- PostgreSQL 13+
- Maven 3.6+

### 1. Setup Database

```bash
# Using Docker
docker run --name tokendb \
  -e POSTGRES_DB=tokendb \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5433:5432 \
  -d postgres

# Or create database manually
psql -U postgres -c "CREATE DATABASE tokendb;"
```

### 2. Configure Application

Edit `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/tokendb
    username: postgres
    password: postgres

api:
  key: your-secret-api-key-change-in-production

encryption:
  secret:
    key: MySecretKey1234567890123456789012  # Change in production!

token:
  rejectionProbability: 0.0  # 0.0 = never reject, 1.0 = always reject
```

### 3. Build and Run

```bash
# Build
mvnw.cmd clean package

# Run
mvnw.cmd spring-boot:run
```

Service will start on `http://localhost:8082`

### 4. Test with curl

```bash
# Ping
curl http://localhost:8082/ping

# Tokenize
curl -X POST http://localhost:8082/tokenize \
  -H "Content-Type: application/json" \
  -H "Authorization: ApiKey your-secret-api-key-change-in-production" \
  -d '{
    "cardNumber": "4111111111111111",
    "cvv": "123",
    "expiry": "12/28",
    "cardholderName": "John Doe"
  }'
```

## API Documentation

### Endpoints

#### 1. GET /ping

Simple health check endpoint (no authentication required).

**Response:**
```
pong
```

#### 2. POST /tokenize

Tokenize credit card information.

**Headers:**
```
Content-Type: application/json
Authorization: ApiKey <your-api-key>
```

**Request Body:**
```json
{
  "cardNumber": "4111111111111111",
  "cvv": "123",
  "expiry": "12/28",
  "cardholderName": "John Doe"
}
```

**Success Response (200 OK):**
```json
{
  "token": "a3f8b2c1-4d5e-6f7g-8h9i-0j1k2l3m4n5o",
  "last4": "1111",
  "status": "ACTIVE",
  "createdAt": "2025-10-22T10:30:45.123"
}
```

**Headers:**
```
X-Transaction-Id: b4c7d8e9-1f2a-3b4c-5d6e-7f8g9h0i1j2k
```

#### 3. GET /api/tokens/health

Detailed health check including database status (no authentication required).

**Response:**
```json
{
  "service": "token-service",
  "status": "UP",
  "timestamp": "2025-10-22T10:30:45.123",
  "message": "Token Service is running successfully",
  "database": "Connected",
  "databaseProductName": "PostgreSQL"
}
```

#### 4. GET /api/tokens/info

Service information (no authentication required).

**Response:**
```json
{
  "service": "token-service",
  "version": "1.0.0",
  "description": "Farmatodo Token Service - Manages authentication tokens",
  "database": "PostgreSQL",
  "endpoints": [
    "GET /api/tokens/health - Health check with database status",
    "GET /api/tokens/info - Service information"
  ]
}
```

### Validation Rules

| Field | Rules |
|-------|-------|
| `cardNumber` | 13-19 digits, must pass Luhn algorithm validation |
| `cvv` | 3-4 digits |
| `expiry` | Format MM/YY (e.g., 12/28), valid month (01-12) |
| `cardholderName` | Minimum 2 characters, required |

### Error Responses

#### 400 Bad Request - Invalid Card Number
```json
{
  "errorCode": "INVALID_CARD_NUMBER",
  "message": "Card number failed Luhn validation",
  "timestamp": "2025-10-22T10:30:45.123",
  "transactionId": "b4c7d8e9-1f2a-3b4c-5d6e-7f8g9h0i1j2k"
}
```

#### 400 Bad Request - Invalid CVV
```json
{
  "errorCode": "INVALID_CVV",
  "message": "CVV must be 3-4 digits",
  "timestamp": "2025-10-22T10:30:45.123",
  "transactionId": "c5d8e9f0-2g3h-4i5j-6k7l-8m9n0o1p2q3r"
}
```

#### 400 Bad Request - Invalid Expiry
```json
{
  "errorCode": "INVALID_EXPIRY",
  "message": "Expiry must be in format MM/YY",
  "timestamp": "2025-10-22T10:30:45.123",
  "transactionId": "d6e9f0g1-3h4i-5j6k-7l8m-9n0o1p2q3r4s"
}
```

#### 401 Unauthorized - Missing/Invalid API Key
```json
{
  "errorCode": "UNAUTHORIZED",
  "message": "Missing Authorization header",
  "timestamp": "2025-10-22T10:30:45.123",
  "transactionId": "e7f0g1h2-4i5j-6k7l-8m9n-0o1p2q3r4s5t"
}
```

#### 422 Unprocessable Entity - Token Rejected
```json
{
  "errorCode": "TOKEN_REJECTED",
  "message": "Token generation was rejected",
  "timestamp": "2025-10-22T10:30:45.123",
  "transactionId": "f8g1h2i3-5j6k-7l8m-9n0o-1p2q3r4s5t6u"
}
```

### Valid Test Card Numbers

These card numbers pass Luhn validation:

```
Visa:
- 4111111111111111
- 4532015112830366

Mastercard:
- 5425233430109903
- 5105105105105100

American Express:
- 378282246310005
- 371449635398431
```

## Configuration

### Application Properties

Located in `src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: token-service

  datasource:
    url: jdbc:postgresql://localhost:5433/tokendb
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update  # Change to 'validate' in production
    show-sql: true      # Set to false in production
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

server:
  port: 8082

# API Key for authentication
api:
  key: your-secret-api-key-change-in-production

# Encryption secret key (256-bit)
encryption:
  secret:
    key: MySecretKey1234567890123456789012

# Token rejection probability (0.0 to 1.0)
token:
  rejectionProbability: 0.0

# Logging configuration
logging:
  level:
    com.farmatodo.token_service: DEBUG
    org.springframework.web: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] [%X{transactionId}] %-5level %logger{36} - %msg%n"
```

### Environment Variables

You can override configuration using environment variables:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://prod-db:5432/tokendb
export SPRING_DATASOURCE_USERNAME=prod_user
export SPRING_DATASOURCE_PASSWORD=prod_password
export API_KEY=production-secure-api-key
export ENCRYPTION_SECRET_KEY=ProductionEncryptionKey123456789
export TOKEN_REJECTIONPROBABILITY=0.0
```

## Testing

### Unit Tests

Run all tests:
```bash
mvnw.cmd test
```

Run specific test class:
```bash
mvnw.cmd test -Dtest=TokenServiceTest
```

### Integration Testing

#### Option 1: PowerShell Script (Windows)

```powershell
.\test-token-service.ps1
```

#### Option 2: Bash Script (Linux/Mac)

```bash
chmod +x test-token-service.sh
./test-token-service.sh
```

#### Option 3: Postman Collection

1. Import `Token-Service.postman_collection.json` into Postman
2. Configure variables:
   - `baseUrl`: http://localhost:8082
   - `apiKey`: your-secret-api-key-change-in-production
3. Run collection

#### Option 4: Manual curl Commands

See individual test cases in the testing scripts.

## Architecture

### Package Structure

```
com.farmatodo.token_service/
├── config/
│   ├── ApiKeyFilter.java          # API key authentication
│   ├── MdcFilter.java              # Transaction ID logging
│   └── SecurityConfig.java         # Security configuration
├── controller/
│   ├── HealthController.java       # Health & info endpoints
│   └── TokenController.java        # Tokenization endpoints
├── dto/
│   ├── CardRequestDTO.java         # Request payload
│   └── TokenResponseDTO.java       # Response payload
├── exception/
│   ├── BusinessException.java      # Custom business exception
│   ├── ErrorResponse.java          # Error response DTO
│   └── GlobalExceptionHandler.java # Global exception handler
├── model/
│   └── TokenizedCard.java          # JPA entity
├── repository/
│   └── TokenRepository.java        # Data access layer
├── service/
│   └── TokenService.java           # Business logic
└── util/
    ├── CardValidator.java          # Card validation & Luhn
    └── EncryptionUtil.java         # AES-GCM encryption
```

### Database Schema

**Table: `tokenized_cards`**

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT |
| token | VARCHAR | NOT NULL, UNIQUE |
| last4 | VARCHAR | NOT NULL |
| card_hash_or_cipher | TEXT | NOT NULL |
| status | VARCHAR | NOT NULL |
| created_at | TIMESTAMP | NOT NULL |

### Request Flow

1. **MDC Filter**: Generates UUID `transactionId`, adds to MDC and response headers
2. **API Key Filter**: Validates `Authorization: ApiKey <key>` header
3. **Controller**: Receives request, delegates to service
4. **Service**:
   - Validates card using Luhn algorithm and format checks
   - Checks rejection probability
   - Encrypts card data using AES-GCM
   - Generates UUID token
   - Persists to database
5. **Response**: Returns token, last4 digits, status, and timestamp

## Security

### Encryption

- **Algorithm**: AES-GCM (Galois/Counter Mode)
- **Key Size**: 256-bit
- **IV**: 12-byte random IV per encryption
- **Tag**: 128-bit authentication tag

**Encrypted Data Format:**
```
[12-byte IV][encrypted data + 16-byte auth tag]
```

Stored as Base64 in database.

### API Key Authentication

- Header format: `Authorization: ApiKey <key>`
- Constant-time comparison (timing attack resistant)
- Public endpoints: `/ping`, `/api/tokens/health`, `/api/tokens/info`

### Best Practices

1. **Change default API key** in production
2. **Use strong encryption key** (32+ random characters)
3. **Enable HTTPS** in production
4. **Set `ddl-auto: validate`** in production
5. **Disable SQL logging** in production (`show-sql: false`)
6. **Use environment variables** for secrets
7. **Rotate API keys** regularly
8. **Monitor transaction logs** for suspicious activity

## Development

### Adding New Validation Rules

Edit `src/main/java/com/farmatodo/token_service/util/CardValidator.java`

### Changing Encryption Algorithm

Edit `src/main/java/com/farmatodo/token_service/util/EncryptionUtil.java`

### Adding New Endpoints

1. Add method to `TokenController.java`
2. Update `ApiKeyFilter.java` if endpoint should be public
3. Add tests to `TokenServiceTest.java`

### Logging

All requests include `transactionId` in MDC context:

```java
logger.info("Processing request");
// Output: [a3f8b2c1-4d5e-6f7g-8h9i] INFO  TokenService - Processing request
```

### Building for Production

```bash
# Build JAR
mvnw.cmd clean package -DskipTests

# JAR location
target/token-service-0.0.1-SNAPSHOT.jar

# Run
java -jar target/token-service-0.0.1-SNAPSHOT.jar
```

## Troubleshooting

### Database Connection Issues

```
Error: Connection refused
```

**Solution:**
- Ensure PostgreSQL is running: `docker ps` or `pg_ctl status`
- Check port: Default is 5433, verify in `application.yml`
- Verify credentials

### API Key Authentication Failures

```json
{"errorCode": "UNAUTHORIZED", "message": "Invalid API key"}
```

**Solution:**
- Ensure header is `Authorization: ApiKey <key>` (not `Bearer`)
- Check API key matches `application.yml`
- No extra spaces in header value

### Luhn Validation Failures

```json
{"errorCode": "INVALID_CARD_NUMBER", "message": "Card number failed Luhn validation"}
```

**Solution:**
- Use test card numbers from [Valid Test Card Numbers](#valid-test-card-numbers)
- Verify card number is 13-19 digits
- Check for typos

## License

© 2025 Farmatodo. All rights reserved.

## Support

For issues or questions, contact the development team or open an issue in the project repository.
