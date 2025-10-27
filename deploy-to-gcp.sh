#!/bin/bash

################################################################################
# Script de Deployment Manual para GCP
# Este script despliega Farmatodo a Google Cloud Platform usando Docker Compose
################################################################################

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration - CAMBIAR SEGÚN TU CONFIGURACIÓN
GCP_PROJECT_ID="planar-momentum-469121-n0"
REGION="us-central1"
ZONE="us-central1-a"
REPOSITORY="farmatodo-repo"
VM_INSTANCE_NAME="farmatodo-app-vm"
VM_MACHINE_TYPE="e2-standard-4"  # 4 vCPUs, 16 GB RAM
VM_BOOT_DISK_SIZE="100GB"

AR_HOST="${REGION}-docker.pkg.dev"

# Services to build
SERVICES=("config-server" "api-gateway" "client-service" "token-service" "product-service" "cart-service" "order-service")

################################################################################
# Helper Functions
################################################################################

print_header() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

################################################################################
# Main Deployment Steps
################################################################################

step_1_authenticate() {
    print_header "Step 1: Authenticate with GCP"

    echo "Logging in to GCP..."
    gcloud auth login

    echo "Setting project to ${GCP_PROJECT_ID}..."
    gcloud config set project "${GCP_PROJECT_ID}"

    echo "Configuring Docker for Artifact Registry..."
    gcloud auth configure-docker "${AR_HOST}" --quiet

    print_success "Authentication completed"
}

step_2_build_services() {
    print_header "Step 2: Build All Services"

    for service in "${SERVICES[@]}"; do
        echo "Building ${service}..."
        cd "${service}"

        if [ -f "mvnw" ]; then
            ./mvnw clean package -DskipTests
        elif [ -f "mvnw.cmd" ]; then
            ./mvnw.cmd clean package -DskipTests
        else
            mvn clean package -DskipTests
        fi

        cd ..
        print_success "${service} built successfully"
    done
}

step_3_build_docker_images() {
    print_header "Step 3: Build Docker Images"

    for service in "${SERVICES[@]}"; do
        echo "Building Docker image for ${service}..."
        cd "${service}"
        docker build -t "farmatodo/${service}:latest" .
        cd ..
        print_success "Docker image built for ${service}"
    done
}

step_4_create_artifact_registry() {
    print_header "Step 4: Create Artifact Registry Repository"

    echo "Checking if repository exists..."
    if gcloud artifacts repositories describe "${REPOSITORY}" --location="${REGION}" --project="${GCP_PROJECT_ID}" >/dev/null 2>&1; then
        print_info "Repository already exists"
    else
        echo "Creating repository..."
        gcloud artifacts repositories create "${REPOSITORY}" \
            --repository-format=docker \
            --location="${REGION}" \
            --description="Docker repository for Farmatodo microservices" \
            --project="${GCP_PROJECT_ID}"
        print_success "Repository created"
    fi
}

step_5_push_images() {
    print_header "Step 5: Push Images to Artifact Registry"

    for service in "${SERVICES[@]}"; do
        echo "Tagging and pushing ${service}..."

        FULL_IMAGE_NAME="${AR_HOST}/${GCP_PROJECT_ID}/${REPOSITORY}/${service}:latest"

        docker tag "farmatodo/${service}:latest" "${FULL_IMAGE_NAME}"
        docker push "${FULL_IMAGE_NAME}"

        print_success "${service} pushed to Artifact Registry"
    done
}

step_6_create_vm() {
    print_header "Step 6: Create/Update GCE VM Instance"

    echo "Checking if VM exists..."
    if gcloud compute instances describe "${VM_INSTANCE_NAME}" --zone="${ZONE}" >/dev/null 2>&1; then
        print_info "VM already exists"

        VM_STATUS=$(gcloud compute instances describe "${VM_INSTANCE_NAME}" --zone="${ZONE}" --format="value(status)")

        if [ "${VM_STATUS}" != "RUNNING" ]; then
            echo "Starting VM..."
            gcloud compute instances start "${VM_INSTANCE_NAME}" --zone="${ZONE}"
            print_success "VM started"
        else
            print_info "VM is already running"
        fi
    else
        echo "Creating new VM instance..."
        gcloud compute instances create "${VM_INSTANCE_NAME}" \
            --zone="${ZONE}" \
            --machine-type="${VM_MACHINE_TYPE}" \
            --boot-disk-size="${VM_BOOT_DISK_SIZE}" \
            --image-family="ubuntu-2204-lts" \
            --image-project="ubuntu-os-cloud" \
            --tags=farmatodo,http-server,https-server \
            --metadata=startup-script='#!/bin/bash
                apt-get update
                apt-get install -y docker.io docker-compose
                systemctl start docker
                systemctl enable docker
                usermod -aG docker $USER
            '

        print_success "VM created"
        echo "Waiting for VM to be ready..."
        sleep 60
    fi
}

step_7_configure_firewall() {
    print_header "Step 7: Configure Firewall Rules"

    # Allow access to services
    if ! gcloud compute firewall-rules describe farmatodo-allow-services --project="${GCP_PROJECT_ID}" >/dev/null 2>&1; then
        echo "Creating firewall rule for services..."
        gcloud compute firewall-rules create farmatodo-allow-services \
            --allow=tcp:8080,tcp:8081,tcp:8082,tcp:8083,tcp:8084,tcp:8085,tcp:8888,tcp:9090 \
            --source-ranges=0.0.0.0/0 \
            --target-tags=farmatodo \
            --description="Allow access to Farmatodo services"
        print_success "Firewall rule created for services"
    else
        print_info "Firewall rule for services already exists"
    fi

    # Allow access to PostgreSQL (optional - for debugging only)
    if ! gcloud compute firewall-rules describe farmatodo-allow-postgres --project="${GCP_PROJECT_ID}" >/dev/null 2>&1; then
        echo "Creating firewall rule for PostgreSQL..."
        gcloud compute firewall-rules create farmatodo-allow-postgres \
            --allow=tcp:5432,tcp:5433,tcp:5434,tcp:5435,tcp:5436 \
            --source-ranges=0.0.0.0/0 \
            --target-tags=farmatodo \
            --description="Allow access to PostgreSQL databases (dev only)"
        print_success "Firewall rule created for PostgreSQL"
    else
        print_info "Firewall rule for PostgreSQL already exists"
    fi
}

step_8_deploy_to_vm() {
    print_header "Step 8: Deploy Application to VM"

    echo "Preparing docker-compose.prod.yml..."

    # Create docker-compose with proper image names
    cat > /tmp/docker-compose.prod.yml <<EOF
version: '3.8'

services:
  config-server:
    image: ${AR_HOST}/${GCP_PROJECT_ID}/${REPOSITORY}/config-server:latest
    container_name: farmatodo-config-server
    environment:
      SPRING_APPLICATION_NAME: config-server
      SERVER_PORT: 8888
      SPRING_CLOUD_CONFIG_SERVER_GIT_URI: https://github.com/santgodev/farmatodo-config-service
      SPRING_CLOUD_CONFIG_SERVER_GIT_DEFAULT_LABEL: master
      SPRING_CLOUD_CONFIG_SERVER_GIT_CLONE_ON_START: "true"
      SPRING_CLOUD_CONFIG_SERVER_GIT_SKIP_SSL_VALIDATION: "true"
      SPRING_CLOUD_CONFIG_SERVER_GIT_TIMEOUT: 30
    ports:
      - "8888:8888"
    networks:
      - farmatodo-network
    healthcheck:
      test: ["CMD-SHELL", "wget --no-verbose --tries=1 --spider http://localhost:8888/actuator/health || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 40s
    restart: unless-stopped

  client-db:
    image: postgres:16-alpine
    container_name: farmatodo-client-db
    environment:
      POSTGRES_DB: clientdb
      POSTGRES_USER: clientuser
      POSTGRES_PASSWORD: clientpass
    ports:
      - "5432:5432"
    volumes:
      - client-db-data:/var/lib/postgresql/data
    networks:
      - farmatodo-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U clientuser -d clientdb"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  token-db:
    image: postgres:16-alpine
    container_name: farmatodo-token-db
    environment:
      POSTGRES_DB: tokendb
      POSTGRES_USER: tokenuser
      POSTGRES_PASSWORD: tokenpass
    ports:
      - "5433:5432"
    volumes:
      - token-db-data:/var/lib/postgresql/data
    networks:
      - farmatodo-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U tokenuser -d tokendb"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  client-service:
    image: ${AR_HOST}/${GCP_PROJECT_ID}/${REPOSITORY}/client-service:latest
    container_name: farmatodo-client-service
    environment:
      SPRING_APPLICATION_NAME: client-service
      SPRING_PROFILES_ACTIVE: prod
      SPRING_CONFIG_IMPORT: "optional:configserver:http://config-server:8888"
      SPRING_CLOUD_CONFIG_FAIL_FAST: "false"
      SPRING_CLOUD_CONFIG_URI: http://config-server:8888
      SPRING_DATASOURCE_URL: jdbc:postgresql://client-db:5432/clientdb
      SPRING_DATASOURCE_USERNAME: clientuser
      SPRING_DATASOURCE_PASSWORD: clientpass
      SPRING_JPA_HIBERNATE_DDL_AUTO: update
      SPRING_JPA_SHOW_SQL: "false"
      SERVER_PORT: 8081
      API_KEY: client-service-api-key-change-in-production
    ports:
      - "8081:8081"
    depends_on:
      config-server:
        condition: service_healthy
      client-db:
        condition: service_healthy
    networks:
      - farmatodo-network
    restart: unless-stopped

  token-service:
    image: ${AR_HOST}/${GCP_PROJECT_ID}/${REPOSITORY}/token-service:latest
    container_name: farmatodo-token-service
    environment:
      SPRING_APPLICATION_NAME: token-service
      SPRING_PROFILES_ACTIVE: prod
      SPRING_CONFIG_IMPORT: "optional:configserver:http://config-server:8888"
      SPRING_CLOUD_CONFIG_FAIL_FAST: "false"
      SPRING_CLOUD_CONFIG_URI: http://config-server:8888
      SPRING_DATASOURCE_URL: jdbc:postgresql://token-db:5432/tokendb
      SPRING_DATASOURCE_USERNAME: tokenuser
      SPRING_DATASOURCE_PASSWORD: tokenpass
      SPRING_JPA_HIBERNATE_DDL_AUTO: update
      SPRING_JPA_SHOW_SQL: "false"
      SERVER_PORT: 8082
      API_KEY: your-secret-api-key-change-in-production
      ENCRYPTION_SECRET_KEY: MySecretKey1234567890123456789012
      TOKEN_REJECTIONPROBABILITY: 0.3
      PAYMENT_REJECTIONPROBABILITY: 0.3
      PAYMENT_RETRYCOUNT: 3
      EMAIL_ENABLED: "false"
      EMAIL_FROM: noreply@farmatodo.com
    ports:
      - "8082:8082"
    depends_on:
      config-server:
        condition: service_healthy
      token-db:
        condition: service_healthy
    networks:
      - farmatodo-network
    restart: unless-stopped

  product-db:
    image: postgres:16-alpine
    container_name: farmatodo-product-db
    environment:
      POSTGRES_DB: productdb
      POSTGRES_USER: productuser
      POSTGRES_PASSWORD: productpass
    ports:
      - "5434:5432"
    volumes:
      - product-db-data:/var/lib/postgresql/data
    networks:
      - farmatodo-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U productuser -d productdb"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  cart-db:
    image: postgres:16-alpine
    container_name: farmatodo-cart-db
    environment:
      POSTGRES_DB: cartdb
      POSTGRES_USER: cartuser
      POSTGRES_PASSWORD: cartpass
    ports:
      - "5435:5432"
    volumes:
      - cart-db-data:/var/lib/postgresql/data
    networks:
      - farmatodo-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U cartuser -d cartdb"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  product-service:
    image: ${AR_HOST}/${GCP_PROJECT_ID}/${REPOSITORY}/product-service:latest
    container_name: farmatodo-product-service
    environment:
      SPRING_APPLICATION_NAME: product-service
      SPRING_PROFILES_ACTIVE: prod
      SPRING_CONFIG_IMPORT: "optional:configserver:http://config-server:8888"
      SPRING_CLOUD_CONFIG_FAIL_FAST: "false"
      SPRING_CLOUD_CONFIG_URI: http://config-server:8888
      SPRING_DATASOURCE_URL: jdbc:postgresql://product-db:5432/productdb
      SPRING_DATASOURCE_USERNAME: productuser
      SPRING_DATASOURCE_PASSWORD: productpass
      SPRING_JPA_HIBERNATE_DDL_AUTO: update
      SPRING_JPA_SHOW_SQL: "false"
      SERVER_PORT: 8083
      API_KEY: product-service-api-key-change-in-production
      PRODUCT_MINSTOCK: 0
    ports:
      - "8083:8083"
    depends_on:
      config-server:
        condition: service_healthy
      product-db:
        condition: service_healthy
    networks:
      - farmatodo-network
    restart: unless-stopped

  cart-service:
    image: ${AR_HOST}/${GCP_PROJECT_ID}/${REPOSITORY}/cart-service:latest
    container_name: farmatodo-cart-service
    environment:
      SPRING_APPLICATION_NAME: cart-service
      SPRING_PROFILES_ACTIVE: prod
      SPRING_CONFIG_IMPORT: "optional:configserver:http://config-server:8888"
      SPRING_CLOUD_CONFIG_FAIL_FAST: "false"
      SPRING_CLOUD_CONFIG_URI: http://config-server:8888
      SPRING_DATASOURCE_URL: jdbc:postgresql://cart-db:5432/cartdb
      SPRING_DATASOURCE_USERNAME: cartuser
      SPRING_DATASOURCE_PASSWORD: cartpass
      SPRING_JPA_HIBERNATE_DDL_AUTO: update
      SPRING_JPA_SHOW_SQL: "false"
      SERVER_PORT: 8084
      API_KEY: cart-service-api-key-change-in-production
      SERVICES_PRODUCT_URL: http://product-service:8083
      SERVICES_PRODUCT_APIKEY: product-service-api-key-change-in-production
    ports:
      - "8084:8084"
    depends_on:
      config-server:
        condition: service_healthy
      cart-db:
        condition: service_healthy
      product-service:
        condition: service_started
    networks:
      - farmatodo-network
    restart: unless-stopped

  order-db:
    image: postgres:16-alpine
    container_name: farmatodo-order-db
    environment:
      POSTGRES_DB: orderdb
      POSTGRES_USER: orderuser
      POSTGRES_PASSWORD: orderpass
    ports:
      - "5436:5432"
    volumes:
      - order-db-data:/var/lib/postgresql/data
    networks:
      - farmatodo-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U orderuser -d orderdb"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  order-service:
    image: ${AR_HOST}/${GCP_PROJECT_ID}/${REPOSITORY}/order-service:latest
    container_name: farmatodo-order-service
    environment:
      SPRING_APPLICATION_NAME: order-service
      SPRING_PROFILES_ACTIVE: prod
      SPRING_CONFIG_IMPORT: "optional:configserver:http://config-server:8888"
      SPRING_CLOUD_CONFIG_FAIL_FAST: "false"
      SPRING_CLOUD_CONFIG_URI: http://config-server:8888
      SPRING_DATASOURCE_URL: jdbc:postgresql://order-db:5432/orderdb
      SPRING_DATASOURCE_USERNAME: orderuser
      SPRING_DATASOURCE_PASSWORD: orderpass
      SPRING_JPA_HIBERNATE_DDL_AUTO: update
      SPRING_JPA_SHOW_SQL: "false"
      SERVER_PORT: 8085
      API_KEY: order-service-api-key-change-in-production
      PAYMENT_REJECTIONPROBABILITY: 0.3
      PAYMENT_RETRYCOUNT: 3
      SERVICES_CLIENT_URL: http://client-service:8081
      SERVICES_CLIENT_APIKEY: client-service-api-key-change-in-production
      SERVICES_TOKEN_URL: http://token-service:8082
      SERVICES_TOKEN_APIKEY: your-secret-api-key-change-in-production
      SERVICES_PRODUCT_URL: http://product-service:8083
      SERVICES_PRODUCT_APIKEY: product-service-api-key-change-in-production
      SERVICES_CART-SERVICE_URL: http://cart-service:8084
      SERVICES_CART-SERVICE_API-KEY: cart-service-api-key-change-in-production
      EMAIL_ENABLED: "false"
      EMAIL_FROM: noreply@farmatodo.com
    ports:
      - "8085:8085"
    depends_on:
      config-server:
        condition: service_healthy
      order-db:
        condition: service_healthy
      client-service:
        condition: service_started
      token-service:
        condition: service_started
      cart-service:
        condition: service_started
    networks:
      - farmatodo-network
    restart: unless-stopped

  api-gateway:
    image: ${AR_HOST}/${GCP_PROJECT_ID}/${REPOSITORY}/api-gateway:latest
    container_name: farmatodo-api-gateway
    environment:
      SPRING_APPLICATION_NAME: api-gateway
      SPRING_PROFILES_ACTIVE: prod
      SPRING_CONFIG_IMPORT: "optional:configserver:http://config-server:8888"
      SPRING_CLOUD_CONFIG_FAIL_FAST: "false"
      SPRING_CLOUD_CONFIG_URI: http://config-server:8888
      SERVER_PORT: 9090
      CLIENT_SERVICE_URL: http://client-service:8081
      TOKEN_SERVICE_URL: http://token-service:8082
      PRODUCT_SERVICE_URL: http://product-service:8083
      CART_SERVICE_URL: http://cart-service:8084
      ORDER_SERVICE_URL: http://order-service:8085
    ports:
      - "9090:9090"
    depends_on:
      config-server:
        condition: service_healthy
      client-service:
        condition: service_started
      token-service:
        condition: service_started
      product-service:
        condition: service_started
      cart-service:
        condition: service_started
      order-service:
        condition: service_started
    networks:
      - farmatodo-network
    restart: unless-stopped

networks:
  farmatodo-network:
    driver: bridge

volumes:
  client-db-data:
  token-db-data:
  product-db-data:
  cart-db-data:
  order-db-data:
EOF

    echo "Copying docker-compose file to VM..."
    gcloud compute scp /tmp/docker-compose.prod.yml "${VM_INSTANCE_NAME}":~/docker-compose.yml --zone="${ZONE}"

    echo "Deploying application on VM..."
    gcloud compute ssh "${VM_INSTANCE_NAME}" --zone="${ZONE}" --command="
        # Authenticate Docker with Artifact Registry
        gcloud auth configure-docker ${AR_HOST} --quiet

        # Stop existing containers
        docker-compose down || true

        # Clean up old images
        docker image prune -f

        # Start services
        docker-compose up -d

        # Wait for services to be ready
        echo 'Waiting for services to be ready...'
        sleep 90

        # Show container status
        docker-compose ps
    "

    print_success "Application deployed to VM"
}

step_9_get_urls() {
    print_header "Step 9: Get Public URLs"

    VM_EXTERNAL_IP=$(gcloud compute instances describe "${VM_INSTANCE_NAME}" --zone="${ZONE}" --format="get(networkInterfaces[0].accessConfigs[0].natIP)")

    echo ""
    echo -e "${GREEN}╔════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║                   ✅ DEPLOYMENT SUCCESSFUL                      ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${BLUE}🌐 VM External IP: ${GREEN}${VM_EXTERNAL_IP}${NC}"
    echo ""
    echo -e "${BLUE}📍 Public Service URLs:${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "  ${YELLOW}🔧 Config Server:${NC}   http://${VM_EXTERNAL_IP}:8888/actuator/health"
    echo -e "  ${YELLOW}🚪 API Gateway:${NC}     http://${VM_EXTERNAL_IP}:9090/api/gateway/health"
    echo -e "  ${YELLOW}👤 Client Service:${NC}  http://${VM_EXTERNAL_IP}:8081/api/clients/health"
    echo -e "  ${YELLOW}🔑 Token Service:${NC}   http://${VM_EXTERNAL_IP}:8082/api/tokens/health"
    echo -e "  ${YELLOW}📦 Product Service:${NC} http://${VM_EXTERNAL_IP}:8083/products/health"
    echo -e "  ${YELLOW}🛒 Cart Service:${NC}    http://${VM_EXTERNAL_IP}:8084/carts/health"
    echo -e "  ${YELLOW}📋 Order Service:${NC}   http://${VM_EXTERNAL_IP}:8085/orders/ping"
    echo ""
    echo -e "${BLUE}📊 PostgreSQL Databases:${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "  ${YELLOW}🗄️  Client DB:${NC}  ${VM_EXTERNAL_IP}:5432 (clientdb/clientuser/clientpass)"
    echo -e "  ${YELLOW}🗄️  Token DB:${NC}   ${VM_EXTERNAL_IP}:5433 (tokendb/tokenuser/tokenpass)"
    echo -e "  ${YELLOW}🗄️  Product DB:${NC} ${VM_EXTERNAL_IP}:5434 (productdb/productuser/productpass)"
    echo -e "  ${YELLOW}🗄️  Cart DB:${NC}    ${VM_EXTERNAL_IP}:5435 (cartdb/cartuser/cartpass)"
    echo -e "  ${YELLOW}🗄️  Order DB:${NC}   ${VM_EXTERNAL_IP}:5436 (orderdb/orderuser/orderpass)"
    echo ""
    echo -e "${BLUE}🔨 Useful Commands:${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "  View logs:         gcloud compute ssh ${VM_INSTANCE_NAME} --zone=${ZONE} --command=\"docker-compose logs -f\""
    echo -e "  View containers:   gcloud compute ssh ${VM_INSTANCE_NAME} --zone=${ZONE} --command=\"docker-compose ps\""
    echo -e "  Restart services:  gcloud compute ssh ${VM_INSTANCE_NAME} --zone=${ZONE} --command=\"docker-compose restart\""
    echo -e "  Stop services:     gcloud compute ssh ${VM_INSTANCE_NAME} --zone=${ZONE} --command=\"docker-compose down\""
    echo ""
}

################################################################################
# Main Execution
################################################################################

main() {
    print_header "Farmatodo - GCP Deployment Script"

    echo "This script will deploy Farmatodo to Google Cloud Platform"
    echo "Configuration:"
    echo "  Project: ${GCP_PROJECT_ID}"
    echo "  Region: ${REGION}"
    echo "  Zone: ${ZONE}"
    echo "  VM Name: ${VM_INSTANCE_NAME}"
    echo ""
    read -p "Do you want to continue? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Deployment cancelled."
        exit 0
    fi

    step_1_authenticate
    step_2_build_services
    step_3_build_docker_images
    step_4_create_artifact_registry
    step_5_push_images
    step_6_create_vm
    step_7_configure_firewall
    step_8_deploy_to_vm
    step_9_get_urls

    print_success "Deployment completed successfully!"
}

# Run main function
main
