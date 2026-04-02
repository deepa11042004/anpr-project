package com.anpr.controller;

import com.anpr.dto.SystemLogResponse;
import com.anpr.service.SystemLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/system-logs")
@RequiredArgsConstructor
public class SystemLogsController {

    private final SystemLogService systemLogService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<SystemLogResponse> getLogs(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "300") Integer maxLines,
            @RequestParam(defaultValue = "300") Integer tailLines) {

        return ResponseEntity.ok(systemLogService.readLogs(cursor, maxLines, tailLines));
    }
}
