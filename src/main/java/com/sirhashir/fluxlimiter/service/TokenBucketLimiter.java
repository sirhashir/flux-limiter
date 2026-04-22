package com.sirhashir.fluxlimiter.service;

import com.sirhashir.fluxlimiter.model.CheckResponse;
import com.sirhashir.fluxlimiter.model.TenantConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TokenBucketLimiter implements RateLimiter{

    private final StringRedisTemplate redisTemplate;
    private DefaultRedisScript<List> script;

    public TokenBucketLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/token_bucket.lua")));
        script.setResultType(List.class);
    }

    @Override
    public CheckResponse check(String key, TenantConfig config) {
        double refillRate = (double) config.getLimit() / config.getWindowSeconds();
        long now = System.currentTimeMillis() / 1000;

        System.out.println("DEBUG: key=" + key + ", capacity=" + config.getLimit() + ", refillRate=" + refillRate + ", now=" + now);

        List<Long> result = redisTemplate.execute(
                script,
                List.of(key),
                String.valueOf(config.getLimit()),
                String.valueOf(refillRate),
                String.valueOf(now)
        );

        System.out.println("DEBUG: result=" + result);

        boolean allowed = result.get(0) == 1L;
        long remaining = result.get(1);
        long retryAfter = result.get(2);
        long resetAt = allowed ? now + config.getWindowSeconds() : now + retryAfter;

        return new CheckResponse(allowed, remaining, resetAt);
    }
}
