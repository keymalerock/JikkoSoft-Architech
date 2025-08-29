package com.jikkosoft.redis.cacheredis.hash;

import org.springframework.stereotype.Component;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Implementación de Consistent Hashing para distribución de claves
 * entre los nodos Redis
 */
@Component
public class ConsistentHashRing {

    private final TreeMap<Long, String> ring = new TreeMap<>();
    private final List<String> nodes = List.of("node1", "node2");
    private final int virtualNodes = 150; // Nodos virtuales por nodo físico

    public ConsistentHashRing() {
        initializeRing();
    }

    /**
     * Inicializa el hash ring con nodos virtuales
     */
    private void initializeRing() {
        for (String node : nodes) {
            for (int i = 0; i < virtualNodes; i++) {
                String virtualNode = node + "_virtual_" + i;
                long hash = computeHash(virtualNode);
                ring.put(hash, node);
            }
        }
    }

    /**
     * Obtiene el nodo responsable para una clave dada
     */
    public String getNode(String key) {
        if (ring.isEmpty()) {
            throw new IllegalStateException("Hash ring está vacío");
        }

        long hash = computeHash(key);
        Map.Entry<Long, String> entry = ring.ceilingEntry(hash);

        // Si no hay entrada mayor, usar la primera del ring (circular)
        return entry != null ? entry.getValue() : ring.firstEntry().getValue();
    }

    /**
     * Obtiene todos los nodos para replicación
     */
    public List<String> getAllNodes() {
        return new ArrayList<>(nodes);
    }

    /**
     * Calcula hash SHA-1 para una clave
     */
    private long computeHash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(key.getBytes());
            long hash = 0;
            for (int i = 0; i < 4; i++) {
                hash <<= 8;
                hash |= (digest[i] & 0xFF);
            }
            return hash;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 no disponible", e);
        }
    }
}