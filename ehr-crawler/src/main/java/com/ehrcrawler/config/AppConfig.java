package com.ehrsyncsafe.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Value("${fhir.base-url}")
    private String fhirBaseUrl;

    @Value("${fhir.timeout-ms:30000}")
    private int fhirTimeoutMs;

    @Value("${fhir.connect-timeout-ms:10000}")
    private int fhirConnectTimeoutMs;

    @Value("${fhir.auth-type:none}")
    private String fhirAuthType;

    public String getFhirBaseUrl() { return fhirBaseUrl; }
    public int getFhirTimeoutMs() { return fhirTimeoutMs; }
    public int getFhirConnectTimeoutMs() { return fhirConnectTimeoutMs; }
    public String getFhirAuthType() { return fhirAuthType; }
}