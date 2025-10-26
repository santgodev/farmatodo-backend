# Config Server - Local File System Setup

## ✅ Configuration Complete!

The Config Server has been successfully configured to use **local file system** storage instead of Git repository.

---

## Configuration Files Location

All configuration files are now stored in:
```
config-server/src/main/resources/config/
```

### Current Configuration Files:

1. **order-service.yml** - Default configuration (applies to all profiles)
2. **order-service-dev.yml** - Development environment configuration
3. **order-service-prod.yml** - Production environment configuration

---

## How It Works

### Config Server Setup

The `config-server/src/main/resources/application.properties` is configured with:

```properties
spring.application.name=config-server
server.port=8888

# Use native (local file system) profile
spring.profiles.active=native
spring.cloud.config.server.native.search-locations=classpath:/config/

# Management endpoints
management.endpoints.web.exposure.include=health,info
```

### Configuration Priority

When a service requests configuration, the Config Server merges files in this order:

1. **Lowest Priority**: `{service-name}.yml` - Default config for all environments
2. **Highest Priority**: `{service-name}-{profile}.yml` - Profile-specific config

For example, order-service running with `dev` profile gets:
- Base config from: `order-service.yml`
- Dev-specific config from: `order-service-dev.yml` (overrides base)

---

## Key Configuration in order-service-dev.yml

```yaml
# Database connection (Docker internal network)
spring:
  datasource:
    url: jdbc:postgresql://order-db:5432/orderdb
    username: orderuser
    password: orderpass

# Service URLs (Docker internal network)
services:
  client:
    url: http://client-service:8081
    apiKey: client-service-api-key-change-in-production
  token:
    url: http://token-service:8082
    apiKey: your-secret-api-key-change-in-production
  product:
    url: http://product-service:8083
    apiKey: product-service-api-key-change-in-production
  cart-service:
    url: http://cart-service:8084
    api-key: cart-service-api-key-change-in-production

# Payment configuration
payment:
  rejectionProbability: 0.3
  retryCount: 3
```

---

## Testing Configuration

### 1. Test Config Server Directly

```bash
# Test default configuration
curl http://localhost:8888/order-service/default

# Test dev configuration
curl http://localhost:8888/order-service/dev

# Test prod configuration
curl http://localhost:8888/order-service/prod
```

### 2. Verify Service is Using Config

Check the order-service logs:
```bash
docker-compose logs order-service | grep "Located environment"
```

You should see:
```
Located environment: name=order-service, profiles=[default], label=null, version=null
Located environment: name=order-service, profiles=[dev], label=null, version=null
```

---

## Adding Configuration for Other Services

To add configuration for other services (client-service, token-service, etc.):

### Step 1: Create Configuration Files

Create these files in `config-server/src/main/resources/config/`:

```
config-server/src/main/resources/config/
├── client-service.yml          # Default config
├── client-service-dev.yml      # Dev config
├── client-service-prod.yml     # Prod config
├── token-service.yml           # Default config
├── token-service-dev.yml       # Dev config
├── token-service-prod.yml      # Prod config
├── product-service.yml         # Default config
├── product-service-dev.yml     # Dev config
├── product-service-prod.yml    # Prod config
├── cart-service.yml            # Default config
├── cart-service-dev.yml        # Dev config
├── cart-service-prod.yml       # Prod config
├── api-gateway.yml             # Default config
├── api-gateway-dev.yml         # Dev config
└── api-gateway-prod.yml        # Prod config
```

### Step 2: Rebuild Config Server

```bash
docker-compose build config-server
```

### Step 3: Restart Services

```bash
docker-compose down
docker-compose up -d
```

---

## Advantages of This Setup

✅ **No Git Dependency**: No need for GitHub repository access
✅ **Faster Startup**: No Git clone operation on startup
✅ **Local Development**: Easy to test configuration changes locally
✅ **Version Control**: Config files are still in Git (in the config-server directory)
✅ **Easy Updates**: Just rebuild config-server and restart services

---

## Environment Variable Override

**Important**: Environment variables in `docker-compose.yml` will ALWAYS override configuration from Config Server.

Priority order (highest to lowest):
1. Environment variables in docker-compose.yml
2. Profile-specific config file (e.g., order-service-dev.yml)
3. Default config file (e.g., order-service.yml)

Example:
```yaml
# docker-compose.yml - HIGHEST PRIORITY
environment:
  SERVER_PORT: 8085
  PAYMENT_RETRYCOUNT: 5

# This overrides anything in config files
```

---

## Migration from Git to Local (Completed)

The following changes were made:

### Before (Git-based):
```properties
spring.cloud.config.server.git.uri=https://github.com/santgodev/farmatodo-config-service
spring.cloud.config.server.git.default-label=master
spring.cloud.config.server.git.clone-on-start=true
```

### After (Local file system):
```properties
spring.profiles.active=native
spring.cloud.config.server.native.search-locations=classpath:/config/
```

---

## Troubleshooting

### Config Server Not Returning Configuration

1. Check config server logs:
   ```bash
   docker-compose logs config-server
   ```

2. Verify files exist in the container:
   ```bash
   docker exec farmatodo-config-server ls -la /app/classes/config/
   ```

3. Rebuild config-server if you added/changed files:
   ```bash
   docker-compose build config-server
   docker-compose up -d config-server
   ```

### Services Not Picking Up Configuration

1. Restart config-server first:
   ```bash
   docker-compose restart config-server
   ```

2. Wait for config-server to be healthy:
   ```bash
   docker-compose ps config-server
   ```

3. Then restart the service:
   ```bash
   docker-compose restart order-service
   ```

---

## Next Steps

1. **Add configurations for other services** following the same pattern
2. **Remove application.yml** from order-service if all config is in config-server
3. **Test different profiles** by changing `SPRING_PROFILES_ACTIVE` environment variable

---

## Verification Checklist

- [x] Config Server using native profile
- [x] Configuration files in `config-server/src/main/resources/config/`
- [x] order-service.yml (default) created
- [x] order-service-dev.yml created with Docker service names
- [x] order-service-prod.yml created with environment variable support
- [x] Config Server rebuilt and restarted
- [x] Order Service successfully loading configuration
- [x] Order Service responding to requests (ping endpoint working)

**Status**: ✅ Configuration Server is fully operational with local file system!
