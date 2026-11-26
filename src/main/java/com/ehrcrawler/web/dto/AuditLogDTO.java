package com.ehrcrawler.web.dto;

public record AuditLogDTO(
    Long id,
    String eventTimestamp,
    String username,
    String actionType,
    String resourceType,
    String resourceId,
    String patientId,
    String ipAddress,
    String status,
    String details
) {}