package com.ehrsyncsafe.web.controller;

import com.ehrsyncsafe.repository.FhirLocalRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/metadata")
public class MetadataController {

    private final FhirLocalRepository repository;

    public MetadataController(FhirLocalRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/last-sync")
    public Map<String, String> getLastSync() {
        return Map.of("lastSync", repository.getLastSyncTimestamp());
    }
}