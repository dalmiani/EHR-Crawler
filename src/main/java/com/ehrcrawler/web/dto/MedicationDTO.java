package com.ehrcrawler.web.dto;

public record MedicationDTO(
    String medicationName,
    String dosage,
    String status
) {}