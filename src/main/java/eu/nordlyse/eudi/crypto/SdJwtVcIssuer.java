package eu.nordlyse.eudi.crypto;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private static final ObjectMapper JSON = new ObjectMapper();

    private final EudiProperties properties;
    private final IssuerKeyStore issuerKeyStore;

    public SdJwtVcIssuer(EudiProperties properties, IssuerKeyStore issuerKeyStore) {
        this.properties = properties;
        this.issuerKeyStore = issuerKeyStore;
    }

    public IssuedSdJwt issue(
            PidDocument document,
            Map<String, Object> holderPublicJwk,
            Instant issuedAt,
            Instant expiresAt,
            int statusIndex
    ) {
        try {
            List<Disclosure> disclosures = new ArrayList<>();
            List<String> sdHashes = new ArrayList<>();

            addDisclosure(disclosures, sdHashes, "family_name", document.familyName());
            addDisclosure(disclosures, sdHashes, "given_name", document.givenName());
            addDisclosure(disclosures, sdHashes, "birthdate", document.birthDate().toString());
            addDisclosure(disclosures, sdHashes, "nationalities", document.nationalities());

            Map<String, Object> placeOfBirth = nested(
                    disclosures,
                    "place_of_birth",
                    stringMembers(document.placeOfBirth().asMap())
            );
            Map<String, Object> address = null;
            if (document.residence() != null && document.residence().hasAnyValue()) {
                address = nested(disclosures, "address", stringMembers(document.residence().asSdJwtAddress()));
            }
            addIfPresent(disclosures, sdHashes, "personal_administrative_number", document.personalAdministrativeNumber());
            String picture = PortraitProcessor.toSdJwtPicture(document.portraitJpeg());
            if (picture != null) {
                addDisclosure(disclosures, sdHashes, "picture", picture);
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

            Map<String, Object> ages = null;
            if (document.ageEqualOrOver() != null && !document.ageEqualOrOver().isEmpty()) {
                Map<String, Object> ageMembers = new LinkedHashMap<>();
                document.ageEqualOrOver().entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(entry -> ageMembers.put(String.valueOf(entry.getKey()), entry.getValue()));
                ages = nested(disclosures, "age_equal_or_over", ageMembers);
            }
            if (document.ageInYears() != null) {
                addDisclosure(disclosures, sdHashes, "age_in_years", document.ageInYears());
            }
            if (document.ageBirthYear() != null) {
                addDisclosure(disclosures, sdHashes, "age_birth_year", document.ageBirthYear());
            }

            Map<String, Object> status = Map.of(
                    "status_list",
                    Map.of(
                            "idx", statusIndex,
                            "uri", properties.statusListUri()
                    )
            );

            JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                    .issuer(properties.issuer())
                    .issueTime(Date.from(issuedAt))
                    .notBeforeTime(Date.from(issuedAt))
                    .expirationTime(Date.from(expiresAt))
                    .claim("vct", properties.vct())
                    .claim("cnf", Map.of("jwk", holderPublicJwk))
                    .claim("status", status)
                    .claim("_sd_alg", "sha-256")
                    .claim("_sd", sdHashes)
                    .claim("place_of_birth", placeOfBirth);
            if (address != null) {
                builder.claim("address", address);
            }
            if (ages != null) {
                builder.claim("age_equal_or_over", ages);
            }
            JWTClaimsSet claims = builder.build();

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

            Map<String, Object> preview = reconstructPreview(claims, document, picture);
            return new IssuedSdJwt(compact.toString(), preview, disclosures);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to issue SD-JWT VC", ex);
        }
    }

    private Map<String, Object> reconstructPreview(JWTClaimsSet claims, PidDocument document, String picture) {
        Map<String, Object> preview = new LinkedHashMap<>(claims.toJSONObject());
        preview.remove("_sd");
        preview.remove("_sd_alg");
        preview.put("family_name", document.familyName());
        preview.put("given_name", document.givenName());
        preview.put("birthdate", document.birthDate().toString());
        preview.put("nationalities", document.nationalities());
        preview.put("place_of_birth", document.placeOfBirth().asMap());
        if (document.residence() != null && document.residence().hasAnyValue()) {
            preview.put("address", document.residence().asSdJwtAddress());
        }
        if (picture != null) {
            preview.put("picture", picture);
        }
        if (document.ageEqualOrOver() != null) {
            Map<String, Boolean> ages = new LinkedHashMap<>();
            document.ageEqualOrOver().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> ages.put(String.valueOf(entry.getKey()), entry.getValue()));
            preview.put("age_equal_or_over", ages);
        }
        putIfPresent(preview, "personal_administrative_number", document.personalAdministrativeNumber());
        putIfPresent(preview, "birth_family_name", document.familyNameBirth());
        putIfPresent(preview, "birth_given_name", document.givenNameBirth());
        if (document.sex() != null) {
            preview.put("sex", document.sex());
        }
        putIfPresent(preview, "email", document.emailAddress());
        putIfPresent(preview, "phone_number", document.mobilePhoneNumber());
        if (document.expiryDate() != null) {
            preview.put("date_of_expiry", document.expiryDate().toString());
        }
        if (document.issuanceDate() != null) {
            preview.put("date_of_issuance", document.issuanceDate().toString());
        }
        putIfPresent(preview, "issuing_authority", document.issuingAuthority());
        putIfPresent(preview, "issuing_country", document.issuingCountry());
        putIfPresent(preview, "document_number", document.documentNumber());
        putIfPresent(preview, "issuing_jurisdiction", document.issuingJurisdiction());
        putIfPresent(preview, "trust_anchor", document.trustAnchor());
        putIfPresent(preview, "attestation_legal_category", document.attestationLegalCategory());
        if (document.ageInYears() != null) {
            preview.put("age_in_years", document.ageInYears());
        }
        if (document.ageBirthYear() != null) {
            preview.put("age_birth_year", document.ageBirthYear());
        }
        return preview;
    }

    private static void putIfPresent(Map<String, Object> target, String key, String value) {
        if (notBlank(value)) {
            target.put(key, value);
        }
    }

    private static Map<String, Object> stringMembers(Map<String, String> source) {
        return new LinkedHashMap<>(source);
    }

    private static Map<String, Object> nested(List<Disclosure> disclosures, String parent, Map<String, Object> members) {
        List<String> hashes = new ArrayList<>();
        for (Map.Entry<String, Object> entry : members.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            Disclosure disclosure = Disclosure.of(parent + "." + entry.getKey(), entry.getKey(), entry.getValue());
            disclosures.add(disclosure);
            hashes.add(disclosure.hash());
        }
        Map<String, Object> object = new LinkedHashMap<>();
        object.put("_sd", hashes);
        return object;
    }

    private static void addIfPresent(List<Disclosure> disclosures, List<String> hashes, String claim, String value) {
        if (notBlank(value)) {
            addDisclosure(disclosures, hashes, claim, value);
        }
    }

    private static void addDisclosure(List<Disclosure> disclosures, List<String> hashes, String claim, Object value) {
        Disclosure disclosure = Disclosure.of(claim, claim, value);
        disclosures.add(disclosure);
        hashes.add(disclosure.hash());
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    public record IssuedSdJwt(String compact, Map<String, Object> payloadPreview, List<Disclosure> disclosures) {
    }

    public record Disclosure(String path, String claim, Object value, String encoded, String hash) {
        static Disclosure of(String path, String claim, Object value) {
            byte[] saltBytes = new byte[16];
            RANDOM.nextBytes(saltBytes);
            String salt = Base64URL.encode(saltBytes).toString();
            List<Object> array = List.of(salt, claim, value);
            try {
                String json = JSON.writeValueAsString(array);
                String encoded = Base64URL.encode(json.getBytes(StandardCharsets.UTF_8)).toString();
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(encoded.getBytes(StandardCharsets.US_ASCII));
                return new Disclosure(path, claim, value, encoded, Base64URL.encode(hash).toString());
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }
    }
}
