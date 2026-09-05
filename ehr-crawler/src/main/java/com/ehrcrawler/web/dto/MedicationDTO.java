package com.ehrsyncsafe.web.dto;

public record MedicationDTO(
    String medicationName,
    String dosage,
    String status
) {}