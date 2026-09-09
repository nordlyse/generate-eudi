package eu.nordlyse.eudi.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Canonical Person Identification Data using CIR 2024/2977 identifiers,
 * plus optional ARF PID Rulebook attributes.
 */
public record PidDocument(
        String familyName,
        String givenName,
        LocalDate birthDate,
        PlaceOfBirth placeOfBirth,
        List<String> nationalities,
        ResidentAddress residence,
        String personalAdministrativeNumber,
        String portraitDataUrl,
        boolean portraitOptOut,
        String familyNameBirth,
        String givenNameBirth,
        Integer sex,
        String emailAddress,
        String mobilePhoneNumber,
        LocalDate expiryDate,
        String issuingAuthority,
        String issuingCountry,
        String documentNumber,
        String issuingJurisdiction,
        String locationStatus,
        LocalDate issuanceDate,
        Map<Integer, Boolean> ageEqualOrOver,
        Integer ageInYears,
        Integer ageBirthYear,
        String trustAnchor,
        String attestationLegalCategory
) {
}
