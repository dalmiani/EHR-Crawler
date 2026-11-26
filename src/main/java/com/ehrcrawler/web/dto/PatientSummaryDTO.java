package com.ehrcrawler.web.dto;

public record PatientSummaryDTO(
		String id,
	    String name,
	    String gender,
	    String birthDate
) {}