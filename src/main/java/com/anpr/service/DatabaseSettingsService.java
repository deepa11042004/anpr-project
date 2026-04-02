package com.anpr.service;

import com.anpr.dto.ActionResponse;
import com.anpr.dto.DatabaseMigrationResponse;
import com.anpr.dto.DatabaseSettingsRequest;
import com.anpr.dto.DatabaseTestResponse;
import com.anpr.entity.AuditLog;
import com.anpr.entity.ScheduledVehicle;
import com.anpr.entity.User;
import com.anpr.repository.AuditLogRepository;
import com.anpr.repository.ScheduledVehicleRepository;
import com.anpr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import java.util.Properties;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseSettingsService {

    private static final Path RUNTIME_CONFIG_FILE = Path.of("config", "runtime-db.properties");

    private final UserRepository userRepository;
    private final ScheduledVehicleRepository scheduledVehicleRepository;
    private final AuditLogRepository auditLogRepository;

    public DatabaseTestResponse testConnection(DatabaseSettingsRequest request) {
        String jdbcUrl = buildJdbcUrl(request);

        try (Connection ignored = DriverManager.getConnection(jdbcUrl, request.getUsername(), request.getPassword())) {
            return DatabaseTestResponse.builder()
                    .success(true)
                    .message("Connection successful")
                    .jdbcUrl(jdbcUrl)
                    .build();
        } catch (Exception ex) {
            log.warn("Database test connection failed: {}", ex.getMessage());
            return DatabaseTestResponse.builder()
                    .success(false)
                    .message("Connection failed: " + ex.getMessage())
                    .jdbcUrl(jdbcUrl)
                    .build();
        }
    }

    public DatabaseMigrationResponse migrateData(DatabaseSettingsRequest request) {
        String jdbcUrl = buildJdbcUrl(request);

        try (Connection connection = DriverManager.getConnection(jdbcUrl, request.getUsername(), request.getPassword())) {
            connection.setAutoCommit(false);
            createTablesIfNotExists(connection);

            int users = migrateUsers(connection);
            int scheduledVehicles = migrateScheduledVehicles(connection);
            int auditLogs = migrateAuditLogs(connection);

            connection.commit();
            return DatabaseMigrationResponse.builder()
                    .success(true)
                    .message("Migration completed successfully")
                    .usersMigrated(users)
                    .scheduledVehiclesMigrated(scheduledVehicles)
                    .auditLogsMigrated(auditLogs)
                    .build();
        } catch (Exception ex) {
            log.error("Migration to external database failed", ex);
            return DatabaseMigrationResponse.builder()
                    .success(false)
                    .message("Migration failed: " + ex.getMessage())
                    .usersMigrated(0)
                    .scheduledVehiclesMigrated(0)
                    .auditLogsMigrated(0)
                    .build();
        }
    }

    public ActionResponse saveRuntimeConfig(DatabaseSettingsRequest request) {
        try {
            Files.createDirectories(RUNTIME_CONFIG_FILE.getParent());

            Properties properties = new Properties();
            properties.setProperty("spring.datasource.url", buildJdbcUrl(request));
            properties.setProperty("spring.datasource.driverClassName", "com.mysql.cj.jdbc.Driver");
            properties.setProperty("spring.datasource.username", request.getUsername());
            properties.setProperty("spring.datasource.password", request.getPassword());
            properties.setProperty("spring.jpa.database-platform", "org.hibernate.dialect.MySQLDialect");
            properties.setProperty("spring.h2.console.enabled", "false");
            properties.setProperty("anpr.erp.bootstrap-enabled", "false");

            try (var outputStream = Files.newOutputStream(RUNTIME_CONFIG_FILE)) {
                properties.store(outputStream, "Runtime DB configuration generated from Settings UI");
            }

            return ActionResponse.builder()
                    .success(true)
                    .message("Configuration saved. Restart the application to apply the new database.")
                    .build();
        } catch (Exception ex) {
            log.error("Failed to save runtime DB configuration", ex);
            return ActionResponse.builder()
                    .success(false)
                    .message("Failed to save config: " + ex.getMessage())
                    .build();
        }
    }

    public ActionResponse resetToDefaultConfig() {
        try {
            if (Files.exists(RUNTIME_CONFIG_FILE)) {
                Files.delete(RUNTIME_CONFIG_FILE);
                log.info("Deleted runtime DB config file: {}", RUNTIME_CONFIG_FILE);
            }

            return ActionResponse.builder()
                    .success(true)
                    .message("Runtime DB override removed. Restart the application to switch back to default H2 configuration.")
                    .build();
        } catch (Exception ex) {
            log.error("Failed to reset runtime DB configuration", ex);
            return ActionResponse.builder()
                    .success(false)
                    .message("Failed to reset config: " + ex.getMessage())
                    .build();
        }
    }

    private int migrateUsers(Connection connection) throws Exception {
        List<User> users = userRepository.findAll();

        String sql = "INSERT INTO users (id, username, password, full_name, email, role, enabled, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE username=VALUES(username), password=VALUES(password), full_name=VALUES(full_name), " +
                "email=VALUES(email), role=VALUES(role), enabled=VALUES(enabled), created_at=VALUES(created_at), updated_at=VALUES(updated_at)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (User user : users) {
                statement.setLong(1, user.getId());
                statement.setString(2, user.getUsername());
                statement.setString(3, user.getPassword());
                statement.setString(4, user.getFullName());
                statement.setString(5, user.getEmail());
                statement.setString(6, user.getRole().name());
                statement.setBoolean(7, user.isEnabled());
                statement.setTimestamp(8, user.getCreatedAt() != null ? Timestamp.valueOf(user.getCreatedAt()) : null);
                statement.setTimestamp(9, user.getUpdatedAt() != null ? Timestamp.valueOf(user.getUpdatedAt()) : null);
                statement.addBatch();
            }
            statement.executeBatch();
        }

        return users.size();
    }

    private int migrateScheduledVehicles(Connection connection) throws Exception {
        List<ScheduledVehicle> rows = scheduledVehicleRepository.findAll();

        String sql = "INSERT INTO scheduled_vehicles (queue_no, uplift_type, date_created, omc, omc_name, uplift_date, " +
                "ticket_number, product_name, requested_quantity, truck_reg_no, trailor_no, driver_name, status, location) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE uplift_type=VALUES(uplift_type), date_created=VALUES(date_created), omc=VALUES(omc), " +
                "omc_name=VALUES(omc_name), uplift_date=VALUES(uplift_date), ticket_number=VALUES(ticket_number), " +
                "product_name=VALUES(product_name), requested_quantity=VALUES(requested_quantity), truck_reg_no=VALUES(truck_reg_no), " +
                "trailor_no=VALUES(trailor_no), driver_name=VALUES(driver_name), status=VALUES(status), location=VALUES(location)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (ScheduledVehicle row : rows) {
                statement.setLong(1, row.getQueueNo());
                statement.setString(2, row.getUpliftType());
                statement.setDate(3, row.getDateCreated() != null ? java.sql.Date.valueOf(row.getDateCreated()) : null);
                statement.setString(4, row.getOmc());
                statement.setString(5, row.getOmcName());
                statement.setDate(6, row.getUpliftDate() != null ? java.sql.Date.valueOf(row.getUpliftDate()) : null);
                statement.setString(7, row.getTicketNumber());
                statement.setString(8, row.getProductName());
                statement.setBigDecimal(9, row.getRequestedQuantity() != null ? row.getRequestedQuantity() : BigDecimal.ZERO);
                statement.setString(10, row.getTruckRegNo());
                statement.setString(11, row.getTrailorNo());
                statement.setString(12, row.getDriverName());
                statement.setString(13, row.getStatus());
                statement.setString(14, row.getLocation());
                statement.addBatch();
            }
            statement.executeBatch();
        }

        return rows.size();
    }

    private int migrateAuditLogs(Connection connection) throws Exception {
        List<AuditLog> logs = auditLogRepository.findAll();

        String sql = "INSERT INTO audit_logs (id, vehicle_number, normalized_plate, timestamp, authorization_outcome, gate_action, " +
                "confidence_score, camera_ip, vehicle_type, direction, country, user_id, rejection_reason, override_reason, " +
                "is_manual_override, plate_image, vehicle_image, company_name, driver_name, queue_no, ticket_number, product_name, " +
                "requested_quantity, truck_reg_no, trailor_no, uplift_date, status, location, omc_code, uplift_type) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE vehicle_number=VALUES(vehicle_number), normalized_plate=VALUES(normalized_plate), " +
                "timestamp=VALUES(timestamp), authorization_outcome=VALUES(authorization_outcome), gate_action=VALUES(gate_action), " +
                "confidence_score=VALUES(confidence_score), camera_ip=VALUES(camera_ip), vehicle_type=VALUES(vehicle_type), " +
                "direction=VALUES(direction), country=VALUES(country), user_id=VALUES(user_id), rejection_reason=VALUES(rejection_reason), " +
                "override_reason=VALUES(override_reason), is_manual_override=VALUES(is_manual_override), plate_image=VALUES(plate_image), " +
                "vehicle_image=VALUES(vehicle_image), company_name=VALUES(company_name), driver_name=VALUES(driver_name), " +
                "queue_no=VALUES(queue_no), ticket_number=VALUES(ticket_number), product_name=VALUES(product_name), " +
                "requested_quantity=VALUES(requested_quantity), truck_reg_no=VALUES(truck_reg_no), trailor_no=VALUES(trailor_no), " +
                "uplift_date=VALUES(uplift_date), status=VALUES(status), location=VALUES(location), omc_code=VALUES(omc_code), uplift_type=VALUES(uplift_type)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (AuditLog logRow : logs) {
                statement.setLong(1, logRow.getId());
                statement.setString(2, logRow.getVehicleNumber());
                statement.setString(3, logRow.getNormalizedPlate());
                statement.setTimestamp(4, logRow.getTimestamp() != null ? Timestamp.valueOf(logRow.getTimestamp()) : null);
                statement.setString(5, logRow.getAuthorizationOutcome() != null ? logRow.getAuthorizationOutcome().name() : null);
                statement.setString(6, logRow.getGateAction() != null ? logRow.getGateAction().name() : null);
                statement.setObject(7, logRow.getConfidenceScore());
                statement.setString(8, logRow.getCameraIp());
                statement.setString(9, logRow.getVehicleType());
                statement.setString(10, logRow.getDirection());
                statement.setString(11, logRow.getCountry());
                statement.setString(12, logRow.getUserId());
                statement.setString(13, logRow.getRejectionReason());
                statement.setString(14, logRow.getOverrideReason());
                statement.setBoolean(15, logRow.isManualOverride());
                statement.setString(16, logRow.getPlateImage());
                statement.setString(17, logRow.getVehicleImage());
                statement.setString(18, logRow.getCompanyName());
                statement.setString(19, logRow.getDriverName());
                if (logRow.getQueueNo() != null) {
                    statement.setLong(20, logRow.getQueueNo());
                } else {
                    statement.setNull(20, Types.BIGINT);
                }
                statement.setString(21, logRow.getTicketNumber());
                statement.setString(22, logRow.getProductName());
                statement.setBigDecimal(23, logRow.getRequestedQuantity());
                statement.setString(24, logRow.getTruckRegNo());
                statement.setString(25, logRow.getTrailorNo());
                statement.setDate(26, logRow.getUpliftDate() != null ? java.sql.Date.valueOf(logRow.getUpliftDate()) : null);
                statement.setString(27, logRow.getStatus());
                statement.setString(28, logRow.getLocation());
                statement.setString(29, logRow.getOmcCode());
                statement.setString(30, logRow.getUpliftType());
                statement.addBatch();
            }
            statement.executeBatch();
        }

        return logs.size();
    }

    private void createTablesIfNotExists(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id BIGINT PRIMARY KEY," +
                    "username VARCHAR(255) NOT NULL UNIQUE," +
                    "password VARCHAR(255) NOT NULL," +
                    "full_name VARCHAR(255) NOT NULL," +
                    "email VARCHAR(255) UNIQUE," +
                    "role VARCHAR(50) NOT NULL," +
                    "enabled BOOLEAN NOT NULL," +
                    "created_at DATETIME(6)," +
                    "updated_at DATETIME(6))");

            statement.execute("CREATE TABLE IF NOT EXISTS scheduled_vehicles (" +
                    "queue_no BIGINT PRIMARY KEY," +
                    "uplift_type VARCHAR(255)," +
                    "date_created DATE," +
                    "omc VARCHAR(100)," +
                    "omc_name VARCHAR(255)," +
                    "uplift_date DATE," +
                    "ticket_number VARCHAR(100)," +
                    "product_name VARCHAR(255)," +
                    "requested_quantity DECIMAL(18,2)," +
                    "truck_reg_no VARCHAR(100)," +
                    "trailor_no VARCHAR(100)," +
                    "driver_name VARCHAR(255)," +
                    "status VARCHAR(100)," +
                    "location VARCHAR(255))");

            statement.execute("CREATE TABLE IF NOT EXISTS audit_logs (" +
                    "id BIGINT PRIMARY KEY," +
                    "vehicle_number VARCHAR(255) NOT NULL," +
                    "normalized_plate VARCHAR(255)," +
                    "timestamp DATETIME(6) NOT NULL," +
                    "authorization_outcome VARCHAR(50) NOT NULL," +
                    "gate_action VARCHAR(50)," +
                    "confidence_score DOUBLE," +
                    "camera_ip VARCHAR(100)," +
                    "vehicle_type VARCHAR(100)," +
                    "direction VARCHAR(100)," +
                    "country VARCHAR(20)," +
                    "user_id VARCHAR(255)," +
                    "rejection_reason TEXT," +
                    "override_reason TEXT," +
                    "is_manual_override BOOLEAN," +
                    "plate_image LONGTEXT," +
                    "vehicle_image LONGTEXT," +
                    "company_name VARCHAR(255)," +
                    "driver_name VARCHAR(255)," +
                    "queue_no BIGINT," +
                    "ticket_number VARCHAR(100)," +
                    "product_name VARCHAR(255)," +
                    "requested_quantity DECIMAL(18,2)," +
                    "truck_reg_no VARCHAR(100)," +
                    "trailor_no VARCHAR(100)," +
                    "uplift_date DATE," +
                    "status VARCHAR(100)," +
                    "location VARCHAR(255)," +
                    "omc_code VARCHAR(100)," +
                    "uplift_type VARCHAR(255))");
        }
    }

    private String buildJdbcUrl(DatabaseSettingsRequest request) {
        return String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=%s&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                request.getHost().trim(),
                request.getPort(),
                request.getDatabaseName().trim(),
                request.isUseSsl());
    }
}
