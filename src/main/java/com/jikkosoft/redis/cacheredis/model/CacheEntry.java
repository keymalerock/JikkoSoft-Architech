package com.jikkosoft.redis.cacheredis.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;

public class CacheEntry {
    private final Object value;
    private final LocalDateTime createdAt;
    private final LocalDateTime expiresAt;
    private long accessCount;
    private LocalDateTime lastAccessed;

    @JsonCreator
    public CacheEntry(@JsonProperty("value") Object value,
                      @JsonProperty("ttlSeconds") long ttlSeconds) {
        this.value = value;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = ttlSeconds > 0 ? createdAt.plusSeconds(ttlSeconds) : null;
        this.accessCount = 1;
        this.lastAccessed = createdAt;
    }

    public Object getValue() { return value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public long getAccessCount() { return accessCount; }
    public LocalDateTime getLastAccessed() { return lastAccessed; }

    /**
     * Incrementa el contador de accesos y actualiza timestamp
     */
    public void incrementAccess() {
        this.accessCount++;
        this.lastAccessed = LocalDateTime.now();
    }

    /**
     * Verifica si la entrada ha expirado según TTL
     */
    @JsonIgnore
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CacheEntry)) return false;
        CacheEntry that = (CacheEntry) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}