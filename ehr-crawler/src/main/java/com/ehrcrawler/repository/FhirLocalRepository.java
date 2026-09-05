package com.ehrsyncsafe.repository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.ehrsyncsafe.config.*;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class FhirLocalRepository {

    private static final Logger log = LoggerFactory.getLogger(FhirLocalRepository.class);
    private final JdbcTemplate jdbcTemplate;
    private final AppConfig appConfig;
    private final IParser fhirParser;

    public FhirLocalRepository(JdbcTemplate jdbcTemplate, AppConfig appConfig) {
        this.jdbcTemplate = jdbcTemplate;
        this.appConfig = appConfig;
        this.fhirParser = FhirContext.forR4().newJsonParser();
        initializeSchema();
    }

    private void initializeSchema() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS fhir_resource (
                resource_id VARCHAR(255) PRIMARY KEY,
                resource_type VARCHAR(100) NOT NULL,
                patient_id VARCHAR(255),
                last_updated TIMESTAMP NOT NULL,
                resource_json CLOB NOT NULL
            )
        """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_resource_type ON fhir_resource(resource_type)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_patient_id ON fhir_resource(patient_id)");

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS sync_metadata (
                key VARCHAR(100) PRIMARY KEY,
                value VARCHAR(500) NOT NULL
            )
        """);
    }

    public void saveResource(IBaseResource resource) {
        Resource r = (Resource) resource;
        String json = fhirParser.encodeResourceToString(r);
        String id = r.getIdElement().getIdPart();
        String type = r.getResourceType().name();
        String patientId = extractPatientId(r);

        jdbcTemplate.update("""
            MERGE INTO fhir_resource (resource_id, resource_type, patient_id, last_updated, resource_json)
            KEY (resource_id) VALUES (?, ?, ?, ?, ?)
        """, id, type, patientId, Instant.now(), json);
    }

    public void updateLastSyncTimestamp() {
        jdbcTemplate.update("""
            MERGE INTO sync_metadata (key, value) KEY (key) VALUES ('last_sync', ?)
        """, Instant.now().toString());
    }

    public String getLastSyncTimestamp() {
        try {
            return jdbcTemplate.queryForObject("SELECT value FROM sync_metadata WHERE key = 'last_sync'", String.class);
        } catch (Exception e) {
            return "Non Disponibile";
        }
    }

    public List<Patient> getAllPatients() {
        List<String> jsons = jdbcTemplate.queryForList("SELECT resource_json FROM fhir_resource WHERE resource_type = 'Patient'", String.class);
        List<Patient> patients = new ArrayList<>();
        for (String json : jsons) {
            patients.add((Patient) fhirParser.parseResource(json));
        }
        return patients;
    }

    public List<MedicationRequest> getMedications(String patientId) {
        List<String> jsons = jdbcTemplate.queryForList("SELECT resource_json FROM fhir_resource WHERE resource_type = 'MedicationRequest' AND patient_id = ?", String.class, patientId);
        List<MedicationRequest> list = new ArrayList<>();
        for (String json : jsons) {
            list.add((MedicationRequest) fhirParser.parseResource(json));
        }
        return list;
    }

    public List<AllergyIntolerance> getAllergies(String patientId) {
        List<String> jsons = jdbcTemplate.queryForList("SELECT resource_json FROM fhir_resource WHERE resource_type = 'AllergyIntolerance' AND patient_id = ?", String.class, patientId);
        List<AllergyIntolerance> list = new ArrayList<>();
        for (String json : jsons) {
            list.add((AllergyIntolerance) fhirParser.parseResource(json));
        }
        return list;
    }

    public List<Observation> getObservations(String patientId) {
        List<String> jsons = jdbcTemplate.queryForList("SELECT resource_json FROM fhir_resource WHERE resource_type = 'Observation' AND patient_id = ?", String.class, patientId);
        List<Observation> list = new ArrayList<>();
        for (String json : jsons) {
            list.add((Observation) fhirParser.parseResource(json));
        }
        return list;
    }

    private String extractPatientId(Resource r) {
        if (r instanceof Patient p) return p.getIdElement().getIdPart();
        if (r instanceof Observation o && o.hasSubject()) return o.getSubject().getReferenceElement().getIdPart();
        if (r instanceof AllergyIntolerance a && a.hasPatient()) return a.getPatient().getReferenceElement().getIdPart();
        if (r instanceof MedicationRequest m && m.hasSubject()) return m.getSubject().getReferenceElement().getIdPart();
        return null;
    }
}