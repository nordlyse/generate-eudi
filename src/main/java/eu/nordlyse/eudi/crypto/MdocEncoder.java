package eu.nordlyse.eudi.crypto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import eu.nordlyse.eudi.config.EudiProperties;
import eu.nordlyse.eudi.domain.PidDocument;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class MdocEncoder {

    private final EudiProperties properties;
    private final ObjectMapper cborMapper = new ObjectMapper(new CBORFactory());
    private final ObjectMapper jsonMapper = new ObjectMapper();

    public MdocEncoder(EudiProperties properties) {
        this.properties = properties;
    }

    public EncodedMdoc encode(PidDocument document) {
        try {
            Map<String, Object> attributes = new LinkedHashMap<>();
            attributes.put("family_name", document.familyName());
            attributes.put("given_name", document.givenName());
            attributes.put("birth_date", document.birthDate().toString());
            attributes.put("place_of_birth", document.placeOfBirth().asMap());
            attributes.put("nationality", document.nationalities());

            if (document.residence() != null) {
                putIfPresent(attributes, "resident_address", document.residence().formatted());
                putIfPresent(attributes, "resident_country", upper(document.residence().country()));
                putIfPresent(attributes, "resident_state", document.residence().state());
                putIfPresent(attributes, "resident_city", document.residence().city());
                putIfPresent(attributes, "resident_postal_code", document.residence().postalCode());
                putIfPresent(attributes, "resident_street", document.residence().street());
                putIfPresent(attributes, "resident_house_number", document.residence().houseNumber());
            }
            putIfPresent(attributes, "personal_administrative_number", document.personalAdministrativeNumber());
            if (!document.portraitOptOut() && document.portraitDataUrl() != null && !document.portraitDataUrl().isBlank()) {
                attributes.put("portrait", document.portraitDataUrl());
            }
            putIfPresent(attributes, "family_name_birth", document.familyNameBirth());
            putIfPresent(attributes, "given_name_birth", document.givenNameBirth());
            if (document.sex() != null) {
                attributes.put("sex", document.sex());
            }
            putIfPresent(attributes, "email_address", document.emailAddress());
            putIfPresent(attributes, "mobile_phone_number", document.mobilePhoneNumber());
            if (document.expiryDate() != null) {
                attributes.put("expiry_date", document.expiryDate().toString());
            }
            putIfPresent(attributes, "issuing_authority", document.issuingAuthority());
            putIfPresent(attributes, "issuing_country", document.issuingCountry());
            putIfPresent(attributes, "document_number", document.documentNumber());
            putIfPresent(attributes, "issuing_jurisdiction", document.issuingJurisdiction());
            if (document.issuanceDate() != null) {
                attributes.put("issuance_date", document.issuanceDate().toString());
            }
            putIfPresent(attributes, "trust_anchor", document.trustAnchor());
            putIfPresent(attributes, "attestation_legal_category", document.attestationLegalCategory());
            if (document.ageEqualOrOver() != null) {
                document.ageEqualOrOver().forEach((age, value) -> attributes.put("age_over_" + age, value));
            }
            if (document.ageInYears() != null) {
                attributes.put("age_in_years", document.ageInYears());
            }
            if (document.ageBirthYear() != null) {
                attributes.put("age_birth_year", document.ageBirthYear());
            }

            Map<String, Object> nameSpaces = Map.of(properties.mdocNamespace(), attributes);
            Map<String, Object> mdoc = new LinkedHashMap<>();
            mdoc.put("docType", properties.mdocDocType());
            mdoc.put("nameSpaces", nameSpaces);
            mdoc.put("note", "Reference CBOR encoding of PID attributes in namespace "
                    + properties.mdocNamespace()
                    + ". A production ISO/IEC 18013-5 mdoc also includes an MSO/COSE_Sign1 Mobile Security Object.");

            byte[] cbor = cborMapper.writeValueAsBytes(jsonMapper.valueToTree(mdoc));
            return new EncodedMdoc(mdoc, toHex(cbor));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to encode mdoc attributes", ex);
        }
    }

    private static void putIfPresent(Map<String, Object> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }

    private static String upper(String value) {
        return value == null || value.isBlank() ? value : value.trim().toUpperCase();
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    public record EncodedMdoc(Map<String, Object> json, String cborHex) {
    }
}
