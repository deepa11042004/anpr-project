package com.anpr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemLogResponse {
    private boolean success;
    private String message;
    private long cursor;
    private long fileSize;
    private List<SystemLogEntry> entries;
}
