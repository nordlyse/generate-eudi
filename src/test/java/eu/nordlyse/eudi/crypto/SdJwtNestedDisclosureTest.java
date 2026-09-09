package eu.nordlyse.eudi.crypto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nordlyse.eudi.config.EudiProperties;
import eu.nordlyse.eudi.domain.PidDocument;
import eu.nordlyse.eudi.domain.PlaceOfBirth;
import eu.nordlyse.eudi.domain.ResidentAddress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SdJwtNestedDisclosureTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void addressAndAgeClaimsAreNestedDisclosures(@TempDir Path temp) throws Exception {
        EudiProperties properties = properties(temp);
        SdJwtVcIssuer issuer = new SdJwtVcIssuer(properties, new IssuerKeyStore(properties));
        PidDocument document = new PidDocument(
                "Dupont",
                "Jean",
                LocalDate.of(1980, 5, 23),
                new PlaceOfBirth("FR", "Île-de-France", "Paris"),
                List.of("FR"),
                new ResidentAddress("123 Via Appia, Rome", "IT", "Lazio", "Rome", "00100", "Via Appia", "123"),
                "FR-1",
                new byte[0],
                true,
                "Dupont",
                "Jean",
                1,
                "jean.dupont@example.eu",
                "+33123456789",
                LocalDate.of(2036, 1, 1),
                "ANTS",
                "FR",
                "EUDI-FR-1",
                "FR-IDF",
                LocalDate.of(2026, 1, 1),
                Map.of(16, true, 18, true, 65, false),
                46,
                1980,
                properties.trustAnchor(),
                "PID"
        );

        SdJwtVcIssuer.IssuedSdJwt issued = issuer.issue(
                document,
                Map.of("kty", "EC", "crv", "P-256", "x", "x", "y", "y"),
                Instant.parse("2026-09-10T00:00:00Z"),
                Instant.parse("2026-09-24T00:00:00Z"),
                0
        );

        JsonNode payload = payload(issued.compact());
        assertThat(payload.get("address").has("street_address")).isFalse();
        assertThat(payload.get("address").get("_sd").isArray()).isTrue();
        assertThat(payload.get("address").toString()).doesNotContain("Via Appia");
        assertThat(payload.get("age_equal_or_over").has("18")).isFalse();
        assertThat(payload.get("age_equal_or_over").get("_sd").isArray()).isTrue();
        assertThat(payload.get("place_of_birth").get("_sd").isArray()).isTrue();
        assertThat(payload.get("status").get("status_list").get("idx").asInt()).isEqualTo(0);
        assertThat(payload.get("status").get("status_list").get("uri").asText()).isEqualTo(properties.statusListUri());

        List<String> paths = issued.disclosures().stream().map(SdJwtVcIssuer.Disclosure::path).toList();
        assertThat(paths)
                .contains("address.street_address", "address.house_number", "age_equal_or_over.18", "age_equal_or_over.65")
                .doesNotContain("address", "age_equal_or_over");
        assertThat(issued.disclosures())
                .anyMatch(item -> "address.street_address".equals(item.path()) && "Via Appia".equals(item.value()))
                .anyMatch(item -> "age_equal_or_over.18".equals(item.path()) && Boolean.TRUE.equals(item.value()));
        assertThat(payload.toString()).doesNotContain("Via Appia");
    }

    private static JsonNode payload(String sdJwt) throws Exception {
        String jwt = sdJwt.split("~", 2)[0];
        String encoded = jwt.split("\\.")[1];
        byte[] json = Base64.getUrlDecoder().decode(encoded);
        return JSON.readTree(json);
    }

    private static EudiProperties properties(Path temp) {
        return new EudiProperties(
                "https://pid.generate-eudi.example",
                "urn:eudi:pid:1",
                "eu.europa.ec.eudi.pid.1",
                "eu.europa.ec.eudi.pid.1",
                "https://pid.generate-eudi.example/trust-anchors",
                14,
                temp.resolve("issuer.jwk.json").toString(),
                temp.toString(),
                "https://pid.generate-eudi.example/statuslists/pid",
                "https://pid.generate-eudi.example/catalog/vct?id=urn:eudi:pid:1",
                new EudiProperties.Officer("officer", "secret")
        );
    }
}
