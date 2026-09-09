package eu.nordlyse.eudi.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AuditEvent(
        Instant at,
        String officer,
        String action,
        String credentialId,
        String documentNumber,
        String detail
) {
}
