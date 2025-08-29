package com.jikkosoft.redis.cacheredis;

import com.jikkosoft.redis.cacheredis.lock.DistributedLockManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para DistributedLockManager
 */
@ExtendWith(MockitoExtension.class)
class DistributedLockManagerTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private DistributedLockManager lockManager;

    @BeforeEach
    void setUp() {
        lockManager = new DistributedLockManager();
        Map<String, RedisTemplate<String, Object>> templateMap = new HashMap<>();
        templateMap.put("node1", redisTemplate);

        // Inyectar manualmente el mapa (en una app real sería @Autowired)
        try {
            var field = DistributedLockManager.class.getDeclaredField("redisTemplateMap");
            field.setAccessible(true);
            field.set(lockManager, templateMap);
        } catch (Exception e) {
            fail("Error configurando test: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Debe adquirir lock exitosamente")
    void testAcquireLockSuccess() {
        // Arrange
        String key = "test:key";
        String nodeId = "node1";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), eq("locked"), any(Duration.class)))
                .thenReturn(true);

        // Act
        boolean acquired = lockManager.acquireLock(key, nodeId);

        // Assert
        assertTrue(acquired);
        verify(valueOperations).setIfAbsent(eq("lock:" + key), eq("locked"), any(Duration.class));
    }

    @Test
    @DisplayName("Debe fallar al adquirir lock cuando ya existe")
    void testAcquireLockFailure() {
        // Arrange
        String key = "test:key";
        String nodeId = "node1";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), eq("locked"), any(Duration.class)))
                .thenReturn(false);

        // Act
        boolean acquired = lockManager.acquireLock(key, nodeId);

        // Assert
        assertFalse(acquired);
        verify(valueOperations, atLeast(1)).setIfAbsent(anyString(), eq("locked"), any(Duration.class));
    }

    @Test
    @DisplayName("Debe liberar lock correctamente")
    void testReleaseLock() {
        // Arrange
        String key = "test:key";
        String nodeId = "node1";

        // Act
        lockManager.releaseLock(key, nodeId);

        // Assert
        verify(redisTemplate).delete("lock:" + key);
    }

    @Test
    @DisplayName("Debe ejecutar operación con lock exitosamente")
    void testExecuteWithLockSuccess() throws Exception {
        // Arrange
        String key = "test:key";
        String nodeId = "node1";
        String expectedResult = "operation result";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), eq("locked"), any(Duration.class)))
                .thenReturn(true);

        DistributedLockManager.LockOperation<String> operation =
                () -> expectedResult;

        // Act
        String result = lockManager.executeWithLock(key, nodeId, operation);

        // Assert
        assertEquals(expectedResult, result);
        verify(redisTemplate).delete("lock:" + key); // Verificar que se liberó el lock
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando no puede adquirir lock")
    void testExecuteWithLockFailure() {
        // Arrange
        String key = "test:key";
        String nodeId = "node1";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), eq("locked"), any(Duration.class)))
                .thenReturn(false);

        DistributedLockManager.LockOperation<String> operation =
                () -> "should not execute";

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                lockManager.executeWithLock(key, nodeId, operation));
    }
}
