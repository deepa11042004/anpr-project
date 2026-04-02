package com.anpr.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectivityCameraTestRequest {

    @NotBlank(message = "Camera host/IP is required")
    private String cameraHost;

    @Min(value = 1, message = "Port must be >= 1")
    @Max(value = 65535, message = "Port must be <= 65535")
    private int cameraPort = 80;

    private boolean useHttps;

    private String cameraUsername;

    private String cameraPassword;

    @NotBlank(message = "Middleware base URL is required")
    private String middlewareBaseUrl;
}
