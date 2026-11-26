package com.ehrcrawler.repository;

import org.hl7.fhir.r4.model.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.Observation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


@Repository
public class FhirLocalRepository {

	private final JdbcTemplate jdbcTemplate;
    private final IParser fhirParser; // Field declaration

    public FhirLocalRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.fhirParser = FhirContext.forR4().newJsonParser(); // Initialize HAPI FHIR parser
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
                meta_key VARCHAR(100) PRIMARY KEY,
                meta_value VARCHAR(500) NOT NULL
            )
        """);
    }

    public void saveResource(String resourceType, String resourceId, String patientId, String resourceJson) {
        jdbcTemplate.update("""
            MERGE INTO fhir_resource (resource_id, resource_type, patient_id, last_updated, resource_json)
            KEY (resource_id)
            VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?)
        """, resourceId, resourceType, patientId, resourceJson);
    }

    public List<String> getResourcesByType(String resourceType) {
        return jdbcTemplate.queryForList("""
            SELECT resource_json FROM fhir_resource WHERE LOWER(resource_type) = LOWER(?)
        """, String.class, resourceType);
    }

    public String getResourceById(String resourceType, String resourceId) {
        try {
            return jdbcTemplate.queryForObject("""
                SELECT resource_json FROM fhir_resource WHERE LOWER(resource_type) = LOWER(?) AND resource_id = ?
            """, String.class, resourceType, resourceId);
        } catch (Exception e) {
            return null;
        }
    }

    public List<String> getMedicationsByPatientId(String patientId) {
        return jdbcTemplate.queryForList("""
            SELECT resource_json FROM fhir_resource 
            WHERE LOWER(resource_type) = 'medicationrequest' AND (patient_id = ? OR patient_id = ?)
        """, String.class, patientId, "Patient/" + patientId);
    }

    public void updateLastSyncTimestamp() {
        jdbcTemplate.update("""
            MERGE INTO sync_metadata (meta_key, meta_value) KEY (meta_key) VALUES ('last_sync', ?)
        """, Instant.now().toString());
    }

    public String getLastSyncTimestamp() {
        try {
            return jdbcTemplate.queryForObject("SELECT meta_value FROM sync_metadata WHERE meta_key = 'last_sync'", String.class);
        } catch (Exception e) {
            return "Non Disponibile";
        }
    }
    
    public List<Patient> getAllPatients() {
        List<String> jsons = jdbcTemplate.queryForList("SELECT resource_json FROM fhir_resource WHERE resource_type = 'Patient'", String.class);
        List<Patient> list = new ArrayList<>();
        for (String json : jsons) {
            list.add((Patient) fhirParser.parseResource(json));
        }
        return list;
    }

    public Patient getPatient(String id) {
        try {
            String json = jdbcTemplate.queryForObject("SELECT resource_json FROM fhir_resource WHERE resource_type = 'Patient' AND resource_id = ?", String.class, id);
            return (Patient) fhirParser.parseResource(json);
        } catch (Exception e) {
            return null;
        }
    }

    public List<MedicationRequest> getMedications(String patientId) {
        List<String> jsons = jdbcTemplate.queryForList("SELECT resource_json FROM fhir_resource WHERE resource_type = 'MedicationRequest' AND patient_id = ?", String.class, patientId);
        List<MedicationRequest> list = new ArrayList<>();
        for (String json : jsons) {
            list.add((MedicationRequest) fhirParser.parseResource(json));
        }
        return list;
    }
    
    public List<Condition> getConditions(String patientId) {
        List<String> jsons = jdbcTemplate.queryForList(
            "SELECT resource_json FROM fhir_resource WHERE resource_type = 'Condition' AND patient_id = ?",
            String.class, patientId
        );
        List<Condition> list = new ArrayList<>();
        for (String json : jsons) {
            list.add((Condition) fhirParser.parseResource(json));
        }
        return list;
    }
    
    public List<AllergyIntolerance> getAllergies(String patientId) {
        List<String> jsons = jdbcTemplate.queryForList(
            "SELECT resource_json FROM fhir_resource WHERE LOWER(resource_type) = 'allergyintolerance' AND (patient_id = ? OR patient_id = ?)",
            String.class, patientId, "Patient/" + patientId
        );
        List<AllergyIntolerance> list = new ArrayList<>();
        for (String json : jsons) {
            list.add((AllergyIntolerance) fhirParser.parseResource(json));
        }
        return list;
    }
    
    public List<Observation> getObservations(String patientId) {
        List<String> jsons = jdbcTemplate.queryForList(
            "SELECT resource_json FROM fhir_resource WHERE resource_type = 'Observation' AND patient_id = ?",
            String.class, patientId
        );
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
        if (r instanceof Condition c && c.hasSubject()) return c.getSubject().getReferenceElement().getIdPart();
        return null;
    }
    
    public void resetDatabase() {
        jdbcTemplate.execute("DELETE FROM fhir_resource");
        jdbcTemplate.execute("DELETE FROM sync_metadata");
    }
}