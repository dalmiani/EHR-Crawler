package com.ehrsyncsafe.scheduler;

import com.ehrsyncsafe.fhirclient.FhirClientConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PreFetchScheduler {

    private static final Logger log = LoggerFactory.getLogger(PreFetchScheduler.class);
    private final FhirClientConnector fhirClientConnector;

    public PreFetchScheduler(FhirClientConnector fhirClientConnector) {
        this.fhirClientConnector = fhirClientConnector;
    }

    @Scheduled(cron = "${fetch.cron}")
    public void scheduledPreFetch() {
        log.info("Scheduled FHIR pre-fetch triggered at {}", LocalDateTime.now());
        try {
            fhirClientConnector.executePreFetch();
            log.info("Scheduled FHIR pre-fetch completed successfully");
        } catch (Exception e) {
            log.error("Scheduled FHIR pre-fetch failed: {}", e.getMessage(), e);
        }
    }
}