package com.ehrcrawler.service;

import com.ehrcrawler.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void logAccess(String actionType, String resourceType, String resourceId, String patientId, HttpServletRequest request, String status, String details) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser"))
                ? auth.getName() : "UNAUTHENTICATED";

        String ipAddress = request != null ? request.getRemoteAddr() : "UNKNOWN";

        auditLogRepository.logEvent(username, actionType, resourceType, resourceId, patientId, ipAddress, status, details);
    }
}