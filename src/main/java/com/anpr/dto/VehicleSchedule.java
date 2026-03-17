package com.anpr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleSchedule {
    private LocalDate date;
    private String clientCode;
    private String companyName;
    private String orderNumber;
    private String productType;
    private String quantity;
    private String vehicleNumber1;
    private String vehicleNumber2;
    private String driverName;
    private String status;
    private String location;
}
