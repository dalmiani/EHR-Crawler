package com.ehrcrawler.web.controller;

import com.ehrcrawler.repository.FhirLocalRepository;
import com.ehrcrawler.service.AuditService;
import com.ehrcrawler.web.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final FhirLocalRepository repository;
    private final AuditService auditService;

    public PatientController(FhirLocalRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @GetMapping
    public List<PatientSummaryDTO> getAllPatients(HttpServletRequest request) {
        List<PatientSummaryDTO> patients = repository.getAllPatients().stream()
                .map(p -> new PatientSummaryDTO(
                        p.getIdElement().getIdPart(),
                        p.hasName() ? p.getNameFirstRep().getNameAsSingleString() : "N/D",
                        p.hasGender() ? p.getGender().toCode() : "N/D",
                        p.hasBirthDate() ? p.getBirthDateElement().getValueAsString() : "N/D"
                ))
                .sorted(Comparator.comparing(PatientSummaryDTO::name, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());

        auditService.logAccess("READ_ALL_PATIENTS", "Patient", null, null, request, "SUCCESS", "Cohort view loaded");
        return patients;
    }

    @GetMapping("/{id}")
    public ResponseEntity<PatientSummaryDTO> getPatient(@PathVariable String id, HttpServletRequest request) {
        Patient p = repository.getPatient(id);
        if (p == null) {
            auditService.logAccess("READ_PATIENT", "Patient", id, id, request, "FAILURE", "Patient record not found");
            return ResponseEntity.notFound().build();
        }
        PatientSummaryDTO dto = new PatientSummaryDTO(
                p.getIdElement().getIdPart(),
                p.hasName() ? p.getNameFirstRep().getNameAsSingleString() : "N/D",
                p.hasGender() ? p.getGender().toCode() : "N/D",
                p.hasBirthDate() ? p.getBirthDateElement().getValueAsString() : "N/D"
        );
        auditService.logAccess("READ_PATIENT", "Patient", id, id, request, "SUCCESS", "Demographics accessed");
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}/medications")
    public List<MedicationDTO> getMedications(@PathVariable String id, HttpServletRequest request) {
        auditService.logAccess("READ_MEDICATIONS", "MedicationRequest", null, id, request, "SUCCESS", "Medication records accessed");
        return repository.getMedications(id).stream()
                .map(m -> new MedicationDTO(
                        m.hasMedicationCodeableConcept() ? m.getMedicationCodeableConcept().getCodingFirstRep().getDisplay() : "N/D",
                        m.hasDosageInstruction() ? m.getDosageInstructionFirstRep().getText() : "N/D",
                        m.hasStatus() ? m.getStatus().toCode() : "N/D"
                ))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}/allergies")
    public List<AllergyDTO> getAllergies(@PathVariable String id, HttpServletRequest request) {
        auditService.logAccess("READ_ALLERGIES", "AllergyIntolerance", null, id, request, "SUCCESS", "Allergy records accessed");
        return repository.getAllergies(id).stream()
                .map(a -> {
                    String substance = a.hasCode() && a.getCode().hasText() ? a.getCode().getText()
                            : (a.hasCode() && a.getCode().hasCoding() ? a.getCode().getCodingFirstRep().getDisplay() : "N/D");
                    String status = a.hasClinicalStatus() && a.getClinicalStatus().hasCoding()
                            ? a.getClinicalStatus().getCodingFirstRep().getCode() : "active";
                    return new AllergyDTO(substance, status);
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}/conditions")
    public List<ConditionDTO> getConditions(@PathVariable String id, HttpServletRequest request) {
        auditService.logAccess("READ_CONDITIONS", "Condition", null, id, request, "SUCCESS", "Active problems accessed");
        return repository.getConditions(id).stream()
                .map(c -> new ConditionDTO(
                        c.hasCode() ? c.getCode().getCodingFirstRep().getDisplay() : "N/D",
                        c.hasClinicalStatus() ? c.getClinicalStatus().getCodingFirstRep().getCode() : "N/D"
                ))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}/vitals")
    public List<ObservationDTO> getVitalSigns(@PathVariable String id, HttpServletRequest request) {
        auditService.logAccess("READ_VITALS", "Observation", null, id, request, "SUCCESS", "Vital signs accessed");
        return repository.getObservations(id).stream()
                .filter(o -> !o.hasCategory() || o.getCategoryFirstRep().getCodingFirstRep().getCode().equalsIgnoreCase("vital-signs"))
                .map(this::mapObservationToDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}/labs")
    public List<ObservationDTO> getLabResults(@PathVariable String id, HttpServletRequest request) {
        auditService.logAccess("READ_LABS", "Observation", null, id, request, "SUCCESS", "Lab results accessed");
        return repository.getObservations(id).stream()
                .filter(o -> o.hasCategory() && o.getCategoryFirstRep().getCodingFirstRep().getCode().equalsIgnoreCase("laboratory"))
                .map(this::mapObservationToDTO)
                .collect(Collectors.toList());
    }

    private ObservationDTO mapObservationToDTO(Observation o) {
        String code = o.hasCode() ? o.getCode().getCodingFirstRep().getDisplay() : "N/D";
        String value = o.hasValueQuantity() ? String.valueOf(o.getValueQuantity().getValue())
                : (o.hasValueCodeableConcept() ? o.getValueCodeableConcept().getCodingFirstRep().getDisplay() : "N/D");
        String unit = o.hasValueQuantity() && o.getValueQuantity().getUnit() != null ? o.getValueQuantity().getUnit() : "";
        String date = o.hasEffectiveDateTimeType() ? o.getEffectiveDateTimeType().getValueAsString() : "N/D";
        String category = o.hasCategory() ? o.getCategoryFirstRep().getCodingFirstRep().getCode() : "general";
        return new ObservationDTO(code, value, unit, date, category);
    }
}