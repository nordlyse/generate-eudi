package eu.nordlyse.eudi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "eudi")
public record EudiProperties(
        String issuer,
        String vct,
        String mdocDocType,
        String mdocNamespace,
        String trustAnchor,
        int technicalValidityDays,
        String issuerKeyFile,
        Officer officer
) {
    public record Officer(String username, String password) {
    }
}
