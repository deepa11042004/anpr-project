package com.anpr.controller;

import com.anpr.dto.AnprEventRequest;
import com.anpr.dto.AnprEventResponse;
import com.anpr.service.AnprService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/anpr")
@RequiredArgsConstructor
@Slf4j
public class AnprController {

    private final AnprService anprService;

    /**
     * ANPR Webhook Endpoint - receives events from ANPR cameras
     * No authentication required as this is called by the camera
     */
    @PostMapping("/event")
    public ResponseEntity<AnprEventResponse> handleAnprEvent(@RequestBody AnprEventRequest request) {
        log.info("Received ANPR event: IP={}, Plate={}", 
                request.getIpAddress(), 
                request.getAnpr() != null ? request.getAnpr().getLicensePlate() : "N/A");

        try {
            AnprEventResponse response = anprService.processAnprEvent(request);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                // Return 200 OK even for rejected vehicles (event was processed successfully)
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            log.error("Error processing ANPR event: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(AnprEventResponse.builder()
                            .success(false)
                            .message("Internal server error: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Health check endpoint for cameras to verify connectivity
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("ANPR Service is running");
    }
}
