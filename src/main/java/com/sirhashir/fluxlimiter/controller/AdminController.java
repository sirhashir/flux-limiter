package com.sirhashir.fluxlimiter.controller;

import com.sirhashir.fluxlimiter.model.TenantConfig;
import com.sirhashir.fluxlimiter.service.TenantConfigService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/tenants")
public class AdminController {

    private final TenantConfigService tenantConfigService;

    public AdminController(TenantConfigService tenantConfigService) {
        this.tenantConfigService = tenantConfigService;
    }

    @PostMapping
    public ResponseEntity<TenantConfig> create(@Valid @RequestBody TenantConfig config) {
        tenantConfigService.save(config);
        return ResponseEntity.ok(config);
    }

    @GetMapping("/{tenantId}")
    public ResponseEntity<TenantConfig> get(@PathVariable String tenantId) {
        return tenantConfigService.get(tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{tenantId}")
    public ResponseEntity<TenantConfig> update(@PathVariable String tenantId, @Valid @RequestBody TenantConfig config) {
        config.setTenantId(tenantId);
        tenantConfigService.save(config);
        return ResponseEntity.ok(config);
    }

    @DeleteMapping("/{tenantId}")
    public ResponseEntity<Void> delete(@PathVariable String tenantId) {
        boolean deleted = tenantConfigService.delete(tenantId);
        if(deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
