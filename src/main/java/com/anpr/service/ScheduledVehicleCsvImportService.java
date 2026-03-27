package com.anpr.service;

import com.anpr.entity.ScheduledVehicle;
import com.anpr.repository.ScheduledVehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledVehicleCsvImportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d/M/yyyy");

    private final ScheduledVehicleRepository scheduledVehicleRepository;
    private final ResourceLoader resourceLoader;

    @Value("${anpr.erp.csv-resource:file:./Database_Schema.csv}")
    private String csvResourcePath;

    @Transactional
    public int importFromCsv(boolean reloadOnStartup) {
        Resource resource = resourceLoader.getResource(csvResourcePath);
        if (!resource.exists()) {
            log.warn("ERP CSV resource not found at {}. Falling back to classpath copy.", csvResourcePath);
            resource = resourceLoader.getResource("classpath:erp/Database_Schema.csv");
            if (!resource.exists()) {
                log.warn("Fallback ERP CSV resource not found in classpath.");
                return 0;
            }
        }

        if (!reloadOnStartup && scheduledVehicleRepository.count() > 0) {
            log.info("Scheduled vehicles already exist in H2. Skipping CSV bootstrap.");
            return 0;
        }

        List<ScheduledVehicle> rows = new ArrayList<>();
        try (InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)) {

            for (CSVRecord record : parser) {
                rows.add(ScheduledVehicle.builder()
                        .queueNo(parseLong(record.get("Queue No.")))
                        .upliftType(record.get("Uplift Type"))
                        .dateCreated(parseDate(record.get("Date Created")))
                        .omc(record.get("OMC"))
                        .omcName(record.get("OMC Name"))
                        .upliftDate(parseDate(record.get("Uplift Date")))
                        .ticketNumber(record.get("Ticket Number"))
                        .productName(record.get("Product Name"))
                        .requestedQuantity(parseQuantity(record.get("Requested Quantity")))
                        .truckRegNo(record.get("Truck Reg No."))
                        .trailorNo(record.get("Trailor No."))
                        .driverName(record.get("Driver Name"))
                        .status(record.get("Status"))
                        .location(record.get("Location"))
                        .build());
            }

            if (reloadOnStartup) {
                scheduledVehicleRepository.deleteAllInBatch();
            }
            scheduledVehicleRepository.saveAll(rows);
            log.info("Loaded {} scheduled vehicle rows from {}", rows.size(), csvResourcePath);
            return rows.size();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to import scheduled vehicles from CSV", ex);
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value.trim(), DATE_FORMATTER);
    }

    private BigDecimal parseQuantity(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        String normalized = value.replace(",", "").trim();
        return new BigDecimal(normalized);
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.parseLong(value.trim());
    }
}
