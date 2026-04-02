package com.anpr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseMigrationResponse {
    private boolean success;
    private String message;
    private int usersMigrated;
    private int scheduledVehiclesMigrated;
    private int auditLogsMigrated;
}
