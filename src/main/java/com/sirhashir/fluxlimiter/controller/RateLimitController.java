package com.sirhashir.fluxlimiter.controller;

import com.sirhashir.fluxlimiter.model.CheckRequest;
import com.sirhashir.fluxlimiter.model.CheckResponse;
import com.sirhashir.fluxlimiter.model.TenantConfig;
import com.sirhashir.fluxlimiter.service.RateLimiter;
import com.sirhashir.fluxlimiter.service.RateLimiterFactory;
import com.sirhashir.fluxlimiter.service.TenantConfigService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RateLimitController {

    private static final String RATE_LIMIT_KEY_PREFIX = "rl:";

    private final TenantConfigService tenantConfigService;
    private final RateLimiterFactory rateLimiterFactory;

    public RateLimitController(TenantConfigService tenantConfigService, RateLimiterFactory rateLimiterFactory) {
        this.tenantConfigService = tenantConfigService;
        this.rateLimiterFactory = rateLimiterFactory;
    }

    @PostMapping("/check")
    public ResponseEntity<CheckResponse> check(@Valid @RequestBody CheckRequest request) throws IllegalAccessException {
        TenantConfig config = tenantConfigService.get(request.getTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + request.getTenantId()));

        RateLimiter limiter = rateLimiterFactory.getLimiter(config.getAlgorithm());
        String redisKey = RATE_LIMIT_KEY_PREFIX + request.getTenantId() + ":" + request.getKey();

        CheckResponse response = limiter.check(redisKey, config);
        return ResponseEntity.ok(response);
    }
}
