package com.anpr.dto;

import com.anpr.entity.AuditLog;
import com.anpr.entity.AuthorizationOutcome;
import com.anpr.entity.GateAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private Long id;
    private String vehicleNumber;
    private String normalizedPlate;
    private LocalDateTime timestamp;
    private AuthorizationOutcome authorizationOutcome;
    private GateAction gateAction;
    private Double confidenceScore;
    private String cameraIp;
    private String vehicleType;
    private String direction;
    private String country;
    private String userId;
    private String rejectionReason;
    private String overrideReason;
    private boolean isManualOverride;
    private String plateImage;
    private String vehicleImage;
    private String companyName;
    private String driverName;
    
    public static AuditLogResponse fromEntity(AuditLog auditLog) {
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .vehicleNumber(auditLog.getVehicleNumber())
                .normalizedPlate(auditLog.getNormalizedPlate())
                .timestamp(auditLog.getTimestamp())
                .authorizationOutcome(auditLog.getAuthorizationOutcome())
                .gateAction(auditLog.getGateAction())
                .confidenceScore(auditLog.getConfidenceScore())
                .cameraIp(auditLog.getCameraIp())
                .vehicleType(auditLog.getVehicleType())
                .direction(auditLog.getDirection())
                .country(auditLog.getCountry())
                .userId(auditLog.getUserId())
                .rejectionReason(auditLog.getRejectionReason())
                .overrideReason(auditLog.getOverrideReason())
                .isManualOverride(auditLog.isManualOverride())
                .plateImage(auditLog.getPlateImage())
                .vehicleImage(auditLog.getVehicleImage())
                .companyName(auditLog.getCompanyName())
                .driverName(auditLog.getDriverName())
                .build();
    }
}
