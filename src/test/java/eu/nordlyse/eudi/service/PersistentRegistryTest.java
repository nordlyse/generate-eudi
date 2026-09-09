package eu.nordlyse.eudi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import eu.nordlyse.eudi.config.EudiProperties;
import eu.nordlyse.eudi.domain.IssuedCredential;
import eu.nordlyse.eudi.domain.PidDocument;
import eu.nordlyse.eudi.domain.PlaceOfBirth;
import eu.nordlyse.eudi.domain.ResidentAddress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PersistentRegistryTest {

    @Test
    void credentialsAndAuditSurviveReload(@TempDir Path temp) {
        EudiProperties properties = properties(temp);
        ObjectMapper mapper = mapper();

        CredentialRegistry first = new CredentialRegistry(properties, mapper);
        AuditLogService audit = new AuditLogService(properties, mapper);
        IssuedCredential credential = sample(temp);
        first.save(credential);
        audit.record("officer", "ISSUE", credential.id(), credential.document().documentNumber(), "test");

        CredentialRegistry reloaded = new CredentialRegistry(properties, mapper);
        AuditLogService auditReloaded = new AuditLogService(properties, mapper);

        IssuedCredential loaded = reloaded.find(credential.id()).orElseThrow();
        assertThat(loaded.document().familyName()).isEqualTo("Dupont");
        assertThat(loaded.statusIndex()).isEqualTo(0);
        assertThat(loaded.issuedBy()).isEqualTo("officer");
        assertThat(auditReloaded.list())
                .anyMatch(event -> "ISSUE".equals(event.action())
                        && "officer".equals(event.officer())
                        && credential.document().documentNumber().equals(event.documentNumber()));
    }

    private static IssuedCredential sample(Path temp) {
        PidDocument document = new PidDocument(
                "Dupont",
                "Jean",
                LocalDate.of(1980, 5, 23),
                new PlaceOfBirth("FR", null, "Paris"),
                List.of("FR"),
                new ResidentAddress(null, "IT", null, "Rome", null, "Via Appia", "123"),
                null,
                new byte[0],
                true,
                null,
                null,
                1,
                null,
                null,
                LocalDate.of(2036, 1, 1),
                "ANTS",
                "FR",
                "EUDI-FR-42",
                "FR-IDF",
                LocalDate.of(2026, 1, 1),
                Map.of(18, true),
                46,
                1980,
                "https://example/trust",
                "PID"
        );
        return new IssuedCredential(
                "cred-1",
                Instant.parse("2026-09-10T00:00:00Z"),
                Instant.parse("2026-09-24T00:00:00Z"),
                document,
                "sdjwt",
                Map.of("vct", "urn:eudi:pid:1"),
                Map.of("docType", "eu.europa.ec.eudi.pid.1"),
                "00",
                Map.of("kty", "EC"),
                "jwe",
                Map.of("kty", "EC"),
                0,
                false,
                "officer"
        );
    }

    private static ObjectMapper mapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private static EudiProperties properties(Path temp) {
        return new EudiProperties(
                "https://pid.generate-eudi.example",
                "urn:eudi:pid:1",
                "eu.europa.ec.eudi.pid.1",
                "eu.europa.ec.eudi.pid.1",
                "https://example/trust",
                14,
                temp.resolve("issuer.jwk.json").toString(),
                temp.toString(),
                "https://pid.generate-eudi.example/statuslists/pid",
                "https://pid.generate-eudi.example/catalog/vct?id=urn:eudi:pid:1",
                new EudiProperties.Officer("officer", "secret")
        );
    }
}
