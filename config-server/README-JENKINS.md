# Jenkins CI/CD Configuration - Farmatodo

## 📍 Important Distinction

**config-server** (this directory) = Spring Cloud Config Server application that provides centralized configuration
**config-service** (separate Git repo) = Repository containing YAML/properties configuration files only

## 📁 Jenkins Files Location

All Jenkins CI/CD files are stored in the **config-server** directory:

```
config-server/
├── Jenkinsfile.template         ← Template for creating service-specific pipelines
└── README-JENKINS.md            ← This file
```

### Why config-server?

The config-server acts as the **central configuration provider** for all microservices, making it the logical location for:
- CI/CD pipeline templates
- Shared Jenkins configuration
- Build automation documentation

---

## 🚀 Quick Start

### Copy Template to a Service

```bash
# Example: Create Jenkins pipeline for client-service
cp config-server/Jenkinsfile.template client-service/Jenkinsfile
```

Then edit `client-service/Jenkinsfile` and update:

```groovy
environment {
    SERVICE_NAME = 'client-service'  // Update this
    SERVICE_PORT = '8081'            // Update this
}
```

---

## 📋 Service Configuration Reference

Copy the template and use these configurations for each service:

### Config Server (Port 8888)
```groovy
SERVICE_NAME = 'config-server'
SERVICE_PORT = '8888'
```

### API Gateway (Port 8080)
```groovy
SERVICE_NAME = 'api-gateway'
SERVICE_PORT = '8080'
```

### Client Service (Port 8081)
```groovy
SERVICE_NAME = 'client-service'
SERVICE_PORT = '8081'
```

### Token Service (Port 8082)
```groovy
SERVICE_NAME = 'token-service'
SERVICE_PORT = '8082'
```

### Product Service (Port 8083)
```groovy
SERVICE_NAME = 'product-service'
SERVICE_PORT = '8083'
```

### Cart Service (Port 8084)
```groovy
SERVICE_NAME = 'cart-service'
SERVICE_PORT = '8084'
```

### Order Service (Port 8085)
```groovy
SERVICE_NAME = 'order-service'
SERVICE_PORT = '8085'
```

---

## 🔄 Progressive Migration Strategy

### Phase 1: Config Server (Week 1)

Start by migrating the config-server itself:

```bash
# 1. Copy template
cp config-server/Jenkinsfile.template config-server/Jenkinsfile

# 2. Update SERVICE_NAME and SERVICE_PORT
# (config-server/Jenkinsfile already has correct values)

# 3. Create Jenkins job pointing to config-server/Jenkinsfile

# 4. Test build
```

**Validation:**
- [ ] Config server builds successfully
- [ ] Docker image is created
- [ ] Service starts and passes health check
- [ ] Can connect to config-service Git repository

### Phase 2: Core Services (Week 2)

Add client-service and token-service:

```bash
# Client Service
cp config-server/Jenkinsfile.template client-service/Jenkinsfile
# Edit: SERVICE_NAME='client-service', SERVICE_PORT='8081'

# Token Service
cp config-server/Jenkinsfile.template token-service/Jenkinsfile
# Edit: SERVICE_NAME='token-service', SERVICE_PORT='8082'
```

### Phase 3: Business Services (Week 3)

Add product, cart, and order services:

```bash
cp config-server/Jenkinsfile.template product-service/Jenkinsfile
cp config-server/Jenkinsfile.template cart-service/Jenkinsfile
cp config-server/Jenkinsfile.template order-service/Jenkinsfile
```

### Phase 4: API Gateway (Week 4)

Complete with API Gateway:

```bash
cp config-server/Jenkinsfile.template api-gateway/Jenkinsfile
```

---

## 📖 Complete Documentation

For full setup instructions, see the root-level documentation:

- **`/JENKINS-SETUP.md`** - Complete installation and configuration guide
- **`/JENKINS-QUICKSTART.md`** - 5-minute quick start guide
- **`/Jenkinsfile`** - Root pipeline for building all services

---

## 🎯 Common Tasks

### Create Pipeline for New Service

```bash
# 1. Copy template
cp config-server/Jenkinsfile.template my-new-service/Jenkinsfile

# 2. Edit my-new-service/Jenkinsfile
#    Update SERVICE_NAME and SERVICE_PORT

# 3. Create Jenkins Pipeline job
#    - New Item → Pipeline
#    - Name: my-new-service
#    - Pipeline script from SCM
#    - Script Path: my-new-service/Jenkinsfile

# 4. Build with Parameters
#    - RUN_TESTS: ✓
#    - BUILD_DOCKER_IMAGE: ✓
#    - Click Build
```

### Update Template for All Services

When updating the template, existing service Jenkinsfiles are NOT automatically updated. You need to:

1. Update `config-server/Jenkinsfile.template`
2. Manually update each service's Jenkinsfile with desired changes
3. Or recreate service Jenkinsfiles from updated template

---

## 🏗️ Architecture

```
Farmatodo Project
├── Jenkinsfile                          ← Root pipeline (all services)
├── JENKINS-SETUP.md                     ← Full setup guide
├── JENKINS-QUICKSTART.md                ← Quick reference
│
├── config-server/                       ← THIS DIRECTORY
│   ├── Jenkinsfile.template             ← Template for services
│   ├── README-JENKINS.md                ← This file
│   └── (Spring Boot app files)
│
├── api-gateway/
│   └── Jenkinsfile                      ← Created from template
│
├── client-service/
│   └── Jenkinsfile                      ← Created from template
│
├── token-service/
│   └── Jenkinsfile                      ← Created from template
│
├── product-service/
│   └── Jenkinsfile                      ← Created from template
│
├── cart-service/
│   └── Jenkinsfile                      ← Created from template
│
└── order-service/
    └── Jenkinsfile                      ← Created from template
```

---

## ⚙️ What About config-service?

**config-service** is a separate Git repository (https://github.com/santgodev/farmatodo-config-service) that contains:
- `client-service-dev.yml`
- `client-service-prod.yml`
- `token-service-dev.yml`
- etc.

It does **NOT** need a Jenkinsfile because:
- It's just configuration files (YAML/properties)
- No code to build or test
- No Docker images to create
- Changes are pulled automatically by config-server

---

## 🛠️ Customization

### Service-Specific Build Steps

Edit the service's Jenkinsfile to add custom stages:

```groovy
stage('Custom Database Migration') {
    steps {
        script {
            sh "./mvnw flyway:migrate"
        }
    }
}
```

### Service-Specific Smoke Tests

```groovy
stage('Smoke Tests') {
    steps {
        script {
            // Test specific endpoint
            sh "curl -f http://localhost:${env.SERVICE_PORT}/api/specific-endpoint"
        }
    }
}
```

---

## 📞 Support

For issues or questions:

1. Check Jenkins console output
2. Review `/JENKINS-SETUP.md` for troubleshooting
3. Check service logs: `docker logs <service-name>`
4. Consult `/CLAUDE.md` for service-specific information

---

**Last Updated:** 2025-01-26
**Location:** config-server/README-JENKINS.md
**Template:** config-server/Jenkinsfile.template
