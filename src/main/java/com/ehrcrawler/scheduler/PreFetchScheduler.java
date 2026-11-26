package com.ehrcrawler.scheduler;

import com.ehrcrawler.fhirclient.FhirClientConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
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

    // NOVITÀ: Esegue il download dei dati ISTANTANEAMENTE all'avvio dell'applicazione
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("Applicazione avviata: Inizio il Pre-fetch immediato dal server FHIR...");
        scheduledPreFetch();
    }

    // Mantiene comunque il controllo ogni 15 minuti
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