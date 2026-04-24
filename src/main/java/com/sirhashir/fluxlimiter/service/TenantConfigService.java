package com.sirhashir.fluxlimiter.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirhashir.fluxlimiter.model.TenantConfig;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.util.Optional;

@Service
public class TenantConfigService {

    private static final String CONFIG_KEY_PREFIX = "config:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public TenantConfigService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void save(TenantConfig config) {
        try {
            String json = objectMapper.writeValueAsString(config);
            redisTemplate.opsForValue().set(CONFIG_KEY_PREFIX + config.getTenantId(), json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialise tenant config", e);
        }
    }

    public Optional<TenantConfig> get(String tenantId) {
        String json = redisTemplate.opsForValue().get(CONFIG_KEY_PREFIX + tenantId);
        if(json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, TenantConfig.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialise tenant config", e);
        }
    }

    public boolean delete(String tenantId) {
        return redisTemplate.delete(CONFIG_KEY_PREFIX + tenantId);
    }
}
