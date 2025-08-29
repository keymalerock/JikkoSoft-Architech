package com.jikkosoft.redis.cacheredis;

import com.jikkosoft.redis.cacheredis.hash.ConsistentHashRing;
import com.jikkosoft.redis.cacheredis.lock.DistributedLockManager;
import com.jikkosoft.redis.cacheredis.model.CacheEntry;
import com.jikkosoft.redis.cacheredis.service.DistributedCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para DistributedCacheService
 */
@ExtendWith(MockitoExtension.class)
class DistributedCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate1;

    @Mock
    private RedisTemplate<String, Object> redisTemplate2;

    @Mock
    private ValueOperations<String, Object> valueOperations1;

    @Mock
    private ValueOperations<String, Object> valueOperations2;

    @Mock
    private ConsistentHashRing hashRing;

    @Mock
    private DistributedLockManager lockManager;

    private DistributedCacheService cacheService;
    private Map<String, RedisTemplate<String, Object>> templateMap;

    @BeforeEach
    void setUp() throws Exception {
        cacheService = new DistributedCacheService();

        templateMap = new HashMap<>();
        templateMap.put("node1", redisTemplate1);
        templateMap.put("node2", redisTemplate2);

        // Inyectar dependencias manualmente para testing
        setPrivateField(cacheService, "redisTemplateMap", templateMap);
        setPrivateField(cacheService, "hashRing", hashRing);
        setPrivateField(cacheService, "lockManager", lockManager);
    }

    @Test
    @DisplayName("Debe almacenar valor exitosamente en nodo primario")
    void testPutSuccess() throws Exception {
        // Arrange
        String key = "test:key";
        String value = "test value";
        Long ttl = 300L;

        when(redisTemplate1.opsForValue()).thenReturn(valueOperations1);
        when(redisTemplate2.opsForValue()).thenReturn(valueOperations2);
        when(hashRing.getNode(key)).thenReturn("node1");
        when(hashRing.getAllNodes()).thenReturn(List.of("node1", "node2"));
        when(lockManager.executeWithLock(eq(key), eq("node1"), any()))
                .thenAnswer(invocation -> {
                    DistributedLockManager.LockOperation<?> operation = invocation.getArgument(2);
                    return operation.execute();
                });
        // Act
        assertDoesNotThrow(() -> cacheService.put(key, value, ttl));

        // Assert
        verify(valueOperations1).set(eq(key), any(CacheEntry.class), eq(ttl), eq(TimeUnit.SECONDS));
        verify(valueOperations2).set(eq(key), any(CacheEntry.class), eq(ttl), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Debe recuperar valor exitosamente")
    void testGetSuccess() throws Exception {
        // Arrange
        String key = "test:key";
        String expectedValue = "test value";
        CacheEntry entry = new CacheEntry(expectedValue, 300);

        when(redisTemplate1.opsForValue()).thenReturn(valueOperations1);
        when(hashRing.getNode(key)).thenReturn("node1");
        when(lockManager.executeWithLock(eq(key), eq("node1"), any()))
                .thenAnswer(invocation -> {
                    DistributedLockManager.LockOperation<?> operation = invocation.getArgument(2);
                    return operation.execute();
                });
        when(valueOperations1.get(key)).thenReturn(entry);

        // Act
        Optional<Object> result = cacheService.get(key);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(expectedValue, result.get());
        verify(valueOperations1).get(key);
        verify(valueOperations1).set(eq(key), any(CacheEntry.class)); // Actualización stats
    }

    @Test
    @DisplayName("Debe verificar existencia de clave")
    void testExists() {
        // Arrange
        String key = "test:key";

        when(hashRing.getNode(key)).thenReturn("node1");
        //when(redisTemplate1.opsForValue()).thenReturn(valueOperations1);
        when(redisTemplate1.hasKey(key)).thenReturn(true);

        // Act
        boolean exists = cacheService.exists(key);

        // Assert
        assertTrue(exists);
        verify(redisTemplate1).hasKey(key);
    }

    // Helper method to set private fields
    private void setPrivateField(Object target, String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
