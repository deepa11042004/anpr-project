package com.anpr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectivityBoomGateTestResponse {
    private boolean success;
    private String message;
    private String relayEndpoint;
}
