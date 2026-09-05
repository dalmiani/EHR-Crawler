package com.ehrsyncsafe.web.controller;

import com.ehrsyncsafe.repository.FhirLocalRepository;
import com.ehrsyncsafe.web.dto.MedicationDTO;
import com.ehrsyncsafe.web.dto.PatientSummaryDTO;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final FhirLocalRepository repository;

    public PatientController(FhirLocalRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<PatientSummaryDTO> getAllPatients() {
        List<Patient> patients = repository.getAllPatients();
        return patients.stream().map(this::toSummaryDTO).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public PatientSummaryDTO getPatient(@PathVariable String id) {
        List<Patient> patients = repository.getAllPatients();
        return patients.stream()
                .filter(p -> p.getIdElement().getIdPart().equals(id))
                .findFirst()
                .map(this::toSummaryDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paziente non trovato"));
    }

    @GetMapping("/{id}/medications")
    public List<MedicationDTO> getMedications(@PathVariable String id) {
        return repository.getMedications(id).stream()
                .map(m -> new MedicationDTO(
                        m.hasMedicationCodeableConcept() ? m.getMedicationCodeableConcept().getCodingFirstRep().getDisplay() : "N/D",
                        m.hasDosageInstruction() ? m.getDosageInstructionFirstRep().getText() : "N/D",
                        m.getStatus().toCode()
                )).collect(Collectors.toList());
    }

    private PatientSummaryDTO toSummaryDTO(Patient p) {
        String name = p.hasName() ? p.getNameFirstRep().getNameAsSingleString() : "Sconosciuto";
        String gender = p.hasGender() ? p.getGender().toCode() : "N/D";
        return new PatientSummaryDTO(p.getIdElement().getIdPart(), name, gender);
    }
}