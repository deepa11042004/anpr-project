package com.anpr.controller;

import com.anpr.dto.ActionResponse;
import com.anpr.dto.DatabaseMigrationResponse;
import com.anpr.dto.DatabaseSettingsRequest;
import com.anpr.dto.DatabaseTestResponse;
import com.anpr.service.ApplicationLifecycleService;
import com.anpr.service.DatabaseSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/settings/database")
@RequiredArgsConstructor
@Slf4j
public class DatabaseSettingsController {

    private final DatabaseSettingsService databaseSettingsService;
    private final ApplicationLifecycleService applicationLifecycleService;

    @PostMapping("/test-connection")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<DatabaseTestResponse> testConnection(@Valid @RequestBody DatabaseSettingsRequest request) {
        DatabaseTestResponse response = databaseSettingsService.testConnection(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/migrate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<DatabaseMigrationResponse> migrate(@Valid @RequestBody DatabaseSettingsRequest request) {
        DatabaseMigrationResponse response = databaseSettingsService.migrateData(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/apply-config")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ActionResponse> applyConfig(@Valid @RequestBody DatabaseSettingsRequest request) {
        ActionResponse response = databaseSettingsService.saveRuntimeConfig(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/restart")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ActionResponse> restart() {
        return ResponseEntity.ok(applicationLifecycleService.restartApplication());
    }

    @PostMapping("/reset-default")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ActionResponse> resetDefault() {
        return ResponseEntity.ok(databaseSettingsService.resetToDefaultConfig());
    }
}
