package com.ehrcrawler.fhirclient;

import com.ehrcrawler.config.AppConfig;
import com.ehrcrawler.repository.FhirLocalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FhirClientConnectorTest {

    @Mock
    private AppConfig appConfig;

    @Mock
    private FhirLocalRepository repository;

    private FhirClientConnector connector;

    @BeforeEach
    void setUp() {
        when(appConfig.getFhirBaseUrl()).thenReturn("http://localhost:8080/fhir");
        when(appConfig.getFhirConnectTimeoutMs()).thenReturn(5000);
        when(appConfig.getFhirTimeoutMs()).thenReturn(10000);

        connector = new FhirClientConnector(appConfig, repository);
    }

    @Test
    void testConnectorInitialization() {
        // Verifica che l'inizializzazione del client non generi eccezioni
        verify(appConfig, times(1)).getFhirBaseUrl();
    }
}