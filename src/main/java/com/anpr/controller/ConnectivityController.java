package com.anpr.controller;

import com.anpr.dto.ConnectivityBoomGateTestRequest;
import com.anpr.dto.ConnectivityBoomGateTestResponse;
import com.anpr.dto.ConnectivityCameraTestRequest;
import com.anpr.dto.ConnectivityCameraTestResponse;
import com.anpr.service.ConnectivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/connectivity")
@RequiredArgsConstructor
public class ConnectivityController {

    private final ConnectivityService connectivityService;

    @PostMapping("/camera/test")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ConnectivityCameraTestResponse> testCamera(@Valid @RequestBody ConnectivityCameraTestRequest request) {
        return ResponseEntity.ok(connectivityService.testCameraConnectivity(request));
    }

    @PostMapping("/boom-gate/test")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ConnectivityBoomGateTestResponse> testBoomGate(@Valid @RequestBody ConnectivityBoomGateTestRequest request) {
        return ResponseEntity.ok(connectivityService.testBoomGate(request));
    }
}
