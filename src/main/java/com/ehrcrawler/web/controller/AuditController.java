package com.ehrcrawler.web.controller;

import com.ehrcrawler.repository.AuditLogRepository;
import com.ehrcrawler.service.AuditService;
import com.ehrcrawler.web.dto.AuditLogDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditController {

    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;

    public AuditController(AuditLogRepository auditLogRepository, AuditService auditService) {
        this.auditLogRepository = auditLogRepository;
        this.auditService = auditService;
    }

    @GetMapping
    public List<AuditLogDTO> getAuditLogs(HttpServletRequest request) {
        auditService.logAccess("READ_AUDIT_LOGS", "AuditLog", null, null, request, "SUCCESS", "User requested system audit trail");
        return auditLogRepository.getAllAuditLogs();
    }
}