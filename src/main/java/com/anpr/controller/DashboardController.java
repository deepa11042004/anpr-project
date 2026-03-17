package com.anpr.controller;

import com.anpr.dto.AuditLogResponse;
import com.anpr.dto.DashboardStats;
import com.anpr.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final AuditLogService auditLogService;

    /**
     * Get dashboard statistics
     * @param period - today, week, month, year, all
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStats> getStats(
            @RequestParam(defaultValue = "today") String period) {
        log.debug("Fetching dashboard stats for period: {}", period);
        DashboardStats stats = auditLogService.getDashboardStats(period);
        return ResponseEntity.ok(stats);
    }

    /**
     * Get recent entries for live feed (last 50)
     */
    @GetMapping("/live-feed")
    public ResponseEntity<List<AuditLogResponse>> getLiveFeed() {
        List<AuditLogResponse> entries = auditLogService.getRecentEntries();
        return ResponseEntity.ok(entries);
    }

    /**
     * Poll for new entries since a specific timestamp
     */
    @GetMapping("/poll")
    public ResponseEntity<List<AuditLogResponse>> pollEntries(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        List<AuditLogResponse> entries = auditLogService.getEntriesSince(since);
        return ResponseEntity.ok(entries);
    }
}
