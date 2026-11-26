package com.ehrcrawler.web.dto;

public record ObservationDTO(
	    String code,
	    String value,
	    String unit,
	    String date,
	    String category
	) {}