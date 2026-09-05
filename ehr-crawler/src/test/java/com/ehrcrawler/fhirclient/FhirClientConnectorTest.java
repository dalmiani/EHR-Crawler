package com.ehrsyncsafe.fhirclient;

import com.ehrsyncsafe.config.AppConfig;
import com.ehrsyncsafe.repository.FhirLocalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FhirClientConnectorTest {

    private AppConfig appConfig;
    private FhirLocalRepository repository;
    private FhirClientConnector connector;

    @BeforeEach
    void setUp() {
        appConfig = mock(AppConfig.class);
        
        // Configuriamo endpoint fittizio per forzare la gestione dell'eccezione
        when(appConfig.getFhirBaseUrl()).thenReturn("http://localhost:9999/fhir-dummy");
        when(appConfig.getFhirTimeoutMs()).thenReturn(500);
        when(appConfig.getFhirConnectTimeoutMs()).thenReturn(500);

        repository = mock(FhirLocalRepository.class);
        connector = new FhirClientConnector(appConfig, repository);
    }

    @Test
    void testExecutePreFetch_HandlesConnectionErrorGracefully() {
        // Poiché non c'è un server FHIR reale sulla porta 9999, 
        // fetchPatientCohort() intercetterà l'eccezione e restituirà una lista vuota.
        
        // When
        connector.executePreFetch();

        // Then - Verifichiamo che il sistema gestisca l'errore senza andare in crash
        // e che chiami comunque l'aggiornamento del timestamp per completare il ciclo
        verify(repository, times(1)).updateLastSyncTimestamp();
        
        // Non essendoci pazienti (causa eccezione), non dovrebbe esserci nessun salvataggio
        verify(repository, never()).saveResource(any()); 
    }
}