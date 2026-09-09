package eu.nordlyse.eudi.web.dto;

public record SchemaField(
        String id,
        String cirIdentifier,
        String sdJwtClaim,
        String mdocAttribute,
        String presence,
        String type,
        String description,
        String source
) {
}
