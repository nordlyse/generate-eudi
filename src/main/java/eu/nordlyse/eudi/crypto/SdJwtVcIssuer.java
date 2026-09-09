package eu.nordlyse.eudi.crypto;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import eu.nordlyse.eudi.config.EudiProperties;
import eu.nordlyse.eudi.domain.PidDocument;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SdJwtVcIssuer {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final JOSEObjectType VC_SD_JWT = new JOSEObjectType("vc+sd-jwt");

    private final EudiProperties properties;
    private final IssuerKeyStore issuerKeyStore;

    public SdJwtVcIssuer(EudiProperties properties, IssuerKeyStore issuerKeyStore) {
        this.properties = properties;
        this.issuerKeyStore = issuerKeyStore;
    }

    public IssuedSdJwt issue(PidDocument document, Map<String, Object> holderPublicJwk, Instant issuedAt, Instant expiresAt) {
        try {
            List<Disclosure> disclosures = new ArrayList<>();
            List<String> sdHashes = new ArrayList<>();

            addDisclosure(disclosures, sdHashes, "family_name", document.familyName());
            addDisclosure(disclosures, sdHashes, "given_name", document.givenName());
            addDisclosure(disclosures, sdHashes, "birthdate", document.birthDate().toString());
            addDisclosure(disclosures, sdHashes, "place_of_birth", document.placeOfBirth().asMap());
            addDisclosure(disclosures, sdHashes, "nationalities", document.nationalities());

            if (document.residence() != null && document.residence().hasAnyValue()) {
                addDisclosure(disclosures, sdHashes, "address", document.residence().asSdJwtAddress());
            }
            addIfPresent(disclosures, sdHashes, "personal_administrative_number", document.personalAdministrativeNumber());
            if (!document.portraitOptOut() && notBlank(document.portraitDataUrl())) {
                addDisclosure(disclosures, sdHashes, "picture", document.portraitDataUrl());
            }
            addIfPresent(disclosures, sdHashes, "birth_family_name", document.familyNameBirth());
            addIfPresent(disclosures, sdHashes, "birth_given_name", document.givenNameBirth());
            if (document.sex() != null) {
                addDisclosure(disclosures, sdHashes, "sex", document.sex());
            }
            addIfPresent(disclosures, sdHashes, "email", document.emailAddress());
            addIfPresent(disclosures, sdHashes, "phone_number", document.mobilePhoneNumber());
            if (document.expiryDate() != null) {
                addDisclosure(disclosures, sdHashes, "date_of_expiry", document.expiryDate().toString());
            }
            if (document.issuanceDate() != null) {
                addDisclosure(disclosures, sdHashes, "date_of_issuance", document.issuanceDate().toString());
            }
            addIfPresent(disclosures, sdHashes, "issuing_authority", document.issuingAuthority());
            addIfPresent(disclosures, sdHashes, "issuing_country", document.issuingCountry());
            addIfPresent(disclosures, sdHashes, "document_number", document.documentNumber());
            addIfPresent(disclosures, sdHashes, "issuing_jurisdiction", document.issuingJurisdiction());
            addIfPresent(disclosures, sdHashes, "trust_anchor", document.trustAnchor());
            addIfPresent(disclosures, sdHashes, "attestation_legal_category", document.attestationLegalCategory());
            if (document.ageEqualOrOver() != null && !document.ageEqualOrOver().isEmpty()) {
                Map<String, Boolean> ages = new LinkedHashMap<>();
                document.ageEqualOrOver().forEach((age, value) -> ages.put(String.valueOf(age), value));
                addDisclosure(disclosures, sdHashes, "age_equal_or_over", ages);
            }
            if (document.ageInYears() != null) {
                addDisclosure(disclosures, sdHashes, "age_in_years", document.ageInYears());
            }
            if (document.ageBirthYear() != null) {
                addDisclosure(disclosures, sdHashes, "age_birth_year", document.ageBirthYear());
            }

            Map<String, Object> cnf = Map.of("jwk", holderPublicJwk);

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(properties.issuer())
                    .issueTime(Date.from(issuedAt))
                    .notBeforeTime(Date.from(issuedAt))
                    .expirationTime(Date.from(expiresAt))
                    .claim("vct", properties.vct())
                    .claim("cnf", cnf)
                    .claim("_sd_alg", "sha-256")
                    .claim("_sd", sdHashes)
                    .build();

            ECKey issuerKey = issuerKeyStore.issuerKey();
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.ES256)
                            .type(VC_SD_JWT)
                            .keyID(issuerKey.getKeyID())
                            .build(),
                    claims
            );
            jwt.sign(new ECDSASigner(issuerKey));

            StringBuilder compact = new StringBuilder(jwt.serialize());
            for (Disclosure disclosure : disclosures) {
                compact.append('~').append(disclosure.encoded());
            }
            compact.append('~');

            Map<String, Object> preview = new LinkedHashMap<>(claims.toJSONObject());
            for (Disclosure disclosure : disclosures) {
                preview.put(disclosure.claim(), disclosure.value());
            }
            preview.remove("_sd");
            preview.remove("_sd_alg");

            return new IssuedSdJwt(compact.toString(), preview, disclosures);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to issue SD-JWT VC", ex);
        }
    }

    private static void addIfPresent(List<Disclosure> disclosures, List<String> hashes, String claim, String value) {
        if (notBlank(value)) {
            addDisclosure(disclosures, hashes, claim, value);
        }
    }

    private static void addDisclosure(List<Disclosure> disclosures, List<String> hashes, String claim, Object value) {
        Disclosure disclosure = Disclosure.of(claim, value);
        disclosures.add(disclosure);
        hashes.add(disclosure.hash());
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    public record IssuedSdJwt(String compact, Map<String, Object> payloadPreview, List<Disclosure> disclosures) {
    }

    public record Disclosure(String claim, Object value, String encoded, String hash) {
        static Disclosure of(String claim, Object value) {
            byte[] saltBytes = new byte[16];
            RANDOM.nextBytes(saltBytes);
            String salt = Base64URL.encode(saltBytes).toString();
            List<Object> array = List.of(salt, claim, value);
            String json = toJsonArray(array);
            String encoded = Base64URL.encode(json.getBytes(StandardCharsets.UTF_8)).toString();
            return new Disclosure(claim, value, encoded, sha256b64(encoded));
        }

        private static String sha256b64(String encodedDisclosure) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(encodedDisclosure.getBytes(StandardCharsets.US_ASCII));
                return Base64URL.encode(hash).toString();
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }

        private static String toJsonArray(List<Object> items) {
            try {
                return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(items);
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }
    }
}
