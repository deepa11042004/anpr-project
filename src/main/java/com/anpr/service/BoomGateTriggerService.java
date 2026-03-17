package com.anpr.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@Slf4j
public class BoomGateTriggerService {

    @Value("${anpr.relay-endpoint-template}")
    private String relayEndpointTemplate;

    @Value("${anpr.camera.username:admin}")
    private String cameraUsername;

    @Value("${anpr.camera.password:admin123}")
    private String cameraPassword;

    private final RestTemplate restTemplate;

    public BoomGateTriggerService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Trigger the boom gate to open by sending an XML PUT request to the camera relay
     */
    public TriggerResult triggerGateOpen(String cameraIp) {
        if (cameraIp == null || cameraIp.isBlank()) {
            log.error("Camera IP is required to trigger gate");
            return TriggerResult.failure("Camera IP is required");
        }

        String endpoint = relayEndpointTemplate.replace("{ip}", cameraIp);
        String xmlBody = "<IOPortData><outputState>high</outputState></IOPortData>";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            
            // Add Basic Authentication if configured
            if (cameraUsername != null && !cameraUsername.isBlank()) {
                String auth = cameraUsername + ":" + cameraPassword;
                String encodedAuth = Base64.getEncoder()
                        .encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                headers.set("Authorization", "Basic " + encodedAuth);
            }

            HttpEntity<String> requestEntity = new HttpEntity<>(xmlBody, headers);

            log.info("Triggering boom gate at {}", endpoint);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.PUT,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Boom gate triggered successfully for camera {}", cameraIp);
                return TriggerResult.success();
            } else {
                log.error("Failed to trigger boom gate. Status: {}, Body: {}", 
                        response.getStatusCode(), response.getBody());
                return TriggerResult.failure("Unexpected response: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Error triggering boom gate at {}: {}", endpoint, e.getMessage(), e);
            return TriggerResult.failure("Error: " + e.getMessage());
        }
    }

    /**
     * Close the boom gate (set relay to low state)
     */
    public TriggerResult triggerGateClose(String cameraIp) {
        if (cameraIp == null || cameraIp.isBlank()) {
            return TriggerResult.failure("Camera IP is required");
        }

        String endpoint = relayEndpointTemplate.replace("{ip}", cameraIp);
        String xmlBody = "<IOPortData><outputState>low</outputState></IOPortData>";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            
            if (cameraUsername != null && !cameraUsername.isBlank()) {
                String auth = cameraUsername + ":" + cameraPassword;
                String encodedAuth = Base64.getEncoder()
                        .encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                headers.set("Authorization", "Basic " + encodedAuth);
            }

            HttpEntity<String> requestEntity = new HttpEntity<>(xmlBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.PUT,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Boom gate closed for camera {}", cameraIp);
                return TriggerResult.success();
            } else {
                return TriggerResult.failure("Unexpected response: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Error closing boom gate at {}: {}", endpoint, e.getMessage());
            return TriggerResult.failure("Error: " + e.getMessage());
        }
    }

    // Inner class for trigger result
    public static class TriggerResult {
        private final boolean success;
        private final String message;

        private TriggerResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static TriggerResult success() {
            return new TriggerResult(true, "Gate triggered successfully");
        }

        public static TriggerResult failure(String message) {
            return new TriggerResult(false, message);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}
