package com.ehrsyncsafe.repository;

import com.ehrsyncsafe.config.AppConfig;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@JdbcTest
class FhirLocalRepositoryTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private FhirLocalRepository repository;

    @BeforeEach
    void setUp() {
        // Mockiamo la configurazione per evitare dipendenze esterne
        AppConfig mockConfig = mock(AppConfig.class);
        
        // Il costruttore inizializza in automatico lo schema H2
        repository = new FhirLocalRepository(jdbcTemplate, mockConfig);
    }

    @Test
    void testSaveAndGetAllPatients() {
        // Given - Creazione di una risorsa Patient di test
        Patient patient = new Patient();
        patient.setId("pt-001");
        patient.addName().setFamily("Rossi").addGiven("Mario");
        patient.setGender(AdministrativeGender.MALE);

        // When - Salvataggio e recupero
        repository.saveResource(patient);
        List<Patient> patients = repository.getAllPatients();

        // Then - Validazione
        assertFalse(patients.isEmpty(), "La lista dei pazienti non dovrebbe essere vuota");
        assertEquals(1, patients.size(), "Dovrebbe esserci esattamente 1 paziente");
        
        Patient savedPatient = patients.get(0);
        assertEquals("pt-001", savedPatient.getIdElement().getIdPart());
        assertEquals(AdministrativeGender.MALE, savedPatient.getGender());
        assertEquals("Mario Rossi", savedPatient.getNameFirstRep().getNameAsSingleString());
    }

    @Test
    void testUpdateLastSyncTimestamp() {
        // When
        repository.updateLastSyncTimestamp();
        String lastSync = repository.getLastSyncTimestamp();

        // Then
        assertNotNull(lastSync);
        assertNotEquals("Non Disponibile", lastSync, "Il timestamp dovrebbe essere stato aggiornato nel DB");
    }
}