# Jenkins CI/CD - Quick Start Guide

## 🚀 5-Minute Setup

### Prerequisites Checklist
- [ ] Jenkins installed and running (http://localhost:8080)
- [ ] Docker installed and running
- [ ] Java 17 installed
- [ ] Git repository cloned

---

## Step 1: Install Jenkins Plugins (5 minutes)

Go to **Manage Jenkins > Manage Plugins > Available**

Search and install:
```
✓ Pipeline
✓ Git Plugin
✓ Docker Pipeline
✓ AnsiColor
✓ Timestamper
✓ JUnit
```

Restart Jenkins after installation.

---

## Step 2: Configure Credentials (2 minutes)

Go to **Manage Jenkins > Manage Credentials > (global) > Add Credentials**

### Add Docker Hub Credentials:
- Kind: **Username with password**
- ID: `docker-hub-credentials`
- Username: Your Docker Hub username
- Password: Your Docker Hub password

### Add Docker Registry URL:
- Kind: **Secret text**
- ID: `docker-registry-url`
- Secret: `docker.io`

---

## Step 3: Create Pipeline Job (3 minutes)

### For All Services (Root Pipeline):

1. **New Item** > Name: `Farmatodo-Microservices`
2. Select **Multibranch Pipeline**
3. **Branch Sources** > Add Git:
   - Repository URL: Your Git repo URL
   - Credentials: (select if private)
4. **Build Configuration**:
   - Script Path: `Jenkinsfile`
5. **Save**

Jenkins will automatically scan and build!

### For Single Service:

1. **New Item** > Name: `client-service`
2. Select **Pipeline**
3. **Pipeline** section:
   - Definition: **Pipeline script from SCM**
   - SCM: Git
   - Repository URL: Your repo URL
   - Script Path: `client-service/Jenkinsfile`
4. **Save**

---

## Step 4: First Build (1 minute)

### Build All Services:
1. Go to `Farmatodo-Microservices` job
2. Click **Build with Parameters**
3. Settings:
   - BUILD_SCOPE: **ALL**
   - RUN_TESTS: ✓
   - BUILD_DOCKER_IMAGES: ✓
   - PUSH_TO_REGISTRY: ✗ (skip for first run)
   - DEPLOY_TO_ENV: ✗ (skip for first run)
4. Click **Build**

### Monitor Progress:
- Click on build number (e.g., #1)
- Click **Console Output**
- Watch the build progress

---

## Progressive Migration Checklist

### ✅ Phase 1: Config Server (Week 1)
- [ ] Update `Jenkinsfile` to build config-server only
- [ ] Run successful build
- [ ] Verify Docker image created
- [ ] Test deployment locally

### ✅ Phase 2: Core Services (Week 2)
- [ ] Add client-service to build
- [ ] Add token-service to build
- [ ] Verify both services build successfully
- [ ] Test inter-service communication

### ✅ Phase 3: Business Services (Week 3)
- [ ] Add product-service
- [ ] Add cart-service
- [ ] Add order-service
- [ ] Run integration tests

### ✅ Phase 4: API Gateway (Week 4)
- [ ] Add api-gateway
- [ ] Test complete system end-to-end
- [ ] Configure production deployment

---

## Common Commands

### Manual Maven Build (Windows):
```bash
cd client-service
mvnw.cmd clean package
```

### Manual Maven Build (Linux/Mac):
```bash
cd client-service
./mvnw clean package
```

### Build Docker Image:
```bash
cd client-service
docker build -t farmatodo/client-service:dev-latest .
```

### Run Local Tests:
```bash
cd client-service
mvnw.cmd test
```

### View Docker Logs:
```bash
docker logs client-service
```

### Deploy with Docker Compose:
```bash
docker-compose up -d
```

---

## Pipeline Parameters Quick Reference

| Parameter | When to Enable | Default |
|-----------|---------------|---------|
| **RUN_TESTS** | Always (except hotfixes) | ✓ |
| **RUN_INTEGRATION_TESTS** | For staging/prod builds | ✗ |
| **BUILD_DOCKER_IMAGES** | Always | ✓ |
| **PUSH_TO_REGISTRY** | For staging/prod only | ✗ |
| **DEPLOY_TO_ENV** | After successful tests | ✗ |

---

## Build Scope Options

- **ALL** - Build all configured services (default)
- **CONFIG_SERVER** - Config server only
- **API_GATEWAY** - API Gateway only
- **CLIENT_SERVICE** - Client service only
- **TOKEN_SERVICE** - Token service only
- **PRODUCT_SERVICE** - Product service only
- **CART_SERVICE** - Cart service only
- **ORDER_SERVICE** - Order service only

---

## Health Check URLs

After deployment, verify services:

```bash
# Config Server
curl http://localhost:8888/actuator/health

# API Gateway
curl http://localhost:8080/api/gateway/health

# Client Service
curl http://localhost:8081/api/clients/health

# Token Service
curl http://localhost:8082/api/tokens/health

# Product Service
curl http://localhost:8083/products/health

# Cart Service
curl http://localhost:8084/carts/health

# Order Service
curl http://localhost:8085/orders/ping
```

---

## Troubleshooting Quick Fixes

### Build Fails with "mvnw not found"
```bash
git update-index --chmod=+x mvnw
chmod +x mvnw
```

### Docker Permission Denied (Linux)
```bash
sudo usermod -aG docker jenkins
sudo systemctl restart jenkins
```

### Out of Memory Error
Update Jenkinsfile:
```groovy
environment {
    MAVEN_OPTS = '-Xmx2048m -Xms1024m'
}
```

### Service Won't Start
```bash
# Check logs
docker logs <service-name>

# Restart service
docker-compose restart <service-name>
```

---

## Next Steps After First Build

1. ✅ Configure email/Slack notifications
2. ✅ Set up SonarQube for code quality
3. ✅ Configure automatic deployment for develop branch
4. ✅ Set up production deployment approval
5. ✅ Create backup/rollback procedures

---

## Example: Migrate Client Service

### 1. Copy Template
```bash
cp config-server/Jenkinsfile.template client-service/Jenkinsfile
```

### 2. Update Configuration
Edit `client-service/Jenkinsfile`:
```groovy
environment {
    SERVICE_NAME = 'client-service'
    SERVICE_PORT = '8081'
    // ... rest of config
}
```

### 3. Create Jenkins Job
- New Item: `client-service`
- Type: Pipeline
- Script Path: `client-service/Jenkinsfile`

### 4. Build
- Build with Parameters
- RUN_TESTS: ✓
- BUILD_DOCKER_IMAGE: ✓
- Build!

### 5. Verify
```bash
docker images | grep client-service
curl http://localhost:8081/api/clients/health
```

---

## Daily Workflow

### Developer Commits Code:
```bash
git add .
git commit -m "feat: add new feature"
git push origin develop
```

### Jenkins Automatically:
1. ✓ Detects commit via webhook/polling
2. ✓ Builds service
3. ✓ Runs tests
4. ✓ Creates Docker image
5. ✓ Deploys to dev environment (if configured)
6. ✓ Sends notification

### Production Deployment:
```bash
# Merge to master
git checkout master
git merge develop
git push origin master

# Jenkins builds and creates images
# Manual approval for production deployment
# Click "Proceed" in Jenkins UI
```

---

## Monitoring Build Status

### Jenkins Dashboard:
- **Blue** = Success ✓
- **Red** = Failure ✗
- **Yellow** = Unstable ⚠
- **Gray** = Not built yet

### Console Output:
- Click build number
- Click "Console Output"
- Search for "ERROR" or "FAILURE"

### Email Notifications:
- Configure in JENKINS-SETUP.md
- Receive notifications on build status

---

## Emergency Rollback

### Via Docker Compose:
```bash
# Stop current version
docker-compose down

# Edit docker-compose.yml to use previous tag
# Change: farmatodo/client-service:dev-latest
# To: farmatodo/client-service:dev-<previous-commit>

# Start previous version
docker-compose up -d
```

### Via Docker:
```bash
# Find previous image
docker images farmatodo/client-service

# Stop current container
docker stop client-service

# Run previous version
docker run -d --name client-service \
  farmatodo/client-service:dev-<previous-tag>
```

---

## Resources

- **Full Setup Guide:** See `JENKINS-SETUP.md`
- **Project Documentation:** See `CLAUDE.md`
- **API Testing:** See `POSTMAN-GUIDE.md`
- **Jenkins Docs:** https://www.jenkins.io/doc/

---

**Need Help?**
1. Check build console output
2. Review service logs: `docker logs <service>`
3. Consult JENKINS-SETUP.md for detailed troubleshooting
4. Check Jenkins community forums

**Last Updated:** 2025-01-26
