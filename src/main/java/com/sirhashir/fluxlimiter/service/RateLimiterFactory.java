package com.sirhashir.fluxlimiter.service;

import com.sirhashir.fluxlimiter.model.Algorithm;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class RateLimiterFactory {

    private final TokenBucketLimiter tokenBucketLimiter;
    private final FixedWindowLimiter fixedWindowLimiter;
    private final SlidingWindowLogLimiter slidingWindowLogLimiter;

    private final Map<Algorithm, RateLimiter> limiterMap = new EnumMap<>(Algorithm.class);

    public RateLimiterFactory(TokenBucketLimiter tokenBucketLimiter,
                              FixedWindowLimiter fixedWindowLimiter,
                              SlidingWindowLogLimiter slidingWindowLogLimiter
    ) {
        this.tokenBucketLimiter = tokenBucketLimiter;
        this.fixedWindowLimiter = fixedWindowLimiter;
        this.slidingWindowLogLimiter = slidingWindowLogLimiter;
    }

    @PostConstruct
    public void init() {
        limiterMap.put(Algorithm.TOKEN_BUCKET, tokenBucketLimiter);
        limiterMap.put(Algorithm.FIXED_WINDOW, fixedWindowLimiter);
        limiterMap.put(Algorithm.SLIDING_WINDOW, slidingWindowLogLimiter);
    }

    public RateLimiter getLimiter(Algorithm algorithm) throws IllegalAccessException {
        RateLimiter rateLimiter = limiterMap.get(algorithm);
        if(rateLimiter == null) {
            throw new IllegalAccessException("No limiter registered for algo: "+algorithm);
        }
        return rateLimiter;
    }
}
