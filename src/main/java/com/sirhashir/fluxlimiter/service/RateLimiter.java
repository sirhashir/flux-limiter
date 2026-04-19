package com.sirhashir.fluxlimiter.service;

import com.sirhashir.fluxlimiter.model.CheckResponse;
import com.sirhashir.fluxlimiter.model.TenantConfig;

public interface RateLimiter {
    CheckResponse check(String key, TenantConfig config);
}
