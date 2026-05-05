package com.sirhashir.fluxlimiter;

import com.sirhashir.fluxlimiter.model.Algorithm;
import com.sirhashir.fluxlimiter.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

public class RateLimiterFactoryTest {

    private TokenBucketLimiter tokenBucketLimiter;
    private FixedWindowLimiter fixedWindowLimiter;
    private SlidingWindowLogLimiter slidingWindowLogLimiter;
    private RateLimiterFactory rateLimiterFactory;

    @BeforeEach
    void setup() {
        tokenBucketLimiter = mock(TokenBucketLimiter.class);
        fixedWindowLimiter = mock(FixedWindowLimiter.class);
        slidingWindowLogLimiter = mock(SlidingWindowLogLimiter.class);

        rateLimiterFactory = new RateLimiterFactory(tokenBucketLimiter, fixedWindowLimiter, slidingWindowLogLimiter);
        rateLimiterFactory.init();
    }

    @Test
    void returnsTokenBucketForTokenBucketAlgorithm() {
        RateLimiter result = rateLimiterFactory.getLimiter(Algorithm.TOKEN_BUCKET);
        assertSame(tokenBucketLimiter, result);
    }

    @Test
    void returnsFixedWindowForFixedWindowAlgorithm() {
        RateLimiter result = rateLimiterFactory.getLimiter(Algorithm.FIXED_WINDOW);
        assertSame(fixedWindowLimiter, result);
    }

    @Test
    void returnsSlidingWindowForSlidingWindowAlgorithm() {
        RateLimiter result = rateLimiterFactory.getLimiter(Algorithm.SLIDING_WINDOW);
        assertSame(slidingWindowLogLimiter, result);
    }
}
