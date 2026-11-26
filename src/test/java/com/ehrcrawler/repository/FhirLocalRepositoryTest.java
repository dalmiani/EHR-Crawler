package com.ehrcrawler.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
class FhirLocalRepositoryTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private FhirLocalRepository repository;

    @BeforeEach
    void setUp() {
        repository = new FhirLocalRepository(jdbcTemplate);
    }

    @Test
    void testSaveAndGetPatientResource() {
        String patientJson = "{\"resourceType\":\"Patient\",\"id\":\"test-p1\",\"name\":[{\"family\":\"Doe\"}]}";
        
        repository.saveResource("Patient", "test-p1", "test-p1", patientJson);

        List<String> patients = repository.getResourcesByType("Patient");
        assertNotNull(patients);
        assertFalse(patients.isEmpty());
        assertEquals(1, patients.size());
    }

    @Test
    void testLastSyncTimestamp() {
        repository.updateLastSyncTimestamp();
        String lastSync = repository.getLastSyncTimestamp();
        
        assertNotNull(lastSync);
        assertNotEquals("Non Disponibile", lastSync);
    }
}