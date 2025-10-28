# Guía de Deployment a Google Cloud Platform (GCP)

Esta guía explica cómo desplegar la aplicación Farmatodo completa a Google Cloud Platform usando Docker Compose en Google Compute Engine.

## 📋 Tabla de Contenidos

- [Visión General](#visión-general)
- [Prerequisitos](#prerequisitos)
- [Configuración Inicial](#configuración-inicial)
- [Deployment con Jenkins](#deployment-con-jenkins)
- [Deployment Manual](#deployment-manual)
- [URLs y Acceso](#urls-y-acceso)
- [Gestión de la Aplicación](#gestión-de-la-aplicación)
- [Troubleshooting](#troubleshooting)
- [Costos Estimados](#costos-estimados)
- [Seguridad](#seguridad)

---

## 🎯 Visión General

Este deployment despliega **toda la aplicación Farmatodo** a GCP usando:

- **Google Compute Engine (GCE)**: VM que ejecuta Docker Compose
- **Google Artifact Registry**: Almacenamiento de imágenes Docker
- **Docker Compose**: Orquestación de todos los servicios (tal cual como localmente)

### Arquitectura de Deployment

```
┌─────────────────────────────────────────────────────┐
│              Google Cloud Platform                  │
│                                                     │
│  ┌──────────────────────────────────────────────┐  │
│  │      Artifact Registry                       │  │
│  │  (Imágenes Docker de los servicios)          │  │
│  └──────────────────────────────────────────────┘  │
│                       ↓                             │
│  ┌──────────────────────────────────────────────┐  │
│  │   Compute Engine VM (e2-standard-4)          │  │
│  │                                              │  │
│  │  ┌────────────────────────────────────────┐ │  │
│  │  │      Docker Compose                    │ │  │
│  │  │                                        │ │  │
│  │  │  • Config Server     (8888)           │ │  │
│  │  │  • API Gateway       (9090)           │ │  │
│  │  │  • Client Service    (8081)           │ │  │
│  │  │  • Token Service     (8082)           │ │  │
│  │  │  • Product Service   (8083)           │ │  │
│  │  │  • Cart Service      (8084)           │ │  │
│  │  │  • Order Service     (8085)           │ │  │
│  │  │                                        │ │  │
│  │  │  • Client DB         (5432)           │ │  │
│  │  │  • Token DB          (5433)           │ │  │
│  │  │  • Product DB        (5434)           │ │  │
│  │  │  • Cart DB           (5435)           │ │  │
│  │  │  • Order DB          (5436)           │ │  │
│  │  └────────────────────────────────────────┘ │  │
│  └──────────────────────────────────────────────┘  │
│                       ↑                             │
│            (IP Pública Accesible)                   │
└─────────────────────────────────────────────────────┘
```

### Servicios Desplegados

| Servicio | Puerto | URL Pública |
|----------|--------|-------------|
| Config Server | 8888 | `http://<VM_IP>:8888/actuator/health` |
| API Gateway | 9090 | `http://<VM_IP>:9090/api/gateway/health` |
| Client Service | 8081 | `http://<VM_IP>:8081/api/clients/health` |
| Token Service | 8082 | `http://<VM_IP>:8082/api/tokens/health` |
| Product Service | 8083 | `http://<VM_IP>:8083/products/health` |
| Cart Service | 8084 | `http://<VM_IP>:8084/carts/health` |
| Order Service | 8085 | `http://<VM_IP>:8085/orders/ping` |

---

## 📦 Prerequisitos

### 1. Cuenta de Google Cloud Platform

- Tener una cuenta de GCP activa
- Proyecto de GCP creado
- Billing habilitado en el proyecto

### 2. Herramientas Locales

```bash
# Google Cloud SDK
gcloud --version

# Docker
docker --version

# Maven (para builds locales)
mvn --version

# Git
git --version
```

### 3. APIs de GCP Habilitadas

Habilita las siguientes APIs en tu proyecto de GCP:

```bash
gcloud services enable compute.googleapis.com
gcloud services enable artifactregistry.googleapis.com
gcloud services enable cloudresourcemanager.googleapis.com
```

O manualmente en: https://console.cloud.google.com/apis/library

---

## ⚙️ Configuración Inicial

### 1. Configurar GCP Project

```bash
# Autenticarse
gcloud auth login

# Configurar proyecto
gcloud config set project planar-momentum-469121-n0

# Verificar configuración
gcloud config list
```

### 2. Crear Service Account (Para Jenkins)

```bash
# Crear service account
gcloud iam service-accounts create farmatodo-deployer \
    --description="Service account for Farmatodo deployment" \
    --display-name="Farmatodo Deployer"

# Asignar roles necesarios
gcloud projects add-iam-policy-binding planar-momentum-469121-n0 \
    --member="serviceAccount:farmatodo-deployer@planar-momentum-469121-n0.iam.gserviceaccount.com" \
    --role="roles/compute.admin"

gcloud projects add-iam-policy-binding planar-momentum-469121-n0 \
    --member="serviceAccount:farmatodo-deployer@planar-momentum-469121-n0.iam.gserviceaccount.com" \
    --role="roles/artifactregistry.admin"

# Crear y descargar key
gcloud iam service-accounts keys create ~/gcp-key.json \
    --iam-account=farmatodo-deployer@planar-momentum-469121-n0.iam.gserviceaccount.com
```

### 3. Configurar Credenciales en Jenkins

1. Ir a Jenkins → Manage Jenkins → Credentials
2. Agregar nueva credencial tipo "Secret file"
3. ID: `gcp-credentials`
4. File: Subir el archivo `gcp-key.json`

---

## 🚀 Deployment con Jenkins

### Opción 1: Pipeline Completo

1. **Crear un nuevo Job en Jenkins**
   - Tipo: Pipeline
   - Nombre: `farmatodo-gcp-deployment`

2. **Configurar el Pipeline**
   - Pipeline script from SCM
   - SCM: Git
   - Repository URL: `https://github.com/santgodev/farmatodo-backend.git`
   - Script Path: `Jenkinsfile.gcp-deploy`

3. **Ejecutar el Pipeline**
   ```
   Click en "Build Now"
   ```

### Opción 2: Usando el Jenkinsfile

```bash
# Desde la raíz del proyecto
git add Jenkinsfile.gcp-deploy
git commit -m "Add GCP deployment pipeline"
git push origin main
```

Luego en Jenkins:
- Crear Pipeline desde SCM apuntando a `Jenkinsfile.gcp-deploy`
- Ejecutar

### Variables de Entorno del Pipeline

Puedes ajustar las siguientes variables en `Jenkinsfile.gcp-deploy`:

| Variable | Valor por Defecto | Descripción |
|----------|-------------------|-------------|
| `GCP_PROJECT_ID` | `planar-momentum-469121-n0` | ID del proyecto GCP |
| `REGION` | `us-central1` | Región de GCP |
| `ZONE` | `us-central1-a` | Zona de GCP |
| `REPOSITORY` | `farmatodo-repo` | Nombre del repo en Artifact Registry |
| `VM_INSTANCE_NAME` | `farmatodo-app-vm` | Nombre de la VM |
| `VM_MACHINE_TYPE` | `e2-standard-4` | Tipo de máquina (4 vCPUs, 16GB RAM) |
| `VM_BOOT_DISK_SIZE` | `100GB` | Tamaño del disco |

### Stages del Pipeline

1. **Checkout**: Clona el repositorio
2. **Build All Services**: Compila todos los servicios Java con Maven
3. **Build Docker Images**: Crea imágenes Docker localmente
4. **Push to Artifact Registry**: Sube imágenes a GCP
5. **Setup GCE VM**: Crea/actualiza la VM
6. **Deploy to GCE**: Despliega con docker-compose
7. **Health Checks**: Verifica que todos los servicios estén funcionando

---

## 🛠️ Deployment Manual

Si prefieres desplegar sin Jenkins, usa el script `deploy-to-gcp.sh`:

### 1. Dar permisos de ejecución

```bash
chmod +x deploy-to-gcp.sh
```

### 2. Configurar variables

Edita el script y ajusta estas variables:

```bash
GCP_PROJECT_ID="planar-momentum-469121-n0"
REGION="us-central1"
ZONE="us-central1-a"
REPOSITORY="farmatodo-repo"
VM_INSTANCE_NAME="farmatodo-app-vm"
VM_MACHINE_TYPE="e2-standard-4"
VM_BOOT_DISK_SIZE="100GB"
```

### 3. Ejecutar

```bash
./deploy-to-gcp.sh
```

El script ejecutará automáticamente todos los pasos:
1. Autenticación con GCP
2. Build de todos los servicios
3. Build de imágenes Docker
4. Creación del Artifact Registry
5. Push de imágenes
6. Creación de la VM
7. Configuración de firewall
8. Deployment de la aplicación
9. Verificación de salud

---

## 🌐 URLs y Acceso

Una vez completado el deployment, obtendrás una salida como esta:

```
╔════════════════════════════════════════════════════════════════╗
║                   ✅ DEPLOYMENT SUCCESSFUL                      ║
╚════════════════════════════════════════════════════════════════╝

🌐 VM External IP: 34.123.45.67

📍 Public Service URLs:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  🔧 Config Server:   http://34.123.45.67:8888/actuator/health
  🚪 API Gateway:     http://34.123.45.67:9090/api/gateway/health
  👤 Client Service:  http://34.123.45.67:8081/api/clients/health
  🔑 Token Service:   http://34.123.45.67:8082/api/tokens/health
  📦 Product Service: http://34.123.45.67:8083/products/health
  🛒 Cart Service:    http://34.123.45.67:8084/carts/health
  📋 Order Service:   http://34.123.45.67:8085/orders/ping
```

### Probar los Servicios

```bash
# Reemplaza <VM_IP> con tu IP pública
VM_IP="34.123.45.67"

# Test Config Server
curl http://${VM_IP}:8888/actuator/health

# Test API Gateway
curl http://${VM_IP}:9090/api/gateway/health

# Test Client Service
curl http://${VM_IP}:8081/api/clients/health

# Test Token Service
curl http://${VM_IP}:8082/api/tokens/health

# Test Product Service
curl http://${VM_IP}:8083/products/health

# Test Cart Service
curl http://${VM_IP}:8084/carts/health

# Test Order Service
curl http://${VM_IP}:8085/orders/ping
```

---

## 🔧 Gestión de la Aplicación

### Ver Logs

```bash
# SSH a la VM
gcloud compute ssh farmatodo-app-vm --zone=us-central1-a

# Ver logs de todos los servicios
docker-compose logs -f

# Ver logs de un servicio específico
docker-compose logs -f client-service

# Ver últimas 100 líneas
docker-compose logs --tail=100 -f
```

### Ver Estado de Contenedores

```bash
# SSH a la VM
gcloud compute ssh farmatodo-app-vm --zone=us-central1-a

# Ver estado
docker-compose ps

# Ver recursos
docker stats
```

### Reiniciar Servicios

```bash
# Reiniciar todos los servicios
gcloud compute ssh farmatodo-app-vm --zone=us-central1-a \
  --command="docker-compose restart"

# Reiniciar un servicio específico
gcloud compute ssh farmatodo-app-vm --zone=us-central1-a \
  --command="docker-compose restart client-service"
```

### Detener la Aplicación

```bash
# Detener todos los contenedores
gcloud compute ssh farmatodo-app-vm --zone=us-central1-a \
  --command="docker-compose down"

# Detener y eliminar volúmenes (⚠️ ELIMINA DATOS DE BD)
gcloud compute ssh farmatodo-app-vm --zone=us-central1-a \
  --command="docker-compose down -v"
```

### Actualizar la Aplicación

```bash
# Pull últimas imágenes y reiniciar
gcloud compute ssh farmatodo-app-vm --zone=us-central1-a \
  --command="docker-compose pull && docker-compose up -d"
```

### Gestionar la VM

```bash
# Detener VM (para ahorrar costos)
gcloud compute instances stop farmatodo-app-vm --zone=us-central1-a

# Iniciar VM
gcloud compute instances start farmatodo-app-vm --zone=us-central1-a

# Eliminar VM (⚠️ PERMANENTE)
gcloud compute instances delete farmatodo-app-vm --zone=us-central1-a
```

---

## 🐛 Troubleshooting

### Problema: Servicios no inician

**Síntoma**: Los contenedores están en estado "Restarting"

**Solución**:
```bash
# Ver logs del servicio problemático
gcloud compute ssh farmatodo-app-vm --zone=us-central1-a \
  --command="docker-compose logs client-service"

# Verificar recursos de la VM
gcloud compute ssh farmatodo-app-vm --zone=us-central1-a \
  --command="free -h && df -h"
```

### Problema: No se puede acceder a los servicios

**Síntoma**: Connection refused al intentar acceder a las URLs

**Solución**:
```bash
# Verificar reglas de firewall
gcloud compute firewall-rules list | grep farmatodo

# Recrear reglas si es necesario
gcloud compute firewall-rules create farmatodo-allow-services \
  --allow=tcp:8080,tcp:8081,tcp:8082,tcp:8083,tcp:8084,tcp:8085,tcp:8888,tcp:9090 \
  --source-ranges=0.0.0.0/0 \
  --target-tags=farmatodo
```

### Problema: Error de autenticación con Artifact Registry

**Síntoma**: Cannot pull image from Artifact Registry

**Solución**:
```bash
# Reautenticar Docker en la VM
gcloud compute ssh farmatodo-app-vm --zone=us-central1-a \
  --command="gcloud auth configure-docker us-central1-docker.pkg.dev --quiet"
```

### Problema: Out of Memory

**Síntoma**: Servicios se matan inesperadamente

**Solución**:
```bash
# Aumentar el tamaño de la VM
gcloud compute instances set-machine-type farmatodo-app-vm \
  --machine-type=e2-standard-8 \
  --zone=us-central1-a
```

### Problema: Base de datos perdió datos

**Síntoma**: Los datos desaparecieron después de reiniciar

**Solución**: Asegurarse de que los volúmenes persisten
```bash
# Ver volúmenes
gcloud compute ssh farmatodo-app-vm --zone=us-central1-a \
  --command="docker volume ls"

# NO usar 'docker-compose down -v' en producción
```

---

## 💰 Costos Estimados

### Configuración Actual (e2-standard-4)

**Especificaciones**:
- 4 vCPUs
- 16 GB RAM
- 100 GB disco SSD

**Costos mensuales aproximados** (región us-central1):
- VM e2-standard-4: ~$121/mes (730 horas)
- Disco SSD 100GB: ~$17/mes
- IP Pública estática: ~$7/mes
- Tráfico de red: ~$10-50/mes (depende del uso)
- Artifact Registry: ~$0.10/GB/mes

**Total estimado**: ~$155-195 USD/mes

### Optimización de Costos

1. **Apagar VM cuando no se usa**:
   ```bash
   # Detener VM (solo pagas el disco)
   gcloud compute instances stop farmatodo-app-vm --zone=us-central1-a
   # Ahorro: ~$121/mes
   ```

2. **Usar IP efímera en lugar de estática**:
   ```bash
   # Ahorro: ~$7/mes
   # Desventaja: IP cambia al reiniciar VM
   ```

3. **Reducir tamaño de VM** (si no se necesita tanto poder):
   ```bash
   # e2-medium (2 vCPUs, 4GB RAM): ~$30/mes
   # e2-standard-2 (2 vCPUs, 8GB RAM): ~$60/mes
   ```

4. **Usar Preemptible VMs** (para dev/test):
   ```bash
   # Ahorro: ~70-80% en costo de VM
   # Desventaja: Google puede terminar la VM en cualquier momento
   ```

---

## 🔒 Seguridad

### Mejores Prácticas de Seguridad

#### 1. Cambiar API Keys por defecto

Editar `docker-compose.prod.yml` y cambiar todas las API keys:

```yaml
API_KEY: ${CLIENT_SERVICE_API_KEY:-NUEVO-API-KEY-SEGURO}
```

#### 2. Usar Secret Manager de GCP

```bash
# Crear secrets
gcloud secrets create client-service-api-key \
  --data-file=/path/to/key.txt \
  --replication-policy=automatic

# Darle acceso a la VM
gcloud secrets add-iam-policy-binding client-service-api-key \
  --member="serviceAccount:farmatodo-deployer@PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"
```

#### 3. Restringir acceso a PostgreSQL

**IMPORTANTE**: Por defecto, los puertos de PostgreSQL están expuestos públicamente.

**Para producción**, elimina la regla de firewall:
```bash
gcloud compute firewall-rules delete farmatodo-allow-postgres
```

O restringe el acceso solo a IPs específicas:
```bash
gcloud compute firewall-rules update farmatodo-allow-postgres \
  --source-ranges=TU_IP_PUBLICA/32
```

#### 4. Usar HTTPS

Configura un Load Balancer con certificado SSL:
```bash
# Reservar IP estática
gcloud compute addresses create farmatodo-ip --global

# Configurar Load Balancer (requiere configuración adicional)
# Ver: https://cloud.google.com/load-balancing/docs/https
```

#### 5. Configurar VPC y Subnets privadas

Para mayor seguridad, coloca las bases de datos en una subnet privada.

#### 6. Habilitar Cloud Armor

Protección contra DDoS y ataques:
```bash
# Crear política de seguridad
gcloud compute security-policies create farmatodo-policy \
  --description="Security policy for Farmatodo"

# Agregar reglas
gcloud compute security-policies rules create 1000 \
  --security-policy=farmatodo-policy \
  --expression="origin.region_code == 'CN'" \
  --action=deny-403
```

---

## 📚 Recursos Adicionales

- [Google Cloud Compute Engine Documentation](https://cloud.google.com/compute/docs)
- [Google Artifact Registry Documentation](https://cloud.google.com/artifact-registry/docs)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Cloud Config Documentation](https://spring.io/projects/spring-cloud-config)

---

## 📝 Notas Importantes

1. **Backups**: Configura snapshots automáticos de los discos:
   ```bash
   gcloud compute disks snapshot farmatodo-app-vm \
     --snapshot-names=farmatodo-backup-$(date +%Y%m%d) \
     --zone=us-central1-a
   ```

2. **Monitoring**: Habilita Cloud Monitoring para ver métricas:
   - CPU, memoria, disco
   - Logs centralizados
   - Alertas

3. **Auto-scaling**: Esta configuración usa una sola VM. Para producción con alto tráfico, considera usar:
   - Instance Groups con auto-scaling
   - Cloud SQL para las bases de datos
   - Cloud Load Balancer
   - Cloud CDN para contenido estático

4. **Disaster Recovery**: Documenta el proceso de recuperación y pruébalo regularmente.

---

## 🆘 Soporte

Para problemas o preguntas:
- GitHub Issues: https://github.com/santgodev/farmatodo-backend/issues
- Email: [tu-email@ejemplo.com]

---

**Última actualización**: 2025-01-27
