package com.anpr.service;

import com.anpr.dto.VehicleSchedule;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@Slf4j
public class GoogleSheetsIntegrationService {

    @Value("${google.sheets.spreadsheet-id}")
    private String spreadsheetId;

    @Value("${google.sheets.credentials-file}")
    private String credentialsFile;

    @Value("${google.sheets.sheet-name:VehicleSchedule}")
    private String sheetName;

    @Value("${google.sheets.columns.date:0}")
    private int dateColumn;

    @Value("${google.sheets.columns.client-code:1}")
    private int clientCodeColumn;

    @Value("${google.sheets.columns.company-name:2}")
    private int companyNameColumn;

    @Value("${google.sheets.columns.order-number:3}")
    private int orderNumberColumn;

    @Value("${google.sheets.columns.product-type:4}")
    private int productTypeColumn;

    @Value("${google.sheets.columns.quantity:5}")
    private int quantityColumn;

    @Value("${google.sheets.columns.vehicle-number-1:6}")
    private int vehicleNumber1Column;

    @Value("${google.sheets.columns.vehicle-number-2:7}")
    private int vehicleNumber2Column;

    @Value("${google.sheets.columns.driver-name:8}")
    private int driverNameColumn;

    @Value("${google.sheets.columns.status:9}")
    private int statusColumn;

    @Value("${google.sheets.columns.location:10}")
    private int locationColumn;

    private Sheets sheetsService;
    private boolean serviceInitialized = false;

    private static final List<DateTimeFormatter> DATE_FORMATTERS = Arrays.asList(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("M/d/yyyy")
    );

    @PostConstruct
    public void init() {
        try {
            GoogleCredentials credentials = GoogleCredentials
                    .fromStream(new FileInputStream(credentialsFile))
                    .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS_READONLY));

            sheetsService = new Sheets.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName("ANPR Access Control")
                    .build();

            serviceInitialized = true;
            log.info("Google Sheets service initialized successfully");
        } catch (IOException | GeneralSecurityException e) {
            log.warn("Failed to initialize Google Sheets service: {}. " +
                    "Vehicle authorization will be skipped until configured properly.", e.getMessage());
            serviceInitialized = false;
        }
    }

    /**
     * Check if a vehicle plate is authorized for the current date
     */
    public VehicleAuthorizationResult checkVehicleAuthorization(String normalizedPlate) {
        if (!serviceInitialized) {
            log.warn("Google Sheets service not initialized. Returning NOT_CONFIGURED status.");
            return VehicleAuthorizationResult.notConfigured();
        }

        try {
            List<VehicleSchedule> schedules = getSchedulesForToday();
            
            for (VehicleSchedule schedule : schedules) {
                String vehicle1 = normalizePlate(schedule.getVehicleNumber1());
                String vehicle2 = normalizePlate(schedule.getVehicleNumber2());
                
                if (normalizedPlate.equals(vehicle1) || normalizedPlate.equals(vehicle2)) {
                    log.info("Vehicle {} found in schedule - Company: {}, Driver: {}", 
                            normalizedPlate, schedule.getCompanyName(), schedule.getDriverName());
                    return VehicleAuthorizationResult.authorized(schedule);
                }
            }
            
            log.info("Vehicle {} not found in today's schedule", normalizedPlate);
            return VehicleAuthorizationResult.notAuthorized("Vehicle not scheduled for today");
            
        } catch (Exception e) {
            log.error("Error checking vehicle authorization: {}", e.getMessage(), e);
            return VehicleAuthorizationResult.error("Failed to check authorization: " + e.getMessage());
        }
    }

    /**
     * Get all scheduled vehicles for today
     */
    public List<VehicleSchedule> getSchedulesForToday() throws IOException {
        if (!serviceInitialized) {
            return Collections.emptyList();
        }

        LocalDate today = LocalDate.now();
        List<VehicleSchedule> todaySchedules = new ArrayList<>();
        
        // Read all data from the sheet
        String range = sheetName + "!A:K"; // Columns A to K
        ValueRange response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();

        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty()) {
            log.info("No data found in Google Sheet");
            return todaySchedules;
        }

        // Skip header row (index 0)
        for (int i = 1; i < values.size(); i++) {
            List<Object> row = values.get(i);
            if (row.isEmpty()) continue;

            try {
                VehicleSchedule schedule = parseRow(row);
                if (schedule != null && schedule.getDate() != null && schedule.getDate().equals(today)) {
                    todaySchedules.add(schedule);
                }
            } catch (Exception e) {
                log.warn("Error parsing row {}: {}", i, e.getMessage());
            }
        }

        log.info("Found {} scheduled vehicles for today", todaySchedules.size());
        return todaySchedules;
    }

    private VehicleSchedule parseRow(List<Object> row) {
        return VehicleSchedule.builder()
                .date(parseDate(getCellValue(row, dateColumn)))
                .clientCode(getCellValue(row, clientCodeColumn))
                .companyName(getCellValue(row, companyNameColumn))
                .orderNumber(getCellValue(row, orderNumberColumn))
                .productType(getCellValue(row, productTypeColumn))
                .quantity(getCellValue(row, quantityColumn))
                .vehicleNumber1(getCellValue(row, vehicleNumber1Column))
                .vehicleNumber2(getCellValue(row, vehicleNumber2Column))
                .driverName(getCellValue(row, driverNameColumn))
                .status(getCellValue(row, statusColumn))
                .location(getCellValue(row, locationColumn))
                .build();
    }

    private String getCellValue(List<Object> row, int index) {
        if (index >= 0 && index < row.size() && row.get(index) != null) {
            return row.get(index).toString().trim();
        }
        return null;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(dateStr.trim(), formatter);
            } catch (DateTimeParseException ignored) {
                // Try next formatter
            }
        }

        log.warn("Unable to parse date: {}", dateStr);
        return null;
    }

    /**
     * Normalize a license plate by removing spaces and converting to uppercase
     */
    public String normalizePlate(String plate) {
        if (plate == null) {
            return "";
        }
        return plate.replaceAll("[\\s\\-]", "").toUpperCase();
    }

    public boolean isServiceInitialized() {
        return serviceInitialized;
    }

    // Inner class for authorization result
    public static class VehicleAuthorizationResult {
        private final boolean authorized;
        private final boolean configured;
        private final String message;
        private final VehicleSchedule schedule;
        private final boolean error;

        private VehicleAuthorizationResult(boolean authorized, boolean configured, String message, 
                                           VehicleSchedule schedule, boolean error) {
            this.authorized = authorized;
            this.configured = configured;
            this.message = message;
            this.schedule = schedule;
            this.error = error;
        }

        public static VehicleAuthorizationResult authorized(VehicleSchedule schedule) {
            return new VehicleAuthorizationResult(true, true, "Vehicle authorized", schedule, false);
        }

        public static VehicleAuthorizationResult notAuthorized(String reason) {
            return new VehicleAuthorizationResult(false, true, reason, null, false);
        }

        public static VehicleAuthorizationResult notConfigured() {
            return new VehicleAuthorizationResult(false, false, 
                    "Google Sheets not configured", null, false);
        }

        public static VehicleAuthorizationResult error(String message) {
            return new VehicleAuthorizationResult(false, true, message, null, true);
        }

        public boolean isAuthorized() { return authorized; }
        public boolean isConfigured() { return configured; }
        public String getMessage() { return message; }
        public VehicleSchedule getSchedule() { return schedule; }
        public boolean isError() { return error; }
    }
}
