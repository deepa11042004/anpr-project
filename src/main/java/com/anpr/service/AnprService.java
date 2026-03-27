package com.anpr.service;

import com.anpr.dto.AnprEventRequest;
import com.anpr.dto.AnprEventResponse;
import com.anpr.dto.ManualOverrideRequest;
import com.anpr.dto.VehicleSchedule;
import com.anpr.entity.AuditLog;
import com.anpr.entity.AuthorizationOutcome;
import com.anpr.entity.GateAction;
import com.anpr.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnprService {

    private final BoomGateTriggerService boomGateTriggerService;
    private final AuditLogRepository auditLogRepository;

    @Value("${anpr.min-confidence-score:85}")
    private double minConfidenceScore;

    @Value("${anpr.authorized-plates:}")
    private String authorizedPlatesConfig;

    /**
     * Process an ANPR event from the camera
     */
    @Transactional
    public AnprEventResponse processAnprEvent(AnprEventRequest request) {
        log.info("Processing ANPR event from camera: {}", request.getIpAddress());

        // Extract data from request
        String originalPlate = request.getAnpr() != null ? request.getAnpr().getLicensePlate() : null;
        Double confidence = request.getAnpr() != null ? request.getAnpr().getConfidence() : null;
        String cameraIp = request.getIpAddress();

        // Validate plate exists
        if (originalPlate == null || originalPlate.isBlank()) {
            log.warn("No license plate in ANPR event");
            return createErrorResponse("No license plate detected", null);
        }

        // Normalize the plate
        String normalizedPlate = normalizePlate(originalPlate);
        log.info("Normalized plate: {} -> {}", originalPlate, normalizedPlate);

        // Check confidence score
        if (confidence == null || confidence < minConfidenceScore) {
            log.info("Low confidence score ({}) for plate: {}", confidence, normalizedPlate);
            return processRejection(request, normalizedPlate, 
                    String.format("Low confidence score: %.1f (minimum: %.1f)", confidence, minConfidenceScore));
        }

        VehicleAuthorizationResult authResult = checkVehicleAuthorization(normalizedPlate);

        if (authResult.isAuthorized()) {
            return processApproval(request, normalizedPlate, authResult.getSchedule());
        } else if (!authResult.isConfigured()) {
            log.warn("Vehicle authorization source not configured - rejecting vehicle by default");
            return processRejection(request, normalizedPlate, 
                    "Authorization service not configured");
        } else {
            return processRejection(request, normalizedPlate, authResult.getMessage());
        }
    }

    /**
     * Process manual gate override
     */
    @Transactional
    public AnprEventResponse processManualOverride(ManualOverrideRequest request, String userId) {
        log.info("Processing manual override for plate: {} by user: {}", 
                request.getVehicleNumber(), userId);

        String normalizedPlate = normalizePlate(request.getVehicleNumber());
        String cameraIp = request.getCameraIp();
        GateAction gateAction = GateAction.REMAINED_CLOSED;

        // Trigger gate if camera IP provided
        if (cameraIp != null && !cameraIp.isBlank()) {
            BoomGateTriggerService.TriggerResult triggerResult = 
                    boomGateTriggerService.triggerGateOpen(cameraIp);
            gateAction = triggerResult.isSuccess() ? GateAction.OPENED : GateAction.ERROR;
        } else {
            gateAction = GateAction.OPENED; // Assume manual gate operation
        }

        // Log the manual override
        AuditLog auditLog = AuditLog.builder()
                .vehicleNumber(request.getVehicleNumber())
                .normalizedPlate(normalizedPlate)
                .timestamp(LocalDateTime.now())
                .authorizationOutcome(AuthorizationOutcome.MANUAL_OVERRIDE)
                .gateAction(gateAction)
                .cameraIp(cameraIp)
                .userId(userId)
                .overrideReason(request.getReason())
                .isManualOverride(true)
                .build();

        AuditLog savedLog = auditLogRepository.save(auditLog);

        return AnprEventResponse.builder()
                .success(gateAction == GateAction.OPENED)
                .message("Manual override processed")
                .vehicleNumber(request.getVehicleNumber())
                .normalizedPlate(normalizedPlate)
                .outcome(AuthorizationOutcome.MANUAL_OVERRIDE)
                .gateAction(gateAction)
                .logId(savedLog.getId())
                .build();
    }

    private AnprEventResponse processApproval(AnprEventRequest request, String normalizedPlate, 
                                               VehicleSchedule schedule) {
        log.info("Vehicle {} APPROVED", normalizedPlate);

        // Trigger boom gate
        BoomGateTriggerService.TriggerResult triggerResult = 
                boomGateTriggerService.triggerGateOpen(request.getIpAddress());
        
        GateAction gateAction = triggerResult.isSuccess() ? GateAction.OPENED : GateAction.ERROR;

        // Create audit log
        AuditLog auditLog = createAuditLog(request, normalizedPlate, 
                AuthorizationOutcome.APPROVED, gateAction, null);
        
        if (schedule != null) {
            auditLog.setCompanyName(schedule.getCompanyName());
            auditLog.setDriverName(schedule.getDriverName());
        }

        AuditLog savedLog = auditLogRepository.save(auditLog);

        return AnprEventResponse.builder()
                .success(true)
                .message("Vehicle authorized - Gate " + (gateAction == GateAction.OPENED ? "opened" : "action failed"))
                .vehicleNumber(request.getAnpr().getLicensePlate())
                .normalizedPlate(normalizedPlate)
                .outcome(AuthorizationOutcome.APPROVED)
                .gateAction(gateAction)
                .logId(savedLog.getId())
                .build();
    }

    private AnprEventResponse processRejection(AnprEventRequest request, String normalizedPlate, 
                                                String reason) {
        log.info("Vehicle {} REJECTED: {}", normalizedPlate, reason);

        // Create audit log
        AuditLog auditLog = createAuditLog(request, normalizedPlate, 
                AuthorizationOutcome.REJECTED, GateAction.REMAINED_CLOSED, reason);

        AuditLog savedLog = auditLogRepository.save(auditLog);

        return AnprEventResponse.builder()
                .success(false)
                .message("Vehicle not authorized: " + reason)
                .vehicleNumber(request.getAnpr() != null ? request.getAnpr().getLicensePlate() : null)
                .normalizedPlate(normalizedPlate)
                .outcome(AuthorizationOutcome.REJECTED)
                .gateAction(GateAction.REMAINED_CLOSED)
                .logId(savedLog.getId())
                .build();
    }

    private AnprEventResponse createErrorResponse(String message, String plate) {
        return AnprEventResponse.builder()
                .success(false)
                .message(message)
                .vehicleNumber(plate)
                .outcome(AuthorizationOutcome.ERROR)
                .gateAction(GateAction.ERROR)
                .build();
    }

    private AuditLog createAuditLog(AnprEventRequest request, String normalizedPlate,
                                     AuthorizationOutcome outcome, GateAction gateAction, 
                                     String rejectionReason) {
        AuditLog.AuditLogBuilder builder = AuditLog.builder()
                .vehicleNumber(request.getAnpr() != null ? request.getAnpr().getLicensePlate() : "UNKNOWN")
                .normalizedPlate(normalizedPlate)
                .timestamp(parseDateTime(request.getDateTime()))
                .authorizationOutcome(outcome)
                .gateAction(gateAction)
                .cameraIp(request.getIpAddress())
                .userId("SYSTEM");

        if (request.getAnpr() != null) {
            builder.confidenceScore(request.getAnpr().getConfidence())
                    .vehicleType(request.getAnpr().getVehicleType())
                    .direction(request.getAnpr().getDirection())
                    .country(request.getAnpr().getCountry());

            if (request.getAnpr().getImage() != null) {
                builder.plateImage(request.getAnpr().getImage().getPlateImage())
                        .vehicleImage(request.getAnpr().getImage().getVehicleImage());
            }
        }

        if (rejectionReason != null) {
            builder.rejectionReason(rejectionReason);
        }

        return builder.build();
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return LocalDateTime.now();
        }

        try {
            // Try ISO format first
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e1) {
            try {
                // Try common camera format
                return LocalDateTime.parse(dateTimeStr, 
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            } catch (Exception e2) {
                log.warn("Unable to parse timestamp: {}, using current time", dateTimeStr);
                return LocalDateTime.now();
            }
        }
    }

    private String normalizePlate(String plate) {
        if (plate == null) {
            return null;
        }
        return plate.replaceAll("\\s+", "").toUpperCase(Locale.ROOT).trim();
    }

    private VehicleAuthorizationResult checkVehicleAuthorization(String normalizedPlate) {
        if (normalizedPlate == null || normalizedPlate.isBlank()) {
            return new VehicleAuthorizationResult(false, "License plate is empty", null, true);
        }

        List<String> authorizedPlates = Arrays.stream(authorizedPlatesConfig.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(this::normalizePlate)
                .collect(Collectors.toList());

        if (authorizedPlates.isEmpty()) {
            return new VehicleAuthorizationResult(false, "No authorization source configured", null, false);
        }

        boolean authorized = authorizedPlates.contains(normalizedPlate);
        if (!authorized) {
            return new VehicleAuthorizationResult(false, "Vehicle not found in authorized source", null, true);
        }

        return new VehicleAuthorizationResult(true, "Vehicle is authorized", null, true);
    }

    private static class VehicleAuthorizationResult {
        private final boolean authorized;
        private final String message;
        private final VehicleSchedule schedule;
        private final boolean configured;

        private VehicleAuthorizationResult(boolean authorized, String message, VehicleSchedule schedule, boolean configured) {
            this.authorized = authorized;
            this.message = message;
            this.schedule = schedule;
            this.configured = configured;
        }

        public boolean isAuthorized() {
            return authorized;
        }

        public String getMessage() {
            return message;
        }

        public VehicleSchedule getSchedule() {
            return schedule;
        }

        public boolean isConfigured() {
            return configured;
        }
    }
}
