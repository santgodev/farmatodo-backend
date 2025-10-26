pipeline {
    agent any

    // Environment variables for the entire pipeline
    environment {
        // Docker registry configuration
        DOCKER_REGISTRY = credentials('docker-registry-url')  // e.g., 'docker.io' or your private registry
        DOCKER_CREDENTIALS = credentials('docker-hub-credentials')  // Jenkins credentials ID

        // Project configuration
        PROJECT_NAME = 'farmatodo'
        BRANCH_NAME = "${env.GIT_BRANCH?.replaceAll('origin/', '') ?: 'master'}"
        BUILD_NUMBER = "${env.BUILD_NUMBER}"

        // Environment determination (dev/staging/prod)
        DEPLOY_ENV = "${BRANCH_NAME == 'master' ? 'prod' : (BRANCH_NAME == 'staging' ? 'staging' : 'dev')}"

        // PostgreSQL configuration (for local testing/integration tests)
        POSTGRES_VERSION = '16-alpine'

        // Services to build (progressive migration - add services as you migrate)
        SERVICES_TO_BUILD = getServicesToBuild()
    }

    // Build triggers
    triggers {
        // Poll SCM every 5 minutes for changes
        pollSCM('H/5 * * * *')

        // Webhook trigger (configure in GitHub/GitLab)
        // githubPush()
    }

    // Pipeline parameters for manual control
    parameters {
        choice(
            name: 'BUILD_SCOPE',
            choices: ['ALL', 'CONFIG_SERVER', 'API_GATEWAY', 'CLIENT_SERVICE', 'TOKEN_SERVICE',
                     'PRODUCT_SERVICE', 'CART_SERVICE', 'ORDER_SERVICE'],
            description: 'Select which service(s) to build'
        )

        booleanParam(
            name: 'RUN_TESTS',
            defaultValue: true,
            description: 'Run unit tests'
        )

        booleanParam(
            name: 'RUN_INTEGRATION_TESTS',
            defaultValue: false,
            description: 'Run integration tests (requires database)'
        )

        booleanParam(
            name: 'BUILD_DOCKER_IMAGES',
            defaultValue: true,
            description: 'Build Docker images'
        )

        booleanParam(
            name: 'PUSH_TO_REGISTRY',
            defaultValue: false,
            description: 'Push Docker images to registry'
        )

        booleanParam(
            name: 'DEPLOY_TO_ENV',
            defaultValue: false,
            description: 'Deploy to target environment'
        )

        choice(
            name: 'LOG_LEVEL',
            choices: ['INFO', 'DEBUG', 'WARN', 'ERROR'],
            description: 'Maven build log level'
        )
    }

    options {
        // Keep only last 10 builds
        buildDiscarder(logRotator(numToKeepStr: '10'))

        // Timeout for entire pipeline
        timeout(time: 60, unit: 'MINUTES')

        // Disable concurrent builds
        disableConcurrentBuilds()

        // Add timestamps to console output
        timestamps()

        // ANSI color output
        ansiColor('xterm')
    }

    stages {
        stage('Initialize') {
            steps {
                script {
                    echo "=========================================="
                    echo "Farmatodo Microservices - CI/CD Pipeline"
                    echo "=========================================="
                    echo "Branch: ${BRANCH_NAME}"
                    echo "Build Number: ${BUILD_NUMBER}"
                    echo "Deploy Environment: ${DEPLOY_ENV}"
                    echo "Build Scope: ${params.BUILD_SCOPE}"
                    echo "Services to Build: ${env.SERVICES_TO_BUILD}"
                    echo "=========================================="

                    // Clean workspace
                    cleanWs()
                }
            }
        }

        stage('Checkout') {
            steps {
                script {
                    echo "Checking out source code..."
                    checkout scm

                    // Get commit information
                    env.GIT_COMMIT_SHORT = sh(
                        script: "git rev-parse --short HEAD",
                        returnStdout: true
                    ).trim()

                    env.GIT_COMMIT_MSG = sh(
                        script: "git log -1 --pretty=%B",
                        returnStdout: true
                    ).trim()

                    echo "Commit: ${env.GIT_COMMIT_SHORT} - ${env.GIT_COMMIT_MSG}"
                }
            }
        }

        stage('Build Services') {
            parallel {
                stage('Config Server') {
                    when {
                        expression { shouldBuild('CONFIG_SERVER') }
                    }
                    steps {
                        buildService('config-server', 'config-server')
                    }
                }

                stage('API Gateway') {
                    when {
                        expression { shouldBuild('API_GATEWAY') }
                    }
                    steps {
                        buildService('api-gateway', 'api-gateway')
                    }
                }

                stage('Client Service') {
                    when {
                        expression { shouldBuild('CLIENT_SERVICE') }
                    }
                    steps {
                        buildService('client-service', 'client-service')
                    }
                }

                stage('Token Service') {
                    when {
                        expression { shouldBuild('TOKEN_SERVICE') }
                    }
                    steps {
                        buildService('token-service', 'token-service')
                    }
                }

                stage('Product Service') {
                    when {
                        expression { shouldBuild('PRODUCT_SERVICE') }
                    }
                    steps {
                        buildService('product-service', 'product-service')
                    }
                }

                stage('Cart Service') {
                    when {
                        expression { shouldBuild('CART_SERVICE') }
                    }
                    steps {
                        buildService('cart-service', 'cart-service')
                    }
                }

                stage('Order Service') {
                    when {
                        expression { shouldBuild('ORDER_SERVICE') }
                    }
                    steps {
                        buildService('order-service', 'order-service')
                    }
                }
            }
        }

        stage('Build Docker Images') {
            when {
                expression { params.BUILD_DOCKER_IMAGES == true }
            }
            parallel {
                stage('Docker: Config Server') {
                    when {
                        expression { shouldBuild('CONFIG_SERVER') }
                    }
                    steps {
                        buildDockerImage('config-server')
                    }
                }

                stage('Docker: API Gateway') {
                    when {
                        expression { shouldBuild('API_GATEWAY') }
                    }
                    steps {
                        buildDockerImage('api-gateway')
                    }
                }

                stage('Docker: Client Service') {
                    when {
                        expression { shouldBuild('CLIENT_SERVICE') }
                    }
                    steps {
                        buildDockerImage('client-service')
                    }
                }

                stage('Docker: Token Service') {
                    when {
                        expression { shouldBuild('TOKEN_SERVICE') }
                    }
                    steps {
                        buildDockerImage('token-service')
                    }
                }

                stage('Docker: Product Service') {
                    when {
                        expression { shouldBuild('PRODUCT_SERVICE') }
                    }
                    steps {
                        buildDockerImage('product-service')
                    }
                }

                stage('Docker: Cart Service') {
                    when {
                        expression { shouldBuild('CART_SERVICE') }
                    }
                    steps {
                        buildDockerImage('cart-service')
                    }
                }

                stage('Docker: Order Service') {
                    when {
                        expression { shouldBuild('ORDER_SERVICE') }
                    }
                    steps {
                        buildDockerImage('order-service')
                    }
                }
            }
        }

        stage('Push to Registry') {
            when {
                expression { params.PUSH_TO_REGISTRY == true }
            }
            steps {
                script {
                    echo "Pushing Docker images to registry..."

                    docker.withRegistry("https://${env.DOCKER_REGISTRY}", env.DOCKER_CREDENTIALS) {
                        getServicesList().each { service ->
                            if (shouldBuild(service.toUpperCase().replaceAll('-', '_'))) {
                                def imageName = "${env.PROJECT_NAME}/${service}"
                                def imageTag = "${env.DEPLOY_ENV}-${env.GIT_COMMIT_SHORT}"

                                echo "Pushing ${imageName}:${imageTag}..."
                                sh "docker push ${imageName}:${imageTag}"
                                sh "docker push ${imageName}:${env.DEPLOY_ENV}-latest"
                            }
                        }
                    }
                }
            }
        }

        stage('Deploy to Environment') {
            when {
                expression { params.DEPLOY_TO_ENV == true }
            }
            steps {
                script {
                    echo "Deploying to ${env.DEPLOY_ENV} environment..."

                    // Use docker-compose for deployment
                    sh """
                        docker-compose -f docker-compose.yml down
                        docker-compose -f docker-compose.yml up -d
                    """

                    echo "Waiting for services to start..."
                    sleep(30)

                    // Health checks
                    performHealthChecks()
                }
            }
        }

        stage('Smoke Tests') {
            when {
                expression { params.DEPLOY_TO_ENV == true }
            }
            steps {
                script {
                    echo "Running smoke tests..."

                    def services = [
                        [name: 'Config Server', url: 'http://localhost:8888/actuator/health', enabled: shouldBuild('CONFIG_SERVER')],
                        [name: 'API Gateway', url: 'http://localhost:8080/api/gateway/health', enabled: shouldBuild('API_GATEWAY')],
                        [name: 'Client Service', url: 'http://localhost:8081/api/clients/health', enabled: shouldBuild('CLIENT_SERVICE')],
                        [name: 'Token Service', url: 'http://localhost:8082/api/tokens/health', enabled: shouldBuild('TOKEN_SERVICE')],
                        [name: 'Product Service', url: 'http://localhost:8083/products/health', enabled: shouldBuild('PRODUCT_SERVICE')],
                        [name: 'Cart Service', url: 'http://localhost:8084/carts/health', enabled: shouldBuild('CART_SERVICE')],
                        [name: 'Order Service', url: 'http://localhost:8085/orders/ping', enabled: shouldBuild('ORDER_SERVICE')]
                    ]

                    services.each { service ->
                        if (service.enabled) {
                            try {
                                sh "curl -f ${service.url} || exit 1"
                                echo "✓ ${service.name} is healthy"
                            } catch (Exception e) {
                                error("✗ ${service.name} health check failed")
                            }
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                echo "=========================================="
                echo "Pipeline Execution Summary"
                echo "=========================================="
                echo "Status: ${currentBuild.result ?: 'SUCCESS'}"
                echo "Duration: ${currentBuild.durationString}"
                echo "=========================================="
            }

            // Publish test results
            junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'

            // Archive artifacts
            archiveArtifacts artifacts: '**/target/*.jar', allowEmptyArchive: true, fingerprint: true

            // Clean up Docker images to save space
            script {
                sh 'docker system prune -f || true'
            }
        }

        success {
            script {
                echo "✓ Pipeline completed successfully!"

                // Send notification (configure as needed)
                // emailext (
                //     subject: "SUCCESS: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
                //     body: "Build succeeded: ${env.BUILD_URL}",
                //     to: "team@example.com"
                // )
            }
        }

        failure {
            script {
                echo "✗ Pipeline failed!"

                // Send notification
                // emailext (
                //     subject: "FAILURE: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
                //     body: "Build failed: ${env.BUILD_URL}",
                //     to: "team@example.com"
                // )
            }
        }

        unstable {
            script {
                echo "⚠ Pipeline is unstable!"
            }
        }
    }
}

// ============================================================================
// Helper Functions
// ============================================================================

def getServicesToBuild() {
    // Progressive migration: Start with these services and add more as you migrate
    // Modify this list to control which services are built by default
    return ['config-server', 'api-gateway', 'client-service', 'token-service',
            'product-service', 'cart-service', 'order-service']
}

def getServicesList() {
    return ['config-server', 'api-gateway', 'client-service', 'token-service',
            'product-service', 'cart-service', 'order-service']
}

def shouldBuild(serviceName) {
    if (params.BUILD_SCOPE == 'ALL') {
        return env.SERVICES_TO_BUILD.contains(serviceName.toLowerCase().replaceAll('_', '-'))
    }
    return params.BUILD_SCOPE == serviceName
}

def buildService(String serviceDir, String serviceName) {
    dir(serviceDir) {
        echo "Building ${serviceName}..."

        // Maven clean package
        def mavenGoals = 'clean package'
        if (!params.RUN_TESTS) {
            mavenGoals += ' -DskipTests'
        }

        // Use Maven wrapper
        if (isUnix()) {
            sh "./mvnw ${mavenGoals} -B -${params.LOG_LEVEL}"
        } else {
            bat "mvnw.cmd ${mavenGoals} -B -${params.LOG_LEVEL}"
        }

        // Run integration tests if requested
        if (params.RUN_INTEGRATION_TESTS) {
            echo "Running integration tests for ${serviceName}..."
            if (isUnix()) {
                sh "./mvnw verify -P integration-tests -B"
            } else {
                bat "mvnw.cmd verify -P integration-tests -B"
            }
        }

        echo "✓ ${serviceName} built successfully"
    }
}

def buildDockerImage(String serviceName) {
    dir(serviceName) {
        echo "Building Docker image for ${serviceName}..."

        def imageName = "${env.PROJECT_NAME}/${serviceName}"
        def imageTag = "${env.DEPLOY_ENV}-${env.GIT_COMMIT_SHORT}"
        def latestTag = "${env.DEPLOY_ENV}-latest"

        // Build Docker image
        sh """
            docker build \
                --build-arg BUILD_DATE=\$(date -u +'%Y-%m-%dT%H:%M:%SZ') \
                --build-arg VCS_REF=${env.GIT_COMMIT_SHORT} \
                --build-arg VERSION=${env.BUILD_NUMBER} \
                -t ${imageName}:${imageTag} \
                -t ${imageName}:${latestTag} \
                .
        """

        echo "✓ Docker image built: ${imageName}:${imageTag}"
    }
}

def performHealthChecks() {
    echo "Performing health checks..."

    def maxRetries = 10
    def retryDelay = 5

    def services = [
        [name: 'Config Server', port: 8888],
        [name: 'API Gateway', port: 8080],
        [name: 'Client Service', port: 8081],
        [name: 'Token Service', port: 8082],
        [name: 'Product Service', port: 8083],
        [name: 'Cart Service', port: 8084],
        [name: 'Order Service', port: 8085]
    ]

    services.each { service ->
        def healthy = false
        def attempt = 0

        while (!healthy && attempt < maxRetries) {
            try {
                sh "curl -f http://localhost:${service.port}/actuator/health || curl -f http://localhost:${service.port}/health || curl -f http://localhost:${service.port}/ping"
                healthy = true
                echo "✓ ${service.name} is healthy"
            } catch (Exception e) {
                attempt++
                echo "⚠ ${service.name} not ready yet (attempt ${attempt}/${maxRetries})"
                sleep(retryDelay)
            }
        }

        if (!healthy) {
            echo "⚠ ${service.name} health check timed out (may not be in build scope)"
        }
    }
}
