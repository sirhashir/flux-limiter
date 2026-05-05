package com.sirhashir.fluxlimiter;

import com.sirhashir.fluxlimiter.model.Algorithm;
import com.sirhashir.fluxlimiter.model.CheckResponse;
import com.sirhashir.fluxlimiter.model.TenantConfig;
import com.sirhashir.fluxlimiter.service.TokenBucketLimiter;
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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
public class TokenBucketLimiterIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private TokenBucketLimiter limiter;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private TenantConfig config;

    @BeforeEach
    void setup() {
        assert redisTemplate.getConnectionFactory() != null;
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();

        config = new TenantConfig();
        config.setTenantId("test-tenant");
        config.setAlgorithm(Algorithm.TOKEN_BUCKET);
        config.setLimit(5);
        config.setWindowSeconds(5);
    }

    @Test
    void firstRequestAllowed() {
        CheckResponse response = limiter.check("rl:test:key1", config);
        assertTrue(response.isAllowed());
        assertEquals(4, response.getRemaining());
    }

    @Test
    void exhaustsBucketAfterLimitRequests() {
        for(int i=0; i<5; i++) {
            CheckResponse response = limiter.check("rl:test:key2", config);
            assertTrue(response.isAllowed(), "Request "+(i+1)+" should be allowed");
        }

        CheckResponse denied = limiter.check("rl:test:key2", config);
        assertFalse(denied.isAllowed());
        assertEquals(0, denied.getRemaining());
    }

    @Test
    void differentKeysAreIndependent() {
        for(int i=0; i<5; i++)
            limiter.check("rl:test:keyA", config);

        CheckResponse responseB = limiter.check("rl:test:keyB", config);
        assertTrue(responseB.isAllowed());
        assertEquals(4, responseB.getRemaining());
        }
}
