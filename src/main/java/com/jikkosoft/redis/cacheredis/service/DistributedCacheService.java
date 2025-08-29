package com.jikkosoft.redis.cacheredis.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jikkosoft.redis.cacheredis.hash.ConsistentHashRing;
import com.jikkosoft.redis.cacheredis.lock.DistributedLockManager;
import com.jikkosoft.redis.cacheredis.model.CacheEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Servicio principal del sistema de caché distribuido
 * Implementa strong consistency y estrategias LFU + TTL
 */
@Service
public class DistributedCacheService {

    private static final Logger logger = LoggerFactory.getLogger(DistributedCacheService.class);

    @Autowired
    private Map<String, RedisTemplate<String, Object>> redisTemplateMap;

    @Autowired
    private ConsistentHashRing hashRing;

    @Autowired
    private DistributedLockManager lockManager;

    /**
     * Almacena un valor en el caché distribuido con replicación
     */
    public void put(String key, Object value, Long ttlSeconds) {
        String primaryNode = hashRing.getNode(key);
        long ttl = ttlSeconds != null ? ttlSeconds : 3600; // TTL por defecto 1 hora, yo deberia poneresto  en el properties

        try {
            // Ejecutar con lock distribuido para strong consistency
            lockManager.executeWithLock(key, primaryNode, () -> {
                CacheEntry entry = new CacheEntry(value, ttl);

                // Escribir en nodo primario
                RedisTemplate<String, Object> primaryTemplate = redisTemplateMap.get(primaryNode);
                primaryTemplate.opsForValue().set(key, entry, ttl, TimeUnit.SECONDS);

                // Replicar en todos los nodos para alta disponibilidad
                for (String node : hashRing.getAllNodes()) {
                    if (!node.equals(primaryNode)) {
                        try {
                            RedisTemplate<String, Object> template = redisTemplateMap.get(node);
                            template.opsForValue().set(key, entry, ttl, TimeUnit.SECONDS);
                        } catch (Exception e) {
                            logger.warn("Error replicando a nodo {}: {}", node, e.getMessage());
                        }
                    }
                }

                logger.info("Clave '{}' almacenada en nodo primario '{}' con TTL {}",
                        key, primaryNode, ttl);
                return null;
            });

        } catch (Exception e) {
            logger.error("Error almacenando clave '{}': {}", key, e.getMessage());
            throw new RuntimeException("Error en operación PUT", e);
        }
    }

    /**
     * Recupera un valor del caché distribuido
     */
    public Optional<Object> get(String key) {
        String primaryNode = hashRing.getNode(key);

        try {
            return lockManager.executeWithLock(key, primaryNode, () -> {
                RedisTemplate<String, Object> template = redisTemplateMap.get(primaryNode);
                //CacheEntry entry = (CacheEntry) template.opsForValue().get(key);

                Object rawObject = template.opsForValue().get(key);

                if (rawObject == null) {
                    return Optional.empty();
                }
                CacheEntry entry = convertToCacheEntry(rawObject);

                if (entry == null) {
                    return Optional.empty();
                }

                // Verificar expiración TTL
                if (entry.isExpired()) {
                    delete(key);
                    return Optional.empty();
                }

                // Actualizar estadísticas LFU
                entry.incrementAccess();
                template.opsForValue().set(key, entry);

                logger.debug("Cache HIT para clave '{}' en nodo '{}'", key, primaryNode);
                return Optional.of(entry.getValue());
            });

        } catch (Exception e) {
            logger.error("Error recuperando clave '{}': {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Elimina una clave del caché distribuido
     */
    public boolean delete(String key) {
        String primaryNode = hashRing.getNode(key);

        try {
            return lockManager.executeWithLock(key, primaryNode, () -> {
                boolean deleted = false;

                // Eliminar de todos los nodos
                for (String node : hashRing.getAllNodes()) {
                    try {
                        RedisTemplate<String, Object> template = redisTemplateMap.get(node);
                        Boolean result = template.delete(key);
                        if (Boolean.TRUE.equals(result)) {
                            deleted = true;
                        }
                    } catch (Exception e) {
                        logger.warn("Error eliminando de nodo {}: {}", node, e.getMessage());
                    }
                }

                logger.info("Clave '{}' eliminada del sistema distribuido", key);
                return deleted;
            });

        } catch (Exception e) {
            logger.error("Error eliminando clave '{}': {}", key, e.getMessage());
            return false;
        }
    }

    /**
     * Verifica si una clave existe en el caché
     */
    public boolean exists(String key) {
        String primaryNode = hashRing.getNode(key);
        RedisTemplate<String, Object> template = redisTemplateMap.get(primaryNode);

        try {
            return template.hasKey(key);
        } catch (Exception e) {
            logger.error("Error verificando existencia de clave '{}': {}", key, e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene estadísticas del nodo especificado
     */
    public Map<String, Object> getNodeStats(String nodeId) {
        if (!redisTemplateMap.containsKey(nodeId)) {
            throw new IllegalArgumentException("Nodo no válido: " + nodeId);
        }

        RedisTemplate<String, Object> template = redisTemplateMap.get(nodeId);
        Map<String, Object> stats = new HashMap<>();

        try {
            // Obtener información básica
            Properties info = template.getConnectionFactory()
                    .getConnection().info();

            stats.put("nodeId", nodeId);
            stats.put("connected", template.getConnectionFactory().getConnection().ping());
            stats.put("keyCount", template.getConnectionFactory().getConnection().dbSize());

            // Métricas de memoria si están disponibles
            if (info.containsKey("used_memory")) {
                stats.put("usedMemory", info.getProperty("used_memory"));
            }

        } catch (Exception e) {
            logger.error("Error obteniendo estadísticas del nodo {}: {}", nodeId, e.getMessage());
            stats.put("error", e.getMessage());
        }

        return stats;
    }

    /**
     * Limpia todas las claves del sistema distribuido
     */
    public void clear() {
        for (String node : hashRing.getAllNodes()) {
            try {
                RedisTemplate<String, Object> template = redisTemplateMap.get(node);
                template.getConnectionFactory().getConnection().flushDb();
                logger.info("Nodo '{}' limpiado exitosamente", node);
            } catch (Exception e) {
                logger.error("Error limpiando nodo {}: {}", node, e.getMessage());
            }
        }
    }

    // metodo para manejar la conversión del objeto
    private CacheEntry convertToCacheEntry(Object object) {
        if (object instanceof CacheEntry) {
            return (CacheEntry) object;
        }

        if (object instanceof java.util.LinkedHashMap) {
            ObjectMapper mapper = new ObjectMapper()
                    .registerModule(new JavaTimeModule()) // Usa registerModule
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            return mapper.convertValue(object, CacheEntry.class);
        }
        return null;
    }
}
