package com.anpr.dto;

import com.anpr.entity.AuthorizationOutcome;
import com.anpr.entity.GateAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnprEventResponse {
    private boolean success;
    private String message;
    private String vehicleNumber;
    private String normalizedPlate;
    private AuthorizationOutcome outcome;
    private GateAction gateAction;
    private Long logId;
}
