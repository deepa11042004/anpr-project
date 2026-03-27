package com.anpr.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "scheduled_vehicles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledVehicle {

    @Id
    @Column(name = "queue_no")
    private Long queueNo;

    @Column(name = "uplift_type")
    private String upliftType;

    @Column(name = "date_created")
    private LocalDate dateCreated;

    @Column(name = "omc")
    private String omc;

    @Column(name = "omc_name")
    private String omcName;

    @Column(name = "uplift_date")
    private LocalDate upliftDate;

    @Column(name = "ticket_number")
    private String ticketNumber;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "requested_quantity")
    private BigDecimal requestedQuantity;

    @Column(name = "truck_reg_no")
    private String truckRegNo;

    @Column(name = "trailor_no")
    private String trailorNo;

    @Column(name = "driver_name")
    private String driverName;

    @Column(name = "status")
    private String status;

    @Column(name = "location")
    private String location;
}
