package com.jikkosoft.redis.cacheredis.model;

/**
 * Response genérica de la API
 */
public record CacheResponse<T>(boolean success, String message, T data) {

    public static <T> CacheResponse<T> success(T data) {
        return new CacheResponse<>(true, "Operación exitosa", data);
    }

    public static <T> CacheResponse<T> success(String message, T data) {
        return new CacheResponse<>(true, message, data);
    }

    public static <T> CacheResponse<T> error(String message) {
        return new CacheResponse<>(false, message, null);
    }
}