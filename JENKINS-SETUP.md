# Jenkins CI/CD Pipeline Setup - Farmatodo Microservices

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Jenkins Installation](#jenkins-installation)
4. [Jenkins Configuration](#jenkins-configuration)
5. [Pipeline Setup](#pipeline-setup)
6. [Progressive Migration Strategy](#progressive-migration-strategy)
7. [Pipeline Usage](#pipeline-usage)
8. [Troubleshooting](#troubleshooting)

---

## Overview

This document provides complete instructions for setting up Jenkins CI/CD pipelines for the Farmatodo microservices project. The setup supports **progressive migration**, allowing you to onboard services to the CI/CD pipeline incrementally.

### Pipeline Features

- ✅ **Parallel Builds** - Multiple services built simultaneously
- ✅ **Automated Testing** - Unit and integration tests
- ✅ **Docker Image Building** - Containerization for each service
- ✅ **Multi-Environment Support** - Dev, Staging, Production
- ✅ **Health Checks** - Automated deployment verification
- ✅ **Progressive Migration** - Migrate services incrementally
- ✅ **Smoke Tests** - Post-deployment validation

### Available Pipelines

1. **Root Pipeline** (`Jenkinsfile`) - Builds all services or selected subset
2. **Service-Specific Pipeline** (`config-server/Jenkinsfile.template`) - Individual service builds

---

## Prerequisites

### 1. Software Requirements

- **Jenkins** 2.400+ (LTS recommended)
- **Java** 17 (same as microservices)
- **Maven** 3.8+
- **Docker** 20.10+
- **Docker Compose** 2.0+
- **Git** 2.30+

### 2. Jenkins Plugins Required

Install the following plugins via Jenkins Plugin Manager (`Manage Jenkins > Manage Plugins`):

```
✓ Pipeline (workflow-aggregator)
✓ Git Plugin
✓ Docker Pipeline Plugin
✓ Docker Commons Plugin
✓ AnsiColor Plugin
✓ Timestamper Plugin
✓ JUnit Plugin
✓ Pipeline: Stage View Plugin
✓ Blue Ocean (optional, for better UI)
✓ Credentials Binding Plugin
```

### 3. System Requirements

- **Disk Space:** Minimum 50GB (for builds, tests, Docker images)
- **RAM:** Minimum 8GB (16GB recommended)
- **CPU:** Minimum 4 cores

---

## Jenkins Installation

### Option 1: Docker Installation (Recommended for Testing)

```bash
# Create Jenkins volume for persistence
docker volume create jenkins_home

# Run Jenkins with Docker support
docker run -d \
  --name jenkins \
  -p 8080:8080 \
  -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  jenkins/jenkins:lts-jdk17

# Get initial admin password
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

### Option 2: Windows Installation

1. Download Jenkins from https://www.jenkins.io/download/
2. Run the installer: `jenkins.msi`
3. Follow the setup wizard
4. Install Java 17 if not already installed
5. Configure Jenkins to use Java 17

### Option 3: Linux Installation (Ubuntu/Debian)

```bash
# Add Jenkins repository
curl -fsSL https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key | sudo tee \
  /usr/share/keyrings/jenkins-keyring.asc > /dev/null

echo deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] \
  https://pkg.jenkins.io/debian-stable binary/ | sudo tee \
  /etc/apt/sources.list.d/jenkins.list > /dev/null

# Install Jenkins
sudo apt-get update
sudo apt-get install jenkins

# Start Jenkins
sudo systemctl start jenkins
sudo systemctl enable jenkins
```

---

## Jenkins Configuration

### Step 1: Initial Setup

1. Access Jenkins at `http://localhost:8080`
2. Enter the initial admin password
3. Install suggested plugins
4. Create admin user
5. Configure Jenkins URL

### Step 2: Configure JDK

1. Go to `Manage Jenkins > Global Tool Configuration`
2. Add JDK 17:
   - Name: `JDK17`
   - JAVA_HOME: Path to Java 17 installation
   - Or enable "Install automatically"

### Step 3: Configure Maven

1. In `Global Tool Configuration`, add Maven:
   - Name: `Maven 3.9`
   - Version: 3.9.6 (or latest)
   - Install automatically: ✓

### Step 4: Configure Docker

1. Ensure Docker is accessible to Jenkins user:

   **Linux:**
   ```bash
   sudo usermod -aG docker jenkins
   sudo systemctl restart jenkins
   ```

   **Windows:**
   - Ensure Docker Desktop is running
   - Share Docker socket with Jenkins

2. Test Docker access in Jenkins:
   - Go to `Manage Jenkins > Script Console`
   - Run: `"docker version".execute().text`

### Step 5: Configure Credentials

Add the following credentials in `Manage Jenkins > Manage Credentials > (global)`:

#### Docker Registry Credentials
- **Kind:** Username with password
- **Scope:** Global
- **ID:** `docker-hub-credentials`
- **Username:** Your Docker Hub username
- **Password:** Your Docker Hub password or token

#### Docker Registry URL
- **Kind:** Secret text
- **Scope:** Global
- **ID:** `docker-registry-url`
- **Secret:** `docker.io` (or your private registry URL)

#### Git Credentials (if private repo)
- **Kind:** Username with password (or SSH key)
- **Scope:** Global
- **ID:** `github-credentials`
- **Username/Password:** Your Git credentials

### Step 6: Configure System Settings

1. Go to `Manage Jenkins > Configure System`
2. Set **# of executors** to match your CPU cores (e.g., 4)
3. Configure **Global Properties** (optional):
   ```
   DOCKER_REGISTRY = docker.io
   PROJECT_NAME = farmatodo
   ```

---

## Pipeline Setup

### Option A: Root Pipeline (All Services)

This pipeline builds all services or a selected subset.

#### 1. Create Multi-Branch Pipeline Job

1. Go to Jenkins Dashboard
2. Click **New Item**
3. Enter name: `Farmatodo-Microservices`
4. Select **Multibranch Pipeline**
5. Click **OK**

#### 2. Configure Branch Sources

- **Branch Sources:** Add Git
- **Project Repository:** Your Git repository URL
- **Credentials:** Select Git credentials (if private repo)
- **Behaviors:**
  - ✓ Discover branches
  - ✓ Discover tags
  - Filter by name: `master`, `develop`, `staging`

#### 3. Build Configuration

- **Script Path:** `Jenkinsfile`
- **Scan Multibranch Pipeline Triggers:** ✓ Periodically if not otherwise run (1 hour)

#### 4. Save and Scan Repository

Click **Save** and Jenkins will automatically scan for branches and run the pipeline.

### Option B: Service-Specific Pipeline

For individual service CI/CD, follow these steps for each service:

#### 1. Copy Template to Service Directory

```bash
# Example for client-service
cp config-server/Jenkinsfile.template client-service/Jenkinsfile
```

#### 2. Update Service Configuration

Edit `client-service/Jenkinsfile` and update:

```groovy
environment {
    SERVICE_NAME = 'client-service'  // Update this
    SERVICE_PORT = '8081'            // Update this
    // ... rest of configuration
}
```

#### 3. Create Pipeline Job in Jenkins

1. Click **New Item**
2. Enter name: `client-service`
3. Select **Pipeline**
4. Click **OK**

#### 4. Configure Pipeline

- **Pipeline Definition:** Pipeline script from SCM
- **SCM:** Git
- **Repository URL:** Your repository URL
- **Credentials:** Select Git credentials
- **Branch Specifier:** `*/master` (or your default branch)
- **Script Path:** `client-service/Jenkinsfile`

#### 5. Save and Build

Click **Save** then **Build Now** to test the pipeline.

---

## Progressive Migration Strategy

### Phase 1: Config Server Only

Start by migrating only the config-server to establish the pipeline.

**Steps:**
1. Update root `Jenkinsfile`, modify `getServicesToBuild()`:
   ```groovy
   def getServicesToBuild() {
       return ['config-server']  // Only config-server
   }
   ```

2. Run pipeline and verify successful build
3. Verify Docker image creation
4. Test deployment

### Phase 2: Add Core Services

Add client-service and token-service:

```groovy
def getServicesToBuild() {
    return ['config-server', 'client-service', 'token-service']
}
```

### Phase 3: Add Business Services

Add product, cart, and order services:

```groovy
def getServicesToBuild() {
    return ['config-server', 'client-service', 'token-service',
            'product-service', 'cart-service', 'order-service']
}
```

### Phase 4: Add API Gateway

Complete migration with API Gateway:

```groovy
def getServicesToBuild() {
    return ['config-server', 'api-gateway', 'client-service',
            'token-service', 'product-service', 'cart-service',
            'order-service']
}
```

### Migration Checklist per Service

- [ ] Copy Jenkinsfile.template to service directory
- [ ] Update SERVICE_NAME and SERVICE_PORT
- [ ] Create Jenkins job
- [ ] Run first build (with tests)
- [ ] Verify Docker image creation
- [ ] Test deployment
- [ ] Configure notifications
- [ ] Update documentation

---

## Pipeline Usage

### Running the Root Pipeline

#### Build All Services
1. Go to `Farmatodo-Microservices` job
2. Click **Build with Parameters**
3. Select:
   - **BUILD_SCOPE:** ALL
   - **RUN_TESTS:** ✓
   - **BUILD_DOCKER_IMAGES:** ✓
   - **PUSH_TO_REGISTRY:** ✗ (for local testing)
   - **DEPLOY_TO_ENV:** ✗ (for build only)
4. Click **Build**

#### Build Single Service
1. Click **Build with Parameters**
2. Select:
   - **BUILD_SCOPE:** CLIENT_SERVICE (or desired service)
   - Other parameters as needed
3. Click **Build**

### Pipeline Parameters Explained

| Parameter | Description | Default |
|-----------|-------------|---------|
| BUILD_SCOPE | Which service(s) to build | ALL |
| RUN_TESTS | Execute unit tests | true |
| RUN_INTEGRATION_TESTS | Execute integration tests | false |
| BUILD_DOCKER_IMAGES | Build Docker images | true |
| PUSH_TO_REGISTRY | Push images to registry | false |
| DEPLOY_TO_ENV | Deploy to environment | false |
| LOG_LEVEL | Maven build log verbosity | INFO |

### Environment-Specific Builds

The pipeline automatically determines the environment based on branch:

| Branch | Environment | Configuration |
|--------|-------------|---------------|
| master | prod | Production configs, strict checks |
| staging | staging | Staging configs, moderate checks |
| develop | dev | Development configs, all checks |

### Running Service-Specific Pipeline

1. Go to specific service job (e.g., `client-service`)
2. Click **Build with Parameters**
3. Configure parameters
4. Click **Build**

---

## Pipeline Stages Explained

### Root Pipeline Stages

1. **Initialize** - Setup environment variables, clean workspace
2. **Checkout** - Clone repository, get commit info
3. **Build Services** - Parallel Maven builds for all services
4. **Build Docker Images** - Parallel Docker image builds
5. **Push to Registry** - Push images to Docker registry (if enabled)
6. **Deploy to Environment** - Deploy using docker-compose (if enabled)
7. **Smoke Tests** - Verify all services are healthy

### Service Pipeline Stages

1. **Initialize** - Setup service-specific environment
2. **Checkout** - Clone repository
3. **Build** - Maven build with optional tests
4. **Unit Tests** - Run unit tests
5. **Integration Tests** - Run integration tests with test database
6. **Code Quality** - SonarQube analysis (if configured)
7. **Build Docker Image** - Build service Docker image
8. **Security Scan** - Scan Docker image for vulnerabilities
9. **Push to Registry** - Push to Docker registry
10. **Deploy** - Deploy service to environment
11. **Health Check** - Verify service health
12. **Smoke Tests** - Run service-specific smoke tests

---

## Advanced Configuration

### SonarQube Integration

1. Install SonarQube Scanner plugin in Jenkins
2. Configure SonarQube server in `Manage Jenkins > Configure System`
3. Uncomment SonarQube section in Jenkinsfile:

```groovy
stage('Code Quality') {
    steps {
        withSonarQubeEnv('SonarQube') {
            sh "./mvnw sonar:sonar"
        }
    }
}
```

### Email Notifications

1. Configure SMTP in `Manage Jenkins > Configure System`
2. Uncomment notification sections in Jenkinsfile:

```groovy
post {
    success {
        emailext (
            subject: "SUCCESS: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
            body: "Build succeeded: ${env.BUILD_URL}",
            to: "team@example.com"
        )
    }
}
```

### Slack Notifications

1. Install Slack Notification plugin
2. Configure Slack workspace in Jenkins
3. Add to Jenkinsfile:

```groovy
post {
    success {
        slackSend (
            color: 'good',
            message: "SUCCESS: ${env.JOB_NAME} - ${env.BUILD_URL}"
        )
    }
}
```

### Kubernetes Deployment

Update deploy stage in service Jenkinsfile:

```groovy
stage('Deploy') {
    steps {
        script {
            sh """
                kubectl set image deployment/${env.SERVICE_NAME} \
                    ${env.SERVICE_NAME}=${env.PROJECT_NAME}/${env.SERVICE_NAME}:${env.DOCKER_IMAGE_TAG} \
                    -n ${env.DEPLOY_ENV}
            """
        }
    }
}
```

---

## Troubleshooting

### Common Issues

#### 1. Permission Denied: Docker Socket

**Error:** `permission denied while trying to connect to the Docker daemon socket`

**Solution (Linux):**
```bash
sudo usermod -aG docker jenkins
sudo systemctl restart jenkins
```

#### 2. Maven Not Found

**Error:** `mvnw: command not found`

**Solution:**
- Ensure Maven wrapper has execute permissions:
  ```bash
  git update-index --chmod=+x mvnw
  chmod +x mvnw
  ```

#### 3. Out of Memory During Build

**Error:** `java.lang.OutOfMemoryError: Java heap space`

**Solution:**
- Increase Jenkins JVM memory: Edit `jenkins.xml` or startup script
- Add to `MAVEN_OPTS`:
  ```groovy
  environment {
      MAVEN_OPTS = '-Xmx2048m -Xms1024m'
  }
  ```

#### 4. Docker Build Fails

**Error:** `Cannot connect to Docker daemon`

**Solution:**
- Verify Docker is running: `docker ps`
- Check Docker socket permissions
- Restart Docker service

#### 5. Tests Fail Due to Database

**Error:** `Connection refused: postgres:5432`

**Solution:**
- Ensure test database is started
- Check docker-compose.test.yml exists
- Verify database credentials

#### 6. Health Check Timeout

**Error:** `Service failed health check`

**Solution:**
- Increase health check timeout
- Check service logs: `docker logs <service-name>`
- Verify service port is correct
- Check database connectivity

### Debug Mode

Enable debug logging in pipeline:

```groovy
parameters {
    choice(
        name: 'LOG_LEVEL',
        choices: ['DEBUG', 'INFO', 'WARN'],  // Change to DEBUG
        description: 'Maven build log level'
    )
}
```

### Viewing Logs

#### Jenkins Console Output
1. Go to build
2. Click **Console Output**
3. Search for errors

#### Docker Service Logs
```bash
# View specific service logs
docker logs <service-name>

# Follow logs in real-time
docker logs -f <service-name>

# View last 100 lines
docker logs --tail 100 <service-name>
```

#### Maven Build Logs
Located in workspace: `target/maven-build.log`

---

## Best Practices

### 1. Branch Strategy

```
master      → Production deployments (auto-deploy disabled)
staging     → Staging environment (auto-deploy enabled)
develop     → Development environment (auto-deploy enabled)
feature/*   → Build only, no deployment
hotfix/*    → Build and test, manual deployment
```

### 2. Versioning

Docker images are tagged with:
- `{env}-{git-commit-short}` - Specific version
- `{env}-latest` - Latest for environment

Example: `farmatodo/client-service:prod-a1b2c3d`

### 3. Security

- ✓ Never commit credentials to Git
- ✓ Use Jenkins credentials store
- ✓ Scan Docker images for vulnerabilities
- ✓ Use least-privilege service accounts
- ✓ Enable RBAC in Jenkins

### 4. Performance

- ✓ Use parallel builds for independent services
- ✓ Cache Maven dependencies
- ✓ Use multi-stage Docker builds
- ✓ Clean up old Docker images regularly

### 5. Testing

- ✓ Run unit tests on every build
- ✓ Run integration tests on develop/staging branches
- ✓ Run smoke tests after deployment
- ✓ Fail fast - stop on first error

---

## Next Steps

1. ✅ Complete Jenkins installation
2. ✅ Install required plugins
3. ✅ Configure credentials
4. ✅ Start with Phase 1 (config-server only)
5. ✅ Verify successful build and deployment
6. ✅ Gradually migrate other services (Phase 2-4)
7. ✅ Configure notifications (email/Slack)
8. ✅ Set up monitoring and alerts
9. ✅ Document service-specific requirements

---

## Additional Resources

- **Jenkins Documentation:** https://www.jenkins.io/doc/
- **Jenkins Pipeline Syntax:** https://www.jenkins.io/doc/book/pipeline/syntax/
- **Docker Plugin:** https://plugins.jenkins.io/docker-workflow/
- **Blue Ocean:** https://www.jenkins.io/doc/book/blueocean/

---

## Support

For issues or questions:
1. Check Jenkins console output for errors
2. Review service logs with `docker logs`
3. Consult this documentation
4. Check Jenkins community forums
5. Review project CLAUDE.md for service-specific info

**Last Updated:** 2025-01-26
