package com.anpr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectivityCameraTestResponse {
    private boolean success;
    private String message;
    private String testedEndpoint;
    private Integer statusCode;
    private String recommendedWebhookUrl;
    private String sampleAnprPayload;
    private String cameraSetupChecklist;
}
