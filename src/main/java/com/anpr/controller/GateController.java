package com.anpr.controller;

import com.anpr.dto.AnprEventResponse;
import com.anpr.dto.ManualOverrideRequest;
import com.anpr.service.AnprService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/gate")
@RequiredArgsConstructor
@Slf4j
public class GateController {

    private final AnprService anprService;

    /**
     * Manual override endpoint for operators/admins
     * Opens the gate manually and logs the action
     */
    @PostMapping("/override")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR')")
    public ResponseEntity<AnprEventResponse> manualOverride(
            @Valid @RequestBody ManualOverrideRequest request,
            Authentication authentication) {
        
        String userId = authentication.getName();
        log.info("Manual override requested by user: {} for vehicle: {}", 
                userId, request.getVehicleNumber());

        AnprEventResponse response = anprService.processManualOverride(request, userId);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.ok(response); // Still return 200 as the action was processed
        }
    }
}
