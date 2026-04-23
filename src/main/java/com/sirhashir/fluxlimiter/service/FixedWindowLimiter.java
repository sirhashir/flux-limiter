package com.sirhashir.fluxlimiter.service;

import com.sirhashir.fluxlimiter.model.CheckResponse;
import com.sirhashir.fluxlimiter.model.TenantConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.List;

public class FixedWindowLimiter implements RateLimiter {

    private final StringRedisTemplate redisTemplate;
    private DefaultRedisScript<List> script;

    public FixedWindowLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/fixed_window.lua")));
        script.setResultType(List.class);
    }

    @Override
    public CheckResponse check(String key, TenantConfig config) {
        long now = System.currentTimeMillis() / 1000;

        List<Long> res = redisTemplate.execute(
                script,
                List.of(key),
                String.valueOf(config.getLimit()),
                String.valueOf(config.getWindowSeconds()),
                String.valueOf(now)
        );
        boolean allowed = res.get(0) == 1L;
        long remaining = res.get(1);
        long reset = res.get(2);

        return new CheckResponse(allowed, remaining, reset);
    }
}
