package com.sirhashir.fluxlimiter.controller;

import com.sirhashir.fluxlimiter.model.CheckRequest;
import com.sirhashir.fluxlimiter.model.CheckResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RateLimitController {

    @PostMapping("/check")
    public ResponseEntity<CheckResponse> check(@Valid @RequestBody CheckRequest request) {
        CheckResponse response = new CheckResponse(true, 99, System.currentTimeMillis()/1000 + 60);
        return ResponseEntity.ok(response);
    }
}
