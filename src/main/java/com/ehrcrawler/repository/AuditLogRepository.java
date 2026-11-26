package com.ehrcrawler.repository;

import com.ehrcrawler.web.dto.AuditLogDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class AuditLogRepository {

    private final JdbcTemplate jdbcTemplate;

    public AuditLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        initializeSchema();
    }

    private void initializeSchema() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS audit_log (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                event_timestamp TIMESTAMP NOT NULL,
                username VARCHAR(100) NOT NULL,
                action_type VARCHAR(50) NOT NULL,
                resource_type VARCHAR(100),
                resource_id VARCHAR(255),
                patient_id VARCHAR(255),
                ip_address VARCHAR(45),
                status VARCHAR(20) NOT NULL,
                details VARCHAR(1000)
            )
        """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_audit_patient ON audit_log(patient_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_audit_user ON audit_log(username)");
    }

    public void logEvent(String username, String actionType, String resourceType, String resourceId, String patientId, String ipAddress, String status, String details) {
        jdbcTemplate.update("""
            INSERT INTO audit_log (event_timestamp, username, action_type, resource_type, resource_id, patient_id, ip_address, status, details)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, Instant.now().toString(), username, actionType, resourceType, resourceId, patientId, ipAddress, status, details);
    }

    public List<AuditLogDTO> getAllAuditLogs() {
        return jdbcTemplate.query("""
            SELECT id, event_timestamp, username, action_type, resource_type, resource_id, patient_id, ip_address, status, details
            FROM audit_log
            ORDER BY id DESC
            LIMIT 200
        """, (rs, rowNum) -> new AuditLogDTO(
            rs.getLong("id"),
            rs.getString("event_timestamp"),
            rs.getString("username"),
            rs.getString("action_type"),
            rs.getString("resource_type"),
            rs.getString("resource_id"),
            rs.getString("patient_id"),
            rs.getString("ip_address"),
            rs.getString("status"),
            rs.getString("details")
        ));
    }
}