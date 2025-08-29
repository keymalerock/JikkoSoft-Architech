package com.jikkosoft.redis.cacheredis.controller;

import com.jikkosoft.redis.cacheredis.model.CachePutRequest;
import com.jikkosoft.redis.cacheredis.model.CacheResponse;
import com.jikkosoft.redis.cacheredis.service.DistributedCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * API REST para el sistema de caché distribuido
 */
@RestController
@RequestMapping("/api/cache")
public class CacheController {

    @Autowired
    private DistributedCacheService cacheService;

    /**
     * Almacenar un valor en el caché
     * POST /api/cache/{key}
     */
    @PostMapping("/{key}")
    public ResponseEntity<CacheResponse<Void>> put(
            @PathVariable String key,
            @RequestBody CachePutRequest request) {

        try {
            cacheService.put(key, request.value(), request.ttlSeconds());
            return ResponseEntity.ok(
                    CacheResponse.success("Valor almacenado exitosamente", null)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CacheResponse.error("Error almacenando valor: " + e.getMessage()));
        }
    }

    /**
     * Recuperar un valor del caché
     * GET /api/cache/{key}
     */
    @GetMapping("/{key}")
    public ResponseEntity<CacheResponse<Object>> get(@PathVariable String key) {
        try {
            Optional<Object> value = cacheService.get(key);

            if (value.isPresent()) {
                return ResponseEntity.ok(
                        CacheResponse.success("Valor encontrado", value.get())
                );
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(CacheResponse.error("Clave no encontrada o expirada"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CacheResponse.error("Error recuperando valor: " + e.getMessage()));
        }
    }

    /**
     * Eliminar un valor del caché
     * DELETE /api/cache/{key}
     */
    @DeleteMapping("/{key}")
    public ResponseEntity<CacheResponse<Boolean>> delete(@PathVariable String key) {
        try {
            boolean deleted = cacheService.delete(key);

            if (deleted) {
                return ResponseEntity.ok(
                        CacheResponse.success("Clave eliminada exitosamente", true)
                );
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(CacheResponse.error("Clave no encontrada"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CacheResponse.error("Error eliminando clave: " + e.getMessage()));
        }
    }

    /**
     * Verificar si una clave existe
     * HEAD /api/cache/{key}
     */
    @RequestMapping(value = "/{key}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> exists(@PathVariable String key) {
        try {
            boolean exists = cacheService.exists(key);
            return exists ? ResponseEntity.ok().build()
                    : ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obtener estadísticas de un nodo
     * GET /api/cache/stats/{nodeId}
     */
    @GetMapping("/stats/{nodeId}")
    public ResponseEntity<CacheResponse<Map<String, Object>>> getNodeStats(
            @PathVariable String nodeId) {

        try {
            Map<String, Object> stats = cacheService.getNodeStats(nodeId);
            return ResponseEntity.ok(
                    CacheResponse.success("Estadísticas obtenidas", stats)
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(CacheResponse.error("Nodo no válido: " + nodeId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CacheResponse.error("Error obteniendo estadísticas: " + e.getMessage()));
        }
    }

    /**
     * Limpiar todo el caché distribuido
     * DELETE /api/cache/clear
     */
    @DeleteMapping("/clear")
    public ResponseEntity<CacheResponse<Void>> clear() {
        try {
            cacheService.clear();
            return ResponseEntity.ok(
                    CacheResponse.success("Caché limpiado exitosamente", null)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CacheResponse.error("Error limpiando caché: " + e.getMessage()));
        }
    }
}