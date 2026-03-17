package com.anpr.service;

import com.anpr.dto.AuditLogResponse;
import com.anpr.dto.DashboardStats;
import com.anpr.entity.AuditLog;
import com.anpr.entity.AuthorizationOutcome;
import com.anpr.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Get paginated audit logs with optional filters
     */
    public Page<AuditLogResponse> getLogs(String vehicleNumber, String outcome, 
                                           LocalDate startDate, LocalDate endDate,
                                           int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        
        AuthorizationOutcome authOutcome = null;
        if (outcome != null && !outcome.isBlank()) {
            try {
                authOutcome = AuthorizationOutcome.valueOf(outcome.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid outcome filter: {}", outcome);
            }
        }

        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

        Page<AuditLog> logs = auditLogRepository.findByFilters(
                vehicleNumber, authOutcome, startDateTime, endDateTime, pageable);

        return logs.map(AuditLogResponse::fromEntity);
    }

    /**
     * Get recent entries for live feed
     */
    public List<AuditLogResponse> getRecentEntries() {
        return auditLogRepository.findTop50ByOrderByTimestampDesc()
                .stream()
                .map(AuditLogResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get entries after a specific timestamp for polling
     */
    public List<AuditLogResponse> getEntriesSince(LocalDateTime since) {
        return auditLogRepository.findByTimestampAfterOrderByTimestampDesc(since)
                .stream()
                .map(AuditLogResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get dashboard statistics
     */
    public DashboardStats getDashboardStats(String period) {
        LocalDateTime since = calculatePeriodStart(period);
        
        Long total = auditLogRepository.countTotalEntriesSince(since);
        Long approved = auditLogRepository.countApprovedSince(since);
        Long rejected = auditLogRepository.countRejectedSince(since);
        Long overrides = auditLogRepository.countManualOverridesSince(since);

        Double approvalRate = total > 0 ? (approved.doubleValue() / total.doubleValue()) * 100 : 0.0;

        return DashboardStats.builder()
                .totalEntries(total)
                .approvedEntries(approved)
                .rejectedEntries(rejected)
                .manualOverrides(overrides)
                .approvalRate(Math.round(approvalRate * 100.0) / 100.0) // Round to 2 decimal places
                .periodDescription(getPeriodDescription(period))
                .build();
    }

    /**
     * Get single log entry by ID
     */
    public AuditLogResponse getLogById(Long id) {
        return auditLogRepository.findById(id)
                .map(AuditLogResponse::fromEntity)
                .orElse(null);
    }

    private LocalDateTime calculatePeriodStart(String period) {
        LocalDateTime now = LocalDateTime.now();
        
        return switch (period != null ? period.toLowerCase() : "today") {
            case "week" -> now.minusWeeks(1);
            case "month" -> now.minusMonths(1);
            case "year" -> now.minusYears(1);
            case "all" -> LocalDateTime.of(2000, 1, 1, 0, 0);
            default -> now.toLocalDate().atStartOfDay(); // Today
        };
    }

    private String getPeriodDescription(String period) {
        return switch (period != null ? period.toLowerCase() : "today") {
            case "week" -> "Last 7 days";
            case "month" -> "Last 30 days";
            case "year" -> "Last 365 days";
            case "all" -> "All time";
            default -> "Today";
        };
    }
}
