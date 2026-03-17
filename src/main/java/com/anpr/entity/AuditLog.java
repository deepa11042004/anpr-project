package com.anpr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_number", nullable = false)
    private String vehicleNumber;

    @Column(name = "normalized_plate")
    private String normalizedPlate;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "authorization_outcome", nullable = false)
    private AuthorizationOutcome authorizationOutcome;

    @Enumerated(EnumType.STRING)
    @Column(name = "gate_action")
    private GateAction gateAction;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "camera_ip")
    private String cameraIp;

    @Column(name = "vehicle_type")
    private String vehicleType;

    @Column(name = "direction")
    private String direction;

    @Column(name = "country")
    private String country;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "override_reason")
    private String overrideReason;

    @Column(name = "is_manual_override")
    private boolean isManualOverride;

    @Lob
    @Column(name = "plate_image", columnDefinition = "CLOB")
    private String plateImage;

    @Lob
    @Column(name = "vehicle_image", columnDefinition = "CLOB")
    private String vehicleImage;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "driver_name")
    private String driverName;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
