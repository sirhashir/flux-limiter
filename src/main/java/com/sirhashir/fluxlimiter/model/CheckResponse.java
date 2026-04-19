package com.sirhashir.fluxlimiter.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CheckResponse {

    private boolean allowed;
    private long remaining;
    private long resetAt;
}
