package com.anpr.controller;

import com.anpr.dto.AuditLogResponse;
import com.anpr.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Slf4j
public class LogsController {

    private final AuditLogService auditLogService;

    /**
     * Get paginated audit logs with optional filters
     */
    @GetMapping
    public ResponseEntity<Page<AuditLogResponse>> getLogs(
            @RequestParam(required = false) String vehicleNumber,
            @RequestParam(required = false) String outcome,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("Fetching logs - vehicle: {}, outcome: {}, page: {}, size: {}", 
                vehicleNumber, outcome, page, size);
        
        Page<AuditLogResponse> logs = auditLogService.getLogs(
                vehicleNumber, outcome, startDate, endDate, page, size);
        
        return ResponseEntity.ok(logs);
    }

    /**
     * Get single log entry by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<AuditLogResponse> getLogById(@PathVariable Long id) {
        AuditLogResponse log = auditLogService.getLogById(id);
        if (log == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(log);
    }
}
