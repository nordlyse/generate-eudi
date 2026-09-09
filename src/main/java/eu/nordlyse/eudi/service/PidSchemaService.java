package eu.nordlyse.eudi.service;

import eu.nordlyse.eudi.web.dto.SchemaField;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PidSchemaService {

    public List<SchemaField> fields() {
        return List.of(
                field("family_name", "family_name", "family_name", "mandatory", "string",
                        "Current last name(s) or surname(s) of the user.", "CIR 2024/2977 Table 1"),
                field("given_name", "given_name", "given_name", "mandatory", "string",
                        "Current first name(s), including middle name(s) where applicable.", "CIR 2024/2977 Table 1"),
                new SchemaField("birth_date", "birth_date", "birthdate", "birth_date", "mandatory", "full-date",
                        "Day, month and year of birth (ISO 8601 YYYY-MM-DD).", "CIR 2024/2977 Table 1"),
                new SchemaField("birth_place", "birth_place", "place_of_birth.{country,region,locality}",
                        "place_of_birth", "mandatory", "object",
                        "Country (ISO 3166-1 alpha-2) and/or region and/or locality of birth. At least one member SHALL be present.",
                        "CIR 2024/2977 Table 1 / PID Rulebook 3.1.4"),
                new SchemaField("nationality", "nationality", "nationalities", "nationality", "mandatory", "string[]",
                        "One or more ISO 3166-1 alpha-2 nationality codes. QU = unknown, QS = no nationality.",
                        "CIR 2024/2977 Table 1"),
                field("portrait", "picture", "portrait", "mandatory-with-opt-out", "image/jpeg",
                        "Facial image (ISO/IEC 39794-5 or 19794-5). The user may opt out; then the attribute is empty.",
                        "CIR 2024/2977 (amended)"),
                field("resident_address", "address.formatted", "resident_address", "optional", "string",
                        "Full current residence or contact address.", "CIR 2024/2977 Table 2"),
                field("resident_country", "address.country", "resident_country", "optional", "string",
                        "ISO 3166-1 alpha-2 country of residence.", "CIR 2024/2977 Table 2"),
                field("resident_state", "address.region", "resident_state", "optional", "string",
                        "State, province, district or local area of residence.", "CIR 2024/2977 Table 2"),
                field("resident_city", "address.locality", "resident_city", "optional", "string",
                        "Municipality, city, town or village of residence.", "CIR 2024/2977 Table 2"),
                field("resident_postal_code", "address.postal_code", "resident_postal_code", "optional", "string",
                        "Postal code of residence.", "CIR 2024/2977 Table 2"),
                field("resident_street", "address.street_address", "resident_street", "optional", "string",
                        "Street name of residence.", "CIR 2024/2977 Table 2"),
                field("resident_house_number", "address.house_number", "resident_house_number", "optional", "string",
                        "House number including any affix or suffix.", "CIR 2024/2977 Table 2"),
                field("personal_administrative_number", "personal_administrative_number",
                        "personal_administrative_number", "optional", "string",
                        "Unique administrative number assigned by the PID provider.", "CIR 2024/2977 Table 2"),
                field("family_name_birth", "birth_family_name", "family_name_birth", "optional", "string",
                        "Last name(s) at the time of birth.", "CIR 2024/2977 Table 2"),
                field("given_name_birth", "birth_given_name", "given_name_birth", "optional", "string",
                        "First name(s) at the time of birth.", "CIR 2024/2977 Table 2"),
                field("sex", "sex", "sex", "optional", "uint",
                        "0=not known, 1=male, 2=female, 3=other, 4=inter, 5=diverse, 6=open, 9=not applicable (ISO/IEC 5218 for 0,1,2,9).",
                        "CIR 2024/2977 Table 2"),
                field("email_address", "email", "email_address", "optional", "string",
                        "Electronic mail address (RFC 5322).", "CIR 2024/2977 Table 2"),
                field("mobile_phone_number", "phone_number", "mobile_phone_number", "optional", "string",
                        "Mobile number starting with '+' and country code, digits only afterwards.", "CIR 2024/2977 Table 2"),
                field("issuing_authority", "issuing_authority", "issuing_authority", "mandatory", "string",
                        "Administrative authority that issued the PID, or ISO 3166-1 alpha-2 country code.",
                        "CIR 2024/2977 Table 5"),
                field("issuing_country", "issuing_country", "issuing_country", "mandatory", "string",
                        "ISO 3166-1 alpha-2 country of the PID provider.", "CIR 2024/2977 Table 5"),
                field("expiry_date", "date_of_expiry", "expiry_date", "optional", "full-date",
                        "Administrative validity end date of the logical PID.", "CIR 2024/2977 / PID Rulebook"),
                field("issuance_date", "date_of_issuance", "issuance_date", "optional", "full-date",
                        "Administrative validity start date of the logical PID.", "CIR 2024/2977 / PID Rulebook"),
                field("document_number", "document_number", "document_number", "optional", "string",
                        "Number assigned to the person identification data by the PID provider.", "CIR 2024/2977"),
                field("issuing_jurisdiction", "issuing_jurisdiction", "issuing_jurisdiction", "optional", "string",
                        "ISO 3166-2 country subdivision code. The country part SHALL match issuing_country.",
                        "CIR 2024/2977"),
                field("location_status", "status.status_list", "(MSO revocation in mdoc)", "optional", "status-list",
                        "Validity status is a Token Status List (idx + uri) in the SD-JWT, not a PID attribute. Published at /statuslists/pid.",
                        "CIR 2024/2977 / IETF Token Status List / PID Rulebook"),
                field("trust_anchor", "trust_anchor", "trust_anchor", "optional", "uri",
                        "URL of a machine-readable trust anchor used to verify the PID.", "PID Rulebook additional"),
                field("attestation_legal_category", "attestation_legal_category", "attestation_legal_category",
                        "optional", "string",
                        "Indicates that the attestation has been issued as a PID.", "PID Rulebook additional"),
                field("age_over_18", "age_equal_or_over.18", "age_over_18", "optional", "boolean",
                        "Whether the user is at least 18 years old.", "ARF PID Rulebook / ISO/IEC 18013-5 7.2.5"),
                field("age_over_NN", "age_equal_or_over.NN", "age_over_NN", "optional", "boolean",
                        "Whether the user is at least NN years old. Multiple distinct NN values may be present.",
                        "ARF PID Rulebook / ISO/IEC 18013-5 7.2.5"),
                field("age_in_years", "age_in_years", "age_in_years", "optional", "uint",
                        "Current age of the user in years.", "ARF PID Rulebook"),
                field("age_birth_year", "age_birth_year", "age_birth_year", "optional", "uint",
                        "Year when the user was born.", "ARF PID Rulebook")
        );
    }

    private static SchemaField field(
            String id,
            String sdJwt,
            String mdoc,
            String presence,
            String type,
            String description,
            String source
    ) {
        return new SchemaField(id, id, sdJwt, mdoc, presence, type, description, source);
    }
}
