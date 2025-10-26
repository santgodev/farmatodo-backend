# Product Search API Documentation

## Overview

The Product Search API allows users to search for products by name or description. All searches are logged asynchronously to ensure fast response times. The system is designed to be easily migrated to GCP Pub/Sub for scalable event processing.

## Endpoint

### GET /products

Search for products by name or description.

**Query Parameters:**
- `query` (optional): Search term to find in product name or description. Defaults to empty string (returns all products).

**Headers:**
- `Authorization: ApiKey <api-key>` (required for authenticated access)

**Example Request:**
```bash
curl "http://localhost:8081/products?query=aspirin" \
  -H "Authorization: ApiKey client-service-api-key-change-in-production"
```

**Example Response (200 OK):**
```json
{
  "query": "aspirin",
  "totalResults": 2,
  "products": [
    {
      "id": 1,
      "name": "Aspirin 500mg",
      "description": "Pain relief medication",
      "price": 10.99,
      "stock": 100,
      "category": "Medications",
      "sku": "ASP-500"
    },
    {
      "id": 2,
      "name": "Aspirin Plus 100mg",
      "description": "Low dose aspirin for heart health",
      "price": 8.50,
      "stock": 75,
      "category": "Medications",
      "sku": "ASP-100"
    }
  ]
}
```

## Features

### 1. Stock Filtering

Only products with stock greater than the configured `product.minStock` value are returned.

**Configuration** (`application.yml`):
```yaml
product:
  minStock: 0  # Only return products with stock > 0
```

To filter out products with low stock:
```yaml
product:
  minStock: 10  # Only show products with more than 10 units
```

### 2. Asynchronous Search Logging

All searches are logged asynchronously to the database without delaying the HTTP response.

**Logged Information:**
- Search term
- Number of results found
- User identifier (IP address or user ID)
- Transaction ID (for request tracing)
- Timestamp

**Database Table:** `search_logs`

**Benefits:**
- Fast API responses (no blocking I/O)
- Analytics data for popular searches
- User behavior tracking
- A/B testing capability

### 3. Case-Insensitive Search

Searches are case-insensitive and match partial terms in both product names and descriptions.

**Examples:**
- `aspirin` matches "Aspirin", "ASPIRIN", "Baby Aspirin"
- `pain` matches products with "pain" in name or description

### 4. Active Products Only

Only products with `status = 'ACTIVE'` are included in search results.

## Architecture

### Component Overview

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │ GET /products?query=term
       ▼
┌─────────────────────┐
│ ProductController   │ ◄── Receives request
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│  ProductService     │ ◄── Searches DB + publishes event
└──────┬──────────────┘
       │
       ├─► [Synchronous]  ──► Database Search ──► Return Results
       │
       └─► [Asynchronous] ──► SearchEventPublisher ──► SearchLogService ──► Save to DB
```

### Asynchronous Processing

**Current Implementation:**
- Uses Spring's `@Async` with `ApplicationEventPublisher`
- Separate thread pool for search logging
- Non-blocking - HTTP response returns immediately

**Thread Pool Configuration:**
```java
Core Pool Size: 2
Max Pool Size: 5
Queue Capacity: 100
Thread Name Prefix: async-search-
```

## Migration to GCP Pub/Sub

The system is designed to be easily migrated to GCP Pub/Sub. Here's how:

### Step 1: Add GCP Dependencies

Add to `pom.xml`:
```xml
<dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>spring-cloud-gcp-starter-pubsub</artifactId>
</dependency>
```

### Step 2: Create GCP Pub/Sub Publisher

Create `GcpPubSubSearchEventPublisher.java`:

```java
package com.farmatodo.client_service.event.impl;

import com.farmatodo.client_service.event.SearchEvent;
import com.farmatodo.client_service.event.SearchEventPublisher;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("gcp")
@RequiredArgsConstructor
public class GcpPubSubSearchEventPublisher implements SearchEventPublisher {

    private final PubSubTemplate pubSubTemplate;

    private static final String TOPIC_NAME = "search-events";

    @Override
    public void publishSearchEvent(SearchEvent event) {
        pubSubTemplate.publish(TOPIC_NAME, event);
    }
}
```

### Step 3: Create GCP Pub/Sub Subscriber

Create `GcpPubSubSearchSubscriber.java`:

```java
package com.farmatodo.client_service.event.subscriber;

import com.farmatodo.client_service.event.SearchEvent;
import com.farmatodo.client_service.service.SearchLogService;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@Profile("gcp")
@RequiredArgsConstructor
public class GcpPubSubSearchSubscriber {

    private final SearchLogService searchLogService;

    @ServiceActivator(inputChannel = "searchEventsChannel")
    public void handleSearchEvent(
            SearchEvent event,
            @Header(GcpPubSubHeaders.ORIGINAL_MESSAGE) BasicAcknowledgeablePubsubMessage message) {

        try {
            searchLogService.handleSearchEvent(event);
            message.ack();
        } catch (Exception e) {
            message.nack();
        }
    }
}
```

### Step 4: Update SearchLogService

Remove `@EventListener` annotation when using GCP:

```java
// Keep method but remove @EventListener for GCP mode
@Async
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void handleSearchEvent(SearchEvent event) {
    // ... existing implementation
}
```

### Step 5: Add Profile Configuration

Update `LocalSearchEventPublisher.java`:
```java
@Component
@Profile("!gcp")  // Only active when NOT running on GCP
@RequiredArgsConstructor
public class LocalSearchEventPublisher implements SearchEventPublisher {
    // ... existing implementation
}
```

### Step 6: Configure GCP

Add to `application-gcp.yml`:
```yaml
spring:
  profiles: gcp
  cloud:
    gcp:
      project-id: your-gcp-project-id
      pubsub:
        subscriber:
          parallel-pull-count: 1
          max-ack-extension-period: 600
```

### Step 7: Deploy

```bash
# Run locally (uses Spring Events)
java -jar client-service.jar

# Run on GCP (uses Pub/Sub)
java -jar client-service.jar --spring.profiles.active=gcp
```

## Testing

### Run Unit Tests

```bash
cd client-service
mvnw.cmd test -Dtest=ProductServiceTest
```

### Manual Testing

#### 1. Create Sample Products

First, insert some test data (you'll need to create an endpoint or use SQL):

```sql
INSERT INTO products (name, description, price, stock, category, sku, status, created_at, updated_at)
VALUES
  ('Aspirin 500mg', 'Pain relief medication', 10.99, 100, 'Medications', 'ASP-500', 'ACTIVE', NOW(), NOW()),
  ('Ibuprofen 400mg', 'Anti-inflammatory pain relief', 12.50, 50, 'Medications', 'IBU-400', 'ACTIVE', NOW(), NOW()),
  ('Vitamin C 1000mg', 'Immune system support', 15.99, 200, 'Vitamins', 'VIT-C-1000', 'ACTIVE', NOW(), NOW()),
  ('Band-Aid Pack', 'Assorted adhesive bandages', 5.99, 5, 'First Aid', 'BA-PACK', 'ACTIVE', NOW(), NOW()),
  ('Out of Stock Item', 'This should not appear', 9.99, 0, 'Other', 'OOS-001', 'ACTIVE', NOW(), NOW());
```

#### 2. Test Search

```bash
# Search for "pain"
curl "http://localhost:8081/products?query=pain" \
  -H "Authorization: ApiKey client-service-api-key-change-in-production"

# Search for "vitamin"
curl "http://localhost:8081/products?query=vitamin" \
  -H "Authorization: ApiKey client-service-api-key-change-in-production"

# Search all products (empty query)
curl "http://localhost:8081/products" \
  -H "Authorization: ApiKey client-service-api-key-change-in-production"
```

#### 3. Verify Stock Filtering

With `product.minStock: 0` - should return 4 products (excluding out-of-stock)

Change to `product.minStock: 10` and restart - should return only 3 products (excluding band-aids with stock=5)

#### 4. Check Search Logs

```sql
SELECT * FROM search_logs ORDER BY searched_at DESC LIMIT 10;
```

Should see all searches logged with:
- Search term
- Results count
- User IP address
- Transaction ID
- Timestamp

## Performance Considerations

### Response Time

- **Target**: < 200ms for typical searches
- **Async logging**: Adds ~0ms to response time (non-blocking)
- **Database query**: Primary performance factor

### Optimization Tips

1. **Database Indexes**
   - `idx_name` on products.name
   - `idx_stock` on products.stock
   - `idx_status` on products.status

2. **Query Performance**
   ```sql
   -- Check query execution plan
   EXPLAIN ANALYZE
   SELECT * FROM products
   WHERE (LOWER(name) LIKE '%aspirin%' OR LOWER(description) LIKE '%aspirin%')
   AND stock > 0
   AND status = 'ACTIVE';
   ```

3. **Caching** (future enhancement)
   - Add Redis for popular searches
   - Cache results for 1-5 minutes
   - Invalidate on product updates

4. **Pagination** (future enhancement)
   - Add `page` and `size` parameters
   - Return max 50 results per page

## Monitoring

### Key Metrics

1. **Search Performance**
   - Average response time
   - P95/P99 latency
   - Queries per second

2. **Search Analytics**
   - Top search terms
   - Searches with zero results
   - Average results per search

3. **Async Processing**
   - Event publishing success rate
   - Search log write latency
   - Async thread pool utilization

### Logging

All searches are logged with transaction IDs for tracing:

```
2025-10-23 10:30:45 [http-nio-8081-exec-1] [a3f8b2c1-4d5e] INFO  ProductService - Searching products with query: 'aspirin', minStock: 0, transaction: a3f8b2c1-4d5e
2025-10-23 10:30:45 [async-search-1] [a3f8b2c1-4d5e] INFO  SearchLogService - Logging search asynchronously - Term: 'aspirin', Results: 2, Transaction: a3f8b2c1-4d5e
```

## Configuration Reference

### application.yml

```yaml
# Product search configuration
product:
  minStock: 0  # Minimum stock level for products to appear in search

# Async configuration (in AsyncConfig.java)
# - Core pool size: 2
# - Max pool size: 5
# - Queue capacity: 100

# API authentication
api:
  key: client-service-api-key-change-in-production
```

## Error Handling

### Search Failures

If database search fails:
- Returns HTTP 500 with error details
- Error logged with transaction ID
- Search log NOT created

### Async Logging Failures

If search logging fails:
- HTTP response still succeeds (non-blocking)
- Error logged but not propagated to user
- Search analytics may have gaps

### Best Practices

1. **Monitor async failures** - Set up alerts for SearchLogService errors
2. **Dead letter queue** - When using Pub/Sub, configure DLQ for failed messages
3. **Retry logic** - Consider retry with exponential backoff for transient failures

## Future Enhancements

1. **Full-Text Search** - PostgreSQL FTS or Elasticsearch
2. **Faceted Search** - Filter by category, price range, etc.
3. **Autocomplete** - Suggest products as user types
4. **Personalization** - Rank results based on user history
5. **Spell Correction** - Handle typos in search terms
6. **Search Analytics Dashboard** - Visualize trends and patterns

## Support

For questions or issues:
- Check logs with transaction ID
- Review search_logs table for analytics
- Monitor async thread pool for bottlenecks

---

**Version**: 1.0
**Last Updated**: 2025-10-23
