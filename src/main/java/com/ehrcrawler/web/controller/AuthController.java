package com.ehrcrawler.web.controller;

import com.ehrcrawler.repository.FhirLocalRepository;
import com.ehrcrawler.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final FhirLocalRepository repository;
    private final AuditService auditService;

    public AuthController(FhirLocalRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials, HttpServletRequest request, HttpServletResponse response) {
        String user = credentials.get("username");
        if ("doctor".equals(user) && "********".equals(credentials.get("password"))) {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new UsernamePasswordAuthenticationToken(
                    user, null, List.of(new SimpleGrantedAuthority("ROLE_CLINICIAN"))));
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);

            auditService.logAccess("LOGIN", "System", null, null, request, "SUCCESS", "Practitioner logged in");
            return ResponseEntity.ok(Map.of("status", "success", "token", "session-local-token"));
        }

        auditService.logAccess("LOGIN", "System", null, null, request, "FAILURE", "Failed login attempt for user: " + user);
        return ResponseEntity.status(401).body(Map.of("error", "Credenziali non valide"));
    }

    @PostMapping("/reset-db")
    public ResponseEntity<?> resetDatabase(HttpServletRequest request) {
        auditService.logAccess("DELETE_ALL_DATA", "Database", null, null, request, "SUCCESS", "Local database cleared by user");
        repository.resetDatabase();
        return ResponseEntity.ok(Map.of("status", "success", "message", "Database locale azzerato"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        auditService.logAccess("LOGOUT", "System", null, null, request, "SUCCESS", "Practitioner logged out");
        SecurityContextHolder.clearContext();
        request.getSession().invalidate();
        return ResponseEntity.ok(Map.of("status", "logged_out"));
    }
}