package com.anpr.service;

import com.anpr.entity.ScheduledVehicle;
import com.anpr.entity.AuthorizationOutcome;
import com.anpr.repository.AuditLogRepository;
import com.anpr.repository.ScheduledVehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledVehicleDecisionService {

    private static final String DEFAULT_ALLOWED_STATUSES = "SECURITY,ACTIVE";
    private static final String DEFAULT_ALLOWED_DIRECTIONS = "APPROACH";

    private final ScheduledVehicleRepository scheduledVehicleRepository;
    private final AuditLogRepository auditLogRepository;

    @Value("${anpr.decision.allowed-statuses:" + DEFAULT_ALLOWED_STATUSES + "}")
    private String allowedStatusesConfig;

    @Value("${anpr.decision.allowed-directions:" + DEFAULT_ALLOWED_DIRECTIONS + "}")
    private String allowedDirectionsConfig;

    @Value("${anpr.decision.allowed-locations:}")
    private String allowedLocationsConfig;

    @Value("${anpr.decision.max-quantity:100000}")
    private BigDecimal maxQuantity;

    @Value("${anpr.decision.enforce-queue-order:true}")
    private boolean enforceQueueOrder;

    public DecisionResult evaluate(String normalizedPlate, LocalDateTime eventTime, String direction) {
        LocalDate eventDate = eventTime.toLocalDate();

        if (normalizedPlate == null || normalizedPlate.isBlank()) {
            return DecisionResult.reject("License plate is empty");
        }

        if (!isDirectionAllowed(direction)) {
            return DecisionResult.reject("Vehicle direction is not eligible for entry");
        }

        List<ScheduledVehicle> todaySchedules = scheduledVehicleRepository.findByUpliftDate(eventDate);
        if (todaySchedules.isEmpty()) {
            return DecisionResult.reject("No uplift schedule found for today");
        }

        List<ScheduledVehicle> plateMatches = todaySchedules.stream()
                .filter(row -> plateMatchesRow(normalizedPlate, row))
                .sorted(Comparator.comparing(ScheduledVehicle::getQueueNo, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.toList());

        if (plateMatches.isEmpty()) {
            return explainNoPlateMatch(normalizedPlate, eventDate);
        }

        Set<Long> servedQueueNos = getServedQueueNos(eventDate);
        Long nextAllowedQueueNo = findNextAllowedQueue(todaySchedules, eventDate, servedQueueNos);
        DecisionResult bestFailed = null;
        for (ScheduledVehicle candidate : plateMatches) {
            List<String> issues = validateRow(candidate, eventDate);

            if (isAlreadyServed(candidate, servedQueueNos)) {
                issues.add("queue already processed earlier");
            }

            if (enforceQueueOrder && nextAllowedQueueNo != null
                    && candidate.getQueueNo() != null
                    && candidate.getQueueNo() > nextAllowedQueueNo) {
                issues.add("queue not yet open; currently serving queue " + nextAllowedQueueNo);
            }

            if (issues.isEmpty()) {
                String matchedField = detectMatchedField(normalizedPlate, candidate);
                String message = String.format(
                        "Authorized: Queue %s, Ticket %s, %s, Driver %s",
                        safe(candidate.getQueueNo()),
                        safe(candidate.getTicketNumber()),
                        safe(candidate.getProductName()),
                        safe(candidate.getDriverName()));
                return DecisionResult.approve(message, candidate, matchedField);
            }

            String failedMessage = String.format(
                    "Matched schedule Queue %s but failed checks: %s",
                    safe(candidate.getQueueNo()),
                    String.join("; ", issues));
            DecisionResult failed = DecisionResult.reject(failedMessage, candidate, detectMatchedField(normalizedPlate, candidate));
            if (bestFailed == null || issues.size() < bestFailed.getValidationIssueCount()) {
                bestFailed = failed.withValidationIssueCount(issues.size());
            }
        }

        return bestFailed != null ? bestFailed : DecisionResult.reject("No valid schedule found for matched plate");
    }

    private Set<Long> getServedQueueNos(LocalDate eventDate) {
        return auditLogRepository.findByAuthorizationOutcomeAndUpliftDate(
                        AuthorizationOutcome.APPROVED,
                        eventDate)
                .stream()
                .map(a -> a.getQueueNo())
                .filter(q -> q != null)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private Long findNextAllowedQueue(List<ScheduledVehicle> rows, LocalDate eventDate, Set<Long> servedQueueNos) {
        return rows.stream()
                .filter(row -> row.getQueueNo() != null)
                .filter(row -> !servedQueueNos.contains(row.getQueueNo()))
                .filter(row -> validateRow(row, eventDate).isEmpty())
                .map(ScheduledVehicle::getQueueNo)
                .min(Long::compareTo)
                .orElse(null);
    }

    private boolean isAlreadyServed(ScheduledVehicle row, Set<Long> servedQueueNos) {
        return row.getQueueNo() != null && servedQueueNos.contains(row.getQueueNo());
    }

    private DecisionResult explainNoPlateMatch(String normalizedPlate, LocalDate eventDate) {
        List<ScheduledVehicle> anyDateMatches = scheduledVehicleRepository
                .findByTruckRegNoIgnoreCaseOrTrailorNoIgnoreCase(normalizedPlate, normalizedPlate);

        if (anyDateMatches.isEmpty()) {
            return DecisionResult.reject("Plate is not present in ERP schedule");
        }

        ScheduledVehicle nearest = anyDateMatches.stream()
                .sorted(Comparator.comparing(ScheduledVehicle::getUpliftDate, Comparator.nullsLast(LocalDate::compareTo)))
                .findFirst()
                .orElse(anyDateMatches.get(0));

        if (nearest.getUpliftDate() != null && !eventDate.equals(nearest.getUpliftDate())) {
            return DecisionResult.reject(
                    String.format("Vehicle is scheduled for %s, not for today", nearest.getUpliftDate()),
                    nearest,
                    detectMatchedField(normalizedPlate, nearest));
        }

        return DecisionResult.reject("Vehicle exists in ERP but not in active entry queue");
    }

    private List<String> validateRow(ScheduledVehicle row, LocalDate eventDate) {
        List<String> issues = new ArrayList<>();

        if (row.getQueueNo() == null || row.getQueueNo() <= 0) {
            issues.add("invalid queue number");
        }
        if (isBlank(row.getUpliftType())) {
            issues.add("uplift type missing");
        }
        if (row.getDateCreated() == null) {
            issues.add("date created missing");
        } else if (row.getDateCreated().isAfter(eventDate)) {
            issues.add("date created is in the future");
        }
        if (isBlank(row.getOmc())) {
            issues.add("OMC code missing");
        } else if (!row.getOmc().trim().matches("[A-Za-z0-9-]{2,20}")) {
            issues.add("OMC code format is invalid");
        }
        if (isBlank(row.getOmcName())) {
            issues.add("OMC name missing");
        }
        if (row.getUpliftDate() == null) {
            issues.add("uplift date missing");
        } else if (!eventDate.equals(row.getUpliftDate())) {
            issues.add("uplift date does not match event date");
        }
        if (isBlank(row.getTicketNumber())) {
            issues.add("ticket number missing");
        }
        if (isBlank(row.getProductName())) {
            issues.add("product name missing");
        }
        if (row.getRequestedQuantity() == null || row.getRequestedQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            issues.add("requested quantity must be greater than zero");
        } else if (maxQuantity != null && row.getRequestedQuantity().compareTo(maxQuantity) > 0) {
            issues.add("requested quantity exceeds maximum allowed threshold");
        }
        if (isBlank(row.getTruckRegNo()) && isBlank(row.getTrailorNo())) {
            issues.add("truck and trailer registration are both missing");
        }
        if (isBlank(row.getDriverName())) {
            issues.add("driver name missing");
        }
        if (isBlank(row.getStatus())) {
            issues.add("status missing");
        } else if (!getAllowedStatuses().contains(normalizeToken(row.getStatus()))) {
            issues.add("status is not allowed for gate entry");
        }
        if (isBlank(row.getLocation())) {
            issues.add("location missing");
        } else if (!isLocationAllowed(row.getLocation())) {
            issues.add("location is not eligible for this gate");
        }

        return issues;
    }

    private boolean plateMatchesRow(String normalizedPlate, ScheduledVehicle row) {
        return normalizedPlate.equals(normalizePlate(row.getTruckRegNo()))
                || normalizedPlate.equals(normalizePlate(row.getTrailorNo()));
    }

    private String detectMatchedField(String normalizedPlate, ScheduledVehicle row) {
        if (normalizedPlate.equals(normalizePlate(row.getTruckRegNo()))) {
            return "truckRegNo";
        }
        if (normalizedPlate.equals(normalizePlate(row.getTrailorNo()))) {
            return "trailorNo";
        }
        return "unknown";
    }

    private boolean isDirectionAllowed(String direction) {
        if (isBlank(direction)) {
            return true;
        }
        return getAllowedDirections().contains(normalizeToken(direction));
    }

    private boolean isLocationAllowed(String location) {
        Set<String> allowed = getAllowedLocations();
        if (allowed.isEmpty()) {
            return true;
        }
        return allowed.contains(normalizeToken(location));
    }

    private Set<String> getAllowedStatuses() {
        return Arrays.stream(allowedStatusesConfig.split(","))
                .map(this::normalizeToken)
                .filter(token -> !token.isBlank())
                .collect(Collectors.toSet());
    }

    private Set<String> getAllowedDirections() {
        return Arrays.stream(allowedDirectionsConfig.split(","))
                .map(this::normalizeToken)
                .filter(token -> !token.isBlank())
                .collect(Collectors.toSet());
    }

    private Set<String> getAllowedLocations() {
        return Arrays.stream(allowedLocationsConfig.split(","))
                .map(this::normalizeToken)
                .filter(token -> !token.isBlank())
                .collect(Collectors.toSet());
    }

    private String normalizePlate(String plate) {
        if (plate == null) {
            return "";
        }
        return plate.replaceAll("\\s+", "").toUpperCase(Locale.ROOT).trim();
    }

    private String normalizeToken(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String safe(Object value) {
        return value == null ? "N/A" : value.toString();
    }

    @lombok.Value
    public static class DecisionResult {
        boolean authorized;
        String message;
        ScheduledVehicle scheduledVehicle;
        String matchedField;
        int validationIssueCount;

        public static DecisionResult approve(String message, ScheduledVehicle scheduledVehicle, String matchedField) {
            return new DecisionResult(true, message, scheduledVehicle, matchedField, 0);
        }

        public static DecisionResult reject(String message) {
            return new DecisionResult(false, message, null, "unknown", Integer.MAX_VALUE);
        }

        public static DecisionResult reject(String message, ScheduledVehicle scheduledVehicle, String matchedField) {
            return new DecisionResult(false, message, scheduledVehicle, matchedField, Integer.MAX_VALUE);
        }

        public DecisionResult withValidationIssueCount(int count) {
            return new DecisionResult(authorized, message, scheduledVehicle, matchedField, count);
        }
    }
}
