package eu.nordlyse.eudi.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IssuedCredential(
        String id,
        Instant issuedAt,
        Instant technicalExpiresAt,
        PidDocument document,
        String sdJwtVc,
        Map<String, Object> sdJwtPayloadPreview,
        Map<String, Object> mdoc,
        String mdocCborHex,
        Map<String, Object> holderPublicJwk,
        String encryptedHolderKey,
        Map<String, Object> issuerPublicJwk,
        int statusIndex,
        boolean revoked,
        String issuedBy
) {
    public IssuedCredential revoke() {
        return new IssuedCredential(
                id,
                issuedAt,
                technicalExpiresAt,
                document,
                sdJwtVc,
                sdJwtPayloadPreview,
                mdoc,
                mdocCborHex,
                holderPublicJwk,
                encryptedHolderKey,
                issuerPublicJwk,
                statusIndex,
                true,
                issuedBy
        );
    }
}
