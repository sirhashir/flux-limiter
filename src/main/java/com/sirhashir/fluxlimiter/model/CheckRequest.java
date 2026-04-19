package com.sirhashir.fluxlimiter.model;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CheckRequest {

    @NotBlank
    private String tenantId;

    @NotBlank
    private String key;
}
