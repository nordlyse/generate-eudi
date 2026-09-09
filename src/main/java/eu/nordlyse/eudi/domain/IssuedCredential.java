package eu.nordlyse.eudi.domain;

import java.time.Instant;
import java.util.Map;

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
        Map<String, Object> issuerPublicJwk
) {
}
