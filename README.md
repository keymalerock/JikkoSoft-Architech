# Sistema de Caché Distribuido (PARTE 1)
# **Querido Reclutador, no omita mi prueba solo si no le funciona el docker o no le levanta el jar,
si tiene dudas con gusto se las aclaro, inviteme a una reunion y con gusto le muestro el porque en caso de error**

# la parte 2 de la prueba (el diagrama ) esta el el folder PARTE2

Sistema de caché distribuido implementado con Spring Boot 3 y Redis, 
diseñado para proporcionar **strong consistency**, **alta disponibilidad** 
y **estrategias inteligentes de expulsión** (LFU + TTL). hecho por Erick Bonilla

## 🏗️ Arquitectura del Sistema

### Componentes Principales
aqui a grosso modo lo que incluye la prueba
- **2 Nodos Redis**: Instancias independientes para distribución de carga
- **Consistent Hashing**: Distribución uniforme de claves entre nodos
- **Distributed Locking**: Garantiza strong consistency en operaciones
- **Replicación Síncrona**: Los datos se replican en ambos nodos
- **API REST**: Interfaz completa para operaciones CRUD

### Patrones de Diseño Implementados
intente hacer estos patrones por cuestiones de tiempo no alcance hacer temas de seguridad , la autoSanacion  self-healing y 
tal vez algo de circuit break para la tolerancia a fallas, 
1. **Consistent Hashing Ring**: Para distribución balanceada de claves
2. **Distributed Lock**: Para coordinación de escrituras concurrentes
3. **Repository Pattern**: Abstracción de acceso a datos Redis
4. **Strategy Pattern**: Múltiples estrategias de expulsión (LFU + TTL)

## 🚀 Características Principales

###  Strong Consistency
- **Distributed Locking**: Evita condiciones de carrera
- **Replicación Síncrona**: Datos consistentes en ambos nodos
- **Timeout Handling**: Prevención de deadlocks

###  High Performance
aqui hay una peculiaridad que no pensaba hacerla asi pero intente y resulto,
por temas practicos, solo hice 2 nodos fisicos standAlone, pero virtualmente configure 150 nodos virtuales por cada nodo
fisico y asi no sobrecargar un solo nodo.
- **Consistent Hashing**: O(log n) para localización de claves
- **Connection Pooling**: Reutilización eficiente de conexiones Redis
- **Operaciones Asíncronas**: Procesamiento no bloqueante

### Intelligent Eviction
- **LFU (Least Frequently Used)**: Expulsión basada en frecuencia de acceso
- **TTL (Time To Live)**: Expiración automática por tiempo
- **Hybrid Strategy**: Combinación inteligente de ambas estrategias

## Requisitos del Sistema

- **Java 17+**
- **Spring Boot 3.2+**
- **Redis 7+**
- **gradle 3.8+**
- **Docker & Docker Compose** (para entorno de desarrollo)

## 🛠️ Instalación y Configuración

### 1. Levantar Infraestructura Redis
```bash
# Iniciar 2 nodos Redis + Redis Commander
docker-compose up -d

# Verificar estado de contenedores
docker-compose ps
```

## API Documentation
### Endpoints Principales

#### 1. Almacenar Valor
```http
POST /api/cache/{key}
Content-Type: application/json

{
  "value": "cualquier objeto JSON",
  "ttlSeconds": 3600
}
```

#### 2. Recuperar Valor
```http
GET /api/cache/{key}
```

#### 3. Eliminar Valor
```http
DELETE /api/cache/{key}
```

#### 4. Verificar Existencia
```http
HEAD /api/cache/{key}
```
Retorna `200 OK` si existe, `404 Not Found` si no existe.

#### 5. Estadísticas de Nodo
```http
GET /api/cache/stats/{nodeId}
```

#### 6. Limpiar Caché
```http
DELETE /api/cache/clear
```

## Características Técnicas Destacadas

### Resolución de Problemas
- ✅ **Consistent Hashing**: Distribución uniforme sin hotspots
- ✅ **Distributed Locking**: Eliminación de condiciones de carrera

### Creatividad e Innovación
- 🚀 **Hybrid Eviction**: Combinación inteligente LFU + TTL
- 🚀 **Dynamic Load Balancing**: Ajuste automático de distribución
