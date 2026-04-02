package com.anpr.service;

import com.anpr.dto.ConnectivityBoomGateTestRequest;
import com.anpr.dto.ConnectivityBoomGateTestResponse;
import com.anpr.dto.ConnectivityCameraTestRequest;
import com.anpr.dto.ConnectivityCameraTestResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConnectivityService {

    private final BoomGateTriggerService boomGateTriggerService;

    public ConnectivityCameraTestResponse testCameraConnectivity(ConnectivityCameraTestRequest request) {
        String protocol = request.isUseHttps() ? "https" : "http";
        String base = String.format("%s://%s:%d", protocol, request.getCameraHost().trim(), request.getCameraPort());
        String endpoint = base + "/ISAPI/System/status";

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(8))
                    .GET();

            if (hasCredentials(request)) {
                builder.header(HttpHeaders.AUTHORIZATION, buildBasicAuth(request.getCameraUsername(), request.getCameraPassword()));
            }

            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();
            boolean ok = code >= 200 && code < 300;

            return ConnectivityCameraTestResponse.builder()
                    .success(ok)
                    .message(ok
                            ? "Camera reachable. Configure ANPR event push to middleware webhook URL."
                            : "Camera reachable but returned non-success status. Verify credentials/ISAPI permissions.")
                    .testedEndpoint(endpoint)
                    .statusCode(code)
                    .recommendedWebhookUrl(normalizeBaseUrl(request.getMiddlewareBaseUrl()) + "/api/anpr/event")
                    .sampleAnprPayload(samplePayload())
                    .cameraSetupChecklist(cameraChecklist())
                    .build();
        } catch (Exception ex) {
            log.warn("Camera connectivity test failed for {}: {}", endpoint, ex.getMessage());
            return ConnectivityCameraTestResponse.builder()
                    .success(false)
                    .message("Camera connectivity failed: " + ex.getMessage())
                    .testedEndpoint(endpoint)
                    .statusCode(null)
                    .recommendedWebhookUrl(normalizeBaseUrl(request.getMiddlewareBaseUrl()) + "/api/anpr/event")
                    .sampleAnprPayload(samplePayload())
                    .cameraSetupChecklist(cameraChecklist())
                    .build();
        }
    }

    public ConnectivityBoomGateTestResponse testBoomGate(ConnectivityBoomGateTestRequest request) {
        BoomGateTriggerService.TriggerResult result = boomGateTriggerService.triggerGateOpen(request.getCameraIp().trim());
        return ConnectivityBoomGateTestResponse.builder()
                .success(result.isSuccess())
                .message(result.getMessage())
                .relayEndpoint("http://" + request.getCameraIp().trim() + "/ISAPI/System/IO/outputs/1/trigger")
                .build();
    }

    private String normalizeBaseUrl(String value) {
        String trimmed = value.trim();
        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private boolean hasCredentials(ConnectivityCameraTestRequest request) {
        return request.getCameraUsername() != null && !request.getCameraUsername().isBlank()
                && request.getCameraPassword() != null;
    }

    private String buildBasicAuth(String username, String password) {
        String token = username + ":" + password;
        String encoded = Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }

    private String samplePayload() {
        return "{\"ipAddress\":\"192.168.1.25\",\"channelID\":1,\"dateTime\":\"2026-03-31T10:00:00+02:00\",\"eventType\":\"ANPR\",\"ANPR\":{\"licensePlate\":\"ABC1234\",\"confidence\":94,\"country\":\"ZM\",\"vehicleType\":\"Truck\",\"direction\":\"Approach\",\"image\":{\"plateImage\":\"BASE64\",\"vehicleImage\":\"BASE64\"}}}";
    }

    private String cameraChecklist() {
        return "1) Camera event type: ANPR. 2) HTTP method: POST. 3) Target URL: /api/anpr/event on middleware. 4) Content-Type: application/json. 5) Ensure camera and middleware are reachable over intranet. 6) Configure camera relay credentials if boom gate trigger requires auth.";
    }
}
