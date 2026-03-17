package com.anpr.repository;

import com.anpr.entity.AuditLog;
import com.anpr.entity.AuthorizationOutcome;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);
    
    List<AuditLog> findByVehicleNumberContainingIgnoreCaseOrderByTimestampDesc(String vehicleNumber);
    
    List<AuditLog> findByAuthorizationOutcome(AuthorizationOutcome outcome);
    
    List<AuditLog> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:vehicleNumber IS NULL OR LOWER(a.vehicleNumber) LIKE LOWER(CONCAT('%', :vehicleNumber, '%'))) AND " +
           "(:outcome IS NULL OR a.authorizationOutcome = :outcome) AND " +
           "(:startDate IS NULL OR a.timestamp >= :startDate) AND " +
           "(:endDate IS NULL OR a.timestamp <= :endDate) " +
           "ORDER BY a.timestamp DESC")
    Page<AuditLog> findByFilters(
            @Param("vehicleNumber") String vehicleNumber,
            @Param("outcome") AuthorizationOutcome outcome,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
    
    // Dashboard statistics queries
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.timestamp >= :since")
    Long countTotalEntriesSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.authorizationOutcome = 'APPROVED' AND a.timestamp >= :since")
    Long countApprovedSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.authorizationOutcome = 'REJECTED' AND a.timestamp >= :since")
    Long countRejectedSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.isManualOverride = true AND a.timestamp >= :since")
    Long countManualOverridesSince(@Param("since") LocalDateTime since);
    
    // Recent entries for live feed
    List<AuditLog> findTop50ByOrderByTimestampDesc();
    
    // Entries after a specific timestamp for polling
    List<AuditLog> findByTimestampAfterOrderByTimestampDesc(LocalDateTime after);
}
