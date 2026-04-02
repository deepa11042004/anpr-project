package com.anpr.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectivityBoomGateTestRequest {

    @NotBlank(message = "Camera IP is required for relay trigger")
    private String cameraIp;
}
