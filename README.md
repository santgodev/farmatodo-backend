# Plataforma de Microservicios Farmatodo

Una arquitectura completa de microservicios con Spring Boot que implementa una plataforma de e-commerce con procesamiento seguro de pagos, gestión de clientes, catálogo de productos, carrito de compras y gestión de pedidos.

## Tabla de Contenidos

- [Descripción General](#descripción-general)
- [Requerimientos Funcionales](#requerimientos-funcionales)
- [Arquitectura](#arquitectura)
- [Tecnologías](#tecnologías)
- [Servicios](#servicios)
- [Requisitos Previos](#requisitos-previos)
- [Guía de Instalación](#guía-de-instalación)
- [Configuración](#configuración)
- [Ejecutar la Aplicación](#ejecutar-la-aplicación)
- [Documentación de la API](#documentación-de-la-api)
- [Pruebas con Postman](#pruebas-con-postman)
- [Pipeline CI/CD](#pipeline-cicd)
- [Características de Seguridad](#características-de-seguridad)
- [Solución de Problemas](#solución-de-problemas)

---

## Descripción General

Farmatodo es una plataforma de e-commerce basada en microservicios diseñada con seguridad, escalabilidad y mantenibilidad en mente. La plataforma implementa prácticas modernas de DevOps incluyendo contenedorización, pruebas automatizadas, análisis estático de código y escaneo de vulnerabilidades.

---

## Requerimientos Funcionales

### 1. Tokenización de Tarjetas de Crédito

**Objetivo:** Manejo seguro de información de tarjetas de crédito mediante tokenización.

**Implementación:**
- **Componente:** Token Service (`token-service`)
- **Funcionalidad:**
  - Recibe datos de tarjeta de crédito (número, CVV, fecha de expiración, nombre del titular)
  - Genera un token único y seguro para la tarjeta
  - Almacena la relación token-tarjeta de forma segura en PostgreSQL
- **Seguridad:**
  - Autenticación de API mediante API Key o Secret Key
  - Los datos de la tarjeta nunca se almacenan en texto plano
  - La generación de tokens utiliza algoritmos seguros
- **Rechazo Configurable:**
  - Mecanismo de rechazo basado en probabilidad (configurable vía `application.yml`)
  - Ejemplo: tasa de rechazo del 30% simula fallos de pago del mundo real
  - Utilizado para probar la lógica de reintentos de pago
- **Endpoints:**
  - `POST /api/tokens/tokenize` - Tokenizar una tarjeta de crédito
  - `POST /api/tokens/payment` - Procesar pago con token

**Configuración:**
```yaml
payment:
  rejectionProbability: 0.3  # 30% de probabilidad de rechazo
```

### 2. API Ping (Verificación de Salud)

**Objetivo:** Proporcionar un endpoint simple para verificar la disponibilidad del servicio.

**Implementación:**
- **Disponible en TODOS los servicios**
- **Endpoints:**
  - `/ping` o `/health` o `/{service}/ping`
  - Retorna `pong` o estado del servicio
  - Código de estado HTTP 200
- **Propósito:**
  - Verificaciones de salud del balanceador de carga
  - Monitoreo y alertas
  - Verificación de descubrimiento de servicios

**Respuestas de Ejemplo:**
```json
// Ping simple
"pong"

// Verificación de salud con detalles
{
  "status": "UP",
  "service": "client-service",
  "timestamp": "2025-01-27T10:30:00Z",
  "database": "connected"
}
```

### 3. Gestión de Clientes

**Objetivo:** Sistema completo de registro y gestión de clientes.

**Implementación:**
- **Componente:** Client Service (`client-service`)
- **Funcionalidad:**
  - Registro de clientes con datos completos del perfil
  - Validación de datos y verificación de unicidad
  - Operaciones CRUD para gestión de clientes
- **Validaciones:**
  - **Email:** Debe ser único y formato válido
  - **Teléfono:** Debe ser único y formato válido
  - **Campos requeridos:** Nombre, email, teléfono, dirección
  - **Campos opcionales:** Teléfono secundario, direcciones adicionales
- **Base de Datos:** PostgreSQL con restricciones de unicidad
- **Endpoints:**
  - `POST /api/clients` - Registrar nuevo cliente
  - `GET /api/clients/{id}` - Obtener detalles del cliente
  - `PUT /api/clients/{id}` - Actualizar información del cliente
  - `DELETE /api/clients/{id}` - Eliminar cliente

**Modelo de Datos:**
```java
Client {
  id: Long
  name: String (requerido)
  email: String (único, requerido)
  phone: String (único, requerido)
  address: String (requerido)
  createdAt: Timestamp
  updatedAt: Timestamp
}
```

### 4. Búsqueda y Gestión de Productos

**Objetivo:** Búsqueda avanzada de productos con analíticas y gestión de inventario.

**Implementación:**
- **Componente:** Product Service (`product-service`)
- **Funcionalidad:**
  - Búsqueda de productos por nombre o descripción
  - Filtrado de productos por nivel mínimo de stock
  - Registro asíncrono de analíticas de búsqueda
- **Características de Búsqueda:**
  - Búsqueda de texto completo en nombre y descripción del producto
  - Filtrado de stock en tiempo real
  - Umbral mínimo de stock configurable
- **Analíticas Asíncronas:**
  - Todas las búsquedas se registran de forma asíncrona
  - No bloquea la operación principal de búsqueda
  - Almacena términos de búsqueda, marcas de tiempo y conteo de resultados
- **Visibilidad de Stock:**
  - Los productos con stock por debajo del umbral están ocultos en la búsqueda
  - Configurable mediante parámetro
  - Umbral predeterminado: 10 unidades
- **Endpoints:**
  - `GET /products?query={searchTerm}` - Buscar productos
  - `GET /products/low-stock?maxStock={threshold}` - Obtener productos con stock bajo
  - `GET /products/{id}` - Obtener detalles del producto

**Configuración:**
```yaml
products:
  minStockThreshold: 10
```

### 5. Carrito de Compras

**Objetivo:** Funcionalidad completa de carrito de compras para operaciones de e-commerce.

**Implementación:**
- **Componente:** Cart Service (`cart-service`)
- **Funcionalidad:**
  - Agregar productos al carrito
  - Actualizar cantidades de productos
  - Eliminar productos del carrito
  - Limpiar todo el carrito
  - Proceso de checkout (preparar para pago)
- **Características:**
  - Un carrito activo por usuario
  - Cálculo automático de totales
  - Subtotales a nivel de artículo
  - Seguimiento de estado del carrito (ACTIVE, COMPLETED, ABANDONED)
- **Seguridad:**
  - Se requiere autenticación con API Key
  - Aislamiento de carrito basado en usuario
- **Endpoints:**
  - `GET /carts/{userId}` - Obtener o crear carrito del usuario
  - `POST /carts/{userId}/items` - Agregar artículo al carrito
  - `PUT /carts/{userId}/items/{productId}` - Actualizar cantidad del artículo
  - `DELETE /carts/{userId}/items/{productId}` - Eliminar artículo
  - `DELETE /carts/{userId}` - Limpiar carrito
  - `POST /carts/{userId}/checkout` - Hacer checkout del carrito

**Modelo de Datos del Carrito:**
```java
Cart {
  id: Long
  userId: Long
  status: CartStatus (ACTIVE, COMPLETED, ABANDONED)
  items: List<CartItem>
  totalAmount: BigDecimal
  createdAt: Timestamp
  updatedAt: Timestamp
}

CartItem {
  id: Long
  productId: Long
  productName: String
  unitPrice: BigDecimal
  quantity: Integer
  subtotal: BigDecimal
}
```

### 6. Gestión de Pedidos y Procesamiento de Pagos

**Objetivo:** Ciclo de vida completo de pedidos con procesamiento integrado de pagos.

**Implementación:**
- **Componente:** Order Service (`order-service`)
- **Funcionalidad:**
  - Crear pedidos desde el carrito
  - Procesar pagos vía servicio de tokens
  - Lógica automática de reintentos para pagos fallidos
  - Seguimiento de estado de pedidos
  - Notificaciones por email
- **Lógica de Reintentos de Pago:**
  - Conteo de reintentos configurable (predeterminado: 3 intentos)
  - Retroceso exponencial entre reintentos
  - Notificación por email en caso de fallo final
- **Registro Centralizado:**
  - Todos los eventos se registran en base de datos central
  - Seguimiento de ID de transacción entre servicios
  - Pista de auditoría completa
- **Ciclo de Vida del Pedido:**
  1. PENDING - Pedido creado
  2. PROCESSING - Pago en progreso
  3. APPROVED - Pago exitoso
  4. REJECTED - Pago fallido después de reintentos

**Configuración:**
```yaml
payment:
  retryCount: 3
  rejectionProbability: 0.3
```

---

## Arquitectura

### Patrón de Microservicios

La plataforma sigue una arquitectura de microservicios con los siguientes principios:

- **Independencia de Servicios:** Cada servicio se puede desplegar independientemente
- **Base de Datos por Servicio:** Cada servicio tiene su propia base de datos PostgreSQL
- **API Gateway:** Punto de entrada centralizado para todas las solicitudes de clientes
- **Descubrimiento de Servicios:** Los servicios se comunican vía red interna
- **Gestión de Configuración:** Configuración centralizada vía Config Server

### Patrones de Comunicación

```
Solicitud del Cliente
     ↓
API Gateway (puerto 8080)
     ↓
Enrutamiento Interno
     ↓
┌─────────────┬─────────────┬─────────────┬─────────────┬─────────────┐
│   Client    │   Token     │  Product    │    Cart     │   Order     │
│  Service    │  Service    │  Service    │  Service    │  Service    │
│  (8081)     │  (8082)     │  (8083)     │  (8084)     │  (8085)     │
└─────────────┴─────────────┴─────────────┴─────────────┴─────────────┘
     ↓              ↓              ↓              ↓              ↓
┌─────────────┬─────────────┬─────────────┬─────────────┬─────────────┐
│  clientdb   │  tokendb    │ productdb   │   cartdb    │  orderdb    │
│ (5432)      │ (5433)      │ (5434)      │  (5435)     │  (5436)     │
└─────────────┴─────────────┴─────────────┴─────────────┴─────────────┘
```

---

## Tecnologías

### Backend
- **Java 17** - Lenguaje de programación
- **Spring Boot 3.5.6** - Framework de aplicación
- **Spring Cloud 2025.0.0** - Toolkit de microservicios
- **Spring Cloud Gateway** - API Gateway
- **Spring Data JPA** - Persistencia de datos
- **Maven** - Automatización de construcción
- **Lombok** - Reducción de código repetitivo

### Base de Datos
- **PostgreSQL** - Base de datos relacional (una por servicio)

### DevOps e Infraestructura
- **Docker** - Contenedorización
- **Docker Compose** - Orquestación de múltiples contenedores
- **Jenkins** - Automatización CI/CD
- **SonarQube** - Análisis estático de código (SAST)
- **Trivy** - Escaneo de vulnerabilidades (seguridad de contenedores)
- **Git** - Control de versiones

### Pruebas
- **Postman** - Pruebas y documentación de API

---

## Servicios

### 1. API Gateway (Puerto 8080)
- **Propósito:** Punto de entrada único para todas las solicitudes de clientes
- **Tecnología:** Spring Cloud Gateway (WebMVC)
- **Características:**
  - Enrutamiento de solicitudes a servicios backend
  - Balanceo de carga
  - Preocupaciones transversales (logging, seguridad)

### 2. Config Server (Puerto 8888/8889)
- **Propósito:** Gestión centralizada de configuración
- **Tecnología:** Spring Cloud Config
- **Características:**
  - Almacenamiento de configuración externa
  - Configuraciones específicas por ambiente
  - Actualizaciones de configuración en tiempo real

### 3. Client Service (Puerto 8081)
- **Propósito:** Gestión de clientes
- **Base de Datos:** clientdb (puerto 5432)
- **Características:**
  - Registro de clientes
  - Gestión de perfiles
  - Validación de datos

### 4. Token Service (Puerto 8082)
- **Propósito:** Tokenización segura de pagos
- **Base de Datos:** tokendb (puerto 5433)
- **Características:**
  - Tokenización de tarjetas de crédito
  - Procesamiento de pagos
  - Simulación de rechazo configurable

### 5. Product Service (Puerto 8083)
- **Propósito:** Gestión de catálogo de productos
- **Base de Datos:** productdb (puerto 5434)
- **Características:**
  - Búsqueda de productos
  - Gestión de inventario
  - Analíticas de búsqueda asíncronas

### 6. Cart Service (Puerto 8084)
- **Propósito:** Operaciones de carrito de compras
- **Base de Datos:** cartdb (puerto 5435)
- **Características:**
  - Gestión de carrito
  - Operaciones de artículos
  - Preparación de checkout

### 7. Order Service (Puerto 8085)
- **Propósito:** Orquestación de pedidos y pagos
- **Base de Datos:** orderdb (puerto 5436)
- **Características:**
  - Creación de pedidos
  - Procesamiento de pagos con reintentos
  - Registro centralizado
  - Notificaciones por email

---

## Requisitos Previos

Antes de instalar la plataforma Farmatodo, asegúrate de tener lo siguiente instalado en tu máquina:

### Software Requerido

1. **Java Development Kit (JDK) 17 o superior**
   - Descarga: https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html
   - Alternativa: OpenJDK https://adoptium.net/
   - Verificar instalación: `java -version`

2. **Maven 3.9.9 o superior**
   - Descarga: https://maven.apache.org/download.cgi
   - Verificar instalación: `mvn -version`

3. **Docker Desktop**
   - Descarga: https://www.docker.com/products/docker-desktop
   - Incluye Docker y Docker Compose
   - Verificar instalación: `docker --version` y `docker-compose --version`

4. **Git**
   - Descarga: https://git-scm.com/downloads
   - Verificar instalación: `git --version`

5. **Cliente PostgreSQL (Opcional, para inspección de base de datos)**
   - Descarga: https://www.postgresql.org/download/
   - O usa pgAdmin: https://www.pgadmin.org/

6. **Postman (Opcional, para pruebas de API)**
   - Descarga: https://www.postman.com/downloads/

### Requerimientos del Sistema

- **Sistema Operativo:** Windows 10/11, macOS 10.15+, o Linux
- **RAM:** Mínimo 8GB (16GB recomendado)
- **Espacio en Disco:** Mínimo 10GB de espacio libre
- **CPU:** 4 núcleos recomendado

---

## Guía de Instalación

Sigue estos pasos para instalar y ejecutar la plataforma Farmatodo desde cero en una máquina nueva.

### Paso 1: Clonar el Repositorio

Abre una terminal/símbolo del sistema y clona el repositorio:

```bash
# Clonar el repositorio
git clone https://github.com/santgodev/farmatodo-backend.git

# Navegar al directorio del proyecto
cd farmatodo-backend
```

### Paso 2: Verificar Instalación de Java y Maven

Asegúrate de que Java y Maven estén instalados correctamente:

```bash
# Verificar versión de Java (debe ser 17 o superior)
java -version

# Verificar versión de Maven (debe ser 3.9.9 o superior)
mvn -version
```

### Paso 3: Construir Todos los Servicios

Construye todos los microservicios usando Maven:

#### En Linux/macOS:
```bash
# Construir todos los servicios secuencialmente
cd api-gateway && ./mvnw clean package && cd .. && \
cd client-service && ./mvnw clean package && cd .. && \
cd token-service && ./mvnw clean package && cd .. && \
cd product-service && ./mvnw clean package && cd .. && \
cd cart-service && ./mvnw clean package && cd .. && \
cd order-service && ./mvnw clean package && cd .. && \
cd config-server && ./mvnw clean package && cd ..
```

#### En Windows:
```cmd
# Construir cada servicio individualmente
cd api-gateway
mvnw.cmd clean package
cd ..

cd client-service
mvnw.cmd clean package
cd ..

cd token-service
mvnw.cmd clean package
cd ..

cd product-service
mvnw.cmd clean package
cd ..

cd cart-service
mvnw.cmd clean package
cd ..

cd order-service
mvnw.cmd clean package
cd ..

cd config-server
mvnw.cmd clean package
cd ..
```

**Nota:** La primera construcción puede tomar varios minutos mientras Maven descarga las dependencias.

### Paso 4: Verificar Instalación de Docker

Asegúrate de que Docker esté ejecutándose:

```bash
# Verificar versión de Docker
docker --version

# Verificar versión de Docker Compose
docker-compose --version

# Verificar que Docker esté ejecutándose
docker ps
```

Si Docker no está ejecutándose, inicia la aplicación Docker Desktop.

### Paso 5: Construir Imágenes Docker

Construye las imágenes Docker para todos los servicios:

```bash
# Construir todas las imágenes Docker usando Docker Compose
docker-compose build
```

Este comando:
- Crea imágenes Docker para los 7 microservicios
- Configura contenedores de base de datos PostgreSQL
- Configura la red entre servicios

**Nota:** Este proceso puede tomar 10-15 minutos en la primera ejecución.

### Paso 6: Iniciar Todos los Servicios

Inicia la plataforma completa:

```bash
# Iniciar todos los servicios en modo desacoplado
docker-compose up -d
```

Esto iniciará:
- 7 microservicios (api-gateway, config-server, client-service, token-service, product-service, cart-service, order-service)
- 5 bases de datos PostgreSQL
- Configuración de red

### Paso 7: Verificar que los Servicios Estén Ejecutándose

Verifica que todos los contenedores estén ejecutándose:

```bash
# Listar todos los contenedores en ejecución
docker ps

# Salida esperada: 12 contenedores ejecutándose (7 servicios + 5 bases de datos)
```

Verificar logs de servicios:

```bash
# Ver logs de todos los servicios
docker-compose logs -f

# Ver logs de un servicio específico
docker-compose logs -f client-service
```

### Paso 8: Verificar Salud de los Servicios

Prueba cada endpoint de servicio:

```bash
# Probar API Gateway
curl http://localhost:8080/api/gateway/health

# Probar Client Service
curl http://localhost:8081/api/clients/health

# Probar Token Service
curl http://localhost:8082/api/tokens/health

# Probar Product Service
curl http://localhost:8083/products/health

# Probar Cart Service
curl http://localhost:8084/carts/health

# Probar Order Service
curl http://localhost:8085/orders/ping
```

Todos los endpoints deben retornar HTTP 200 con respuesta exitosa.

### Paso 9: Inicializar Base de Datos (Opcional)

Si necesitas inicializar las bases de datos con datos de ejemplo:

1. Acceder al contenedor de base de datos:
```bash
docker exec -it farmatodo-clientdb-1 psql -U postgres -d clientdb
```

2. Ejecutar tus scripts SQL o usar los datos semilla proporcionados.

3. Salir: `\q`

### Paso 10: Importar Colección de Postman

1. Abrir Postman
2. Hacer clic en el botón **Import**
3. Seleccionar los siguientes archivos del repositorio:
   - `Farmatodo-Postman-Collection.json`
   - `Farmatodo-Postman-Environment.json`
4. La colección incluirá todos los endpoints de API con ejemplos
5. Seleccionar el ambiente "Farmatodo-Local" del menú desplegable de ambientes

---

## Configuración

### Variables de Entorno

Cada servicio puede configurarse mediante variables de entorno o archivos `application.yml`.

#### Propiedades de Configuración Clave

**Token Service (`token-service/src/main/resources/application.yml`):**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/tokendb
    username: postgres
    password: postgres

payment:
  rejectionProbability: 0.3  # 30% tasa de rechazo
```

**Order Service (`order-service/src/main/resources/application.yml`):**
```yaml
payment:
  retryCount: 3  # Número de intentos de reintento de pago
  rejectionProbability: 0.3

clients:
  service:
    url: http://client-service:8081  # URL de servicio interno
```

**Cart Service (`cart-service/src/main/resources/application.yml`):**
```yaml
security:
  api-key: cart-service-api-key-change-in-production
```

### Configuración de Base de Datos

Cada servicio tiene su propia base de datos PostgreSQL:

| Servicio | Base de Datos | Puerto | Usuario | Contraseña |
|---------|--------------|--------|---------|-----------|
| Client | clientdb | 5432 | postgres | postgres |
| Token | tokendb | 5433 | postgres | postgres |
| Product | productdb | 5434 | postgres | postgres |
| Cart | cartdb | 5435 | postgres | postgres |
| Order | orderdb | 5436 | postgres | postgres |

### Modificar Configuración

1. **Para desarrollo local:**
   - Editar archivos `application.yml` en cada servicio
   - Reconstruir y reiniciar el servicio

2. **Para despliegue Docker:**
   - Editar `docker-compose.yml`
   - Agregar variables de entorno bajo definición de servicio
   - Reiniciar servicios: `docker-compose restart <service-name>`

---

## Ejecutar la Aplicación

### Usando Docker Compose (Recomendado)

Iniciar todos los servicios:
```bash
docker-compose up -d
```

Detener todos los servicios:
```bash
docker-compose down
```

Detener y eliminar volúmenes (inicio limpio):
```bash
docker-compose down -v
```

Reiniciar un servicio específico:
```bash
docker-compose restart client-service
```

Ver logs de servicio:
```bash
docker-compose logs -f client-service
```

### Ejecutar Servicios Individuales Localmente (Desarrollo)

Para desarrollo, puedes ejecutar servicios individualmente:

```bash
# Navegar al directorio del servicio
cd client-service

# Ejecutar con Maven
./mvnw spring-boot:run

# O en Windows
mvnw.cmd spring-boot:run
```

**Nota:** Asegúrate de que PostgreSQL esté ejecutándose y accesible antes de iniciar servicios localmente.

---

## Documentación de la API

### URLs Base

- **Producción/Docker:** `http://localhost:8080` (vía API Gateway)
- **Acceso Directo a Servicios:**
  - Client Service: `http://localhost:8081`
  - Token Service: `http://localhost:8082`
  - Product Service: `http://localhost:8083`
  - Cart Service: `http://localhost:8084`
  - Order Service: `http://localhost:8085`

### Endpoints del API Gateway

```
GET  /api/gateway/health  - Verificación de salud
GET  /api/gateway/info    - Información del servicio
```

### Endpoints del Client Service

```
POST   /api/clients              - Crear nuevo cliente
GET    /api/clients/{id}         - Obtener cliente por ID
PUT    /api/clients/{id}         - Actualizar cliente
DELETE /api/clients/{id}         - Eliminar cliente
GET    /api/clients/health       - Verificación de salud
```

**Ejemplo de Crear Cliente:**
```json
POST /api/clients
Content-Type: application/json

{
  "name": "Juan Pérez",
  "email": "juan.perez@example.com",
  "phone": "+573001234567",
  "address": "Calle 123 #45-67, Bogotá, Colombia"
}
```

### Endpoints del Token Service

```
POST /api/tokens/tokenize  - Tokenizar tarjeta de crédito
POST /api/tokens/payment   - Procesar pago
GET  /api/tokens/health    - Verificación de salud
```

**Ejemplo de Tokenizar Tarjeta:**
```json
POST /api/tokens/tokenize
Content-Type: application/json

{
  "cardNumber": "4532015112830366",
  "cvv": "123",
  "expirationDate": "12/25",
  "cardholderName": "Juan Pérez"
}
```

**Respuesta:**
```json
{
  "token": "tok_abc123xyz789",
  "status": "APPROVED",
  "message": "Tokenización exitosa"
}
```

### Endpoints del Product Service

```
GET /products?query={term}           - Buscar productos
GET /products/low-stock?maxStock={n} - Obtener productos con stock bajo
GET /products/{id}                   - Obtener producto por ID
GET /products/health                 - Verificación de salud
```

**Ejemplo de Búsqueda de Productos:**
```
GET /products?query=aspirina
```

### Endpoints del Cart Service

**Autenticación Requerida:** Todos los endpoints del carrito requieren API Key en el header:
```
Authorization: ApiKey cart-service-api-key-change-in-production
```

```
GET    /carts/{userId}                      - Obtener carrito del usuario
POST   /carts/{userId}/items                - Agregar artículo al carrito
PUT    /carts/{userId}/items/{productId}    - Actualizar cantidad del artículo
DELETE /carts/{userId}/items/{productId}    - Eliminar artículo del carrito
DELETE /carts/{userId}                      - Limpiar carrito
POST   /carts/{userId}/checkout             - Hacer checkout del carrito
GET    /carts/health                        - Verificación de salud
```

**Ejemplo de Agregar Artículo al Carrito:**
```json
POST /carts/1/items
Authorization: ApiKey cart-service-api-key-change-in-production
Content-Type: application/json

{
  "productId": 101,
  "productName": "Aspirina 500mg",
  "unitPrice": 10.50,
  "quantity": 2
}
```

### Endpoints del Order Service

```
POST /orders       - Crear nuevo pedido
GET  /orders/{id}  - Obtener pedido por ID
GET  /orders/ping  - Verificación de salud
```

**Ejemplo de Crear Pedido:**
```json
POST /orders
Content-Type: application/json

{
  "clientId": 1,
  "token": "tok_abc123xyz789",
  "products": [
    {
      "productId": 101,
      "productName": "Aspirina 500mg",
      "unitPrice": 10.50,
      "quantity": 2
    }
  ]
}
```

---

## Pruebas con Postman

### Importar Colección

1. Abrir Postman
2. Hacer clic en **Import**
3. Seleccionar archivos:
   - `Farmatodo-Postman-Collection.json`
   - `Farmatodo-Postman-Environment.json`

### Usando la Colección

La colección incluye carpetas organizadas para cada servicio con solicitudes de ejemplo:

1. **Pruebas de Client Service**
   - Crear Cliente
   - Obtener Cliente
   - Actualizar Cliente
   - Eliminar Cliente

2. **Pruebas de Token Service**
   - Tokenizar Tarjeta
   - Procesar Pago

3. **Pruebas de Product Service**
   - Buscar Productos
   - Obtener Productos con Stock Bajo

4. **Pruebas de Cart Service**
   - Obtener Carrito
   - Agregar Artículo
   - Actualizar Cantidad
   - Eliminar Artículo
   - Checkout

5. **Pruebas de Order Service**
   - Crear Pedido
   - Obtener Estado del Pedido

### Flujo de Pruebas

Sigue la guía en `POSTMAN-GUIDE.md` para un flujo completo de pruebas.

**Secuencia Rápida de Prueba:**
1. Crear un cliente
2. Buscar productos
3. Agregar productos al carrito
4. Tokenizar una tarjeta de crédito
5. Crear un pedido con el token
6. Verificar estado del pedido

---

## Pipeline CI/CD

La plataforma usa Jenkins para integración y despliegue continuo con escaneo integral de seguridad.

### Etapas del Pipeline

Cada servicio sigue este pipeline:

1. **Checkout & Build**
   - Clonar repositorio
   - Compilar con Maven
   - Ejecutar pruebas unitarias

2. **Análisis SonarQube (SAST)**
   - Análisis estático de código
   - Métricas de calidad de código
   - Detección de vulnerabilidades de seguridad

3. **Escaneo de Código Fuente con Trivy**
   - Escaneo de vulnerabilidades del sistema de archivos
   - Detección de vulnerabilidades en dependencias
   - Escanea vulnerabilidades HIGH y CRITICAL

4. **Construir Imagen Docker**
   - Crear aplicación contenedorizada
   - Etiquetar con versión

5. **Escaneo de Imagen con Trivy**
   - Escaneo de vulnerabilidades de imagen Docker
   - Detección de vulnerabilidades de imagen base
   - Validación de mejores prácticas de seguridad

6. **Despliegue**
   - Detener contenedor existente
   - Iniciar nuevo contenedor
   - Verificación de salud

### Configuración del Jenkinsfile

Cada servicio tiene un `Jenkinsfile` con:
- Configuración de JDK 17
- Configuración de Maven 3.9.9
- Integración con SonarQube
- Escaneo de seguridad con Trivy
- Construcción y despliegue Docker
- Archivado de artefactos

### Ejecutar CI/CD Localmente

Para simular el pipeline CI/CD localmente:

```bash
# Construir
mvn clean package

# Ejecutar escaneo de sistema de archivos con Trivy
trivy fs . --severity HIGH,CRITICAL

# Construir imagen Docker
docker build -t farmatodo/client-service:latest .

# Ejecutar escaneo de imagen con Trivy
trivy image farmatodo/client-service:latest --severity HIGH,CRITICAL

# Desplegar
docker run -d --name client-service -p 8081:8081 farmatodo/client-service:latest
```

---

## Características de Seguridad

### 1. Autenticación de API

**Cart Service:**
- Se requiere autenticación con API Key para todos los endpoints
- La clave se configura en `application.yml`
- Header: `Authorization: ApiKey {key}`

### 2. Tokenización de Tarjetas de Crédito

**Token Service:**
- Nunca almacena datos de tarjeta de crédito en texto plano
- Genera tokens únicos y seguros
- Proceso de tokenización unidireccional
- Mapeo token-tarjeta encriptado en base de datos

### 3. Pruebas de Seguridad de Aplicación Estática (SAST)

**Integración con SonarQube:**
- Análisis automatizado de calidad de código
- Detección de vulnerabilidades de seguridad
- Identificación de code smells
- Seguimiento de deuda técnica

### 4. Escaneo de Seguridad de Contenedores

**Integración con Trivy:**
- Escaneo de vulnerabilidades del sistema de archivos
- Escaneo de imágenes Docker
- Detección de vulnerabilidades en dependencias
- Monitoreo continuo de seguridad

### 5. Seguridad de Base de Datos

- Bases de datos separadas por servicio
- Acceso protegido por contraseña
- Pool de conexiones
- Declaraciones preparadas (prevención de inyección SQL)

### 6. Seguridad de Red

**Red Docker:**
- Comunicación interna entre servicios
- Sin acceso externo directo a bases de datos
- API Gateway como punto de entrada único

---

## Solución de Problemas

### Problemas Comunes y Soluciones

#### 1. Los Servicios No Inician

**Problema:** Los contenedores fallan al iniciar o se cierran inmediatamente.

**Solución:**
```bash
# Verificar logs del contenedor
docker-compose logs <service-name>

# Causas comunes:
# - Puerto ya en uso
# - Base de datos no lista
# - Errores de configuración

# Corregir conflictos de puerto
# Editar docker-compose.yml y cambiar puertos del host

# Reiniciar con bases de datos limpias
docker-compose down -v
docker-compose up -d
```

#### 2. Errores de Conexión a Base de Datos

**Problema:** El servicio no puede conectarse a la base de datos.

**Solución:**
```bash
# Verificar que la base de datos esté ejecutándose
docker ps | grep postgres

# Verificar logs de la base de datos
docker logs <database-container-name>

# Verificar cadena de conexión en application.yml
# Asegurar que el hostname coincida con el nombre del servicio en docker-compose
```

#### 3. Fallos en Construcción de Maven

**Problema:** La construcción de Maven falla con errores de dependencias.

**Solución:**
```bash
# Limpiar caché de Maven
mvn clean

# Forzar actualización de dependencias
mvn clean install -U

# Saltar pruebas si es necesario
mvn clean package -DskipTests
```

#### 4. Fallos en Construcción de Docker

**Problema:** La construcción de imagen Docker falla.

**Solución:**
```bash
# Asegurar que la construcción de Maven fue exitosa primero
mvn clean package

# Verificar que el daemon de Docker esté ejecutándose
docker info

# Intentar construir sin caché
docker-compose build --no-cache
```

#### 5. Conflictos de Puerto

**Problema:** Error de puerto ya en uso.

**Solución:**
```bash
# En Windows: Encontrar proceso usando el puerto
netstat -ano | findstr :8081

# En Linux/Mac: Encontrar proceso usando el puerto
lsof -i :8081

# Matar el proceso o cambiar puerto en docker-compose.yml
```

#### 6. Errores de Memoria Insuficiente

**Problema:** Los servicios se cierran con OutOfMemoryError.

**Solución:**
```bash
# Aumentar límite de memoria de Docker en configuración de Docker Desktop
# Recomendado: 8GB mínimo

# O reducir número de servicios ejecutándose
# Ejecutar solo servicios esenciales para desarrollo
```

#### 7. Fallos en Pruebas de Postman

**Problema:** Las solicitudes de API retornan errores en Postman.

**Solución:**
- Verificar que los servicios estén ejecutándose: `docker ps`
- Verificar endpoints de salud de servicios
- Asegurar que el ambiente correcto esté seleccionado en Postman
- Verificar API keys en headers (para Cart Service)
- Verificar formato del cuerpo de solicitud (JSON)

### Logs y Depuración

**Ver todos los logs de servicios:**
```bash
docker-compose logs -f
```

**Ver logs de servicio específico:**
```bash
docker-compose logs -f client-service
```

**Seguir logs en tiempo real:**
```bash
docker-compose logs -f --tail=100
```

**Acceder al contenedor del servicio:**
```bash
docker exec -it <container-name> /bin/sh
```

**Acceder a la base de datos:**
```bash
docker exec -it farmatodo-clientdb-1 psql -U postgres -d clientdb
```

### Obtener Ayuda

Si encuentras problemas no cubiertos aquí:

1. Consulta el rastreador de problemas: https://github.com/santgodev/farmatodo-backend/issues
2. Revisa logs específicos del servicio
3. Verifica que todos los requisitos previos estén instalados
4. Asegúrate de que Docker tenga recursos suficientes asignados

---

## Estructura del Proyecto

```
farmatodo-backend/
├── api-gateway/                 # Servicio API Gateway
│   ├── src/
│   ├── Dockerfile
│   ├── Jenkinsfile
│   ├── pom.xml
│   └── sonar-project.properties
├── config-server/              # Servicio de configuración
│   ├── src/
│   ├── Dockerfile
│   ├── Jenkinsfile
│   ├── pom.xml
│   └── sonar-project.properties
├── client-service/             # Servicio de gestión de clientes
│   ├── src/
│   ├── Dockerfile
│   ├── Jenkinsfile
│   ├── pom.xml
│   └── sonar-project.properties
├── token-service/              # Servicio de tokenización de pagos
│   ├── src/
│   ├── Dockerfile
│   ├── Jenkinsfile
│   ├── pom.xml
│   └── sonar-project.properties
├── product-service/            # Servicio de catálogo de productos
│   ├── src/
│   ├── Dockerfile
│   ├── Jenkinsfile
│   ├── pom.xml
│   └── sonar-project.properties
├── cart-service/               # Servicio de carrito de compras
│   ├── src/
│   ├── Dockerfile
│   ├── Jenkinsfile
│   ├── pom.xml
│   └── sonar-project.properties
├── order-service/              # Servicio de gestión de pedidos
│   ├── src/
│   ├── Dockerfile
│   ├── Jenkinsfile
│   ├── pom.xml
│   └── sonar-project.properties
├── docker-compose.yml          # Orquestación Docker
├── Farmatodo-Postman-Collection.json
├── Farmatodo-Postman-Environment.json
├── POSTMAN-GUIDE.md
├── CLAUDE.md
├── README.md                   # Versión en inglés
└── README_ES.md                # Este archivo (versión en español)
```

---

## Contribuciones

¡Las contribuciones son bienvenidas! Por favor sigue estas pautas:

1. Hacer fork del repositorio
2. Crear una rama de característica: `git checkout -b feature/nueva-caracteristica`
3. Hacer commit de tus cambios: `git commit -am 'Agregar nueva característica'`
4. Hacer push a la rama: `git push origin feature/nueva-caracteristica`
5. Crear un Pull Request

---

## Licencia

Este proyecto es parte de un ejercicio educativo y no está licenciado para uso comercial.

---

## Autores

- **Santiago González** - [@santgodev](https://github.com/santgodev)

---

## Agradecimientos

- Documentación de Spring Boot
- Documentación de Docker
- Patrones de diseño de microservicios
- Mejores prácticas de DevOps

---

**Última Actualización:** 27 de Enero de 2025
**Versión:** 1.0.0
