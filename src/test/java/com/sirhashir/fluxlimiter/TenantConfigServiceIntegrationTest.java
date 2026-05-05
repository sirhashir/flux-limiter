package com.sirhashir.fluxlimiter;

import com.sirhashir.fluxlimiter.model.Algorithm;
import com.sirhashir.fluxlimiter.model.TenantConfig;
import com.sirhashir.fluxlimiter.service.TenantConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
public class TenantConfigServiceIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private TenantConfigService tenantConfigService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setup() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
    }

    @Test
    void savedConfigCanBeRetrieved() {
        TenantConfig config = new TenantConfig();
        config.setTenantId("tenant-x");
        config.setAlgorithm(Algorithm.TOKEN_BUCKET);
        config.setLimit(100);
        config.setWindowSeconds(60);

        tenantConfigService.save(config);

        Optional<TenantConfig> retrieved = tenantConfigService.get("tenant-x");

        assertTrue(retrieved.isPresent());
        assertEquals("tenant-x", retrieved.get().getTenantId());
        assertEquals(Algorithm.TOKEN_BUCKET, retrieved.get().getAlgorithm());
        assertEquals(100, retrieved.get().getLimit());
        assertEquals(60, retrieved.get().getWindowSeconds());
    }

    @Test
    void missingTenantReturnsEmpty() {
        Optional<TenantConfig> retrieved = tenantConfigService.get("nonexistent");
        assertFalse(retrieved.isPresent());
    }

    @Test
    void deleteRemovesConfig() {
        TenantConfig config = new TenantConfig();
        config.setTenantId("tenant-y");
        config.setAlgorithm(Algorithm.FIXED_WINDOW);
        config.setLimit(10);
        config.setWindowSeconds(60);

        tenantConfigService.save(config);
        assertTrue(tenantConfigService.get("tenant-y").isPresent());

        boolean deleted = tenantConfigService.delete("tenant-y");

        assertTrue(deleted);
        assertFalse(tenantConfigService.get("tenant-y").isPresent());
    }

    @Test
    void deleteReturnsFalseForMissingTenant() {
        boolean deleted = tenantConfigService.delete("nonexistent");
        assertFalse(deleted);
    }
}
