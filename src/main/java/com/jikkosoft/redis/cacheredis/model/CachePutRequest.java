package com.jikkosoft.redis.cacheredis.model;

/**
 * Request para operaciones PUT
 */
public record CachePutRequest(Object value, Long ttlSeconds) {
    public CachePutRequest {
        if (ttlSeconds != null && ttlSeconds < 0) {
            throw new IllegalArgumentException("TTL no puede ser negativo");
        }
    }
}
