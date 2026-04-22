package com.sirhashir.fluxlimiter.controller;

import com.sirhashir.fluxlimiter.model.Algorithm;
import com.sirhashir.fluxlimiter.model.CheckRequest;
import com.sirhashir.fluxlimiter.model.CheckResponse;
import com.sirhashir.fluxlimiter.model.TenantConfig;
import com.sirhashir.fluxlimiter.service.TokenBucketLimiter;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RateLimitController {

    private final TokenBucketLimiter tokenBucketLimiter;

    public RateLimitController(TokenBucketLimiter tokenBucketLimiter) {
        this.tokenBucketLimiter = tokenBucketLimiter;
    }

    @PostMapping("/check")
    public ResponseEntity<CheckResponse> check(@Valid @RequestBody CheckRequest request) {
        TenantConfig config = new TenantConfig();
        config.setTenantId(request.getTenantId());
        config.setAlgorithm(Algorithm.TOKEN_BUCKET);
        config.setLimit(5);
        config.setWindowSeconds(5);

        String redisKey = "r1:" + request.getTenantId() + ":" + request.getKey();

        CheckResponse response = tokenBucketLimiter.check(redisKey, config);
        return ResponseEntity.ok(response);
    }
}
