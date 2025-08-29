package com.jikkosoft.redis.cacheredis.lock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

/**
 * Maneja locks distribuidos para garantizar strong consistency
 */
@Component
public class DistributedLockManager {

    @Autowired
    private Map<String, RedisTemplate<String, Object>> redisTemplateMap;

    private static final String LOCK_PREFIX = "lock:";
    private static final Duration LOCK_TIMEOUT = Duration.ofSeconds(5);
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 100;

    /**
     * Intenta adquirir un lock distribuido
     */
    public boolean acquireLock(String key, String nodeId) {
        String lockKey = LOCK_PREFIX + key;
        RedisTemplate<String, Object> template = redisTemplateMap.get(nodeId);

        for (int i = 0; i < MAX_RETRIES; i++) {
            Boolean acquired = template.opsForValue()
                    .setIfAbsent(lockKey, "locked", LOCK_TIMEOUT);

            if (Boolean.TRUE.equals(acquired)) {
                return true;
            }

            // Delay antes del siguiente intento
            try {
                Thread.sleep(RETRY_DELAY_MS * (i + 1));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    /**
     * Libera un lock distribuido
     */
    public void releaseLock(String key, String nodeId) {
        String lockKey = LOCK_PREFIX + key;
        RedisTemplate<String, Object> template = redisTemplateMap.get(nodeId);
        template.delete(lockKey);
    }

    /**
     * Ejecuta operación con lock distribuido
     */
    public <T> T executeWithLock(String key, String nodeId,
                                 LockOperation<T> operation) throws Exception {
        if (!acquireLock(key, nodeId)) {
            throw new RuntimeException("No se pudo adquirir lock para key: " + key);
        }

        try {
            return operation.execute();
        } finally {
            releaseLock(key, nodeId);
        }
    }

    @FunctionalInterface
    public interface LockOperation<T> {
        T execute() throws Exception;
    }
}