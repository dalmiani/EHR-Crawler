package com.ehrsyncsafe.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        if ("dr.mariani".equals(credentials.get("username")) && "password123".equals(credentials.get("password"))) {
            return ResponseEntity.ok(Map.of("status", "success", "token", "session-local-token"));
        }
        return ResponseEntity.status(401).body(Map.of("error", "Credenziali non valide"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("status", "logged_out"));
    }
}