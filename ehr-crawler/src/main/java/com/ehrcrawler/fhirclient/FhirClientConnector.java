package com.ehrsyncsafe.fhirclient;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.ehrsyncsafe.config.*;
import com.ehrsyncsafe.repository.*;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class FhirClientConnector {

    private static final Logger log = LoggerFactory.getLogger(FhirClientConnector.class);
    private final AppConfig appConfig;
    private final FhirLocalRepository repository;
    private final IGenericClient fhirClient;

    public FhirClientConnector(AppConfig appConfig, FhirLocalRepository repository) {
        this.appConfig = appConfig;
        this.repository = repository;
        FhirContext ctx = FhirContext.forR4();
        this.fhirClient = ctx.newRestfulGenericClient(appConfig.getFhirBaseUrl());
        this.fhirClient.setSocketTimeout(appConfig.getFhirTimeoutMs());
        this.fhirClient.setConnectTimeout(appConfig.getFhirConnectTimeoutMs());
    }

    public void executePreFetch() {
        log.info("Starting FHIR pre-fetch from {}", appConfig.getFhirBaseUrl());
        List<Patient> patients = fetchPatientCohort();
        log.info("Fetched {} patients for cohort", patients.size());

        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Patient patient : patients) {
            repository.saveResource(patient);
            futures.add(CompletableFuture.runAsync(() -> fetchPatientResources(patient.getIdElement().getIdPart()), executor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();
        repository.updateLastSyncTimestamp();
    }

    private List<Patient> fetchPatientCohort() {
        try {
            Bundle bundle = fhirClient.search()
                    .forResource(Patient.class)
                    .returnBundle(Bundle.class)
                    .execute();
            List<Patient> patients = new ArrayList<>();
            bundle.getEntry().forEach(entry -> {
                if (entry.getResource() instanceof Patient p) {
                    patients.add(p);
                }
            });
            return patients;
        } catch (Exception e) {
            log.error("Failed to fetch patient cohort from remote EHR", e);
            return new ArrayList<>();
        }
    }

    private void fetchPatientResources(String patientId) {
        fetchResourceForPatient(AllergyIntolerance.class, patientId);
        fetchResourceForPatient(MedicationRequest.class, patientId);
        fetchResourceForPatient(Observation.class, patientId);
        fetchResourceForPatient(Condition.class, patientId);
    }

    private <T extends Resource> void fetchResourceForPatient(Class<T> resourceClass, String patientId) {
        try {
            Bundle bundle = fhirClient.search()
                    .forResource(resourceClass)
                    .where(new ca.uhn.fhir.rest.gclient.ReferenceClientParam("patient").hasId(patientId))
                    .returnBundle(Bundle.class)
                    .execute();
            bundle.getEntry().forEach(entry -> repository.saveResource(entry.getResource()));
        } catch (Exception e) {
            log.error("Error fetching {} for patient {}: {}", resourceClass.getSimpleName(), patientId, e.getMessage());
        }
    }
}