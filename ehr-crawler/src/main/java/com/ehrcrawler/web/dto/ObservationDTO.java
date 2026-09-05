package com.ehrsyncsafe.web.dto;

public record ObservationDTO(
    String code,
    String value,
    String unit,
    String dateTime
) {}