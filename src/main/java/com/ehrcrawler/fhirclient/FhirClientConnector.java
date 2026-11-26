package com.ehrcrawler.fhirclient;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.util.BundleUtil;
import com.ehrcrawler.config.AppConfig;
import com.ehrcrawler.repository.FhirLocalRepository;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FhirClientConnector {
    private static final Logger log = LoggerFactory.getLogger(FhirClientConnector.class);
    private final FhirLocalRepository repository;
    private final AppConfig appConfig;
    private final FhirContext ctx;
    private final IGenericClient fhirClient;

    public FhirClientConnector(AppConfig appConfig, FhirLocalRepository repository) {
        this.appConfig = appConfig;
        this.repository = repository;
        this.ctx = FhirContext.forR4();
        this.ctx.getRestfulClientFactory().setConnectTimeout(appConfig.getFhirConnectTimeoutMs());
        this.ctx.getRestfulClientFactory().setSocketTimeout(appConfig.getFhirTimeoutMs());
        this.fhirClient = this.ctx.newRestfulGenericClient(appConfig.getFhirBaseUrl());
    }

    public void executePreFetch() {
        log.info("Starting FHIR pre-fetch from {}", appConfig.getFhirBaseUrl());
        IParser jsonParser = ctx.newJsonParser();
        try {
            Bundle patientBundle = fhirClient.search()
                    .forResource(Patient.class)
                    .returnBundle(Bundle.class)
                    .execute();
            List<Patient> patients = BundleUtil.toListOfResourcesOfType(ctx, patientBundle, Patient.class);
            log.info("Fetched {} patients for cohort", patients.size());

            for (Patient patient : patients) {
                String patientId = patient.getIdElement().getIdPart();
                String patientJson = jsonParser.encodeResourceToString(patient);
                repository.saveResource("Patient", patientId, patientId, patientJson);

                // 1. Download Farmaci
                try {
                    Bundle medBundle = fhirClient.search()
                            .forResource(MedicationRequest.class)
                            .where(MedicationRequest.PATIENT.hasId(patientId))
                            .returnBundle(Bundle.class)
                            .execute();
                    List<MedicationRequest> meds = BundleUtil.toListOfResourcesOfType(ctx, medBundle, MedicationRequest.class);
                    for (MedicationRequest med : meds) {
                        repository.saveResource("MedicationRequest", med.getIdElement().getIdPart(), patientId, jsonParser.encodeResourceToString(med));
                    }
                } catch (Exception e) {
                    log.warn("Impossibile recuperare i farmaci per il paziente {}: {}", patientId, e.getMessage());
                }

                // 2. Download Allergie
                try {
                    Bundle allergyBundle = fhirClient.search()
                            .forResource(AllergyIntolerance.class)
                            .where(AllergyIntolerance.PATIENT.hasId(patientId))
                            .returnBundle(Bundle.class)
                            .execute();
                    List<AllergyIntolerance> allergies = BundleUtil.toListOfResourcesOfType(ctx, allergyBundle, AllergyIntolerance.class);
                    for (AllergyIntolerance allergy : allergies) {
                        repository.saveResource("AllergyIntolerance", allergy.getIdElement().getIdPart(), patientId, jsonParser.encodeResourceToString(allergy));
                    }
                } catch (Exception e) {
                    log.warn("Impossibile recuperare le allergie per il paziente {}: {}", patientId, e.getMessage());
                }

                // 3. Download Patologie (Condition)
                try {
                    Bundle condBundle = fhirClient.search()
                            .forResource(Condition.class)
                            .where(Condition.PATIENT.hasId(patientId))
                            .returnBundle(Bundle.class)
                            .execute();
                    List<Condition> conditions = BundleUtil.toListOfResourcesOfType(ctx, condBundle, Condition.class);
                    for (Condition cond : conditions) {
                        repository.saveResource("Condition", cond.getIdElement().getIdPart(), patientId, jsonParser.encodeResourceToString(cond));
                    }
                } catch (Exception e) {
                    log.warn("Impossibile recuperare le condizioni per il paziente {}: {}", patientId, e.getMessage());
                }

                // 4. Download Osservazioni (Vitals/Labs)
                try {
                    Bundle obsBundle = fhirClient.search()
                            .forResource(Observation.class)
                            .where(Observation.PATIENT.hasId(patientId))
                            .returnBundle(Bundle.class)
                            .execute();
                    List<Observation> observations = BundleUtil.toListOfResourcesOfType(ctx, obsBundle, Observation.class);
                    for (Observation obs : observations) {
                        repository.saveResource("Observation", obs.getIdElement().getIdPart(), patientId, jsonParser.encodeResourceToString(obs));
                    }
                } catch (Exception e) {
                    log.warn("Impossibile recuperare le osservazioni per il paziente {}: {}", patientId, e.getMessage());
                }
            }

            repository.updateLastSyncTimestamp();
            log.info("Sincronizzazione FHIR completata con successo");
        } catch (Exception e) {
            log.error("Errore durante la sincronizzazione FHIR: {}", e.getMessage(), e);
        }
    }
}