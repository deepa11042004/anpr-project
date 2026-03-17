package com.anpr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStats {
    private Long totalEntries;
    private Long approvedEntries;
    private Long rejectedEntries;
    private Long manualOverrides;
    private Double approvalRate;
    private String periodDescription;
}
