package eu.nordlyse.eudi.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PidIssueResponse(
        String id,
        String documentNumber,
        Instant issuedAt,
        Instant technicalExpiresAt,
        LocalDate administrativeExpiryDate,
        String vct,
        String mdocDocType,
        String sdJwtVc,
        Map<String, Object> sdJwtPayloadPreview,
        List<DisclosureView> disclosures,
        Map<String, Object> mdoc,
        String mdocCborHex,
        Map<String, Object> holderPublicJwk,
        String encryptedHolderKey,
        String holderKeyRecoverySecret,
        String holderKeyWarning,
        Map<String, Object> issuerPublicJwk,
        String givenName,
        String familyName,
        Integer statusIndex,
        String statusListUri,
        String typeMetadataUri,
        Boolean revoked,
        String issuedBy
) {
    public record DisclosureView(String path, String claim, String disclosure) {
    }
}
