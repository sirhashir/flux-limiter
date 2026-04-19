package com.sirhashir.fluxlimiter.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TenantConfig {

    @NotBlank
    private String tenantId;

    @NotNull
    private Algorithm algorithm;

    @Min(1)
    private int limit;

    @Min(1)
    private int windowSeconds;
}
