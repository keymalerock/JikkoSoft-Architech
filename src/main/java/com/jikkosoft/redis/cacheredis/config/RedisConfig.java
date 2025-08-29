package com.jikkosoft.redis.cacheredis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RedisConfig {

    @Value("${cache.redis.node1.host:localhost}")
    private String node1Host;

    @Value("${cache.redis.node1.port:6379}")
    private int node1Port;

    @Value("${cache.redis.node2.host:localhost}")
    private String node2Host;

    @Value("${cache.redis.node2.port:6380}")
    private int node2Port;

    /**
     * Factory para conexión Redis Nodo 1
     */
    @Bean("redisConnectionFactoryNode1")
    @Primary
    public RedisConnectionFactory redisConnectionFactoryNode1() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(node1Host);
        config.setPort(node1Port);
        return new JedisConnectionFactory(config);
    }

    /**
     * Factory para conexión Redis Nodo 2
     */
    @Bean("redisConnectionFactoryNode2")
    public RedisConnectionFactory redisConnectionFactoryNode2() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(node2Host);
        config.setPort(node2Port);
        return new JedisConnectionFactory(config);
    }

    /**
     * RedisTemplate para Nodo 1
     */
    @Bean("redisTemplateNode1")
    @Primary
    public RedisTemplate<String, Object> redisTemplateNode1() {
        return createRedisTemplate(redisConnectionFactoryNode1());
    }

    /**
     * RedisTemplate para Nodo 2
     */
    @Bean("redisTemplateNode2")
    public RedisTemplate<String, Object> redisTemplateNode2() {
        return createRedisTemplate(redisConnectionFactoryNode2());
    }

    /**
     * Mapa de templates Redis para acceso por nodo
     */
    @Bean("redisTemplateMap")
    public Map<String, RedisTemplate<String, Object>> redisTemplateMap() {
        Map<String, RedisTemplate<String, Object>> templateMap = new HashMap<>();
        templateMap.put("node1", redisTemplateNode1());
        templateMap.put("node2", redisTemplateNode2());
        return templateMap;
    }

    /**
     * Crea y configura un RedisTemplate con serializadores apropiados
     */
    private RedisTemplate<String, Object> createRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        ObjectMapper objectMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();

        // Configurar serializers
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // Configurar serializadores
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.setEnableTransactionSupport(true);

        template.afterPropertiesSet();
        return template;
    }
}
